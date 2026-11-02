package service;

import dal.BillingDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Contract;
import model.Invoice;
import model.MeterReading;
import model.MeterRow;
import model.UtilityPrice;

/**
 *
 * @author Lenovo
 */
public class BillingService {
    // Theo CK_MeterReading_period và CK_Invoice_period trong database/setup.sql.
    public static final int MIN_YEAR = 2020;
    public static final int MAX_YEAR = 2100;
    private static final String INDEX_PATTERN = "[0-9]{1,9}";

    public MeterSheet loadSheet(YearMonth period) throws SQLException, BillingException {
        requireValidPeriod(period);
        try (BillingDAO dao = new BillingDAO()) {
            requireConnection(dao);
            List<MeterRow> rows = dao.findMeterRows(period);
            rows.forEach(row -> applyOldIndexes(row, period));
            return new MeterSheet(period, rows, dao.findPriceOn(period.atDay(1)), dao.findInvoicesByPeriod(period));
        }
    }

    /** Ghi chỉ số mới cho các phòng được nhập; phòng để trống cả hai ô thì bỏ qua. */
    public int saveReadings(YearMonth period, List<ReadingInput> inputs) throws SQLException, BillingException {
        requireValidPeriod(period);
        try (BillingDAO dao = new BillingDAO()) {
            Connection connection = requireConnection(dao);
            Map<Integer, MeterRow> rowsByRoom = new LinkedHashMap<>();
            for (MeterRow row : dao.findMeterRows(period)) {
                applyOldIndexes(row, period);
                rowsByRoom.put(row.getContract().getRoomId(), row);
            }
            List<String> errors = new ArrayList<>();
            List<MeterReading> readings = new ArrayList<>();
            for (ReadingInput input : inputs) {
                MeterRow row = rowsByRoom.get(input.roomId());
                if (row == null) {
                    errors.add("Phòng ID " + input.roomId() + " không có hợp đồng hiệu lực trong kỳ này.");
                    continue;
                }
                if (input.electric().isEmpty() && input.water().isEmpty()) {
                    continue;
                }
                String room = "Phòng " + row.getContract().getRoomCode() + ": ";
                if (row.isRecorded()) {
                    errors.add(room + "đã ghi chỉ số tháng " + format(period) + ", mỗi phòng chỉ ghi một lần mỗi tháng.");
                    continue;
                }
                if (!row.isOldIndexKnown()) {
                    errors.add(room + "chưa có chỉ số tháng " + format(period.minusMonths(1))
                            + ", hãy ghi bù tháng đó trước.");
                    continue;
                }
                Integer electric = newIndex(input.electric(), row.getOldElectric(), room + "chỉ số điện", errors);
                Integer water = newIndex(input.water(), row.getOldWater(), room + "chỉ số nước", errors);
                if (electric != null && water != null) {
                    MeterReading reading = new MeterReading();
                    reading.setRoomId(input.roomId());
                    reading.setYear(period.getYear());
                    reading.setMonth(period.getMonthValue());
                    reading.setElectricIndex(electric);
                    reading.setWaterIndex(water);
                    readings.add(reading);
                }
            }
            if (errors.isEmpty() && readings.isEmpty()) {
                errors.add("Chưa nhập chỉ số mới cho phòng nào.");
            }
            if (!errors.isEmpty()) {
                throw new BillingException(errors);
            }
            connection.setAutoCommit(false);
            try {
                for (MeterReading reading : readings) {
                    dao.insertReading(reading);
                }
                connection.commit();
            } catch (SQLException ex) {
                rollback(connection, ex);
                if (isDuplicateKey(ex)) {
                    throw new BillingException(List.of("Chỉ số tháng " + format(period)
                            + " vừa được ghi bởi thao tác khác. Hãy tải lại trang và kiểm tra."), true);
                }
                throw ex;
            }
            return readings.size();
        }
    }

    /**
     * Lập hoá đơn cho mọi hợp đồng ACTIVE đã có đủ chỉ số của kỳ và chưa có hoá đơn kỳ đó.
     * Bấm lại cho cùng tháng chỉ bỏ qua hợp đồng đã có hoá đơn; UQ_Invoice_ContractYearMonth
     * chặn thêm khi hai yêu cầu chạy đồng thời.
     */
    public GenerationResult generateInvoices(YearMonth period) throws SQLException, BillingException {
        requireValidPeriod(period);
        try (BillingDAO dao = new BillingDAO()) {
            Connection connection = requireConnection(dao);
            UtilityPrice price = dao.findPriceOn(period.atDay(1));
            if (price == null) {
                throw new BillingException(List.of("Chưa có bảng giá điện nước hiệu lực vào ngày 01/"
                        + format(period) + "."));
            }
            List<Invoice> invoices = new ArrayList<>();
            List<String> missingRooms = new ArrayList<>();
            int alreadyInvoiced = 0;
            for (MeterRow row : dao.findMeterRows(period)) {
                applyOldIndexes(row, period);
                if (row.isInvoiced()) {
                    alreadyInvoiced++;
                } else if (row.isRecorded() && row.isOldIndexKnown()) {
                    invoices.add(calculateInvoice(row, price, period));
                } else {
                    missingRooms.add(row.getContract().getRoomCode());
                }
            }
            connection.setAutoCommit(false);
            try {
                for (Invoice invoice : invoices) {
                    dao.insertInvoice(invoice);
                }
                connection.commit();
            } catch (SQLException ex) {
                rollback(connection, ex);
                if (isDuplicateKey(ex)) {
                    throw new BillingException(List.of("Hoá đơn tháng " + format(period)
                            + " vừa được lập bởi thao tác khác. Hãy tải lại trang và kiểm tra."), true);
                }
                throw ex;
            }
            return new GenerationResult(invoices.size(), alreadyInvoiced, missingRooms);
        }
    }

    /**
     * Công thức NT-17: tổng = tiền phòng + (điện mới − cũ) × giá điện
     * + (nước mới − cũ) × giá nước + phí dịch vụ × số người. Hạn trả ngày 10 tháng sau.
     */
    public Invoice calculateInvoice(MeterRow row, UtilityPrice price, YearMonth period) {
        Contract contract = row.getContract();
        int electricUsed = row.getElectricUsed();
        int waterUsed = row.getWaterUsed();
        BigDecimal electricFee = price.getElectricPrice().multiply(BigDecimal.valueOf(electricUsed));
        BigDecimal waterFee = price.getWaterPrice().multiply(BigDecimal.valueOf(waterUsed));
        BigDecimal serviceFee = price.getServiceFeePerPerson().multiply(BigDecimal.valueOf(contract.getNumPeople()));
        BigDecimal roomFee = contract.getMonthlyRent();

        Invoice invoice = new Invoice();
        invoice.setContractId(contract.getContractId());
        invoice.setPriceId(price.getPriceId());
        invoice.setYear(period.getYear());
        invoice.setMonth(period.getMonthValue());
        invoice.setRoomFee(roomFee);
        invoice.setElectricUsed(electricUsed);
        invoice.setElectricFee(electricFee);
        invoice.setWaterUsed(waterUsed);
        invoice.setWaterFee(waterFee);
        invoice.setServiceFee(serviceFee);
        invoice.setTotal(roomFee.add(electricFee).add(waterFee).add(serviceFee));
        invoice.setStatus(Invoice.STATUS_UNPAID);
        invoice.setDueDate(period.plusMonths(1).atDay(10));
        return invoice;
    }

    public void requireValidPeriod(YearMonth period) throws BillingException {
        if (period == null || period.getYear() < MIN_YEAR || period.getYear() > MAX_YEAR) {
            throw new BillingException(List.of("Kỳ ghi số phải nằm trong các năm " + MIN_YEAR + "–" + MAX_YEAR + "."));
        }
        // Chỉ số là số cuối tháng nên không ghi hay lập hoá đơn cho tháng chưa tới.
        if (period.isAfter(YearMonth.now())) {
            throw new BillingException(List.of("Không thể ghi chỉ số hoặc lập hoá đơn cho tháng "
                    + format(period) + " vì kỳ này chưa tới."));
        }
    }

    /**
     * Chỉ số cũ = chỉ số tháng trước của phòng; tháng đầu của hợp đồng thì lấy chỉ số lúc
     * nhận phòng, vì chỉ số tháng trước (nếu có) thuộc người thuê cũ.
     */
    private void applyOldIndexes(MeterRow row, YearMonth period) {
        Contract contract = row.getContract();
        if (!YearMonth.from(contract.getStartDate()).isBefore(period)) {
            row.setOldIndexes(contract.getStartElectricIndex(), contract.getStartWaterIndex());
        } else if (row.getPrevious() != null) {
            row.setOldIndexes(row.getPrevious().getElectricIndex(), row.getPrevious().getWaterIndex());
        }
    }

    private Integer newIndex(String value, int oldIndex, String label, List<String> errors) {
        if (value.isEmpty()) {
            errors.add(label + " mới chưa được nhập.");
            return null;
        }
        if (!value.matches(INDEX_PATTERN)) {
            errors.add(label + " mới phải là số nguyên từ 0 đến 999.999.999.");
            return null;
        }
        int index = Integer.parseInt(value);
        if (index < oldIndex) {
            errors.add(label + " mới (" + index + ") không được nhỏ hơn chỉ số cũ (" + oldIndex + ").");
            return null;
        }
        return index;
    }

    private static String format(YearMonth period) {
        return String.format("%02d/%d", period.getMonthValue(), period.getYear());
    }

    private boolean isDuplicateKey(SQLException ex) {
        return ex.getErrorCode() == 2627 || ex.getErrorCode() == 2601;
    }

    private void rollback(Connection connection, SQLException ex) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            ex.addSuppressed(rollbackError);
        }
    }

    private Connection requireConnection(BillingDAO dao) throws SQLException {
        if (dao.getConnection() == null) {
            throw new SQLException("Database connection is unavailable.");
        }
        return dao.getConnection();
    }

    /** Ô nhập chỉ số của một phòng, giữ nguyên chuỗi đã trim để báo lỗi theo từng phòng. */
    public record ReadingInput(int roomId, String electric, String water) { }

    public static class BillingException extends Exception {
        private static final long serialVersionUID = 1L;
        private final List<String> messages;
        private final boolean conflict;

        public BillingException(List<String> messages) {
            this(messages, false);
        }

        public BillingException(List<String> messages, boolean conflict) {
            super(String.join(" ", messages));
            this.messages = List.copyOf(messages);
            this.conflict = conflict;
        }

        public List<String> getMessages() { return messages; }
        public boolean isConflict() { return conflict; }
    }

    public static class GenerationResult {
        private final int created;
        private final int alreadyInvoiced;
        private final List<String> missingRooms;

        public GenerationResult(int created, int alreadyInvoiced, List<String> missingRooms) {
            this.created = created;
            this.alreadyInvoiced = alreadyInvoiced;
            this.missingRooms = List.copyOf(missingRooms);
        }

        public int getCreated() { return created; }
        public int getAlreadyInvoiced() { return alreadyInvoiced; }
        public List<String> getMissingRooms() { return missingRooms; }
    }

    public static class MeterSheet {
        private final YearMonth period;
        private final List<MeterRow> rows;
        private final UtilityPrice price;
        private final List<Invoice> invoices;

        public MeterSheet(YearMonth period, List<MeterRow> rows, UtilityPrice price, List<Invoice> invoices) {
            this.period = period;
            this.rows = rows;
            this.price = price;
            this.invoices = invoices;
        }

        public int getYear() { return period.getYear(); }
        public int getMonth() { return period.getMonthValue(); }
        public List<MeterRow> getRows() { return rows; }
        public UtilityPrice getPrice() { return price; }
        public List<Invoice> getInvoices() { return invoices; }

        public long getRecordedCount() { return rows.stream().filter(MeterRow::isRecorded).count(); }
        public long getInvoicedCount() { return rows.stream().filter(MeterRow::isInvoiced).count(); }

        public boolean isEditable() {
            return rows.stream().anyMatch(row -> !row.isRecorded() && row.isOldIndexKnown());
        }

        public BigDecimal getInvoiceTotal() {
            return invoices.stream().map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}

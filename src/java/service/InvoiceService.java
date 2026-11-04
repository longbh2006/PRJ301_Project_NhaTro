package service;

import dal.InvoiceDAO;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import model.Invoice;
import model.InvoiceSummary;

public class InvoiceService {
    public static final int PAGE_SIZE = 5;
    public static final int MANAGER_PAGE_SIZE = 10;
    public static final String FILTER_OVERDUE = "OVERDUE";
    private static final Set<String> MANAGER_STATUS_FILTERS =
            Set.of("", Invoice.STATUS_UNPAID, Invoice.STATUS_PAID, FILTER_OVERDUE);

    public InvoicePage findPageForTenant(int tenantId, int page) throws SQLException {
        if (tenantId <= 0 || page <= 0) {
            throw new IllegalArgumentException("Mã người thuê và số trang phải là số nguyên dương.");
        }
        try (InvoiceDAO dao = new InvoiceDAO()) {
            requireConnection(dao);
            long total = dao.countByTenant(tenantId);
            int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
            // Trang vượt tổng (vd. sửa ?page=99) được đưa về trang cuối thay vì hiện bảng rỗng.
            int currentPage = Math.min(page, totalPages);
            List<Invoice> invoices = dao.findPageByTenant(tenantId,
                    (long) (currentPage - 1) * PAGE_SIZE, PAGE_SIZE);
            return new InvoicePage(invoices, total, currentPage, totalPages);
        }
    }

    /**
     * Trả null khi hoá đơn không tồn tại hoặc không thuộc người thuê này.
     */
    public Invoice findDetailForTenant(int tenantId, int invoiceId) throws SQLException {
        if (tenantId <= 0 || invoiceId <= 0) {
            throw new IllegalArgumentException("Mã hoá đơn không hợp lệ.");
        }
        try (InvoiceDAO dao = new InvoiceDAO()) {
            requireConnection(dao);
            return dao.findByIdForTenant(invoiceId, tenantId);
        }
    }

    /**
     * Dùng cho OwnershipFilter: id sai định dạng/âm trả false để filter trả
     * 404 giống hệt hoá đơn của người khác.
     */
    public boolean isOwnedByTenant(int tenantId, int invoiceId) throws SQLException {
        if (tenantId <= 0 || invoiceId <= 0) {
            return false;
        }
        try (InvoiceDAO dao = new InvoiceDAO()) {
            requireConnection(dao);
            return dao.isOwnedByTenant(invoiceId, tenantId);
        }
    }

    /**
     * NT-39: danh sách hoá đơn mọi phòng cho quản lý. status rỗng = mọi trạng thái;
     * OVERDUE không phải trạng thái lưu trong CSDL mà là UNPAID đã qua dueDate.
     */
    public ManagerInvoicePage findPageForManager(Integer year, Integer month, String status,
            String roomCode, String tenantName, int page) throws SQLException {
        if (status == null || !MANAGER_STATUS_FILTERS.contains(status)) {
            throw new IllegalArgumentException("Trạng thái hoá đơn không hợp lệ.");
        }
        // Năm và tháng lọc độc lập (chỉ năm = cả năm); miền giá trị theo CK_Invoice_period.
        if (year != null && (year < 2020 || year > 2100)) {
            throw new IllegalArgumentException("Năm phải từ 2020 đến 2100.");
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("Tháng phải từ 1 đến 12.");
        }
        if (roomCode == null || roomCode.length() > 10) {
            throw new IllegalArgumentException("Mã phòng không được vượt quá 10 ký tự.");
        }
        if (tenantName == null || tenantName.length() > 100) {
            throw new IllegalArgumentException("Tên người thuê không được vượt quá 100 ký tự.");
        }
        if (page <= 0) {
            throw new IllegalArgumentException("Số trang phải là số nguyên dương.");
        }
        boolean overdueOnly = FILTER_OVERDUE.equals(status);
        String storedStatus = overdueOnly ? Invoice.STATUS_UNPAID : status;
        try (InvoiceDAO dao = new InvoiceDAO()) {
            requireConnection(dao);
            InvoiceSummary summary = dao.summarizeForManager(year, month, storedStatus, overdueOnly,
                    roomCode, tenantName);
            int totalPages = (int) Math.max(1, (summary.getCount() + MANAGER_PAGE_SIZE - 1) / MANAGER_PAGE_SIZE);
            int currentPage = Math.min(page, totalPages);
            List<Invoice> invoices = dao.findPageForManager(year, month, storedStatus, overdueOnly,
                    roomCode, tenantName, (long) (currentPage - 1) * MANAGER_PAGE_SIZE, MANAGER_PAGE_SIZE);
            return new ManagerInvoicePage(invoices, summary, currentPage, totalPages);
        }
    }

    /**
     * NT-39: chi tiết một hoá đơn bất kỳ cho quản lý; null khi không tồn tại.
     */
    public Invoice findDetailForManager(int invoiceId) throws SQLException {
        if (invoiceId <= 0) {
            throw new IllegalArgumentException("Mã hoá đơn không hợp lệ.");
        }
        try (InvoiceDAO dao = new InvoiceDAO()) {
            requireConnection(dao);
            return dao.findByIdForManager(invoiceId);
        }
    }

    private void requireConnection(InvoiceDAO dao) throws SQLException {
        if (dao.getConnection() == null) {
            throw new SQLException("Không thể kết nối đến cơ sở dữ liệu.");
        }
    }

    public static class InvoicePage {
        private final List<Invoice> invoices;
        private final long total;
        private final int page;
        private final int totalPages;

        public InvoicePage(List<Invoice> invoices, long total, int page, int totalPages) {
            this.invoices = invoices;
            this.total = total;
            this.page = page;
            this.totalPages = totalPages;
        }

        public List<Invoice> getInvoices() { return invoices; }
        public long getTotal() { return total; }
        public int getPage() { return page; }
        public int getTotalPages() { return totalPages; }
    }

    public static class ManagerInvoicePage {
        private final List<Invoice> invoices;
        private final InvoiceSummary summary;
        private final int page;
        private final int totalPages;

        public ManagerInvoicePage(List<Invoice> invoices, InvoiceSummary summary, int page, int totalPages) {
            this.invoices = invoices;
            this.summary = summary;
            this.page = page;
            this.totalPages = totalPages;
        }

        public List<Invoice> getInvoices() { return invoices; }
        public InvoiceSummary getSummary() { return summary; }
        public int getPage() { return page; }
        public int getTotalPages() { return totalPages; }
    }
}

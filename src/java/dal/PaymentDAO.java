package dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import model.Invoice;
import model.Payment;

/**
 * Truy vấn thanh toán VNPay (UC07 · NT-44, Q16 trong queries-mau.sql).
 * Giữ connection kế thừa đến khi đóng DAO để service gom nhiều câu vào một
 * transaction; DAO không tự commit/rollback.
 */
public class PaymentDAO extends DBContext implements AutoCloseable {

    /** Hoá đơn của đúng người thuê; hoá đơn người khác trả null như không tồn tại. */
    public Invoice findInvoiceForTenant(int invoiceId, int tenantId) throws SQLException {
        String sql = """
                SELECT i.*
                FROM Invoice i JOIN Contract c ON c.contractId = i.contractId
                WHERE i.invoiceId = ? AND c.tenantId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapInvoice(rs) : null;
            }
        }
    }

    /**
     * Q16a: tạo một lần thử PENDING. Số tiền lấy từ Invoice.total trong CSDL,
     * điều kiện "của mình + UNPAID" nằm ngay trong câu INSERT nên không ai
     * chen vào giữa bước kiểm và bước ghi. Không đủ điều kiện trả null.
     */
    public Payment insertPending(int invoiceId, int tenantId, String txnRef) throws SQLException {
        String sql = """
                INSERT INTO Payment (invoiceId, txnRef, amount)
                OUTPUT INSERTED.*
                SELECT i.invoiceId, ?, i.total
                FROM Invoice i JOIN Contract c ON c.contractId = i.contractId
                WHERE i.invoiceId = ? AND c.tenantId = ? AND i.status = 'UNPAID'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, txnRef);
            ps.setInt(2, invoiceId);
            ps.setInt(3, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPayment(rs) : null;
            }
        }
    }

    /** Q16b: khoá dòng Payment; hai IPN cùng lúc thì IPN sau đợi IPN trước commit. */
    public Payment lockByTxnRef(String txnRef) throws SQLException {
        String sql = "SELECT * FROM Payment WITH (UPDLOCK, HOLDLOCK) WHERE txnRef = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, txnRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPayment(rs) : null;
            }
        }
    }

    /** Trang return chỉ đọc trạng thái hiện tại, không khoá. */
    public Payment findByTxnRef(String txnRef) throws SQLException {
        String sql = "SELECT * FROM Payment WHERE txnRef = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, txnRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPayment(rs) : null;
            }
        }
    }

    /** Chuyển PENDING → SUCCESS/FAILED; WHERE status = 'PENDING' giữ đúng chiều chuyển trạng thái. */
    public boolean finishPending(int paymentId, boolean success, String vnpTransactionNo,
            String bankCode, String responseCode) throws SQLException {
        String sql = """
                UPDATE Payment
                SET status = ?, paidAt = CASE WHEN ? = 'SUCCESS' THEN SYSDATETIME() END,
                    vnpTransactionNo = ?, bankCode = ?, responseCode = ?
                WHERE paymentId = ? AND status = 'PENDING'
                """;
        String status = success ? "SUCCESS" : "FAILED";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setString(3, vnpTransactionNo);
            ps.setString(4, bankCode);
            ps.setString(5, responseCode);
            ps.setInt(6, paymentId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Chỉ IPN gọi tới đây (UNPAID → PAID). */
    public boolean markInvoicePaid(int invoiceId) throws SQLException {
        String sql = "UPDATE Invoice SET status = 'PAID', paidAt = SYSDATETIME() WHERE invoiceId = ? AND status = 'UNPAID'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            return ps.executeUpdate() == 1;
        }
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("paymentId"));
        p.setInvoiceId(rs.getInt("invoiceId"));
        p.setTxnRef(rs.getString("txnRef"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setStatus(rs.getString("status"));
        p.setVnpTransactionNo(rs.getString("vnpTransactionNo"));
        p.setBankCode(rs.getString("bankCode"));
        p.setResponseCode(rs.getString("responseCode"));
        p.setCreatedAt(rs.getObject("createdAt", LocalDateTime.class));
        p.setPaidAt(rs.getObject("paidAt", LocalDateTime.class));
        return p;
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceId(rs.getInt("invoiceId"));
        inv.setContractId(rs.getInt("contractId"));
        inv.setPriceId(rs.getInt("priceId"));
        inv.setYear(rs.getInt("year"));
        inv.setMonth(rs.getInt("month"));
        inv.setRoomFee(rs.getBigDecimal("roomFee"));
        inv.setElectricUsed(rs.getInt("electricUsed"));
        inv.setElectricFee(rs.getBigDecimal("electricFee"));
        inv.setWaterUsed(rs.getInt("waterUsed"));
        inv.setWaterFee(rs.getBigDecimal("waterFee"));
        inv.setServiceFee(rs.getBigDecimal("serviceFee"));
        inv.setTotal(rs.getBigDecimal("total"));
        inv.setStatus(rs.getString("status"));
        inv.setDueDate(rs.getObject("dueDate", LocalDate.class));
        inv.setCreatedAt(rs.getObject("createdAt", LocalDateTime.class));
        inv.setPaidAt(rs.getObject("paidAt", LocalDateTime.class));
        return inv;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}

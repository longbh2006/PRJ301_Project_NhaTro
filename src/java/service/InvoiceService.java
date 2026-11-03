package service;

import dal.InvoiceDAO;
import java.sql.SQLException;
import java.util.List;
import model.Invoice;

public class InvoiceService {
    public static final int PAGE_SIZE = 5;

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
}

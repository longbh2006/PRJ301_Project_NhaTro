package service;

import dal.BillingDAO;
import java.sql.SQLException;
import model.Invoice;

public class InvoiceService {

    public boolean isInvoiceOwnedByTenant(int invoiceId, int tenantId) throws SQLException {
        if (invoiceId <= 0 || tenantId <= 0) {
            return false;
        }
        try (BillingDAO dao = new BillingDAO()) {
            if (dao.getConnection() == null) {
                throw new SQLException("Không thể kết nối đến cơ sở dữ liệu.");
            }
            return dao.isInvoiceOwnedByTenant(invoiceId, tenantId);
        }
    }

    /**
     * Trả null khi hoá đơn không tồn tại hoặc không thuộc tenant: controller
     * trả 404 cho cả hai trường hợp để không lộ id nào có thật.
     */
    public Invoice getInvoiceForTenant(int invoiceId, int tenantId) throws SQLException {
        if (invoiceId <= 0 || tenantId <= 0) {
            return null;
        }
        try (BillingDAO dao = new BillingDAO()) {
            if (dao.getConnection() == null) {
                throw new SQLException("Không thể kết nối đến cơ sở dữ liệu.");
            }
            return dao.findInvoiceByIdForTenant(invoiceId, tenantId);
        }
    }
}

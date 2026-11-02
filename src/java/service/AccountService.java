package service;

import dal.AccountDAO;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import model.Account;
import util.PasswordHasher;

public class AccountService {

    public static final int PAGE_SIZE = 6;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{4,30}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[0-9]{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static class AccountPageData {

        private final List<Account> accounts;
        private final int currentPage;
        private final int totalPages;

        public AccountPageData(List<Account> accounts, int currentPage, int totalPages) {
            this.accounts = accounts;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
        }

        public List<Account> getAccounts() {
            return accounts;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getTotalPages() {
            return totalPages;
        }
    }

    public String normalizeRoleFilter(String role) {
        if (role == null || role.trim().isEmpty()) {
            return null;
        }
        String r = role.trim().toUpperCase();
        if ("ADMIN".equals(r) || "MANAGER".equals(r) || "TENANT".equals(r)) {
            return r;
        }
        return null;
    }

    public int calculateTotalPages(int totalRecords) {
        if (totalRecords <= 0) {
            return 0;
        }
        return (totalRecords + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public int normalizePage(String pageString, int totalPages, int totalRecords) {
        if (totalRecords <= 0 || totalPages <= 0) {
            return 0;
        }
        int page = 1;
        if (pageString != null && !pageString.trim().isEmpty()) {
            try {
                page = Integer.parseInt(pageString.trim());
            } catch (NumberFormatException e) {
                page = 1;
            }
        }
        if (page < 1) {
            page = 1;
        } else if (page > totalPages) {
            page = totalPages;
        }
        return page;
    }

    // Gộp đếm và lấy danh sách vào chung 1 kết nối DAO duy nhất
    public AccountPageData getAccountPageData(String keyword, String role, String pageString) {
        String cleanRole = normalizeRoleFilter(role);
        try (AccountDAO dao = new AccountDAO()) {
            int totalRecords = dao.countAccounts(keyword, cleanRole);
            int totalPages = calculateTotalPages(totalRecords);
            int currentPage = normalizePage(pageString, totalPages, totalRecords);

            List<Account> accounts;
            if (currentPage <= 0) {
                accounts = new ArrayList<>();
            } else {
                accounts = dao.findAccountsPage(keyword, cleanRole, currentPage, PAGE_SIZE);
            }
            return new AccountPageData(accounts, currentPage, totalPages);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi CSDL khi tải danh sách tài khoản.", e);
        }
    }

    public void createManager(String username, String password, String fullName, String phone, String email) {
        // 1. Kiểm tra username (4-30 ký tự theo setup.sql:82)
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        }
        String cleanUsername = username.trim();
        if (cleanUsername.length() < 4 || cleanUsername.length() > 30) {
            throw new IllegalArgumentException("Tên đăng nhập phải từ 4 đến 30 ký tự.");
        }
        if (!USERNAME_PATTERN.matcher(cleanUsername).matches()) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ được chứa chữ cái không dấu, số và các ký tự ., _, -.");
        }

        // 2. Kiểm tra mật khẩu (8-1024 ký tự thống nhất với AuthService.java:87)
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        if (password.length() < 8 || password.length() > 1024) {
            throw new IllegalArgumentException("Mật khẩu phải từ 8 đến 1024 ký tự.");
        }

        // 3. Kiểm tra họ tên
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        String cleanFullName = fullName.trim();
        if (cleanFullName.length() > 100) {
            throw new IllegalArgumentException("Họ và tên không được vượt quá 100 ký tự.");
        }

        // 4. Kiểm tra số điện thoại (Tùy chọn theo setup.sql:74; nếu nhập phải đúng 10 số theo setup.sql:84)
        String cleanPhone = (phone != null) ? phone.trim() : "";
        if (!cleanPhone.isEmpty() && !PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (bắt đầu bằng số 0, gồm đúng 10 chữ số).");
        }

        // 5. Kiểm tra email (Tùy chọn)
        String cleanEmail = (email != null) ? email.trim() : "";
        if (!cleanEmail.isEmpty()) {
            if (cleanEmail.length() > 100 || !EMAIL_PATTERN.matcher(cleanEmail).matches()) {
                throw new IllegalArgumentException("Địa chỉ email không đúng định dạng hoặc vượt quá 100 ký tự.");
            }
        }

        String passwordHash = PasswordHasher.hash(password);

        // 6. Dùng try-with-resources để tự động đóng kết nối kể cả khi ném ngoại lệ trùng tên
        try (AccountDAO dao = new AccountDAO()) {
            if (dao.usernameExists(cleanUsername)) {
                throw new IllegalArgumentException("Tên đăng nhập '" + cleanUsername + "' đã tồn tại.");
            }
            dao.insertManagerAccount(cleanUsername, passwordHash, cleanFullName, cleanPhone, cleanEmail);
        } catch (SQLException e) {
            if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) {
                throw new IllegalArgumentException("Tên đăng nhập '" + cleanUsername + "' đã tồn tại.");
            }
            throw new RuntimeException("Lỗi CSDL khi tạo tài khoản quản lý.", e);
        }
    }

    public void updateAccountStatus(int targetAccountId, boolean newActiveStatus, Account currentAdmin) {
        if (currentAdmin == null) {
            throw new IllegalArgumentException("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
        }
        if (targetAccountId <= 0) {
            throw new IllegalArgumentException("ID tài khoản không hợp lệ.");
        }

        if (currentAdmin.getAccountId() == targetAccountId && !newActiveStatus) {
            throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản đang đăng nhập của chính mình.");
        }

        try (AccountDAO dao = new AccountDAO()) {
            boolean updated = dao.updateActiveStatus(targetAccountId, newActiveStatus);
            if (!updated) {
                throw new IllegalArgumentException("Không tìm thấy tài khoản cần cập nhật trạng thái.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi CSDL khi cập nhật trạng thái tài khoản.", e);
        }
    }
}

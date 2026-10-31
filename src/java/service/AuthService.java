package service;

import dal.AccountDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import model.Account;
import model.RegistrationForm;
import util.PasswordHasher;

public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    public int register(RegistrationForm form, String password)
            throws RegistrationException, SQLException {
        Map<String, String> errors = validate(form, password);
        if (!errors.isEmpty()) {
            throw new RegistrationException(errors);
        }
        String passwordHash = PasswordHasher.hash(password);
        LocalDate dateOfBirth = form.getDateOfBirth().isEmpty()
                ? null : LocalDate.parse(form.getDateOfBirth());
        try (AccountDAO dao = createAccountDAO()) {
            if (dao.usernameExists(form.getUsername())) {
                errors.put("username", "Tên đăng nhập đã được sử dụng.");
            }
            if (dao.idCardNoExists(form.getIdCardNo())) {
                errors.put("idCardNo", "Số CCCD đã được đăng ký.");
            }
            if (!errors.isEmpty()) {
                throw new RegistrationException(errors);
            }
            Connection con = dao.getConnection();
            boolean autoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                int accountId = dao.insertAccount(con, form, passwordHash, "TENANT");
                dao.insertTenant(con, accountId, form, dateOfBirth);
                con.commit();
                con.setAutoCommit(autoCommit);
                return accountId;
            } catch (SQLException | RuntimeException ex) {
                try {
                    con.rollback();
                } catch (SQLException rollbackFailure) {
                    ex.addSuppressed(rollbackFailure);
                    throw ex;
                }
                try {
                    con.setAutoCommit(autoCommit);
                } catch (SQLException resetFailure) {
                    ex.addSuppressed(resetFailure);
                }
                throw ex;
            }
        } catch (SQLException ex) {
            // UNIQUE vẫn bảo vệ dữ liệu nếu hai yêu cầu vượt qua kiểm tra cùng lúc.
            for (SQLException error = ex; error != null; error = error.getNextException()) {
                if (error.getErrorCode() == 2601 || error.getErrorCode() == 2627) {
                    String message = String.valueOf(error.getMessage()).toLowerCase(Locale.ROOT);
                    if (message.contains("uq_account_username")) {
                        errors.put("username", "Tên đăng nhập đã được sử dụng.");
                    } else if (message.contains("uq_tenant_idcardno")) {
                        errors.put("idCardNo", "Số CCCD đã được đăng ký.");
                    } else {
                        errors.put("form", "Tên đăng nhập hoặc CCCD đã được đăng ký.");
                    }
                    throw new RegistrationException(errors);
                }
            }
            throw ex;
        }
    }

    protected AccountDAO createAccountDAO() throws SQLException {
        return new AccountDAO();
    }

    private Map<String, String> validate(RegistrationForm form, String password) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!form.getUsername().matches("[A-Za-z0-9_.-]{4,30}")) {
            errors.put("username", "Tên đăng nhập gồm 4–30 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        }
        if (password == null || password.length() < 8 || password.length() > 1024) {
            errors.put("password", "Mật khẩu phải có từ 8 đến 1024 ký tự.");
        }
        if (form.getFullName().isEmpty() || form.getFullName().length() > 100) {
            errors.put("fullName", "Họ tên phải có từ 1 đến 100 ký tự.");
        }
        if (!form.getIdCardNo().matches("[0-9]{12}")) {
            errors.put("idCardNo", "CCCD phải gồm đúng 12 chữ số.");
        }
        if (!form.getPhone().isEmpty() && !form.getPhone().matches("[0-9]{10}")) {
            errors.put("phone", "Số điện thoại phải gồm đúng 10 chữ số.");
        }
        if (!form.getEmail().isEmpty() && (form.getEmail().length() > 100
                || !form.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) {
            errors.put("email", "Email không hợp lệ hoặc dài quá 100 ký tự.");
        }
        if (form.getSchool().length() > 100) {
            errors.put("school", "Tên trường không được dài quá 100 ký tự.");
        }
        if (form.getHometown().length() > 100) {
            errors.put("hometown", "Quê quán không được dài quá 100 ký tự.");
        }
        if (!form.getDateOfBirth().isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(form.getDateOfBirth());
                if (date.getYear() < 1 || date.isAfter(LocalDate.now())) {
                    errors.put("dateOfBirth", "Ngày sinh phải hợp lệ và không ở tương lai.");
                }
            } catch (DateTimeParseException ex) {
                errors.put("dateOfBirth", "Ngày sinh không hợp lệ.");
            }
        }
        return errors;
    }

    public Account login(String username, String password) throws SQLException {
        // 1. Mở DAO trong try‑with‑resources
        try (AccountDAO dao = createAccountDAO()) {
            // 1. Lấy tài khoản
            Account account = dao.findByUsername(username);

            // 2. Không có tài khoản thì return null
            if (account == null) {
                return null;
            }

            // 3. Sai mật khẩu thì return null
            if (!PasswordHasher.verify(password, account.getPasswordHash())) {
                return null;
            }

            // 4. Bị khoá thì return null
            if (!account.isActive()) {
                return null;
            }

            // 5. Xóa hash
            account.setPasswordHash(null);

            // 6. Trả về account đã được “sạch” hash
            return account;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Đăng nhập lỗi do CSDL", e);
            throw e;
        }
    }

}

package dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import model.RegistrationForm;
import model.Account;
public class AccountDAO extends DBContext implements AutoCloseable {

    public AccountDAO() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is unavailable.");
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT accountId FROM Account WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean idCardNoExists(String idCardNo) throws SQLException {
        String sql = "SELECT tenantId FROM Tenant WHERE idCardNo = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idCardNo);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /** Service quản lý transaction và connection truyền vào. */
    public int insertAccount(Connection con, RegistrationForm form, String passwordHash,
            String role) throws SQLException {
        String sql = """
                INSERT INTO Account (username, passwordHash, fullName, phone, email, role, active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, form.getUsername());
            statement.setString(2, passwordHash);
            statement.setNString(3, form.getFullName());
            statement.setString(4, nullable(form.getPhone()));
            statement.setString(5, nullable(form.getEmail()));
            statement.setString(6, role);
            statement.setBoolean(7, true);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Account was not inserted.");
            }
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (!result.next()) {
                    throw new SQLException("Account INSERT did not return a generated ID.");
                }
                return result.getInt(1);
            }
        }
    }

    public void insertTenant(Connection con, int accountId, RegistrationForm form,
            LocalDate dateOfBirth) throws SQLException {
        String sql = """
                INSERT INTO Tenant (tenantId, idCardNo, school, dateOfBirth, hometown)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, accountId);
            statement.setString(2, form.getIdCardNo());
            statement.setNString(3, nullable(form.getSchool()));
            statement.setObject(4, dateOfBirth, Types.DATE);
            statement.setNString(5, nullable(form.getHometown()));
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Tenant was not inserted.");
            }
        }
    }

    private static String nullable(String value) {
        return value.isEmpty() ? null : value;
    }

    public Account findByUsername(String username) throws SQLException {
    // 1. Khai báo câu SQL
    String sql = "SELECT accountId, username, passwordHash, fullName, role, active "
               + "FROM Account WHERE username = ?";

    // 2. try‑with‑resources cho PreparedStatement
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, username);          // set tham số

        // 3. try‑with‑resources cho ResultSet
        try (ResultSet rs = ps.executeQuery()) {
            // Nếu không có dòng nào
            if (!rs.next()) {
                return null;
            }

            // 4. Tạo đối tượng Account và gán từng cột
            Account account = new Account();
            account.setAccountId(rs.getInt("accountId"));
            account.setUsername(rs.getString("username"));
            account.setPasswordHash(rs.getString("passwordHash"));
            account.setFullName(rs.getString("fullName"));
            account.setRole(rs.getString("role"));
            account.setActive(rs.getBoolean("active"));

            // 5. Trả về Account
            return account;
        }
    }
}

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}

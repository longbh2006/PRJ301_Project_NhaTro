package dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RegistrationTestDAO extends DBContext implements AutoCloseable {

    public Map<String, String> findRegistration(String username) throws SQLException {
        String sql = """
                SELECT a.accountId, a.passwordHash, a.fullName, a.phone, a.email, a.role,
                       a.active, t.tenantId, t.idCardNo, t.school, t.dateOfBirth, t.hometown
                FROM Account a LEFT JOIN Tenant t ON t.tenantId = a.accountId
                WHERE a.username = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                Map<String, String> values = new LinkedHashMap<>();
                String[] columns = {"accountId", "passwordHash", "fullName", "phone", "email",
                        "role", "active", "tenantId", "idCardNo", "school", "dateOfBirth", "hometown"};
                for (String column : columns) {
                    values.put(column, result.getString(column));
                }
                return values;
            }
        }
    }

    public void removeRegistration(String username) throws SQLException {
        String tenantSql = "DELETE FROM Tenant WHERE tenantId IN (SELECT accountId FROM Account WHERE username = ?)";
        String accountSql = "DELETE FROM Account WHERE username = ?";
        try (PreparedStatement tenant = connection.prepareStatement(tenantSql);
                PreparedStatement account = connection.prepareStatement(accountSql)) {
            tenant.setString(1, username);
            tenant.executeUpdate();
            account.setString(1, username);
            account.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}

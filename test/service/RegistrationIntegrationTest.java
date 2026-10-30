package service;

import dal.AccountDAO;
import dal.RegistrationTestDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import model.RegistrationForm;
import util.PasswordHasher;

public final class RegistrationIntegrationTest {

    public static void checkHttpRegistration(String username) throws SQLException {
        try (RegistrationTestDAO dao = new RegistrationTestDAO()) {
            Map<String, String> stored = dao.findRegistration(username);
            check(stored != null && "TENANT".equals(stored.get("role"))
                    && "Nguyễn Thị Ánh".equals(stored.get("fullName"))
                    && PasswordHasher.verify("Test password 123", stored.get("passwordHash")),
                    "HTTP registration persistence");
        }
    }

    public static void cleanupHttpRegistration(String username) throws SQLException {
        try (RegistrationTestDAO dao = new RegistrationTestDAO()) {
            dao.removeRegistration(username);
        }
    }

    public static void main(String[] args) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "nt30_" + suffix;
        String secondUsername = "nt30_r_" + suffix;
        String idCardNo = String.format("%012d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1_000_000_000_000L));
        String password = "  Mật khẩu thử 123  ";
        RegistrationForm form = new RegistrationForm(username, "Nguyễn Thị Ánh", "0912345678",
                "nt30@example.test", idCardNo, "Đại học FPT", "2004-02-29", "Thái Bình");
        try (RegistrationTestDAO fixtures = new RegistrationTestDAO()) {
            try {
                int accountId = new AuthService().register(form, password);
                Map<String, String> stored = fixtures.findRegistration(username);
                check(stored != null, "Registration committed");
                check(Integer.toString(accountId).equals(stored.get("accountId")), "Generated ID returned");
                check(stored.get("accountId").equals(stored.get("tenantId")), "Shared Account/Tenant ID");
                check("TENANT".equals(stored.get("role")), "TENANT role");
                check("1".equals(stored.get("active")), "Active account");
                check(PasswordHasher.verify(password, stored.get("passwordHash")), "Stored hash verifies");
                check(!password.equals(stored.get("passwordHash")), "No plaintext password");
                check("Nguyễn Thị Ánh".equals(stored.get("fullName")), "Vietnamese full name");
                check("Đại học FPT".equals(stored.get("school")), "Vietnamese school");
                check("Thái Bình".equals(stored.get("hometown")), "Vietnamese hometown");
                System.out.println("PASS registration: commit, IDs, TENANT, PBKDF2 and Unicode");

                expectDuplicate(new AuthService(), form, password, "username");
                RegistrationForm duplicateId = new RegistrationForm(secondUsername, "Kiểm thử", "", "",
                        idCardNo, "", "", "");
                expectDuplicate(new AuthService(), duplicateId, password, "idCardNo");
                check(fixtures.findRegistration(secondUsername) == null, "Precheck left no Account");
                System.out.println("PASS duplicate username/CCCD: friendly error, no orphan Account");

                // Giả lập kiểm tra trùng bị vượt qua do đăng ký đồng thời; SQL Server vẫn chặn INSERT thứ hai.
                RaceAuthService raceService = new RaceAuthService(false);
                expectDuplicate(raceService, duplicateId, password, "idCardNo");
                check(raceService.dao.tenantAttempted, "Second INSERT attempted");
                check(raceService.dao.transactionRestored, "Transaction state restored after rollback");
                check(raceService.dao.getConnection().isClosed(), "Owned connection closed after failure");
                check(fixtures.findRegistration(secondUsername) == null, "Second INSERT failure rolled back Account");
                System.out.println("PASS actual second INSERT UNIQUE failure: service rolls back Account");

                RaceAuthService failureService = new RaceAuthService(true);
                try {
                    failureService.register(duplicateId, password);
                    throw new AssertionError("Expected simulated persistence error");
                } catch (SQLException expected) {
                    check("HY000".equals(expected.getSQLState()), "Persistence failure propagated");
                }
                check(fixtures.findRegistration(secondUsername) == null, "Non-UNIQUE failure rolled back Account");
                System.out.println("PASS non-UNIQUE second INSERT failure: rollback and propagate");

                String otherId = idCardNo.equals("999999999999") ? "999999999998" : "999999999999";
                RegistrationForm optional = new RegistrationForm(secondUsername, "Tối giản", "", "", otherId, "", "", "");
                new AuthService().register(optional, password);
                Map<String, String> minimal = fixtures.findRegistration(secondUsername);
                check(minimal.get("dateOfBirth") == null && minimal.get("phone") == null
                        && minimal.get("email") == null && minimal.get("school") == null
                        && minimal.get("hometown") == null, "Optional blanks stored as SQL NULL");
                System.out.println("PASS optional fields stored as SQL NULL");
            } finally {
                fixtures.removeRegistration(secondUsername);
                fixtures.removeRegistration(username);
            }
        }
    }

    private static void expectDuplicate(AuthService service, RegistrationForm form, String password,
            String field) throws Exception {
        try {
            service.register(form, password);
            throw new AssertionError("Duplicate accepted");
        } catch (RegistrationException ex) {
            check(ex.getErrors().containsKey(field), "Friendly duplicate error: " + field);
        }
    }

    private static void check(boolean passed, String message) {
        if (!passed) {
            throw new AssertionError(message);
        }
    }

    private static final class RaceAuthService extends AuthService {
        private final boolean persistenceFailure;
        private RaceAccountDAO dao;

        private RaceAuthService(boolean persistenceFailure) {
            this.persistenceFailure = persistenceFailure;
        }

        @Override
        protected AccountDAO createAccountDAO() throws SQLException {
            dao = new RaceAccountDAO(persistenceFailure);
            return dao;
        }
    }

    private static final class RaceAccountDAO extends AccountDAO {
        private final boolean persistenceFailure;
        private boolean tenantAttempted;
        private boolean transactionRestored;

        private RaceAccountDAO(boolean persistenceFailure) throws SQLException {
            this.persistenceFailure = persistenceFailure;
        }

        @Override
        public boolean idCardNoExists(String idCardNo) {
            return false;
        }

        @Override
        public void insertTenant(Connection con, int accountId, RegistrationForm form,
                LocalDate dateOfBirth) throws SQLException {
            tenantAttempted = true;
            if (persistenceFailure) {
                throw new SQLException("Simulated second INSERT failure", "HY000");
            }
            super.insertTenant(con, accountId, form, dateOfBirth);
        }

        @Override
        public void close() throws SQLException {
            transactionRestored = connection.getAutoCommit();
            super.close();
        }
    }
}

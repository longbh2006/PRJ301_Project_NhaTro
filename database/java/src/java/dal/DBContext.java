package dal;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * MỘT chỗ duy nhất biết cách kết nối CSDL NhaTroSV (thẻ NT-23).
 * Mọi DAO chỉ gọi:  try (Connection c = DBContext.getConnection()) { ... }
 *
 * Cấu hình đọc theo thứ tự (cái sau THẮNG cái trước):
 *   1. file db.properties trong classpath (src/java/db.properties → WEB-INF/classes) — KHÔNG commit
 *   2. biến môi trường DB_URL, DB_USER, DB_PASS (nếu có đặt)
 * Cần file mssql-jdbc-13.x.jre11.jar trong web/WEB-INF/lib (hoặc Libraries của project).
 */
public class DBContext {

    private static final String DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final Properties CFG = loadConfig();

    private DBContext() { }   // không ai cần new DBContext()

    public static Connection getConnection() throws SQLException {
        try {
            // Driver nằm trong WEB-INF/lib do class loader của web app nạp, DriverManager không tự thấy
            // ⇒ thiếu dòng này sẽ gặp lỗi "No suitable driver found for jdbc:sqlserver://..."
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Thiếu mssql-jdbc trong WEB-INF/lib (hoặc Libraries của project)", e);
        }
        String url  = get("DB_URL",  "db.url");
        String user = get("DB_USER", "db.user");
        String pass = get("DB_PASS", "db.password");
        if (url == null || user == null || pass == null) {
            throw new SQLException("Chưa cấu hình CSDL: chép db.properties.example thành src/java/db.properties rồi sửa mật khẩu");
        }
        return DriverManager.getConnection(url, user, pass);
    }

    /** Biến môi trường thắng; không có thì lấy trong db.properties. */
    private static String get(String envKey, String propKey) {
        String v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v.trim();
        v = CFG.getProperty(propKey);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static Properties loadConfig() {
        Properties p = new Properties();
        try (InputStream in = DBContext.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) p.load(in);   // không có file cũng được nếu đã đặt biến môi trường
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được db.properties", e);
        }
        return p;
    }

    /** Chạy thử nhanh (Run File trong NetBeans): in "Kết nối OK" + số phòng. */
    public static void main(String[] args) throws SQLException {
        try (Connection c = getConnection();
             var ps = c.prepareStatement("SELECT COUNT(*) FROM Room");
             var rs = ps.executeQuery()) {
            rs.next();
            System.out.println("Kết nối OK — CSDL " + c.getCatalog() + " có " + rs.getInt(1) + " phòng");
        }
    }
}

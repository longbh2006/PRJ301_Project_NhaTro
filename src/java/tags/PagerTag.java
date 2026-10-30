package tags;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

public class PagerTag extends SimpleTagSupport {

    // 3 biến private tương ứng với 3 thuộc tính
    private int page;
    private int totalPages;
    private String url;

    // Các hàm setter public để Container gán giá trị từ file JSP
    public void setPage(int page) {
        this.page = page;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // Hàm tự viết: Chuyển đổi các ký tự đặc biệt thành thực thể HTML để tránh vỡ
    // giao diện / XSS
    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // Ghi đè phương thức doTag()
    @Override
    public void doTag() throws JspException, IOException {
        // BƯỚC 6: Ca rỗng (Chặn đầu hàm)
        // Nếu không có dữ liệu (0 trang) hoặc quy định riêng của nhóm, thoát ngay không
        // in gì cả.
        if (totalPages <= 0) {
            return;
        }

        // BƯỚC 1: Lấy nơi để in (JspWriter)
        JspWriter out = getJspContext().getOut();

        // BƯỚC 2: Chuẩn bị URL
        // Escape chuỗi đầu vào để an toàn
        String safeUrl = escape(this.url);

        // Kiểm tra xem URL đã có tham số nào chưa (có dấu '?' chưa)
        // Nếu url đã chứa query string (vd: /rooms?roomTypeId=1&floor=2)
        // thì nối thêm &page=, ngược lại nối ?page=
        String separator = safeUrl.contains("?") ? "&amp;" : "?";
        String baseUrl = safeUrl + separator + "page=";

        // Dùng StringBuilder để gom HTML in một lần cho gọn
        StringBuilder html = new StringBuilder();

        // Khởi tạo thẻ div bọc ngoài (có thể đổi class cho phù hợp)
        html.append("<div class=\"pagination\">\n");

        // BƯỚC 3: In nút "Trước" (Previous)
        if (page > 1) {
            html.append("  <a href=\"").append(baseUrl).append(page - 1).append("\">Trước</a>\n");
        } else {
            // Khi đang ở trang 1, nút "Trước" vô hiệu hóa (không hiện liên kết)
            html.append("  <span class=\"disabled\">Trước</span>\n");
        }

        // BƯỚC 4: Vòng lặp các số trang
        for (int i = 1; i <= totalPages; i++) {
            if (i == page) {
                // Trang hiện tại -> in chữ thường (hoặc thẻ active), không in liên kết
                html.append("  <span class=\"active\">").append(i).append("</span>\n");
            } else {
                // Các trang khác -> In liên kết
                html.append("  <a href=\"").append(baseUrl).append(i).append("\">").append(i).append("</a>\n");
            }
        }

        // BƯỚC 5: In nút "Sau" (Next)
        if (page < totalPages) {
            html.append("  <a href=\"").append(baseUrl).append(page + 1).append("\">Sau</a>\n");
        } else {
            // Đã ở trang cuối -> vô hiệu hóa nút "Sau"
            html.append("  <span class=\"disabled\">Sau</span>\n");
        }

        html.append("</div>\n");

        // BƯỚC 1 (Tiếp): Gọi JspWriter để in khối HTML
        out.write(html.toString());
    }
}

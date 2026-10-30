package controller;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import service.RentalService;

public class RentalRequestWebIntegrationTest {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public static void run(String baseUrl, String tenantCookie, String otherTenantCookie,
            String managerCookie, int otherTenantId, int roomId, int occupiedRoomId) throws Exception {
        HttpResponse<String> guest = get(baseUrl + "/tenant/requests", null);
        require(guest.statusCode() == 302 && location(guest).endsWith("/login"), "Guest GET must redirect to login");
        HttpResponse<String> guestPost = post(baseUrl, null, Map.of());
        require(guestPost.statusCode() == 302, "Guest POST must not create a request");
        require(get(baseUrl + "/tenant/requests", managerCookie).statusCode() == 403, "Manager GET must be forbidden");
        require(post(baseUrl, managerCookie, Map.of()).statusCode() == 403, "Manager POST must be forbidden");
        require(get(baseUrl + "/WEB-INF/views/tenant/request-form.jsp", tenantCookie).statusCode() == 404,
                "JSP must not be public");
        require(get(baseUrl + "/tenant/requests?roomId=abc", tenantCookie).statusCode() == 400, "Invalid room ID");
        require(get(baseUrl + "/rooms/detail?roomId=2147483647", null).statusCode() == 404, "Unknown room");
        System.out.println("PASS: HTTP authentication, role, invalid IDs and protected JSPs");

        HttpResponse<String> detail = get(baseUrl + "/rooms/detail?roomId=" + roomId, tenantCookie);
        require(detail.statusCode() == 200 && detail.body().contains("Gửi yêu cầu thuê phòng này"), "Tenant room detail must offer rental");
        HttpResponse<String> form = get(baseUrl + "/tenant/requests?roomId=" + roomId, tenantCookie);
        require(form.statusCode() == 200 && form.body().contains("name=\"moveInDate\""), "JSP form must render");

        String note = "NT34 kiểm thử <script>alert(1)</script> & tiếng Việt";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("roomId", String.valueOf(roomId));
        fields.put("moveInDate", LocalDate.now().toString());
        fields.put("months", "abc");
        fields.put("numPeople", "1");
        fields.put("note", note);
        fields.put("tenantId", String.valueOf(otherTenantId));
        HttpResponse<String> invalid = post(baseUrl, tenantCookie, fields);
        require(invalid.statusCode() == 200 && invalid.body().contains("số nguyên hợp lệ"), "Invalid numeric input must show form error");
        require(invalid.body().contains("&lt;script&gt;") && !invalid.body().contains("<script>alert(1)</script>"),
                "Form must escape a reflected XSS note");
        require(invalid.body().contains("value=\"abc\""), "Invalid values must remain editable");
        fields.put("months", "1");
        fields.put("moveInDate", "2026-02-30");
        require(post(baseUrl, tenantCookie, fields).body().contains("Ngày dọn vào không hợp lệ"), "Invalid calendar date");
        fields.put("moveInDate", LocalDate.now().toString());
        fields.put("roomId", String.valueOf(occupiedRoomId));
        HttpResponse<String> occupied = post(baseUrl, tenantCookie, fields);
        require(occupied.statusCode() == 200 && occupied.body().contains("Phòng đã có người thuê"), "Occupied-room acceptance criterion");
        System.out.println("PASS: room detail, rental form, validation, preserved values, reflected XSS and occupied-room rejection");

        fields.put("roomId", String.valueOf(roomId));
        HttpResponse<String> success = post(baseUrl, tenantCookie, fields);
        require(success.statusCode() == 302 && location(success).endsWith("/tenant/requests"), "Success must follow PRG");
        HttpResponse<String> list = get(baseUrl + "/tenant/requests", tenantCookie);
        require(list.statusCode() == 200 && list.body().contains("Đã gửi yêu cầu thuê phòng"), "Success flash");
        Matcher requestLink = Pattern.compile("requestId=(\\d+)").matcher(list.body());
        require(requestLink.find(), "Request must provide a details link");
        String requestId = requestLink.group(1);
        require(list.body().contains("Chờ duyệt") && list.body().contains("YC"), "Pending request must appear");
        HttpResponse<String> requestDetail = get(baseUrl + "/tenant/requests?requestId=" + requestId, tenantCookie);
        require(requestDetail.statusCode() == 200 && requestDetail.body().contains("NT34 kiểm thử"), "Own request detail must render");
        require(requestDetail.body().contains("&lt;script&gt;") && !requestDetail.body().contains("<script>alert(1)</script>"), "Stored XSS must be escaped");
        require(!get(baseUrl + "/tenant/requests", tenantCookie).body().contains("Đã gửi yêu cầu thuê phòng"), "Flash must show only once");
        require(get(baseUrl + "/tenant/requests?requestId=" + requestId, otherTenantCookie).statusCode() == 404,
                "Forged form tenantId must be ignored and the other tenant cannot see this request");
        require(get(baseUrl + "/tenant/requests?status=APPROVED", tenantCookie).body().contains("Không tìm thấy yêu cầu"),
                "Status filter must exclude a pending request");
        String roomCode = new RentalService().findRoom(roomId).getRoomCode();
        require(get(baseUrl + "/tenant/requests?q=" + URLEncoder.encode(roomCode, StandardCharsets.UTF_8), tenantCookie)
                .body().contains("requestId=" + requestId), "Search must match the actual room code");
        String requestCode = String.format(java.util.Locale.ROOT, "YC%03d", Integer.parseInt(requestId));
        require(get(baseUrl + "/tenant/requests?q=" + requestCode, tenantCookie).body().contains("requestId=" + requestId),
                "Request code search must find the matching request");
        require(get(baseUrl + "/tenant/requests?status=INVALID", tenantCookie).statusCode() == 400,
                "Invalid status must be rejected on the server");
        require(get(baseUrl + "/tenant/requests?q=" + "a".repeat(101), tenantCookie).statusCode() == 400,
                "Long search input must be rejected on the server");
        HttpResponse<String> duplicate = post(baseUrl, tenantCookie, fields);
        require(duplicate.statusCode() == 200 && duplicate.body().contains("đã có một yêu cầu đang chờ duyệt"),
                "Sequential duplicate acceptance criterion");
        System.out.println("PASS: PRG, one-time flash, PENDING list, UTF-8, stored XSS, session ownership and duplicate rejection");
        System.out.println("PASS: request details ownership, status filter, room search and server-side filter validation");
    }

    private static HttpResponse<String> get(String url, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).GET();
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> post(String baseUrl, String cookie, Map<String, String> fields) throws Exception {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8));
        }
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + "/tenant/requests"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String location(HttpResponse<?> response) {
        return response.headers().firstValue("Location").orElse("");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

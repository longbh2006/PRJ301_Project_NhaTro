# NT-36 — Duyệt / từ chối yêu cầu thuê (UC09)

Yêu cầu gốc: [NT-36](https://cuongthai.com/work/prj301-nha-tro-sinh-vien/NT/list?issue=36).
Đối chiếu schema ở [Docs 3](https://cuongthai.com/work/prj301-nha-tro-sinh-vien/NT/docs/3)
và bảng URL ở [Docs 4](https://cuongthai.com/work/prj301-nha-tro-sinh-vien/NT/docs/4).

## Sử dụng

- `GET /manager/requests`: danh sách PENDING, lọc tùy chọn bằng `roomId` (ID số của phòng).
- `POST /manager/requests`, `action=approve`: gửi `requestId`, `deposit`,
  `electricIndex`, `waterIndex`, `csrfToken`; `roomId` chỉ giữ bộ lọc khi quay về.
- `POST /manager/requests`, `action=reject`: gửi `requestId`, `csrfToken`;
  không cần nhập tiền cọc hoặc chỉ số điện nước.
- Thành công redirect về GET (PRG), thông báo trong session chỉ hiển thị một lần.
  Dữ liệu không hợp lệ trả 400 và giữ các giá trị đã nhập; yêu cầu đã xử lý trả 409.

Controller đăng nhập cần đặt **Integer `accountId`** và chuỗi **`role=MANAGER`**
trong session sau khi xác thực. Servlet từ chối cả GET và POST nếu thiếu session,
thiếu ID hoặc sai vai trò. Service kiểm tra lại tài khoản quản lý còn hoạt động
trong CSDL trước khi ghi. ID quản lý, người thuê và giá phòng không lấy từ form.
POST cần token CSRF sinh ở server. JSP escape tên người thuê, mã phòng và ghi chú.

## Giao dịch và cạnh tranh

Service mở một RentalDAO, dùng connection kế thừa của DAO cho toàn bộ giao dịch:

1. Tắt auto-commit, kiểm tra quản lý, đọc request và khóa dòng Room bằng
   `UPDLOCK, HOLDLOCK`. Đọc lại request sau khi lấy khóa để phát hiện yêu cầu cũ.
2. Kiểm tra trạng thái phòng, hợp đồng ACTIVE, trạng thái PENDING và sức chứa.
3. Đổi request sang APPROVED bằng cập nhật có điều kiện PENDING, ghi thời gian
   và ID người quyết định.
4. Tạo Contract ACTIVE: giá thuê lấy từ giá phòng hiện tại, tiền cọc do quản lý
   nhập (cho phép 0), chỉ số điện nước là số nguyên không âm. Ngày bắt đầu lấy từ
   request, ngày kết thúc bằng ngày bắt đầu cộng số tháng yêu cầu.
5. Từ chối những request còn PENDING khác cùng phòng, ghi thông tin quyết định.
6. Commit; bất kỳ lỗi nào trước commit đều rollback. DAO đóng connection khi
   kết thúc phạm vi sử dụng. Connection truyền cho `insertContract` không bị
   phương thức đó đóng, commit hoặc rollback.

Khóa phòng giúp các thao tác duyệt cùng phòng chạy theo cùng thứ tự khóa,
tránh việc hai transaction giữ hai request khác nhau rồi chờ nhau khi cập nhật
các request còn lại. Cập nhật có điều kiện ngăn thao tác duyệt hoặc từ chối từ
ghi đè một quyết định đã có.

**Vì sao SELECT trước chưa đủ?** Hai transaction có thể cùng đọc thấy chưa có
hợp đồng ACTIVE trước khi bên nào ghi. Vì vậy schema NT-22 phải có hai chỉ mục
UNIQUE có lọc `UX_Contract_ActiveRoom` và `UX_Contract_ActiveTenant`, áp dụng cho
trạng thái ACTIVE. Chỉ mục là ràng buộc cuối cùng ngay cả khi một luồng khác
không lấy khóa phòng. Lỗi SQL Server 2601/2627 được rollback và chuyển thành
thông báo phòng vừa được thuê hoặc người thuê đã có hợp đồng. Lỗi khác không
bị gán nhầm thành xung đột; lỗi deadlock 1205 có thông báo yêu cầu thử lại.

## Kiểm tra trực tiếp

Cần JDK 17, Tomcat 10.1, driver SQL Server và cấu hình
`web/WEB-INF/ConnectDB.properties` của DBContext cũ. Không commit cấu hình.
Giữ DBContext và EncodingFilter nguyên trạng.

Build và triển khai chính ứng dụng theo [hướng dẫn VS Code](vscode-tomcat.md).
Kiểm tra giao diện hoặc HTTP của chức năng thật; không tạo file demo, test,
servlet thử hoặc ứng dụng kiểm thử riêng theo AGENTS.md.

Sau khi tích hợp đăng nhập, dùng tài khoản quản lý để kiểm tra:

1. Mở `/manager/requests`, lọc theo phòng, kiểm tra danh sách và trạng thái rỗng.
2. Duyệt một request PENDING; xác nhận Contract ACTIVE có giá thuê hiện tại,
   tiền cọc, chỉ số điện nước, ngày bắt đầu/kết thúc và người quyết định đúng.
   Các request PENDING khác cùng phòng phải REJECTED.
3. Tải lại trang sau thành công: không gửi lại POST; thông báo chỉ hiển thị một lần.
4. Gửi lại quyết định cũ hoặc mở hai phiên quản lý duyệt hai request cùng phòng:
   chỉ có một Contract ACTIVE, bên còn lại nhận thông báo xung đột.
5. Từ chối một request PENDING: không tạo hợp đồng; không cho thay đổi lại quyết định.
6. Kiểm tra dữ liệu sai, CSRF, ghi chú chứa HTML và quyền truy cập của khách/TENANT.

Kết quả lịch sử ngày **29/10/2026**, trước khi chốt quy tắc kiểm tra trực tiếp:
8 nhóm kiểm tra SQL Server và 19 kiểm tra HTTP đã đạt, gồm rollback, UNIQUE,
ba lần duyệt đồng thời, dữ liệu sai, phân quyền, XSS, CSRF và PRG.
Các file kiểm thử riêng đã được loại bỏ theo quy tắc mới. Không coi các kết quả
này là bằng chứng đã kiểm tra lại toàn bộ giao diện sau khi tích hợp chức năng khác.

## Điểm tích hợp còn phụ thuộc các chức năng khác

Source gốc hiện chưa có controller đăng nhập, AuthFilter hoặc trang danh sách
phòng UC01. NT-36 không thêm các chức năng này. Khi ghép UC03, dùng đúng hợp đồng
session ở trên; giữ lớp kiểm tra quyền ở servlet ngay cả khi có AuthFilter.

Danh sách phòng trống UC01 cần chọn phòng ACTIVE **không có Contract ACTIVE**.
Không đổi Room.status sang giá trị mới như OCCUPIED: schema chỉ cho ACTIVE,
MAINTENANCE, HIDDEN. Kiểm tra CSDL trước đó đã xác nhận sau khi duyệt, phòng không còn thỏa điều
kiện trống trong CSDL. Kiểm tra trực quan phòng biến mất trên UC01 cần thực hiện
sau khi ghép source UC01. Hai chỉ mục UNIQUE thuộc schema NT-22; nhánh này không
tự thay schema, DBContext hoặc cấu hình filter.

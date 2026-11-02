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

Controller đăng nhập đặt đối tượng `account`, **Integer `accountId`** và chuỗi
**`role=MANAGER`** trong session sau khi xác thực. AuthFilter chuyển khách chưa
đăng nhập tới `/login?next=/manager/requests`, chặn TENANT bằng 403.
Servlet kiểm tra lại cả GET và POST nếu thiếu session,
thiếu ID hoặc sai vai trò. Service kiểm tra lại tài khoản quản lý còn hoạt động
trong CSDL trước khi ghi. ID quản lý, người thuê và giá phòng không lấy từ form.
POST cần token CSRF sinh ở server. JSP hiển thị bảng và escape tên người thuê,
mã phòng, ghi chú. Mapping servlet đặt tại `web/WEB-INF/web.xml`, không dùng annotation.

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

Build và triển khai chính ứng dụng theo [hướng dẫn chạy cục bộ](run-local.md).
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

## Kết quả kiểm tra lại ngày 31/10/2026

Sau khi merge `main`, build Ant thành công với 25 lớp Java, chạy chính ứng dụng
trên Tomcat 10.1 tại `http://127.0.0.1:8082/Project_NhaTro`. Worktree riêng dùng
cấu hình CSDL cục bộ và thư viện NetBeans/JSTL cục bộ; không sửa DBContext,
EncodingFilter, schema hoặc commit cấu hình kết nối.

- Đăng nhập thật bằng tài khoản quản lý, mở bảng: có 3 yêu cầu PENDING.
  Lọc `roomId=3` còn đúng #8 và #9; giao diện hiển thị tiếng Việt đúng.
- Mở hai tab trước khi duyệt. Duyệt #8 với cọc 1.230.000, điện 100, nước 20:
  redirect về danh sách vẫn giữ bộ lọc; tải lại không gửi lại POST và mất thông báo một lần.
- Gửi Duyệt #9 từ tab cũ: hiện **“Phòng vừa được người khác thuê.”**.
  CSDL xác nhận #8 APPROVED, #9 REJECTED, `decidedBy=2` và chỉ một hợp đồng ACTIVE
  của phòng 3: giá thuê 2.500.000, cọc 1.230.000, điện 100, nước 20,
  thời hạn 01/11/2026–01/11/2027.
- Bấm Từ chối #10 mà không nhập cọc/chỉ số: redirect thành công; CSDL xác nhận
  #10 REJECTED, người quyết định 2, không tạo hợp đồng cho #10.
- HTTP thật: lọc phòng âm và POST cọc âm trả 400; POST token CSRF sai trả 403;
  khách bị chuyển tới đăng nhập; TENANT đăng nhập thật truy cập trang quản lý trả 403.
- Đọc metadata CSDL xác nhận đã có `UX_Contract_ActiveRoom` và
  `UX_Contract_ActiveTenant`, đều UNIQUE có lọc `status='ACTIVE'`; schema NT-22
  trong `database/setup.sql` cũng đã khai báo. Không thêm chỉ mục trùng.

Kiểm tra này đã thay đổi dữ liệu mẫu #8, #9, #10 qua chức năng thật như trên;
không đặt lại dữ liệu bằng SQL và không tạo file demo, harness hoặc endpoint thử.
Hai tab dùng cùng tài khoản quản lý, gửi lần lượt; chưa chạy hai tài khoản quản lý
gửi đồng thời, chưa ép lỗi SQL tại INSERT để kiểm tra rollback trong lượt này,
chưa kiểm tra lại XSS hoặc trực quan danh sách phòng trống UC01.

Danh sách phòng trống UC01 đã có trên `main` và lọc phòng ACTIVE không có Contract
ACTIVE. Không đổi Room.status sang OCCUPIED vì schema chỉ cho ACTIVE,
MAINTENANCE, HIDDEN. Giữ kiểm tra quyền trong servlet dù đã có AuthFilter.

## Gỡ xung đột PR #15 ngày 31/10/2026

Gộp `main` vào NT-36: giữ `.gitignore` của NT-36 và thuộc tính session
`fullName` từ `main`. AGENTS.md được Git gộp tự động, giữ cả quy tắc kiểm tra
trực tiếp và quy tắc commit/push; không sửa DBContext hoặc EncodingFilter.

Build Ant thành công với 35 lớp Java. Chạy ứng dụng thật tại cổng 18015:
khách mở `/manager/requests` nhận 302 tới đăng nhập; tài khoản quản lý đăng nhập
nhận 302 về `/home`; danh sách yêu cầu, lọc `roomId=3`, trang chủ và chi tiết
phòng P202 trả 200. Lọc `roomId=-1` trả 400; POST token CSRF sai trả 403.
Không duyệt/từ chối yêu cầu hoặc thay đổi dữ liệu nghiệp vụ trong lượt này.

# NT-48 — UC11 Quản lý hợp đồng

## Sử dụng

- `GET /manager/contracts`: lọc tùy chọn bằng `status` (`ACTIVE`, `ENDED`),
  `roomId` (ID phòng), `tenantName` (tìm chứa tên), `page` (mặc định 1).
- Mỗi trang tối đa 10 hợp đồng, sắp xếp `contractId DESC`. DAO đếm tổng và
  lấy trang bằng hai truy vấn có cùng điều kiện. Trang vượt tổng được đưa
  về trang cuối; số trang không dương, không nguyên hoặc vượt giới hạn int trả 400.
- Mẫu phân trang dùng `ORDER BY ... OFFSET ? ROWS FETCH NEXT ? ROWS ONLY`
  theo [tài liệu Microsoft](https://learn.microsoft.com/en-us/sql/t-sql/queries/select-order-by-clause-transact-sql).
- Ký tự `%`, `_`, `[` và `\` người dùng nhập được escape trước khi truyền
  vào `LIKE`. DAO tự thêm `%` hai đầu, dùng tham số `PreparedStatement`.
- `POST /manager/contracts`: `action=end`, `contractId`, `csrfToken`.
  Các trường bộ lọc và số trang chỉ giữ vị trí khi tải lại/redirect.
- ID quản lý lấy từ `accountId` trong session đăng nhập, vai trò `MANAGER`;
  service kiểm tra lại tài khoản quản lý còn hoạt động trong CSDL trước khi ghi.
- Thành công redirect GET, thông báo session hiển thị một lần. Không hợp lệ
  trả 400; hợp đồng đã kết thúc hoặc còn nợ trả 409 và giữ bộ lọc.
  Thiếu/sai CSRF trả 403. URL đăng ký tập trung trong `web.xml`.

## Hóa đơn và giao dịch

Trên `main` tại commit `8fcd777` đã có model `Invoice` và bảng `Invoice`.
Nguồn xác nhận trạng thái là schema, không phải một hằng chuỗi sẵn có trong model:

- `database/setup.sql`: `Invoice.status` NOT NULL, DEFAULT `UNPAID`,
  `CK_Invoice_status` chỉ cho `UNPAID`/`PAID`; `CK_Invoice_paidAt` ràng buộc
  thời điểm thanh toán tương ứng. Quá hạn được tính từ `dueDate`, không phải
  một trạng thái khác.
- `database/queries-mau.sql`, Q12/Q14d: công nợ lọc `UNPAID`; luồng tạo hóa đơn
  mẫu ghi `UNPAID`. `BillingDAO.insertInvoice` trên nhánh NT-27 không truyền
  cột status, nên nhận DEFAULT từ CSDL.
- Đọc trực tiếp CSDL đang dùng ngày 31/10/2026: DEFAULT là `('UNPAID')`,
  `CK_Invoice_status` và `CK_Invoice_paidAt` đều có `is_disabled=0`,
  `is_not_trusted=0`. Có 6 hóa đơn PAID (18.116.000 đồng), 2 UNPAID
  (8.364.000 đồng). Không có trạng thái khác.

NT-48 bổ sung `Invoice.STATUS_UNPAID`/`STATUS_PAID` theo schema này. DAO đọc
tổng hóa đơn theo **mọi trạng thái** của hợp đồng, thay vì chỉ lọc UNPAID.
Service từ chối nếu gặp trạng thái chưa hỗ trợ (kể cả null), rồi mới kiểm tra
tổng UNPAID. Vì vậy nếu quy ước/schema của NT-37 thay đổi, một trạng thái lạ
không bị bỏ qua rồi hiểu nhầm là hợp đồng hết nợ. Chưa xác nhận implementation
NT-37 vì nhánh đó không có trong các remote refs hiện tại.

Service giữ một connection kế thừa của `ContractDAO`, tắt auto-commit và:

1. Kiểm tra quản lý còn hoạt động, tìm phòng của hợp đồng.
2. Khóa Room bằng `UPDLOCK, HOLDLOCK` trước khi khóa Contract.
   Đọc lại trạng thái; chỉ chấp nhận `ACTIVE`.
3. Đọc tổng `Invoice.total` theo trạng thái bằng `BigDecimal`, với
   `UPDLOCK, HOLDLOCK` trên tập hóa đơn của hợp đồng. Service từ chối trạng thái
   chưa hỗ trợ. Tổng UNPAID lớn hơn 0 thì từ chối và báo đúng số tiền.
4. Cập nhật có điều kiện `ACTIVE` → `ENDED`, ghi `endedAt = SYSDATETIME()`.
5. Commit; lỗi trước commit thì rollback. DAO chỉ truy vấn và đóng connection
   do chính DAO mở, không tự commit/rollback.

Schema `database/setup.sql` xác định phòng có người thuê bằng Contract ACTIVE,
không có trạng thái `VACANT`/`OCCUPIED` trong Room. Kết thúc hợp đồng đồng thời
giải phóng phòng theo phép suy ra này, trong cùng transaction. Giữ nguyên
`Room.status` để không mở lại phòng đang `MAINTENANCE` hoặc `HIDDEN`.
Luồng lập hóa đơn sau này vẫn cần kiểm tra hợp đồng còn ACTIVE trong giao dịch
của chính nó trước khi tạo hóa đơn mới.

## Phạm vi đối chiếu khóa NT-36

Đã đọc toàn bộ `RentalService.approve` và các DAO được gọi trên
`origin/nt-36-manager-request-approval` tại commit `88707ed`:

1. Tắt auto-commit; đọc Account quản lý, đọc RentalRequest.
2. `lockRoomForApproval` lấy `UPDLOCK, HOLDLOCK` trên Room và JOIN RoomType.
3. Đọc lại RentalRequest; tìm Contract ACTIVE của phòng.
4. UPDATE RentalRequest được duyệt, INSERT Contract, UPDATE các yêu cầu PENDING
   khác cùng phòng; commit hoặc rollback.

NT-48 đọc Account bằng HOLDLOCK và tìm roomId trước, sau đó lấy khóa cập nhật
Room → Contract → Invoice rồi UPDATE Contract. Hai luồng cùng lấy khóa cập nhật
Room trước khi thay đổi Contract của phòng đó. Đây là điểm tương thích đã
đối chiếu; **không khẳng định toàn bộ chuỗi khóa giống nhau hoặc không thể
deadlock**. NT-36 còn chạm RentalRequest/RoomType và các khóa do FK/chỉ mục.
Chưa chạy cạnh tranh giữa duyệt thuê và kết thúc vì người dùng chỉ cho phép
kiểm tra không thay đổi dữ liệu. NT-36 cũng chưa được merge vào main hiện tại.

## Kết quả kiểm tra trực tiếp ngày 31/10/2026

Build 35 source Java và WAR bằng Ant/JDK 17 thành công. Chạy chính ứng dụng
bằng `scripts/run-local.ps1` ở `http://127.0.0.1:8090/Project_NhaTro`, kết nối
SQL Server bằng cấu hình cục bộ hiện có. Không sửa DBContext/EncodingFilter,
không tạo test, harness hay endpoint thử.

Worktree mới cần cấu hình build NetBeans đang có trên máy:
`user.properties.file=C:/Users/Khoa/AppData/Roaming/NetBeans/17/build.properties`.
Hai tham chiếu JSTL 2.0 cũ trong project được override khi chạy bằng hai JAR
JSTL 3.0 thực tế ở `web/WEB-INF/lib`; không thay cấu hình project trong NT-48.
File `ConnectDB.properties` chỉ sao chép cục bộ, vẫn bị Git bỏ qua.

Đã kiểm tra HTTP qua đăng nhập thật:

- Danh sách 6 hợp đồng hiển thị đúng tên phòng/người thuê; ACTIVE có 5,
  ENDED có 1. Lọc roomId=2 trả HĐ-3; tên `An`, `Nguyễn`, `Bình` trả đúng dữ liệu.
- Tìm `%`, `_`, `[`, `\` và chuỗi SQL injection trả tập rỗng; chuỗi HTML được
  escape trong ô tìm kiếm. Các truy vấn không bị mở rộng bởi wildcard nhập tay.
- Trang 2147483647 về trang 1 vì dữ liệu chỉ có một trang.
- Trang 0, âm, chữ, vượt giới hạn int; mã phòng không hợp lệ;
  trạng thái ngoài danh sách và tên dài 101 ký tự đều trả 400.
- POST kết thúc HĐ-3 trả 409, báo chính xác **3.410.000 đồng** chưa thanh toán.
  Bộ lọc ACTIVE/phòng 2/tên Bình được giữ nguyên. Sau từ chối, HĐ-3 vẫn ACTIVE,
  phòng 2 vẫn không xuất hiện trong danh sách phòng trống.
- HĐ-1 đã ENDED trả 409; ID không tồn tại, ID 0 và action không hỗ trợ trả 400.
  CSRF sai trả 403. Gửi managerId giả trong form không thay ID quản lý từ session.
- TENANT và ADMIN đều bị từ chối GET/POST bằng 403; khách GET chuyển đăng nhập.

Chưa kiểm tra: POST kết thúc thành công/PRG/thông báo một lần, phòng xuất hiện
trở lại sau khi kết thúc, rollback sau lỗi ghi và cạnh tranh đồng thời. Các ca
này cần thay đổi dữ liệu hợp đồng hiện có; không thực hiện theo lựa chọn
chỉ kiểm tra không đổi dữ liệu của người dùng.
Chưa kiểm tra chuyển trang có dữ liệu vì chỉ có 6 hợp đồng, nhỏ hơn page size 10.
Kiểm tra giao diện thực hiện bằng HTML trả qua HTTP, chưa kiểm tra trực quan
trên trình duyệt.

## Kiểm tra lại sau phản hồi review

Người dùng đã xác nhận chỉ kiểm tra không đổi dữ liệu. Không kết thúc HĐ-2,
không thêm hóa đơn hoặc sửa schema để dựng trường hợp thử.

- Đã chạy `clean dist` của chính `build.xml`, bằng Ant đi kèm NetBeans 17,
  JDK 17 và các override thư viện ở trên. Kết quả: `BUILD SUCCESSFUL`,
  biên dịch 35 file Java, tạo WAR mới. Sau đó chạy ứng dụng bằng script hiện có.
- Chưa bấm Clean and Build trong cửa sổ NetBeans: kết nối điều khiển Windows
  trả `Computer Use native pipe is unavailable`. Kết quả Ant CLI không được
  coi là đã thao tác giao diện NetBeans.
- HTTP qua đăng nhập quản lý thật: danh sách và bộ lọc kết hợp trả 200;
  POST HĐ-3 trả 409 với nợ **3.410.000 đồng**, HĐ-4 trả 409 với nợ
  **4.954.000 đồng**. HĐ-3 có cả hóa đơn PAID và UNPAID nên cũng kiểm tra được
  việc tiền PAID không cộng vào công nợ.
- Hợp đồng đã ENDED trả 409; CSRF sai trả 403; trạng thái lọc sai và page=0
  trả 400. GET kiểm tra lại xác nhận HĐ-2 còn ACTIVE.
- Chưa chạy nhánh từ chối trạng thái hóa đơn lạ qua HTTP vì CHECK hiện tại
  không cho lưu dữ liệu đó và không được phép đổi schema/dữ liệu để tạo nó.
  Chưa chạy thành công/PRG, rollback sau lỗi ghi hoặc cạnh tranh đồng thời.

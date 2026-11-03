# NT-42 — OwnershipFilter chặn IDOR

## IDOR là gì

IDOR (Insecure Direct Object Reference) là lỗi để người dùng đổi id trên URL
và đọc dữ liệu của người khác. Ví dụ, người thuê A mở hoá đơn của mình tại
`/tenant/invoices/detail?id=5`, sau đó đổi thành `?id=11`. Nếu server chỉ kiểm
tra "đã đăng nhập, đúng vai trò TENANT" thì A sẽ xem được hoá đơn của B.
Kiểm tra vai trò chưa đủ; server còn phải kiểm tra **bản ghi này có phải của
người đang đăng nhập không**.

## Chặn ở 3 lớp

| Lớp | Vị trí | Kiểm tra gì | Khi sai |
| --- | --- | --- | --- |
| 1 | `AuthorizationFilter` (`/*`) | Đã đăng nhập và có vai trò `TENANT` | Chuyển tới `/login` hoặc trả 403 |
| 2 | `OwnershipFilter` (3 URL bên dưới) | Lấy id trên URL/form và tìm `Invoice`/`MaintenanceRequest` → `Contract.tenantId`. Giá trị này phải bằng `accountId` trong session | 404 |
| 3 | DAO: `InvoiceDAO.findByIdForTenant`, `PaymentDAO.findInvoiceForTenant`/`insertPending`, `MaintenanceDAO.findRequestByIdForTenant` | Câu SELECT/INSERT luôn có `AND c.tenantId = ?` (id lấy từ session) | Không có dòng nào → servlet trả 404 |

URL được bảo vệ, kèm tham số id mà filter đọc:

| URL | Trang thuộc | Tham số |
| --- | --- | --- |
| `/tenant/invoices/detail` | NT-38 | `id` (hoá đơn) |
| `/tenant/pay` (GET và POST) | NT-44 | `invoiceId` (hoá đơn) |
| `/tenant/issues/detail` | NT-42 | `id` (sự cố) |

Trong `web.xml`, `OwnershipFilter` được map sau `AuthFilter`, vì vậy filter này
chỉ chạy khi đã chắc chắn người dùng là TENANT. Trang hợp đồng `/tenant/contract`
không nhận id trên URL. Trang này chỉ đọc hợp đồng theo `accountId` trong
session, nên không có tham số để sửa. Các đường dẫn `/payment/ipn` và
`/payment/return` do VNPay gọi về, không có session người thuê, nên không thuộc
filter.

**Vì sao trả 404 thay vì 403?** 403 cho kẻ dò biết "id này có tồn tại nhưng không
phải của bạn". Khi dùng 404, id của người khác, id không tồn tại (`999`), id sai
định dạng (`abc`) và thiếu id đều nhận cùng một trang. Nhóm đã so nội dung HTML
của các phản hồi này và thấy giống nhau từng byte.

## Kịch bản thử

Kiểm tra trực tiếp ngày 03/11/2026 trên bản đã gộp `main` (gồm NT-37, NT-38,
NT-44). Ứng dụng được build bằng `scripts/run-local.ps1` (Tomcat 10.1, cổng 8080,
SQL Server, VNPay cổng giả). Mật khẩu mẫu `NhaTro@2026`. Dữ liệu là CSDL đang
dùng chung của nhóm. CSDL đã có thêm hoá đơn tháng 10 do NT-37 lập, nên id hoá
đơn khác với `database/setup.sql`.

- **Người thuê A** = `an.nguyen` (phòng P101): hoá đơn 3, 5, 9 (đã trả) và sự cố 2.
- **Người thuê B** = `binh.tran` (phòng P102): hoá đơn 4, 6 (đã trả), 11 (chưa trả) và sự cố 4.

| Ca | Tài khoản | Thao tác | Kết quả mong đợi | Thực tế | Ảnh |
| --- | --- | --- | --- | --- | --- |
| IDOR-01 | A | `/tenant/invoices/detail?id=5` (hoá đơn của A) | 200, INV005 phòng P101, tổng 2.293.000 ₫ | Đạt | [Ảnh](screenshots/NT42-01-A-own-invoice.png) |
| IDOR-02 | B | `/tenant/pay?invoiceId=11` (hoá đơn của B) | 200, hoá đơn #11 chưa trả, có nút VNPay. Xác nhận id 11 có tồn tại | Đạt | [Ảnh](screenshots/NT42-02-B-own-unpaid-pay.png) |
| IDOR-03 | A | Đổi URL thành `/tenant/invoices/detail?id=11` | 404 | Đạt | [Ảnh](screenshots/NT42-03-A-idor-invoice-404.png) |
| IDOR-04 | A | `/tenant/pay?invoiceId=5` (hoá đơn của A) | 200, báo đã thanh toán | Đạt | [Ảnh](screenshots/NT42-04-A-own-pay.png) |
| IDOR-05 | A | Đổi URL thành `/tenant/pay?invoiceId=11` | 404 | Đạt | [Ảnh](screenshots/NT42-05-A-idor-pay-404.png) |
| IDOR-06 | A | `/tenant/issues/detail?id=2` (sự cố của A) | 200, SC002 phòng P101 | Đạt | [Ảnh](screenshots/NT42-06-A-own-issue.png) |
| IDOR-07 | A | Đổi URL thành `/tenant/issues/detail?id=4` (sự cố của B) | 404 | Đạt | [Ảnh](screenshots/NT42-07-A-idor-issue-404.png) |
| IDOR-08 | A | `/tenant/invoices/detail?id=999` (không tồn tại) | 404, HTML giống hệt IDOR-03 | Đạt | [Ảnh](screenshots/NT42-08-A-nonexistent-404.png) |
| IDOR-09 | B | `/tenant/invoices/detail?id=5`, `/tenant/pay?invoiceId=5`, `/tenant/issues/detail?id=2` | 404 cả ba (chiều ngược lại) | Đạt | — |
| IDOR-10 | A | `id=abc`, thiếu id, `/tenant/pay?id=5` (sai tên tham số) | 404 | Đạt | — |
| IDOR-11 | A | POST `/tenant/pay` với `invoiceId=11` (hoá đơn của B) | 404, không tạo giao dịch | Đạt | — |
| IDOR-12 | A | POST `/tenant/pay` với `invoiceId=5` (của A, đã trả) | Filter cho qua; servlet từ chối và redirect về `/tenant/pay?invoiceId=5` | Đạt | — |
| IDOR-13 | Khách | `/tenant/pay?invoiceId=5` | 302 tới `/login?next=/tenant/pay` (lớp 1) | Đạt | — |
| IDOR-14 | `quanly` | `/tenant/pay?invoiceId=5` | 403 (lớp 1) | Đạt | — |
| IDOR-15 | A | **Tạm gỡ** mapping `OwnershipFilter` rồi gọi lại IDOR-03, 05, 07, 11 | Vẫn 404 nhờ lớp 3; trang của A vẫn 200 | Đạt | — |

IDOR-15 chỉ đổi `web.xml` trên máy để kiểm tra riêng lớp 3. Nhóm build lại và
chạy các ca, sau đó khôi phục `web.xml`, build lại và chạy lại IDOR-01 đến
IDOR-07. Commit giữ nguyên mapping đầy đủ.

Sau khi gộp, nhóm cũng mở lại các trang chính của mọi vai trò và nhận 200:
trang chủ, danh sách phòng, đăng nhập, `/tenant/*`, `/manager/*` và `/admin/*`.

Ảnh chỉ chụp phần nội dung trang bằng Chrome headless, không có thanh URL. URL
và tài khoản của từng ảnh được ghi trong bảng trên. Ảnh 404 của IDOR-03, 05, 07
và 08 giống nhau vì server trả cùng một trang.

## Chưa kiểm tra

- Luồng trả tiền trọn vẹn qua VNPay (POST hoá đơn chưa trả → cổng giả → IPN).
  Luồng này ghi dữ liệu thật vào CSDL dùng chung. IDOR-12 đã xác nhận filter
  cho POST hợp lệ đi tiếp tới servlet.

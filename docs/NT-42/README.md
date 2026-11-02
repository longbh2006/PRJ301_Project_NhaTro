# NT-42 — OwnershipFilter chặn IDOR

## IDOR là gì

IDOR (Insecure Direct Object Reference) là lỗi để người dùng đổi id trên URL
và đọc dữ liệu của người khác. Ví dụ, người thuê A mở hoá đơn của mình tại
`/tenant/invoices/detail?id=5`, sau đó đổi thành `?id=6`. Nếu server chỉ kiểm
tra "đã đăng nhập, đúng vai trò TENANT" thì A sẽ xem được hoá đơn của B.
Kiểm tra vai trò chưa đủ; server còn phải kiểm tra **bản ghi này có phải của
người đang đăng nhập không**.

## Chặn ở 3 lớp

| Lớp | Vị trí | Kiểm tra gì | Khi sai |
| --- | --- | --- | --- |
| 1 | `AuthorizationFilter` (`/*`) | Đã đăng nhập và có vai trò `TENANT` | Chuyển tới `/login` hoặc trả 403 |
| 2 | `OwnershipFilter` (3 URL bên dưới) | Lấy `id` trên URL và tìm `Invoice`/`MaintenanceRequest` → `Contract.tenantId`. Giá trị này phải bằng `accountId` trong session | 404 |
| 3 | DAO: `BillingDAO.findInvoiceByIdForTenant`, `MaintenanceDAO.findRequestByIdForTenant` | Câu SELECT luôn có `AND c.tenantId = ?` (id lấy từ session) | Không có dòng nào → servlet trả 404 |

URL được bảo vệ: `/tenant/invoices/detail`, `/tenant/pay` (id hoá đơn) và
`/tenant/issues/detail` (id sự cố). Trong `web.xml`, `OwnershipFilter` được map
sau `AuthFilter`, vì vậy filter này chỉ chạy khi đã chắc chắn người dùng là TENANT.
Trang hợp đồng `/tenant/contract` không nhận id trên URL. Trang này chỉ đọc hợp
đồng theo `accountId` trong session, nên không có tham số để sửa.

**Vì sao trả 404 thay vì 403?** 403 cho kẻ dò biết "id này có tồn tại nhưng không
phải của bạn". Khi dùng 404, id của người khác, id không tồn tại (`999`), id sai
định dạng (`abc`) và thiếu id đều nhận cùng một trang. Nhóm đã so nội dung HTML
của các phản hồi này và thấy giống nhau từng byte.

Ba trang được viết tối thiểu để chạy kịch bản: xem chi tiết hoá đơn, xác nhận
thanh toán (chưa nối VNPay) và xem chi tiết sự cố. Mã sự cố trong
`/tenant/issues` có liên kết sang trang chi tiết.

## Kịch bản thử

Kiểm tra trực tiếp ngày 02/11/2026 trên ứng dụng thật. Ứng dụng được build bằng
`scripts/run-local.ps1 -Port 8091` (Tomcat 10.1, SQL Server dùng dữ liệu của
`database/setup.sql`). Mật khẩu mẫu `NhaTro@2026`.

- **Người thuê A** = `an.nguyen` (accountId 4, hợp đồng 2, phòng P101): hoá đơn 3, 5 và sự cố 2.
- **Người thuê B** = `binh.tran` (accountId 5, hợp đồng 3, phòng P102): hoá đơn 4, 6 và sự cố 4.

| Ca | Tài khoản | Thao tác | Kết quả mong đợi | Thực tế | Ảnh |
| --- | --- | --- | --- | --- | --- |
| IDOR-01 | A | `/tenant/invoices/detail?id=5` (hoá đơn của A) | 200, HD005 phòng P101, tổng 2.293.000 | Đạt | [Ảnh](screenshots/NT42-01-A-own-invoice.png) |
| IDOR-02 | B | `/tenant/pay?id=6` (hoá đơn của B) | 200, HD006 phòng P102, 3.410.000, chưa trả. Xác nhận id 6 có tồn tại | Đạt | [Ảnh](screenshots/NT42-02-B-own-unpaid-pay.png) |
| IDOR-03 | A | Đổi URL thành `/tenant/invoices/detail?id=6` | 404 | Đạt | [Ảnh](screenshots/NT42-03-A-idor-invoice-404.png) |
| IDOR-04 | A | `/tenant/pay?id=5` (hoá đơn của A) | 200, báo đã thanh toán lúc 03/10/2026 08:41 | Đạt | [Ảnh](screenshots/NT42-04-A-own-pay.png) |
| IDOR-05 | A | Đổi URL thành `/tenant/pay?id=6` | 404 | Đạt | [Ảnh](screenshots/NT42-05-A-idor-pay-404.png) |
| IDOR-06 | A | `/tenant/issues/detail?id=2` (sự cố của A) | 200, SC002 phòng P101 | Đạt | [Ảnh](screenshots/NT42-06-A-own-issue.png) |
| IDOR-07 | A | Đổi URL thành `/tenant/issues/detail?id=4` (sự cố của B) | 404 | Đạt | [Ảnh](screenshots/NT42-07-A-idor-issue-404.png) |
| IDOR-08 | A | `/tenant/invoices/detail?id=999` (không tồn tại) | 404, HTML giống hệt IDOR-03 | Đạt | [Ảnh](screenshots/NT42-08-A-nonexistent-404.png) |
| IDOR-09 | B | `/tenant/invoices/detail?id=3`, `/tenant/pay?id=5`, `/tenant/issues/detail?id=2` | 404 cả ba (chiều ngược lại) | Đạt | — |
| IDOR-10 | A | Ba URL với `id=abc`, `id=` | 404 | Đạt | — |
| IDOR-11 | A | `?id=4&id=3`, `?id=%2B4`, `/tenant/pay;x=1?id=4` | 404 (filter và servlet đọc cùng một giá trị `id`) | Đạt | — |
| IDOR-12 | Khách | `/tenant/invoices/detail?id=3` | 302 tới `/login?next=/tenant/invoices/detail` (lớp 1) | Đạt | — |
| IDOR-13 | `quanly` | `/tenant/invoices/detail?id=3` | 403 (lớp 1) | Đạt | — |
| IDOR-14 | A | POST tới `/tenant/issues/detail?id=2` | 405, trang chi tiết chỉ cho xem | Đạt | — |
| IDOR-15 | A | **Tạm gỡ** mapping `OwnershipFilter` rồi gọi lại IDOR-03, 05, 07 | Vẫn 404 nhờ lớp 3; trang của A vẫn 200 | Đạt | — |

IDOR-15 chỉ đổi `web.xml` trên máy để kiểm tra riêng lớp 3. Nhóm build lại và
chạy các ca, sau đó khôi phục `web.xml` (đã so khớp hash với bản gốc), build lại
và chạy lại IDOR-01 đến IDOR-07. Commit giữ nguyên mapping đầy đủ.

Ảnh chỉ chụp phần nội dung trang bằng Chrome headless, không có thanh URL. URL
và tài khoản của từng ảnh được ghi trong bảng trên. Ảnh 404 của IDOR-03, 05, 07
và 08 giống nhau vì server trả cùng một trang.

## Chưa kiểm tra

- Nối VNPay ở `/tenant/pay`: thuộc nhiệm vụ thanh toán riêng.
- Trang danh sách `/tenant/invoices`: chưa có trên `main`, nên chỉ mở được chi
  tiết hoá đơn bằng URL trực tiếp.

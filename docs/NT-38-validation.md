# NT-38 — UC06 Người thuê xem hóa đơn của mình

## Sử dụng

- `GET /tenant/invoices?page=N`: hóa đơn của các hợp đồng thuộc người đang đăng nhập,
  sắp `year DESC, month DESC, invoiceId DESC` (Q15a). Mỗi trang 5 hóa đơn, phân trang
  bằng `nt:pager` (PagerTag). Thiếu `page` = trang 1; `page` không nguyên dương trả 400;
  trang vượt tổng được đưa về trang cuối.
- `GET /tenant/invoices/detail?id=N`: bảng từng khoản (tiền phòng, điện, nước, dịch vụ)
  kèm số lượng, đơn giá của bảng giá đã dùng (`Invoice.priceId`) và chỉ số cũ → mới
  của phòng trong kỳ (Q15b). `id` thiếu/không nguyên dương trả 400.
- Tiền in bằng tag `ui:money` (bọc `fmt:formatNumber`, locale cố định `vi_VN`):
  `1.250.000 ₫`, không phụ thuộc ngôn ngữ trình duyệt.
- Nút "Thanh toán VNPay" chỉ hiện khi hóa đơn `UNPAID`; form trỏ tới `POST /tenant/pay`
  nhưng đang `disabled` cho tới khi UC07 (tuần 4) cài servlet.
- ID người thuê lấy từ `account` trong session; không nhận từ URL/form.
- SQL nằm trong `InvoiceDAO` mới (không sửa `BillingDAO`) để không xung đột với nhánh
  `NT-37-hoa-don` đang viết lại `BillingDAO`/`BillingService`.

## Ghi chú IDOR

Ticket yêu cầu ghi lại rằng sửa `id` trên URL **chưa** bị chặn và sẽ chặn bằng
`OwnershipFilter` ở tuần 4. Bản này đã giữ điều kiện `AND c.tenantId = ?` trong câu chi tiết
đúng như Q15b của `database/queries-mau.sql` (lớp chặn ở DAO), nên:

- Người thuê mở hóa đơn của người khác (hoặc id không tồn tại) nhận **404**, không phân biệt
  hai trường hợp để không lộ id hợp lệ.
- `OwnershipFilter` của tuần 4 **vẫn chưa có**; khi thêm, nó là lớp chặn sớm hơn ở tầng filter,
  không thay thế điều kiện trong DAO.

## Thao tác đã kiểm tra trực tiếp (02/11/2026)

Ứng dụng thật qua `scripts/run-local.ps1` (Tomcat 10.1, JDK 17, DB local `NhaTroSV`), gọi HTTP:

- Chưa đăng nhập: `/tenant/invoices` → 302 về `/login?next=/tenant/invoices`.
- `binh.tran`: danh sách chỉ có hóa đơn hợp đồng P102, kỳ mới nhất trước; định dạng
  `2.500.000 ₫`; INV006 có nhãn "Chưa trả" + "Quá hạn"; menu "Hóa đơn" active.
- Chi tiết INV006 (UNPAID): 170 kWh × 3.800 ₫ = 646.000 ₫, 8 m³ × 18.000 ₫ = 144.000 ₫,
  2 người × 60.000 ₫ = 120.000 ₫, tổng 3.410.000 ₫ khớp DB; có nút VNPay.
  Chi tiết INV004 (PAID): có ngày thanh toán, không có nút VNPay.
- `binh.tran` mở id 5, 7, 1 (của người khác) và 999 → 404; id `abc`, `-1`, `0`, thiếu,
  `99999999999` → 400. `page=abc`, `page=0` → 400; `page=99` → 200, trang cuối.
- `an.nguyen` mở id 5 (của mình) → 200, id 6 (của Bình) → 404.
- Tài khoản MANAGER mở `/tenant/invoices/detail` → 403 (AuthorizationFilter).

## Chưa kiểm tra

- Phân trang nhiều trang: DB mẫu có tối đa 3 hóa đơn/người thuê (< 5/trang) nên chỉ thấy
  một trang; không chèn thêm dữ liệu vào DB dùng chung để thử.
- Chưa xem giao diện trên trình duyệt/điện thoại; chỉ kiểm tra HTML trả về.

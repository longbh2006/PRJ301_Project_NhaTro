# NT-44 — UC07 Thanh toán hoá đơn qua VNPay sandbox

## Luồng

| URL | Ai gọi | Việc làm |
|---|---|---|
| `GET /tenant/pay?invoiceId=N` | người thuê | Xem tóm tắt hoá đơn và nút "Thanh toán qua VNPay". Hoá đơn người khác/không tồn tại trả 404, id sai trả 400. |
| `POST /tenant/pay` (`invoiceId`) | người thuê | Hoá đơn phải của mình + `UNPAID` ⇒ tạo `Payment` PENDING (`txnRef = INV<id>-<yyyyMMddHHmmss>`, số tiền lấy từ `Invoice.total` trong CSDL) ⇒ dựng URL VNPay 2.1.0, ký HMAC-SHA512 ⇒ redirect. Hoá đơn đã trả: redirect về trang GET kèm thông báo. |
| `GET /payment/ipn` | VNPay (server → server) | Kiểm chữ ký ⇒ tìm `Payment` theo `vnp_TxnRef` (khoá `UPDLOCK, HOLDLOCK`) ⇒ kiểm `vnp_Amount = amount × 100` ⇒ đã xử lý rồi thì trả `02` ⇒ ghi `Payment` SUCCESS/FAILED + `Invoice` PAID trong **một** transaction. Trả JSON `{"RspCode","Message"}`: `00` · `01` không thấy đơn · `02` đã xác nhận · `04` sai số tiền · `97` sai chữ ký · `99` lỗi khác. |
| `GET /payment/return` | trình duyệt | Kiểm chữ ký rồi hiện thông báo theo `vnp_ResponseCode` và trạng thái `Payment` trong CSDL. Lần thử còn PENDING thì hỏi querydr; chỉ khi bật `vnp_TrustReturnUrl` mới ghi theo tham số return đã ký (xem cuối file). |
| `/payment/fake-vnpay` | trình duyệt (local) | Cổng VNPay giả, xem bên dưới. Tắt (mặc định) thì trả 404. |

`POST /tenant/pay` nhận `invoiceId` từ query hoặc form, nên form của NT-38
(`invoice-detail.jsp`, trường ẩn `invoiceId`) dùng được ngay khi bỏ `disabled`.

## Cấu hình (không commit)

`TmnCode`, `HashSecret`, `PayUrl`, `ReturnUrl`, `IpnUrl` đọc bởi `util.VnPayConfig`:
biến môi trường `VNPAY_*` được ưu tiên, thiếu thì đọc `web/WEB-INF/VNPay.properties`
(đã thêm vào `.gitignore`). Mẫu: [`docs/VNPay.properties.example`](VNPay.properties.example).
Thiếu TmnCode/HashSecret/ReturnUrl thì không tạo lần thử PENDING và báo lỗi cấu hình.

## Cổng giả khi chạy local

VNPay sandbox không gọi được IPN về `127.0.0.1`. Đặt `vnp_FakeGateway=true`,
`vnp_PayUrl=.../payment/fake-vnpay`, `vnp_IpnUrl=.../payment/ipn` (mẫu ở file example).
Cổng giả làm phần việc của VNPay bằng cùng HashSecret:

1. Kiểm chữ ký URL thanh toán; URL bị sửa ⇒ "Sai chữ ký (mã lỗi 70)".
2. Nút "Thanh toán thành công" (`00`, ngân hàng NCB) hoặc "Hủy giao dịch" (`24`):
   ký bộ tham số kết quả, gọi IPN máy chủ bằng HTTP, hiện phản hồi IPN.
3. "Gửi lại IPN" gọi lại IPN với đúng bộ tham số cũ (giống VNPay retry).
4. "Quay về website" mở ReturnUrl với bộ tham số đã ký.

## Kiểm tra ngày 03/11/2026 (Tomcat 10.1 local, CSDL NhaTroSV, cổng giả bật)

| Thao tác | Kết quả thực tế |
|---|---|
| `binh.tran` mở `/tenant/pay?invoiceId=6` (của mình, UNPAID) | 200, badge "Chưa trả", có nút thanh toán |
| Mở/POST `invoiceId=7` (của `chi.le`) | 404 |
| `invoiceId=abc` | 400 |
| Khách chưa đăng nhập mở `/tenant/pay` | 302 về `/login?next=/tenant/pay` |
| POST `invoiceId=6` | 302 sang cổng với `vnp_Amount=341000000`, `vnp_TxnRef=INV6-20261103081423`; tính lại HMAC-SHA512 bằng `openssl` khớp `vnp_SecureHash` |
| Sửa `vnp_Amount` trên URL cổng / sửa trường ẩn trước khi bấm trả | "Sai chữ ký (mã lỗi 70)", không gọi IPN |
| Bấm "Thanh toán thành công" | IPN `00`; Invoice 6 → PAID, Payment SUCCESS (`NCB`, `00`, có paidAt) |
| "Gửi lại IPN" 2 lần | `02 Order already confirmed` cả hai lần; vẫn chỉ 1 Payment SUCCESS, `paidAt` không đổi |
| Hai IPN **đồng thời** cho hoá đơn 9 (`an.nguyen`) | một `00`, một `02`; 1 dòng SUCCESS |
| Trang return sau khi trả | "VNPay báo giao dịch thành công", "Đã ghi nhận thanh toán", số tiền 3.410.000 VNĐ |
| Sửa `vnp_Amount` trên URL return / tự gõ URL return không có chữ ký đúng | "Chữ ký không hợp lệ", không ghi gì |
| URL return **có chữ ký đúng** báo `00` cho lần thử PENDING của hoá đơn 7 (không có IPN) | Trang hiện "Đang chờ VNPay xác nhận"; Invoice 7 vẫn UNPAID, Payment 8 vẫn PENDING |
| IPN với chữ ký cũ nhưng sửa số tiền | `97` |
| IPN chữ ký đúng nhưng số tiền sai (hoá đơn 7) | `04`, không ghi |
| IPN chữ ký đúng, `txnRef` không tồn tại | `01` |
| "Hủy giao dịch" cho hoá đơn 11 | IPN `00`; Payment FAILED (`24`), Invoice 11 vẫn UNPAID; trang return "Bạn đã hủy giao dịch" |
| POST lại hoá đơn đã PAID | 302 về trang hoá đơn, "Hóa đơn này đã được thanh toán", không tạo Payment mới |
| `vnp_FakeGateway=false` | `/payment/fake-vnpay` trả 404 |

Dữ liệu CSDL local đã đổi sau khi kiểm tra (hoá đơn 6, 9 PAID; Payment 10–12);
chạy lại `database/setup.sql` để về dữ liệu mẫu.

## Sửa sau khi merge vào main (03/11/2026)

Lỗi: sau khi merge NT-38 + NT-44, nút "Thanh toán VNPay" trên `/tenant/invoices/detail`
vẫn mang thuộc tính `disabled` từ NT-38 nên bấm không được. Lần kiểm tra trước chỉ đi
qua trang `/tenant/pay` của NT-44 trên nhánh riêng, chưa đi qua trang chi tiết hoá đơn.

Đã bỏ `disabled` và đổi dòng ghi chú. Kiểm tra lại trên `main` đã merge (có cả
OwnershipFilter của NT-42): `chi.le` → `/tenant/invoices` → chi tiết hoá đơn 7 → nút gửi
`POST /tenant/pay` → 302 sang cổng → "Thanh toán thành công" → IPN `00` → trang return
"Đã ghi nhận thanh toán" → trang chi tiết đổi badge "Đã trả", nút thanh toán biến mất.
Thiếu `VNPay.properties` thì bấm nút sẽ quay về `/tenant/pay` kèm thông báo "Thiếu cấu hình VNPay".

## Chưa kiểm tra

- **Sandbox VNPay thật với thẻ test NCB**: chưa có TmnCode/HashSecret sandbox trên máy này.
  Khi có, đặt vào `VNPay.properties` với `vnp_FakeGateway=false`. IPN sandbox chỉ tới được
  nếu `/payment/ipn` có địa chỉ công khai (khai IPN URL trong trang merchant); nếu không,
  trang return sẽ hiện "Đang chờ VNPay xác nhận" và hoá đơn giữ UNPAID — đúng thiết kế.
- Đọc cấu hình từ biến môi trường `VNPAY_*` (mới kiểm đường đọc file).
- `POST /tenant/pay` chưa có CSRF token: form NT-38 chưa gửi token. POST giả mạo chỉ tạo
  một lần thử PENDING và mở trang VNPay, người dùng vẫn phải tự nhập thẻ/OTP.

## VNPAY-QR (04/11/2026)

### Chọn phương thức

`/tenant/pay` cho chọn phương thức, gửi trong `vnp_BankCode`: **VNPAY-QR** (`VNPAYQR`,
mặc định), thẻ ATM nội địa (`VNBANK`), thẻ quốc tế (`INTCARD`), hoặc để trống cho cổng
tự hiện danh sách. Giá trị khác trả 400. Nút trên trang chi tiết hoá đơn mở trang chọn này.

### QR trên cổng giả local

Chọn VNPAY-QR với `vnp_FakeGateway=true` thì `/payment/fake-vnpay` hiện mã QR (qrcodejs 1.0.0,
file `web/js/qrcode.min.js` khớp SRI cdnjs) trỏ tới `/payment/fake-vnpay/qr?token=…`:

1. Điện thoại quét mã (hoặc bấm link dưới mã) → trang "Xác nhận thanh toán" / "Hủy giao dịch".
2. Xác nhận → cổng giả ký kết quả (`vnp_BankCode=VNPAYQR`, `vnp_CardType=QRCODE`) và gọi IPN
   như VNPay; mỗi vé dùng một lần, hết hạn 15 phút, mã vé ngẫu nhiên 128 bit.
3. Trang máy tính hỏi `/payment/fake-vnpay/qr-status` mỗi 2 giây, xong thì tự về ReturnUrl.

Điện thoại chỉ mở được khi Tomcat nghe trên mạng LAN:
`.\scripts\run-local.ps1 -ListenAddress 0.0.0.0` (Windows Firewall sẽ hỏi cho phép Java).
Địa chỉ trong mã QR tự đổi 127.0.0.1 sang IP LAN của máy; đặt `vnp_FakeQrBaseUrl` nếu dò sai.

| Thao tác (Tomcat local cổng 8081, nghe 127.0.0.1) | Kết quả thực tế |
|---|---|
| `binh.tran` chọn VNPAY-QR, hoá đơn 11 | Cổng giả hiện "Phương thức VNPAY-QR", QR trỏ `http://10.33.99.159:8081/.../qr?token=…`; status `done=false` |
| Mở link QR (không cookie, như điện thoại) → "Xác nhận thanh toán" | IPN `00`; status `done=true, paid=true` kèm ReturnUrl; return "Đã ghi nhận thanh toán"; Payment 25 SUCCESS `VNPAYQR`, Invoice 11 PAID |
| POST lại cùng vé (huỷ) | Không gọi IPN lần hai; vẫn 1 dòng SUCCESS |
| Vé sai / không tồn tại | GET 404, POST 400, status `found=false` |
| `chi.le` chọn VNPAY-QR rồi "Hủy giao dịch" trên máy tính | Về return "Không thành công"; Payment FAILED `24`, Invoice 10 vẫn UNPAID |
| Chọn thẻ ATM (`VNBANK`) | Cổng giả không hiện QR, vẫn hai nút như cũ |

Chưa kiểm tra: quét bằng điện thoại thật qua Wi‑Fi (môi trường kiểm tra không cho mở
Tomcat ra mạng LAN) và vẽ QR trên trình duyệt thật (mới kiểm tra HTML/JS được trả về).
Dữ liệu CSDL local đã đổi: hoá đơn 11 PAID; chạy lại `database/setup.sql` để về dữ liệu mẫu.

### VNPay sandbox thật (TmnCode `9K605ALU`)

- `vnp_BankCode=VNPAYQR` ⇒ VNPay báo "Ngân hàng thanh toán không được hỗ trợ": terminal
  sandbox này chưa bật VNPAY-QR. `VNBANK` hoặc để trống ⇒ trang "Chọn phương thức thanh toán
  (Test)" chỉ có "Thẻ nội địa và tài khoản ngân hàng". Thẻ test: NCB `9704198526191432198`,
  `NGUYEN VAN A`, `07/15`, OTP `123456`.
- IPN không tới được `127.0.0.1`. Code có gọi API querydr khi trang return thấy lần thử còn
  PENDING (chỉ ghi khi checksum phản hồi đúng), nhưng ngày 04/11/2026 sandbox trả
  `{"vnp_ResponseCode":"99","vnp_Message":"Unexpected error"}` cho **mọi** yêu cầu querydr,
  kể cả chữ ký sai hay TmnCode sai, nên chưa xác nhận được đường này.
- Chạy local không tunnel: đặt `vnp_TrustReturnUrl=true` (mặc định `false`). Khi querydr không
  xác nhận được, trang return ghi kết quả từ chính tham số return **đã kiểm chữ ký HashSecret**,
  đúng `vnp_TmnCode`; số tiền và trạng thái PENDING vẫn kiểm như IPN nên tải lại trang không ghi
  lần hai. Điểm yếu: người dùng đóng trình duyệt trước khi VNPay chuyển về thì lần thử giữ
  PENDING. Server thật để `false` và dùng IPN.

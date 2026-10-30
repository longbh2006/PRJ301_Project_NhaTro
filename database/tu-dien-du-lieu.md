# Từ điển dữ liệu — CSDL `NhaTroSV` (PRJ301 · Quản lý nhà trọ sinh viên)

Khớp 100% với `setup.sql` và `erd.mmd` trong cùng thư mục. Dùng thẳng cho **chương 4 báo cáo** (NT-15).
SQL Server 2019/2022, collation `SQL_Latin1_General_CP1_CI_AS`.

**Đọc bảng:** *Null* = cột được để trống không (✗ = bắt buộc có giá trị). Tên ràng buộc ghi trong ngoặc là đúng tên trong `setup.sql`,
khi SQL Server báo lỗi nó in đúng tên này (vd `UX_Contract_ActiveRoom`).

**Kiểu dữ liệu dùng chung**

| Kiểu | Dùng cho | Vì sao |
|---|---|---|
| `INT IDENTITY(1,1)` | khoá chính | SQL Server tự sinh 1, 2, 3… Lấy id vừa sinh bằng `SCOPE_IDENTITY()` / `getGeneratedKeys()`, không dùng `MAX(id)` |
| `NVARCHAR(n)` | chữ tiếng Việt | `VARCHAR` biến "Nguyễn" thành "Nguy?n". INSERT phải viết `N'…'` |
| `VARCHAR(n)` | mã, username, email, trạng thái | chữ không dấu |
| `DECIMAL(12,0)` | tiền VND | chính xác tuyệt đối; `FLOAT` làm tròn sai (0.1 + 0.2 ≠ 0.3) |
| `DATE` / `DATETIME2(0)` | ngày / ngày + giờ (tới giây) | `DATETIME2` chính xác hơn `DATETIME` cũ |
| `BIT` | đúng/sai | 1/0 |

---

## 1. `Account` — tài khoản đăng nhập (mọi vai trò chung MỘT bảng)

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| accountId | INT IDENTITY | ✗ | PK (`PK_Account`) | mã tài khoản |
| username | VARCHAR(30) | ✗ | UNIQUE (`UQ_Account_username`); CHECK dài 4–30 (`CK_Account_username`) | tên đăng nhập |
| passwordHash | VARCHAR(255) | ✗ | | chuỗi băm PBKDF2 `pbkdf2_sha256$600000$<salt Base64>$<hash Base64>` (~90 ký tự). KHÔNG lưu mật khẩu thô |
| fullName | NVARCHAR(100) | ✗ | | họ tên |
| phone | VARCHAR(10) | ✓ | CHECK đúng 10 chữ số (`CK_Account_phone`) | điện thoại |
| email | VARCHAR(100) | ✓ | CHECK dạng `x@y.z` (`CK_Account_email`) | email |
| role | VARCHAR(10) | ✗ | CHECK `ADMIN`/`MANAGER`/`TENANT` (`CK_Account_role`) | vai trò ⇒ AuthorizationFilter dựa vào đây |
| active | BIT | ✗ | DEFAULT 1 | 0 = bị khoá, không cho đăng nhập (UC15) |
| createdAt | DATETIME2(0) | ✗ | DEFAULT `SYSDATETIME()` | lúc tạo |

## 2. `Tenant` — hồ sơ người thuê (1–1 với Account)

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| tenantId | INT | ✗ | PK + FK → `Account.accountId` (`FK_Tenant_Account`) | **PK cũng là FK** ⇒ mỗi tài khoản tối đa 1 hồ sơ |
| idCardNo | VARCHAR(12) | ✗ | UNIQUE (`UQ_Tenant_idCardNo`); CHECK đúng 12 chữ số | số CCCD |
| school | NVARCHAR(100) | ✓ | | trường đang học |
| dateOfBirth | DATE | ✓ | | ngày sinh |
| hometown | NVARCHAR(100) | ✓ | | quê quán |

## 3. `RoomType` — loại phòng (danh mục)

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| roomTypeId | INT IDENTITY | ✗ | PK | |
| name | NVARCHAR(50) | ✗ | UNIQUE (`UQ_RoomType_name`) | "Phòng đơn", "Phòng đôi"… |
| capacity | INT | ✗ | CHECK 1–8 | số người tối đa ⇒ UC04 kiểm `numPeople ≤ capacity` |
| description | NVARCHAR(255) | ✓ | | mô tả |

## 4. `Room` — phòng

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| roomId | INT IDENTITY | ✗ | PK | |
| roomCode | VARCHAR(10) | ✗ | UNIQUE (`UQ_Room_roomCode`) | mã phòng "P101" |
| roomTypeId | INT | ✗ | FK → RoomType (`FK_Room_RoomType`) | loại phòng |
| floor | INT | ✗ | CHECK ≥ 1 | tầng |
| area | DECIMAL(5,1) | ✓ | CHECK > 0 | diện tích m² |
| monthlyPrice | DECIMAL(12,0) | ✗ | CHECK > 0 (`CK_Room_monthlyPrice`) | giá niêm yết **hiện tại** |
| status | VARCHAR(12) | ✗ | DEFAULT `ACTIVE`; CHECK `ACTIVE`/`MAINTENANCE`/`HIDDEN` | mở cho thuê / đang sửa / ẩn (thay cho xoá) |
| imageUrl | VARCHAR(255) | ✓ | | đường dẫn ảnh |
| description | NVARCHAR(500) | ✓ | | mô tả |

> Không có cột "đang có người thuê". **Phòng trống = `status = 'ACTIVE'` và không có hợp đồng ACTIVE** (truy vấn `NOT EXISTS`).

## 5. `UtilityPrice` — bảng giá điện, nước, dịch vụ

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| priceId | INT IDENTITY | ✗ | PK | |
| electricPrice | DECIMAL(12,0) | ✗ | CHECK ≥ 0 | đ/kWh |
| waterPrice | DECIMAL(12,0) | ✗ | CHECK ≥ 0 | đ/m³ |
| serviceFeePerPerson | DECIMAL(12,0) | ✗ | CHECK ≥ 0 | đ/người/tháng |
| effectiveFrom | DATE | ✗ | UNIQUE (`UQ_UtilityPrice_effectiveFrom`) | áp dụng từ ngày này |
| createdAt | DATETIME2(0) | ✗ | DEFAULT | |

> Đổi giá = **thêm dòng mới**, không sửa dòng cũ. Giá dùng cho tháng M = dòng có `effectiveFrom ≤ ngày 1 tháng M`, mới nhất.

## 6. `RentalRequest` — yêu cầu thuê

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| requestId | INT IDENTITY | ✗ | PK | |
| tenantId | INT | ✗ | FK → Tenant | người gửi |
| roomId | INT | ✗ | FK → Room | phòng muốn thuê |
| moveInDate | DATE | ✗ | | ngày dọn vào (≥ hôm nay: kiểm trong Java) |
| months | INT | ✗ | CHECK 1–24 | số tháng thuê |
| numPeople | INT | ✗ | CHECK 1–8 | số người ở |
| note | NVARCHAR(500) | ✓ | | ghi chú của người thuê |
| status | VARCHAR(10) | ✗ | DEFAULT `PENDING`; CHECK 4 giá trị | `PENDING → APPROVED / REJECTED / CANCELLED` |
| createdAt | DATETIME2(0) | ✗ | DEFAULT | lúc gửi |
| decidedAt | DATETIME2(0) | ✓ | xem `CK_RentalRequest_decision` | lúc rời PENDING |
| decidedBy | INT | ✓ | FK → Account (`FK_RentalRequest_DecidedBy`) | quản lý duyệt/từ chối; NULL nếu người thuê tự huỷ |
| decisionNote | NVARCHAR(255) | ✓ | | lý do từ chối |

`CK_RentalRequest_decision`: PENDING ⇒ decidedAt, decidedBy đều NULL · APPROVED/REJECTED ⇒ cả hai có · CANCELLED ⇒ có decidedAt, decidedBy NULL.

## 7. `Contract` — hợp đồng thuê (giao dịch trung tâm)

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| contractId | INT IDENTITY | ✗ | PK | |
| requestId | INT | ✗ | FK → RentalRequest; UNIQUE (`UQ_Contract_requestId`) | 1 yêu cầu ⇒ tối đa 1 hợp đồng |
| tenantId | INT | ✗ | FK → Tenant | người thuê |
| roomId | INT | ✗ | FK → Room | phòng |
| startDate | DATE | ✗ | | ngày bắt đầu |
| endDate | DATE | ✗ | CHECK `endDate > startDate` | ngày hết hạn dự kiến |
| monthlyRent | DECIMAL(12,0) | ✗ | CHECK > 0 | giá thuê **chụp lại lúc ký** |
| deposit | DECIMAL(12,0) | ✗ | DEFAULT 0; CHECK ≥ 0 | tiền cọc |
| numPeople | INT | ✗ | CHECK 1–8 | số người ở ⇒ phí dịch vụ |
| startElectricIndex | INT | ✗ | CHECK ≥ 0 | chỉ số điện lúc nhận phòng (tháng đầu dùng làm chỉ số cũ) |
| startWaterIndex | INT | ✗ | CHECK ≥ 0 | chỉ số nước lúc nhận phòng |
| status | VARCHAR(10) | ✗ | DEFAULT `ACTIVE`; CHECK `ACTIVE`/`ENDED` | |
| createdAt | DATETIME2(0) | ✗ | DEFAULT | lúc duyệt |
| endedAt | DATETIME2(0) | ✓ | `CK_Contract_endedAt`: có ⇔ ENDED | lúc kết thúc |

> `tenantId`, `roomId` lặp lại thông tin của `RentalRequest` **có chủ đích**: chỉ mục có lọc cần `roomId` nằm ngay trên Contract, và JOIN ngắn hơn. Kiểm tra nhanh 4.4 trong setup.sql xác nhận chúng luôn khớp yêu cầu.

## 8. `MeterReading` — chỉ số điện nước cuối tháng của PHÒNG

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| readingId | INT IDENTITY | ✗ | PK | |
| roomId | INT | ✗ | FK → Room | đồng hồ gắn với phòng (không với người) |
| year, month | INT | ✗ | UNIQUE (`roomId, year, month`); CHECK năm 2020–2100, tháng 1–12 | kỳ ghi |
| electricIndex | INT | ✗ | CHECK ≥ 0 | số trên đồng hồ điện (kWh) |
| waterIndex | INT | ✗ | CHECK ≥ 0 | số trên đồng hồ nước (m³) |
| recordedAt | DATETIME2(0) | ✗ | DEFAULT | lúc ghi |

## 9. `Invoice` — hoá đơn tháng

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| invoiceId | INT IDENTITY | ✗ | PK | |
| contractId | INT | ✗ | FK → Contract | hợp đồng nào |
| priceId | INT | ✗ | FK → UtilityPrice | bảng giá đã dùng để tính |
| year, month | INT | ✗ | UNIQUE (`contractId, year, month`) | kỳ hoá đơn: 1 hợp đồng 1 hoá đơn/tháng |
| roomFee | DECIMAL(12,0) | ✗ | ≥ 0 | = `Contract.monthlyRent` |
| electricUsed | INT | ✗ | ≥ 0 | kWh = mới − cũ |
| electricFee | DECIMAL(12,0) | ✗ | ≥ 0 | = electricUsed × electricPrice |
| waterUsed | INT | ✗ | ≥ 0 | m³ |
| waterFee | DECIMAL(12,0) | ✗ | ≥ 0 | = waterUsed × waterPrice |
| serviceFee | DECIMAL(12,0) | ✗ | ≥ 0 | = serviceFeePerPerson × Contract.numPeople |
| total | DECIMAL(12,0) | ✗ | CHECK = tổng 4 khoản (`CK_Invoice_total`) | tổng tiền |
| status | VARCHAR(10) | ✗ | DEFAULT `UNPAID`; CHECK `UNPAID`/`PAID` | chỉ IPN VNPay chuyển sang PAID |
| dueDate | DATE | ✗ | | hạn trả (mặc định ngày 10 tháng sau) |
| createdAt | DATETIME2(0) | ✗ | DEFAULT | lúc lập |
| paidAt | DATETIME2(0) | ✓ | `CK_Invoice_paidAt`: có ⇔ PAID | lúc trả (báo cáo doanh thu theo cột này) |

> **Quá hạn** không phải trạng thái lưu: `status = 'UNPAID' AND dueDate < hôm nay`, tính lúc truy vấn.
> **Công thức (NT-17):** Tổng = tiền phòng + (điện mới − cũ) × giá điện + (nước mới − cũ) × giá nước + phí dịch vụ × số người.
> Chỉ số cũ = chỉ số tháng trước của phòng; tháng đầu của hợp đồng (startDate ≥ ngày 1 của tháng) thì = `startElectricIndex/startWaterIndex`.
> Tiền phòng tính trọn tháng (không chia theo ngày) — giả định đơn giản của nhóm.

## 10. `Payment` — mỗi lần thử thanh toán VNPay

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| paymentId | INT IDENTITY | ✗ | PK | |
| invoiceId | INT | ✗ | FK → Invoice | trả cho hoá đơn nào |
| txnRef | VARCHAR(50) | ✗ | UNIQUE (`UQ_Payment_txnRef`) | `vnp_TxnRef` mình gửi đi, vd `INV7-20261016213000` |
| amount | DECIMAL(12,0) | ✗ | CHECK > 0 | VND thật (gửi VNPay nhân 100) |
| status | VARCHAR(10) | ✗ | DEFAULT `PENDING`; CHECK 3 giá trị | `PENDING → SUCCESS / FAILED` |
| vnpTransactionNo | VARCHAR(20) | ✓ | | mã giao dịch phía VNPay |
| bankCode | VARCHAR(20) | ✓ | | vd `NCB` |
| responseCode | VARCHAR(5) | ✓ | | `vnp_ResponseCode` (`00` OK, `24` khách huỷ, `51` không đủ số dư) |
| createdAt | DATETIME2(0) | ✗ | DEFAULT | lúc chuyển sang VNPay |
| paidAt | DATETIME2(0) | ✓ | `CK_Payment_paidAt`: có ⇔ SUCCESS | lúc IPN xác nhận |

## 11. `MaintenanceRequest` — sự cố

| Cột | Kiểu | Null | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| ticketId | INT IDENTITY | ✗ | PK | |
| contractId | INT | ✗ | FK → Contract | gắn hợp đồng ⇒ biết phòng nào, ai báo |
| title | NVARCHAR(150) | ✗ | | tiêu đề |
| description | NVARCHAR(1000) | ✓ | | mô tả |
| status | VARCHAR(12) | ✗ | DEFAULT `OPEN`; CHECK 3 giá trị | `OPEN → IN_PROGRESS → DONE` |
| managerNote | NVARCHAR(500) | ✓ | | ghi chú quản lý (người thuê xem được) |
| createdAt | DATETIME2(0) | ✗ | DEFAULT | lúc báo |
| resolvedAt | DATETIME2(0) | ✓ | `CK_MaintenanceRequest_resolved`: có ⇔ DONE | lúc xong |

---

## Quan hệ

| Quan hệ | Kiểu | Cột khoá ngoại | Đọc là |
|---|---|---|---|
| Account – Tenant | **1–1** (0..1) | `Tenant.tenantId` (PK = FK) | một tài khoản TENANT có đúng một hồ sơ |
| RoomType – Room | 1–n | `Room.roomTypeId` | một loại có nhiều phòng |
| Tenant – RentalRequest | 1–n | `RentalRequest.tenantId` | một người gửi nhiều yêu cầu (qua thời gian) |
| Room – RentalRequest | 1–n | `RentalRequest.roomId` | một phòng được nhiều người xin |
| Account – RentalRequest | 1–n | `RentalRequest.decidedBy` | một quản lý duyệt nhiều yêu cầu |
| RentalRequest – Contract | **1–1** (0..1) | `Contract.requestId` UNIQUE | một yêu cầu thành tối đa một hợp đồng |
| Tenant – Contract | 1–n | `Contract.tenantId` | |
| Room – Contract | 1–n | `Contract.roomId` | một phòng nhiều hợp đồng qua các năm (chỉ 1 ACTIVE) |
| Room – MeterReading | 1–n | `MeterReading.roomId` | |
| Contract – Invoice | 1–n | `Invoice.contractId` | |
| UtilityPrice – Invoice | 1–n | `Invoice.priceId` | |
| Invoice – Payment | 1–n | `Payment.invoiceId` | nhiều lần thử, tối đa 1 SUCCESS |
| Contract – MaintenanceRequest | 1–n | `MaintenanceRequest.contractId` | |

Nhiều–nhiều **Tenant ↔ Room** (một người thuê nhiều phòng qua các năm, một phòng nhiều người qua các năm) được tách bằng bảng trung gian **Contract** (có thêm thuộc tính riêng: ngày, giá, trạng thái).
Mọi khoá ngoại để mặc định `NO ACTION` (không `CASCADE`): xoá dòng cha đang có dòng con ⇒ lỗi 547 ⇒ dùng **xoá mềm** (đổi `status`).

## Luật nghiệp vụ → ràng buộc

| # | Luật | Chặn ở CSDL bằng | Còn phải kiểm trong Java |
|---|---|---|---|
| 1 | ★ Một phòng chỉ 1 hợp đồng ACTIVE (kể cả 2 quản lý cùng bấm Duyệt) | `UX_Contract_ActiveRoom` (UNIQUE có lọc `WHERE status='ACTIVE'`) | kiểm trước để báo đẹp; bắt lỗi 2601 ⇒ "Phòng vừa được người khác thuê" |
| 2 | Một người chỉ 1 hợp đồng ACTIVE | `UX_Contract_ActiveTenant` | UC04 kiểm trước |
| 3 | Một người chỉ 1 yêu cầu PENDING (bấm Gửi 2 lần) | `UX_RentalRequest_PendingTenant` | UC04 kiểm trước |
| 4 | ★ Một hợp đồng 1 hoá đơn mỗi tháng (bấm Lập 2 lần) | `UQ_Invoice_ContractYearMonth` | `NOT EXISTS` trong câu lập hoá đơn |
| 5 | Mỗi phòng ghi chỉ số 1 lần mỗi tháng | `UQ_MeterReading_RoomYearMonth` | |
| 6 | IPN VNPay gọi 2 lần không ghi 2 lần | `UX_Payment_SuccessInvoice` + `UQ_Payment_txnRef` | kiểm `status = 'PENDING'` với `UPDLOCK` |
| 7 | Mã phòng, username, CCCD, tên loại phòng, ngày hiệu lực giá không trùng | `UQ_…` | báo lỗi đẹp trên form |
| 8 | Giá > 0, số tháng 1–24, sức chứa 1–8, chỉ số ≥ 0, tháng 1–12 | `CK_…` | validate form (đừng tin `required` của HTML) |
| 9 | Trạng thái chỉ nhận giá trị trong sơ đồ trạng thái (NT-14) | `CK_…_status` cho cả 6 bảng | đúng **chiều** chuyển: `WHERE status = '<trạng thái cũ>'` trong UPDATE |
| 10 | Tổng hoá đơn = tổng các khoản | `CK_Invoice_total` | |
| 11 | Cột thời điểm khớp trạng thái (PAID ⇔ paidAt, ENDED ⇔ endedAt, DONE ⇔ resolvedAt…) | `CK_Invoice_paidAt`, `CK_Contract_endedAt`, `CK_Payment_paidAt`, `CK_MaintenanceRequest_resolved`, `CK_RentalRequest_decision` | |
| 12 | Không có dữ liệu "mồ côi" (hoá đơn của hợp đồng không tồn tại…) | mọi `FK_…` | |
| 13 | Phòng đã có lịch sử không xoá thật | FK chặn DELETE (547) | UC13 "xoá" = `UPDATE status='HIDDEN'` |
| 14 | Chỉ kết thúc hợp đồng khi không còn nợ | — | điều kiện `NOT EXISTS (UNPAID)` trong chính câu UPDATE (Q12b) |
| 15 | Số người ≤ sức chứa · ngày vào ≥ hôm nay · chỉ số mới ≥ cũ · Tenant phải là tài khoản TENANT | — (luật giữa nhiều bảng/dòng) | service Java; setup.sql mục 4.7 kiểm lại chỉ số |
| 16 | Người thuê chỉ thấy dữ liệu CỦA MÌNH (IDOR) | — | `AND c.tenantId = ?` (id từ session) trong mọi câu SELECT của người thuê + OwnershipFilter |

## Dữ liệu mẫu (tóm tắt)

- **Mật khẩu thử của mọi tài khoản: `NhaTro@2026`.** admin (ADMIN) · quanly (MANAGER) · quanly2 (MANAGER, **đã khoá**) · an.nguyen, binh.tran, chi.le, dung.pham, ha.vu, khanh.do, lan.hoang (TENANT).
- 4 loại phòng (1 loại chưa có phòng) · 12 phòng: 4 đang thuê (P101, P102, P201, P203), 5 trống, 2 MAINTENANCE, 1 HIDDEN (P302).
- Bảng giá 2 mốc: 01/01/2026 (3.500 / 15.000 / 50.000) và 01/09/2026 (3.800 / 18.000 / 60.000).
- Yêu cầu đủ 4 trạng thái; R8 + R9 cùng chờ phòng P103 (để thử duyệt và thử đua).
- 5 hợp đồng (4 ACTIVE, 1 ENDED) · chỉ số tháng 6–9 · 8 hoá đơn: 6 PAID, I6 UNPAID quá hạn (Bình), I7 UNPAID gia hạn tới 31/10 (Chi).
- 9 lần thanh toán: 6 SUCCESS, 2 FAILED, 1 PENDING · 5 sự cố: 3 DONE, 1 IN_PROGRESS, 1 OPEN.
- "Quá hạn" tính theo ngày chạy truy vấn ⇒ sau 31/10/2026 hoá đơn I7 cũng thành quá hạn.

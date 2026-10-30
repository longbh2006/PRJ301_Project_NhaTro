/* =====================================================================
   queries-mau.sql — CÁC CÂU SQL MẪU cho DAO của từng người (CSDL NhaTroSV)
   ---------------------------------------------------------------------
   • Chạy setup.sql TRƯỚC (cần dữ liệu mẫu). Chạy cả file từ trên xuống (F5).
   • Mỗi câu có DECLARE @biến để chạy thử ngay trong SSMS.
     Trong Java: thay @biến bằng dấu ?  và dùng PreparedStatement.setXxx(…)
     — KHÔNG BAO GIỜ nối chuỗi SQL bằng dấu + (lỗi SQL injection).
   • Các câu GHI (INSERT/UPDATE) làm THẬT. Chạy file lần 2 thì vài câu sẽ báo
     "đã duyệt rồi / trùng" — đó là luật đang chặn đúng. Muốn dữ liệu mẫu về như cũ:
     chạy lại setup.sql.

   Mục lục
     LONG  (phòng + CSDL)      Q01 UC01 tìm phòng trống + lọc + phân trang
                               Q02 UC01 đếm tổng để tính số trang (PagerTag)
                               Q03 UC01 chi tiết 1 phòng
                               Q04 UC13 admin: phòng, loại phòng, bảng giá
                               Q05 UC15 admin: tài khoản (tìm, lọc, phân trang, khoá)
                               Q06 UC08 người thuê báo sự cố + xem sự cố của mình
                               Q07 UC12 quản lý xử lý sự cố (đúng chiều trạng thái)
     KHOA  (hợp đồng + giao diện)
                               Q08 UC02 đăng ký: Account + Tenant trong 1 transaction
                               Q09 UC04 gửi yêu cầu thuê (kiểm luật rồi INSERT)
                               Q10 UC05 yêu cầu của tôi, huỷ yêu cầu, hợp đồng của tôi
                               Q11 UC09 ★ duyệt yêu cầu ⇒ tạo hợp đồng (transaction + chống đua)
                               Q12 UC11 quản lý hợp đồng: lọc, phân trang, kết thúc
     ANH TÚ (hoá đơn + thanh toán)
                               Q13 UC03 đăng nhập: lấy tài khoản theo username
                               Q14 UC10 ★ ghi chỉ số + lập hoá đơn tháng (giá theo ngày hiệu lực)
                               Q15 UC06 hoá đơn của tôi + chi tiết (chặn IDOR)
                               Q16 UC07 VNPay: tạo lần thử trả + xử lý IPN (chạy 2 lần không ghi 2 lần)
                               Q17 UC14 báo cáo doanh thu 12 tháng, công nợ, tỉ lệ lấp đầy
   ===================================================================== */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;   -- bắt buộc khi ghi vào bảng có chỉ mục có lọc (sqlcmd mặc định tắt)
GO
USE NhaTroSV;
GO

/* =====================================================================
   PHẦN LONG — Phòng, loại phòng, tài khoản, sự cố
   ===================================================================== */

-- Q01 · UC01 · NT-32 · RoomDAO.search(type, minPrice, maxPrice, floor, page, size)
-- "Phòng trống" = Room.status = 'ACTIVE' VÀ không có hợp đồng ACTIVE (NOT EXISTS).
-- Ô lọc nào bỏ trống thì truyền NULL ⇒ (@x IS NULL OR …) bỏ qua điều kiện đó.
-- Java: cách dễ đọc hơn là ghép thêm " AND r.roomTypeId = ?" vào StringBuilder KHI ô lọc có giá trị,
--       rồi set tham số theo đúng thứ tự (vẫn là PreparedStatement, giá trị không bao giờ nối vào chuỗi).
DECLARE @type INT = NULL, @minPrice DECIMAL(12,0) = 1800000, @maxPrice DECIMAL(12,0) = 3000000,
        @floor INT = NULL, @page INT = 1, @size INT = 3;
SELECT r.roomId, r.roomCode, rt.name AS roomTypeName, rt.capacity, r.floor, r.area, r.monthlyPrice, r.imageUrl
FROM Room r
JOIN RoomType rt ON rt.roomTypeId = r.roomTypeId
WHERE r.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = r.roomId AND c.status = 'ACTIVE')
  AND (@type     IS NULL OR r.roomTypeId   =  @type)
  AND (@minPrice IS NULL OR r.monthlyPrice >= @minPrice)
  AND (@maxPrice IS NULL OR r.monthlyPrice <= @maxPrice)
  AND (@floor    IS NULL OR r.floor        =  @floor)
ORDER BY r.monthlyPrice, r.roomId                       -- thêm roomId để thứ tự ổn định khi trùng giá
OFFSET (@page - 1) * @size ROWS FETCH NEXT @size ROWS ONLY;   -- phân trang của SQL Server (bắt buộc có ORDER BY)
-- Java: "... ORDER BY r.monthlyPrice, r.roomId OFFSET ? ROWS FETCH NEXT ? ROWS ONLY"
--       ps.setInt(n, (page - 1) * size); ps.setInt(n + 1, size);
GO

-- Q02 · UC01 · NT-32 · RoomDAO.countSearch(...) — CÙNG điều kiện WHERE với Q01, để tính totalPages
DECLARE @type INT = NULL, @minPrice DECIMAL(12,0) = 1800000, @maxPrice DECIMAL(12,0) = 3000000, @floor INT = NULL, @size INT = 3;
SELECT COUNT(*) AS totalRows,
       (COUNT(*) + @size - 1) / @size AS totalPages      -- làm tròn lên: 4 phòng, 3/trang ⇒ 2 trang
FROM Room r
WHERE r.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = r.roomId AND c.status = 'ACTIVE')
  AND (@type     IS NULL OR r.roomTypeId   =  @type)
  AND (@minPrice IS NULL OR r.monthlyPrice >= @minPrice)
  AND (@maxPrice IS NULL OR r.monthlyPrice <= @maxPrice)
  AND (@floor    IS NULL OR r.floor        =  @floor);
GO

-- Q03 · UC01 · NT-32 · RoomDAO.findById(id) — trang /rooms/detail?id=… (không có dòng ⇒ servlet trả 404)
--      Khách chỉ được xem phòng không bị ẩn. Cột isAvailable để JSP quyết định hiện nút "Đăng ký thuê".
DECLARE @roomId INT = 3;
SELECT r.roomId, r.roomCode, r.floor, r.area, r.monthlyPrice, r.status, r.description, r.imageUrl,
       rt.roomTypeId, rt.name AS roomTypeName, rt.capacity,
       CASE WHEN r.status = 'ACTIVE'
             AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = r.roomId AND c.status = 'ACTIVE')
            THEN 1 ELSE 0 END AS isAvailable
FROM Room r
JOIN RoomType rt ON rt.roomTypeId = r.roomTypeId
WHERE r.roomId = @roomId AND r.status <> 'HIDDEN';
GO

-- Q04 · UC13 · NT-33 · AdminRoomDAO — admin quản lý phòng, loại phòng, bảng giá
-- Q04a Danh sách MỌI phòng (cả MAINTENANCE/HIDDEN) + người đang thuê + đã từng có hợp đồng chưa
SELECT r.roomId, r.roomCode, rt.name AS roomTypeName, r.floor, r.monthlyPrice, r.status,
       a.fullName AS currentTenant,
       (SELECT COUNT(*) FROM Contract c2 WHERE c2.roomId = r.roomId) AS contractCount   -- > 0 ⇒ KHÔNG được xoá thật
FROM Room r
JOIN RoomType rt     ON rt.roomTypeId = r.roomTypeId
LEFT JOIN Contract c ON c.roomId = r.roomId AND c.status = 'ACTIVE'
LEFT JOIN Account a  ON a.accountId = c.tenantId
ORDER BY r.roomCode;
GO
-- Q04b Thêm phòng / sửa phòng (mã trùng ⇒ lỗi 2627, giá ≤ 0 ⇒ lỗi 547: bắt SQLException, hiện lại form)
INSERT INTO Room (roomCode, roomTypeId, floor, area, monthlyPrice, description)
VALUES ('P401', 2, 4, 24.0, 2900000, N'Phòng mới tầng 4');
UPDATE Room SET monthlyPrice = 3000000, description = N'Phòng mới tầng 4, có máy giặt chung'
WHERE roomId = (SELECT roomId FROM Room WHERE roomCode = 'P401');
GO
-- Q04c "Xoá" phòng = XOÁ MỀM: đổi status sang HIDDEN (giữ lịch sử hợp đồng, hoá đơn).
--      Chỉ cho ẩn khi phòng KHÔNG có người đang thuê (@@ROWCOUNT = 0 ⇒ báo lỗi cho admin).
DECLARE @roomId INT = (SELECT roomId FROM Room WHERE roomCode = 'P401');
UPDATE Room SET status = 'HIDDEN'
WHERE roomId = @roomId
  AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = @roomId AND c.status = 'ACTIVE');
SELECT @@ROWCOUNT AS soPhongDaAn;   -- Java: int n = ps.executeUpdate(); if (n == 0) báo "Phòng đang có người thuê"
GO
-- Q04d Loại phòng + số phòng mỗi loại (LEFT JOIN để loại chưa có phòng vẫn hiện, đếm = 0)
--      Xoá loại phòng đang có phòng dùng ⇒ khoá ngoại tự chặn (lỗi 547) ⇒ báo "Loại phòng đang được dùng".
SELECT rt.roomTypeId, rt.name, rt.capacity, COUNT(r.roomId) AS roomCount
FROM RoomType rt
LEFT JOIN Room r ON r.roomTypeId = rt.roomTypeId
GROUP BY rt.roomTypeId, rt.name, rt.capacity
ORDER BY rt.roomTypeId;
GO
-- Q04e Bảng giá điện nước: KHÔNG sửa giá cũ, chỉ THÊM mốc giá mới (trùng ngày hiệu lực ⇒ lỗi 2627)
--      Ở đây chỉ XEM; câu thêm mốc mới để dạng chú thích để không làm đổi dữ liệu mẫu:
-- INSERT INTO UtilityPrice (electricPrice, waterPrice, serviceFeePerPerson, effectiveFrom) VALUES (4000, 20000, 60000, '2027-01-01');
SELECT priceId, electricPrice, waterPrice, serviceFeePerPerson, effectiveFrom,
       CASE WHEN priceId = (SELECT TOP (1) priceId FROM UtilityPrice
                            WHERE effectiveFrom <= CAST(SYSDATETIME() AS DATE) ORDER BY effectiveFrom DESC)
            THEN N'Đang áp dụng' ELSE N'' END AS ghiChu
FROM UtilityPrice ORDER BY effectiveFrom DESC;
GO

-- Q05 · UC15 · NT-43 · AccountDAO — admin quản lý tài khoản
-- Q05a Tìm theo tên/username + lọc vai trò + phân trang
DECLARE @keyword NVARCHAR(100) = N'an', @role VARCHAR(10) = NULL, @page INT = 1, @size INT = 5;
SELECT accountId, username, fullName, phone, email, role, active, createdAt
FROM Account
WHERE (@keyword IS NULL OR fullName LIKE N'%' + @keyword + N'%' OR username LIKE '%' + @keyword + '%')
  AND (@role IS NULL OR role = @role)
ORDER BY accountId
OFFSET (@page - 1) * @size ROWS FETCH NEXT @size ROWS ONLY;
-- Java: ps.setString(1, "%" + keyword + "%")  — dấu % thêm ở JAVA, câu SQL chỉ có "fullName LIKE ?"
GO
-- Q05b Tạo tài khoản MANAGER (passwordHash do PasswordHasher.hash(...) sinh ra trong Java)
INSERT INTO Account (username, passwordHash, fullName, phone, role)
VALUES ('quanly3', 'pbkdf2_sha256$600000$IUls0lZl/CAfvEia7e77wA==$4A0uNk8v0tBwyqgqKRB55XgcsQ2W3ciMWbsd09EEHt8=',
        N'Ngô Thị Mai', '0912000011', 'MANAGER');   -- mượn chuỗi băm của 'quanly' ⇒ mật khẩu cũng là NhaTro@2026
GO
-- Q05c Khoá / mở khoá — KHÔNG cho admin tự khoá chính mình (@me = id admin đang đăng nhập, lấy từ session)
DECLARE @targetId INT = (SELECT accountId FROM Account WHERE username = 'quanly3'), @me INT = 1, @active BIT = 0;
UPDATE Account SET active = @active WHERE accountId = @targetId AND accountId <> @me;
SELECT @@ROWCOUNT AS soTaiKhoanDaDoi;   -- 0 ⇒ không có hoặc đang cố tự khoá mình
GO

-- Q06 · UC08 · NT-46 · MaintenanceDAO — người thuê báo sự cố
-- Q06a Lấy hợp đồng ACTIVE của người đang đăng nhập (không có ⇒ không cho báo)
DECLARE @tenantId INT = 4;   -- = id trong session, KHÔNG lấy từ URL
SELECT contractId FROM Contract WHERE tenantId = @tenantId AND status = 'ACTIVE';
-- Q06b Ghi sự cố — gắn contractId lấy được ở trên. Viết gộp 1 câu: không có hợp đồng ACTIVE thì không chèn gì
INSERT INTO MaintenanceRequest (contractId, title, description)
SELECT c.contractId, N'Quạt trần kêu to', N'Bật số 3 là rung mạnh'
FROM Contract c WHERE c.tenantId = @tenantId AND c.status = 'ACTIVE';
-- Q06c Danh sách sự cố của tôi (cả của hợp đồng cũ), mới nhất trước
SELECT m.ticketId, r.roomCode, m.title, m.status, m.managerNote, m.createdAt, m.resolvedAt
FROM MaintenanceRequest m
JOIN Contract c ON c.contractId = m.contractId
JOIN Room r     ON r.roomId     = c.roomId
WHERE c.tenantId = @tenantId
ORDER BY m.createdAt DESC, m.ticketId DESC;
GO

-- Q07 · UC12 · NT-47 · MaintenanceDAO — quản lý xử lý sự cố
-- Q07a Lọc theo trạng thái, phòng
DECLARE @status VARCHAR(12) = NULL, @roomCode VARCHAR(10) = NULL;
SELECT m.ticketId, r.roomCode, a.fullName AS tenantName, m.title, m.status, m.managerNote, m.createdAt
FROM MaintenanceRequest m
JOIN Contract c ON c.contractId = m.contractId
JOIN Room r     ON r.roomId     = c.roomId
JOIN Account a  ON a.accountId  = c.tenantId
WHERE (@status IS NULL OR m.status = @status)
  AND (@roomCode IS NULL OR r.roomCode = @roomCode)
ORDER BY CASE m.status WHEN 'OPEN' THEN 1 WHEN 'IN_PROGRESS' THEN 2 ELSE 3 END, m.createdAt;
GO
-- Q07b Chuyển trạng thái ĐÚNG CHIỀU: điều kiện "status hiện tại" nằm ngay trong WHERE
--      ⇒ không nhảy được DONE → OPEN; @@ROWCOUNT = 0 nghĩa là chuyển sai chiều.
DECLARE @ticketId INT = 5;
UPDATE MaintenanceRequest SET status = 'IN_PROGRESS', managerNote = N'Đã gọi thợ điện lạnh, hẹn sáng mai'
WHERE ticketId = @ticketId AND status = 'OPEN';
SELECT @@ROWCOUNT AS OPEN_sang_IN_PROGRESS;
UPDATE MaintenanceRequest SET status = 'DONE', resolvedAt = SYSDATETIME(), managerNote = N'Đã vệ sinh dàn lạnh, hết chảy nước'
WHERE ticketId = @ticketId AND status = 'IN_PROGRESS';
SELECT @@ROWCOUNT AS IN_PROGRESS_sang_DONE;
UPDATE MaintenanceRequest SET status = 'OPEN' WHERE ticketId = @ticketId AND status = 'IN_PROGRESS';   -- thử đi ngược
SELECT @@ROWCOUNT AS thu_DONE_ve_OPEN_phai_bang_0;
GO

/* =====================================================================
   PHẦN KHOA — Đăng ký, yêu cầu thuê, hợp đồng
   ===================================================================== */

-- Q08 · UC02 · NT-30 · AccountDAO.register — 2 INSERT trong MỘT transaction
--      Lỗi ở lệnh 2 (vd CCCD trùng) ⇒ ROLLBACK cả lệnh 1 ⇒ không có Account "mồ côi".
--      Java: conn.setAutoCommit(false); INSERT Account với Statement.RETURN_GENERATED_KEYS;
--            rs = ps.getGeneratedKeys() ⇒ id; INSERT Tenant(id, …); conn.commit(); catch ⇒ conn.rollback().
--      KHÔNG dùng SELECT MAX(accountId): 2 người đăng ký cùng lúc sẽ lấy nhầm id của nhau.
BEGIN TRY
    BEGIN TRAN;
    DECLARE @newId INT;
    INSERT INTO Account (username, passwordHash, fullName, phone, email, role)
    VALUES ('minh.ngo', 'pbkdf2_sha256$600000$LrhQcaDV5ZzsmfzJKKGZlw==$ic2JN1yxQEg4NO3HyjVqyo7t/rI4iQ8/MWQxM2c4P5U=',
            N'Ngô Đức Minh', '0987000012', 'minh.ngo@gmail.com', 'TENANT');   -- mật khẩu NhaTro@2026 (mượn băm của an.nguyen)
    SET @newId = SCOPE_IDENTITY();       -- id vừa sinh trong CHÍNH phạm vi lệnh của mình
    INSERT INTO Tenant (tenantId, idCardNo, school, dateOfBirth, hometown)
    VALUES (@newId, '001206099999', N'Đại học FPT Hà Nội', '2006-04-04', N'Hải Phòng');
    COMMIT;
    SELECT @newId AS newAccountId, N'Đăng ký thành công' AS ketQua;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    SELECT ERROR_NUMBER() AS loiSo, ERROR_MESSAGE() AS thongBao;   -- 2627 ⇒ username hoặc CCCD đã có
END CATCH;
GO

-- Q09 · UC04 · NT-34 · RentalDAO.insertRequest — người thuê gửi yêu cầu thuê
-- Q09a Kiểm luật (service/RentalService): phòng trống? đủ sức chứa? tôi đã có hợp đồng ACTIVE / yêu cầu PENDING chưa?
DECLARE @tenantId INT = (SELECT accountId FROM Account WHERE username = 'minh.ngo'),
        @roomId INT = 9, @numPeople INT = 1;
SELECT
  roomAvailable    = CASE WHEN EXISTS (SELECT 1 FROM Room r WHERE r.roomId = @roomId AND r.status = 'ACTIVE')
                           AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = @roomId AND c.status = 'ACTIVE')
                          THEN 1 ELSE 0 END,
  fitsCapacity     = CASE WHEN @numPeople <= (SELECT rt.capacity FROM Room r JOIN RoomType rt ON rt.roomTypeId = r.roomTypeId
                                              WHERE r.roomId = @roomId) THEN 1 ELSE 0 END,
  hasActiveContract = CASE WHEN EXISTS (SELECT 1 FROM Contract WHERE tenantId = @tenantId AND status = 'ACTIVE') THEN 1 ELSE 0 END,
  hasPendingRequest = CASE WHEN EXISTS (SELECT 1 FROM RentalRequest WHERE tenantId = @tenantId AND status = 'PENDING') THEN 1 ELSE 0 END;
-- Q09b Đủ điều kiện (1,1,0,0) ⇒ INSERT. Bấm "Gửi" 2 lần liền ⇒ lần 2 bị UX_RentalRequest_PendingTenant chặn (lỗi 2601).
INSERT INTO RentalRequest (tenantId, roomId, moveInDate, months, numPeople, note)
VALUES (@tenantId, @roomId, '2026-11-15', 6, @numPeople, N'Em muốn xem phòng chiều thứ 7');
SELECT SCOPE_IDENTITY() AS newRequestId;
GO

-- Q10 · UC05 · NT-35 · RentalDAO.findRequestsByTenant / cancel / findActiveContract
DECLARE @tenantId INT = (SELECT accountId FROM Account WHERE username = 'minh.ngo');   -- = id trong session
-- Q10a Yêu cầu CỦA TÔI (WHERE tenantId = id trong session ⇒ sửa id trên URL cũng không xem được của người khác)
SELECT rr.requestId, r.roomCode, rr.moveInDate, rr.months, rr.numPeople, rr.status, rr.createdAt, rr.decisionNote
FROM RentalRequest rr JOIN Room r ON r.roomId = rr.roomId
WHERE rr.tenantId = @tenantId
ORDER BY rr.createdAt DESC;
-- Q10b Huỷ: chỉ yêu cầu CỦA MÌNH và đang PENDING (huỷ yêu cầu đã APPROVED ⇒ 0 dòng ⇒ báo lỗi)
DECLARE @requestId INT = (SELECT MAX(requestId) FROM RentalRequest WHERE tenantId = @tenantId);   -- (chỉ để demo; Java lấy từ form)
UPDATE RentalRequest SET status = 'CANCELLED', decidedAt = SYSDATETIME()
WHERE requestId = @requestId AND tenantId = @tenantId AND status = 'PENDING';
SELECT @@ROWCOUNT AS soYeuCauDaHuy;
-- Q10c Hợp đồng ACTIVE của tôi (trang /tenant/contract) — ví dụ của An (id 4)
SELECT c.contractId, r.roomCode, rt.name AS roomTypeName, c.startDate, c.endDate, c.monthlyRent, c.deposit, c.numPeople
FROM Contract c
JOIN Room r      ON r.roomId = c.roomId
JOIN RoomType rt ON rt.roomTypeId = r.roomTypeId
WHERE c.tenantId = 4 AND c.status = 'ACTIVE';
GO

-- Q11 · UC09 · NT-36 · ★ RentalService.approve — GIAO DỊCH TRUNG TÂM, thầy chắc chắn hỏi
-- Q11a Danh sách chờ duyệt (lọc theo phòng)
DECLARE @roomCode VARCHAR(10) = NULL;
SELECT rr.requestId, r.roomCode, a.fullName AS tenantName, rr.moveInDate, rr.months, rr.numPeople, rr.note, rr.createdAt
FROM RentalRequest rr
JOIN Room r    ON r.roomId = rr.roomId
JOIN Account a ON a.accountId = rr.tenantId
WHERE rr.status = 'PENDING' AND (@roomCode IS NULL OR r.roomCode = @roomCode)
ORDER BY rr.createdAt;
GO
-- Q11b DUYỆT yêu cầu R8 (Khánh, phòng P103). Ba việc trong MỘT transaction:
--   (1) yêu cầu → APPROVED  (2) INSERT hợp đồng ACTIVE (chụp giá phòng, cọc, chỉ số lúc nhận phòng)
--   (3) các yêu cầu PENDING khác của CÙNG phòng → REJECTED (ở đây: R9 của Hà)
-- Chống đua (race condition) có 2 lớp:
--   • WITH (UPDLOCK) khoá dòng yêu cầu ⇒ 2 quản lý bấm Duyệt CÙNG yêu cầu: người sau phải đợi, rồi thấy hết PENDING.
--   • UX_Contract_ActiveRoom ⇒ 2 quản lý duyệt 2 yêu cầu KHÁC nhau cùng phòng: INSERT thứ 2 lỗi 2601
--     ⇒ Java bắt SQLException (getErrorCode() == 2601 hoặc 2627), rollback, báo "Phòng vừa được người khác thuê".
--   (SELECT kiểm "phòng trống" trước đó KHÔNG đủ: cả 2 người cùng kiểm thấy trống — xem kiem-tra.txt phần 3.)
-- Java: conn.setAutoCommit(false); 3 PreparedStatement; conn.commit(); catch (SQLException e) { conn.rollback(); }
DECLARE @requestId INT = 8, @managerId INT = 2,             -- @managerId = id quản lý trong session
        @startE INT = 4100, @startW INT = 700,              -- chỉ số lúc nhận phòng: quản lý nhập trên form
        @deposit DECIMAL(12,0) = NULL;                      -- NULL ⇒ cọc 1 tháng tiền phòng
BEGIN TRY
    BEGIN TRAN;
    DECLARE @roomId INT, @tenantId INT, @moveIn DATE, @months INT, @people INT;
    SELECT @roomId = roomId, @tenantId = tenantId, @moveIn = moveInDate, @months = months, @people = numPeople
    FROM RentalRequest WITH (UPDLOCK)
    WHERE requestId = @requestId AND status = 'PENDING';
    IF @roomId IS NULL
        THROW 50001, N'Yêu cầu không còn ở trạng thái chờ duyệt', 1;
    IF NOT EXISTS (SELECT 1 FROM Room WHERE roomId = @roomId AND status = 'ACTIVE')
        THROW 50002, N'Phòng đang sửa hoặc đã ẩn, không duyệt được', 1;

    UPDATE RentalRequest SET status = 'APPROVED', decidedAt = SYSDATETIME(), decidedBy = @managerId
    WHERE requestId = @requestId;                                                         -- (1)

    INSERT INTO Contract (requestId, tenantId, roomId, startDate, endDate, monthlyRent, deposit,
                          numPeople, startElectricIndex, startWaterIndex, status)
    SELECT @requestId, @tenantId, @roomId, @moveIn, DATEADD(MONTH, @months, @moveIn),
           r.monthlyPrice, COALESCE(@deposit, r.monthlyPrice), @people, @startE, @startW, 'ACTIVE'
    FROM Room r WHERE r.roomId = @roomId;                                                 -- (2) chỉ mục có lọc chặn ở đây

    UPDATE RentalRequest SET status = 'REJECTED', decidedAt = SYSDATETIME(), decidedBy = @managerId,
                             decisionNote = N'Phòng đã được duyệt cho yêu cầu khác'
    WHERE roomId = @roomId AND status = 'PENDING' AND requestId <> @requestId;          -- (3)
    COMMIT;
    SELECT N'Duyệt thành công' AS ketQua, SCOPE_IDENTITY() AS newContractId;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    SELECT N'Không duyệt được' AS ketQua, ERROR_NUMBER() AS loiSo, ERROR_MESSAGE() AS thongBao;
END CATCH;
-- Kết quả: R8 APPROVED, R9 REJECTED, P103 có đúng 1 hợp đồng ACTIVE
SELECT requestId, tenantId, status, decisionNote FROM RentalRequest WHERE roomId = 3;
GO
-- Q11c TỪ CHỐI một yêu cầu (R10 của Lan) — chỉ khi còn PENDING
DECLARE @requestId INT = 10, @managerId INT = 2;
UPDATE RentalRequest SET status = 'REJECTED', decidedAt = SYSDATETIME(), decidedBy = @managerId,
                         decisionNote = N'Phòng đã có khách đặt cọc giữ chỗ'
WHERE requestId = @requestId AND status = 'PENDING';
SELECT @@ROWCOUNT AS soYeuCauDaTuChoi;
GO

-- Q12 · UC11 · NT-48 · ContractDAO — quản lý hợp đồng
-- Q12a Lọc theo trạng thái, phòng, tên người thuê + phân trang (đếm tổng: COUNT(*) với cùng WHERE như Q02)
DECLARE @status VARCHAR(10) = NULL, @roomCode VARCHAR(10) = NULL, @name NVARCHAR(100) = NULL, @page INT = 1, @size INT = 10;
SELECT c.contractId, r.roomCode, a.fullName AS tenantName, c.startDate, c.endDate, c.monthlyRent, c.status, c.endedAt,
       (SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.contractId = c.contractId AND i.status = 'UNPAID') AS unpaidTotal
FROM Contract c
JOIN Room r    ON r.roomId = c.roomId
JOIN Account a ON a.accountId = c.tenantId
WHERE (@status IS NULL OR c.status = @status)
  AND (@roomCode IS NULL OR r.roomCode = @roomCode)
  AND (@name IS NULL OR a.fullName LIKE N'%' + @name + N'%')
ORDER BY c.status, c.startDate DESC, c.contractId DESC
OFFSET (@page - 1) * @size ROWS FETCH NEXT @size ROWS ONLY;
GO
-- Q12b KẾT THÚC hợp đồng — chỉ khi KHÔNG còn hoá đơn UNPAID. Gộp điều kiện vào CHÍNH câu UPDATE
--      ⇒ một lệnh, không có khe hở giữa "kiểm" và "ghi". 0 dòng ⇒ báo số tiền còn nợ (câu dưới).
--      (Lưu ý nghiệp vụ: lập hoá đơn tháng cuối TRƯỚC rồi mới kết thúc hợp đồng.)
DECLARE @contractId INT = 3;   -- hợp đồng của Bình: còn hoá đơn tháng 9 chưa trả ⇒ phải bị chặn
UPDATE Contract SET status = 'ENDED', endedAt = SYSDATETIME()
WHERE contractId = @contractId AND status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM Invoice WHERE contractId = @contractId AND status = 'UNPAID');
SELECT @@ROWCOUNT AS soHopDongDaKetThuc,
       (SELECT COALESCE(SUM(total), 0) FROM Invoice WHERE contractId = @contractId AND status = 'UNPAID') AS conNo;
GO

/* =====================================================================
   PHẦN ANH TÚ — Đăng nhập, chỉ số, hoá đơn, VNPay, báo cáo
   ===================================================================== */

-- Q13 · UC03 · NT-31 · AccountDAO.findByUsername — đăng nhập
--      SQL CHỈ lấy theo username. So mật khẩu làm trong JAVA: PasswordHasher.verify(matKhauNhap, passwordHash).
--      Không có dòng HOẶC verify sai ⇒ cùng MỘT thông báo "Sai tên đăng nhập hoặc mật khẩu".
--      active = 0 ⇒ "Tài khoản đã bị khoá" (thử với quanly2).
DECLARE @username VARCHAR(30) = 'an.nguyen';
SELECT accountId, username, passwordHash, fullName, role, active
FROM Account WHERE username = @username;
GO

-- Q14 · UC10 · NT-37 · ★ BillingDAO — ghi chỉ số điện nước + lập hoá đơn tháng
-- Q14a currentPrice(): bảng giá có hiệu lực tại một ngày = dòng có effectiveFrom ≤ ngày đó, MỚI NHẤT
DECLARE @onDate DATE = '2026-10-01';
SELECT TOP (1) priceId, electricPrice, waterPrice, serviceFeePerPerson, effectiveFrom
FROM UtilityPrice WHERE effectiveFrom <= @onDate ORDER BY effectiveFrom DESC;
GO
-- Q14b Trang /manager/meters?year=2026&month=10: mọi phòng có hợp đồng ACTIVE + chỉ số CŨ tự điền
--      Chỉ số cũ = chỉ số tháng trước của phòng; tháng đầu của hợp đồng thì = startElectricIndex/startWaterIndex.
DECLARE @y INT = 2026, @m INT = 10;
DECLARE @first DATE = DATEFROMPARTS(@y, @m, 1);
SELECT c.contractId, r.roomId, r.roomCode, a.fullName AS tenantName,
       oldElectric = CASE WHEN c.startDate >= @first THEN c.startElectricIndex ELSE prev.electricIndex END,
       oldWater    = CASE WHEN c.startDate >= @first THEN c.startWaterIndex    ELSE prev.waterIndex    END,
       cur.electricIndex AS newElectric, cur.waterIndex AS newWater     -- NULL = tháng này chưa ghi
FROM Contract c
JOIN Room r    ON r.roomId = c.roomId
JOIN Account a ON a.accountId = c.tenantId
LEFT JOIN MeterReading prev ON prev.roomId = c.roomId
      AND prev.[year] = YEAR(DATEADD(MONTH, -1, @first)) AND prev.[month] = MONTH(DATEADD(MONTH, -1, @first))
LEFT JOIN MeterReading cur  ON cur.roomId = c.roomId AND cur.[year] = @y AND cur.[month] = @m
WHERE c.status = 'ACTIVE' AND c.startDate <= EOMONTH(@first)       -- bỏ hợp đồng bắt đầu sau tháng này
ORDER BY r.roomCode;
GO
-- Q14c insertReading(): ghi chỉ số tháng 10 cho 4 phòng (trùng phòng+tháng ⇒ lỗi 2627; chỉ số mới < cũ ⇒ Java chặn trước)
INSERT INTO MeterReading (roomId, [year], [month], electricIndex, waterIndex) VALUES
 (1, 2026, 10, 2620, 422),   -- P101: 85 kWh, 4 m³
 (2, 2026, 10, 5650, 846),   -- P102: 180 kWh, 9 m³
 (5, 2026, 10, 8530, 1250),  -- P201: 240 kWh, 17 m³
 (7, 2026, 10, 3380, 663);   -- P203: 135 kWh, 11 m³
GO
-- Q14d generateInvoices(year, month): lập hoá đơn cho MỌI hợp đồng ACTIVE có chỉ số tháng đó, MỘT câu INSERT…SELECT
--   Tổng = tiền phòng + (điện mới − cũ) × giá điện + (nước mới − cũ) × giá nước + phí dịch vụ × số người (NT-17)
--   Giá lấy theo bảng giá có hiệu lực ngày 1 của tháng. Hạn trả = ngày 10 tháng sau.
--   NOT EXISTS ⇒ bấm "Lập hoá đơn" 2 lần không sinh trùng (và UQ_Invoice_ContractYearMonth chặn thêm một lớp).
DECLARE @y INT = 2026, @m INT = 10;
DECLARE @first DATE = DATEFROMPARTS(@y, @m, 1);
INSERT INTO Invoice (contractId, priceId, [year], [month], roomFee, electricUsed, electricFee,
                     waterUsed, waterFee, serviceFee, total, status, dueDate)
SELECT c.contractId, gia.priceId, @y, @m,
       c.monthlyRent,
       dung.eUsed, dung.eUsed * gia.electricPrice,
       dung.wUsed, dung.wUsed * gia.waterPrice,
       gia.serviceFeePerPerson * c.numPeople,
       c.monthlyRent + dung.eUsed * gia.electricPrice + dung.wUsed * gia.waterPrice + gia.serviceFeePerPerson * c.numPeople,
       'UNPAID',
       DATEADD(DAY, 9, DATEADD(MONTH, 1, @first))                      -- ngày 10 tháng sau
FROM Contract c
JOIN MeterReading cur ON cur.roomId = c.roomId AND cur.[year] = @y AND cur.[month] = @m
LEFT JOIN MeterReading prev ON prev.roomId = c.roomId
      AND prev.[year] = YEAR(DATEADD(MONTH, -1, @first)) AND prev.[month] = MONTH(DATEADD(MONTH, -1, @first))
CROSS APPLY (SELECT TOP (1) p.priceId, p.electricPrice, p.waterPrice, p.serviceFeePerPerson
             FROM UtilityPrice p WHERE p.effectiveFrom <= @first ORDER BY p.effectiveFrom DESC) gia
CROSS APPLY (SELECT
       eUsed = cur.electricIndex - CASE WHEN c.startDate >= @first THEN c.startElectricIndex ELSE prev.electricIndex END,
       wUsed = cur.waterIndex    - CASE WHEN c.startDate >= @first THEN c.startWaterIndex    ELSE prev.waterIndex    END) dung
WHERE c.status = 'ACTIVE'
  AND c.startDate <= EOMONTH(@first)
  AND dung.eUsed IS NOT NULL AND dung.wUsed IS NOT NULL            -- thiếu chỉ số tháng trước ⇒ bỏ qua, quản lý ghi bù
  AND NOT EXISTS (SELECT 1 FROM Invoice i WHERE i.contractId = c.contractId AND i.[year] = @y AND i.[month] = @m);
SELECT @@ROWCOUNT AS soHoaDonVuaLap;     -- lần 1: 4 · chạy lại lần 2: 0
SELECT invoiceId, contractId, roomFee, electricUsed, electricFee, waterUsed, waterFee, serviceFee, total, dueDate
FROM Invoice WHERE [year] = @y AND [month] = @m ORDER BY contractId;
-- Tính tay để đối chiếu (giá mốc 2: 3.800 đ/kWh, 18.000 đ/m³, 60.000 đ/người):
--   P101 (An, 1 người):  1.800.000 + 85×3.800 + 4×18.000 + 60.000  = 2.255.000
--   P201 (Chi, 3 người): 3.500.000 + 240×3.800 + 17×18.000 + 180.000 = 4.898.000
GO

-- Q15 · UC06 · NT-38 · BillingDAO.findInvoicesByTenant / findInvoiceById
-- Q15a Hoá đơn của tôi, mới nhất trước, phân trang (PagerTag của Long). Hoá đơn không giữ tenantId
--      ⇒ đi Invoice → Contract và lọc Contract.tenantId = id trong session.
DECLARE @tenantId INT = 5, @page INT = 1, @size INT = 5;
SELECT i.invoiceId, i.[year], i.[month], r.roomCode, i.total, i.status, i.dueDate, i.paidAt,
       CASE WHEN i.status = 'UNPAID' AND i.dueDate < CAST(SYSDATETIME() AS DATE) THEN 1 ELSE 0 END AS isOverdue
FROM Invoice i
JOIN Contract c ON c.contractId = i.contractId
JOIN Room r     ON r.roomId     = c.roomId
WHERE c.tenantId = @tenantId
ORDER BY i.[year] DESC, i.[month] DESC, i.invoiceId DESC
OFFSET (@page - 1) * @size ROWS FETCH NEXT @size ROWS ONLY;
GO
-- Q15b Chi tiết 1 hoá đơn — "AND c.tenantId = ?" là lớp chặn IDOR thứ 3 (sau AuthorizationFilter, OwnershipFilter)
--      Bình (5) mở hoá đơn 5 của An ⇒ 0 dòng ⇒ servlet trả 404.
DECLARE @tenantId INT = 5, @invoiceId INT = 5;
SELECT i.*, r.roomCode, p.electricPrice, p.waterPrice, p.serviceFeePerPerson
FROM Invoice i
JOIN Contract c     ON c.contractId = i.contractId
JOIN Room r         ON r.roomId     = c.roomId
JOIN UtilityPrice p ON p.priceId    = i.priceId
WHERE i.invoiceId = @invoiceId AND c.tenantId = @tenantId;
SELECT COUNT(*) AS hoaDonCuaChinhMinh_phaiBang1
FROM Invoice i JOIN Contract c ON c.contractId = i.contractId
WHERE i.invoiceId = 6 AND c.tenantId = 5;
GO

-- Q16 · UC07 · NT-44 · PaymentDAO — VNPay sandbox
-- Q16a POST /tenant/pay: hoá đơn phải CỦA MÌNH + UNPAID ⇒ tạo 1 lần thử PENDING (txnRef duy nhất) ⇒ dựng URL VNPay
--      (txnRef sinh trong Java, ví dụ "INV" + invoiceId + "-" + yyyyMMddHHmmss; vnp_Amount = amount × 100)
DECLARE @tenantId INT = 5, @invoiceId INT = 6;
INSERT INTO Payment (invoiceId, txnRef, amount, status)
SELECT i.invoiceId, CONCAT('INV', i.invoiceId, '-', FORMAT(SYSDATETIME(), 'yyyyMMddHHmmss')), i.total, 'PENDING'
FROM Invoice i JOIN Contract c ON c.contractId = i.contractId
WHERE i.invoiceId = @invoiceId AND c.tenantId = @tenantId AND i.status = 'UNPAID';
SELECT paymentId, txnRef, amount, status FROM Payment WHERE invoiceId = @invoiceId ORDER BY paymentId;
GO
-- Q16b GET /payment/ipn — VNPay gọi máy chủ (đã KIỂM CHỮ KÝ trong Java trước khi tới đây).
--      Mọi thứ trong MỘT transaction; IPN gọi 2 lần ⇒ lần 2 trả '02' (đã xác nhận), không ghi 2 lần.
--      Mã trả về cho VNPay: 00 OK · 01 không thấy đơn · 02 đã xác nhận rồi · 04 sai số tiền.
--      Thử với lần thử PENDING có sẵn của hoá đơn 7 (Chi): txnRef 'INV7-20261016213000'.
--      CHẠY KHỐI NÀY 2 LẦN để thấy lần 2 ra '02'. (/payment/return chỉ HIỂN THỊ, không bao giờ ghi PAID.)
DECLARE @txnRef VARCHAR(50) = 'INV7-20261016213000', @vnpAmount BIGINT = 495400000,   -- VNPay gửi số tiền × 100
        @vnpResponseCode VARCHAR(5) = '00', @vnpTransactionNo VARCHAR(20) = '14760001', @bankCode VARCHAR(20) = 'NCB';
DECLARE @lan INT = 1;
WHILE @lan <= 2
BEGIN
    DECLARE @rsp VARCHAR(2) = NULL, @paymentId INT = NULL, @invoiceId INT = NULL, @amount DECIMAL(12,0) = NULL, @pStatus VARCHAR(10) = NULL;
    BEGIN TRY
        BEGIN TRAN;
        SELECT @paymentId = paymentId, @invoiceId = invoiceId, @amount = amount, @pStatus = status
        FROM Payment WITH (UPDLOCK) WHERE txnRef = @txnRef;          -- khoá dòng: 2 IPN cùng lúc thì cái sau đợi
        IF @paymentId IS NULL                    SET @rsp = '01';
        ELSE IF @pStatus <> 'PENDING'            SET @rsp = '02';
        ELSE IF @amount * 100 <> @vnpAmount      SET @rsp = '04';
        ELSE IF @vnpResponseCode = '00'
        BEGIN
            UPDATE Payment SET status = 'SUCCESS', paidAt = SYSDATETIME(), vnpTransactionNo = @vnpTransactionNo,
                               bankCode = @bankCode, responseCode = @vnpResponseCode
            WHERE paymentId = @paymentId;
            UPDATE Invoice SET status = 'PAID', paidAt = SYSDATETIME()
            WHERE invoiceId = @invoiceId AND status = 'UNPAID';
            SET @rsp = '00';
        END
        ELSE
        BEGIN
            UPDATE Payment SET status = 'FAILED', vnpTransactionNo = @vnpTransactionNo, bankCode = @bankCode,
                               responseCode = @vnpResponseCode
            WHERE paymentId = @paymentId;
            SET @rsp = '00';                      -- đã ghi nhận kết quả (thất bại) ⇒ vẫn trả 00 cho VNPay
        END
        COMMIT;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        SET @rsp = '99';
    END CATCH;
    SELECT @lan AS lanGoiIPN, @rsp AS RspCode;   -- Java: response.getWriter().write("{\"RspCode\":\"" + rsp + "\",...}")
    SET @lan += 1;
END;
SELECT i.invoiceId, i.status, i.paidAt, p.txnRef, p.status AS paymentStatus
FROM Invoice i JOIN Payment p ON p.invoiceId = i.invoiceId WHERE i.invoiceId = 7;
GO

-- Q17 · UC14 · NT-45 · ReportDAO — báo cáo cho quản lý
-- Q17a Doanh thu ĐÃ THU theo từng tháng của 1 năm (theo ngày trả paidAt). Bảng 12 tháng: tháng không có tiền vẫn hiện 0.
--      Khoảng ngày viết ">= đầu năm AND < đầu năm sau" (không dùng BETWEEN với ngày giờ ⇒ sót giờ ngày cuối).
DECLARE @year INT = 2026;
SELECT t.thang, COALESCE(d.doanhThu, 0) AS doanhThu, COALESCE(d.soHoaDon, 0) AS soHoaDon
FROM (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12)) AS t(thang)
LEFT JOIN (
    SELECT MONTH(paidAt) AS thang, SUM(total) AS doanhThu, COUNT(*) AS soHoaDon
    FROM Invoice
    WHERE status = 'PAID' AND paidAt >= DATEFROMPARTS(@year, 1, 1) AND paidAt < DATEFROMPARTS(@year + 1, 1, 1)
    GROUP BY MONTH(paidAt)
) d ON d.thang = t.thang
ORDER BY t.thang;
GO
-- Q17b Công nợ: tổng còn nợ, trong đó quá hạn (UNPAID và dueDate < hôm nay)
SELECT COUNT(*) AS soHoaDonChuaTra,
       COALESCE(SUM(total), 0) AS tongConNo,
       COALESCE(SUM(CASE WHEN dueDate < CAST(SYSDATETIME() AS DATE) THEN total END), 0) AS noQuaHan
FROM Invoice WHERE status = 'UNPAID';
GO
-- Q17c Phòng đang thuê / trống / tỉ lệ lấp đầy (trên số phòng đang mở cho thuê — status ACTIVE)
--      "Đang thuê" = có hợp đồng ACTIVE, tính cả hợp đồng đã ký nhưng chưa tới ngày vào ở (P103 sau Q11b).
SELECT COUNT(*) AS soPhongMo,
       SUM(CASE WHEN c.contractId IS NOT NULL THEN 1 ELSE 0 END) AS dangThue,
       SUM(CASE WHEN c.contractId IS NULL     THEN 1 ELSE 0 END) AS phongTrong,
       CAST(100.0 * SUM(CASE WHEN c.contractId IS NOT NULL THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0) AS DECIMAL(5,1)) AS tiLeLapDayPhanTram
FROM Room r
LEFT JOIN Contract c ON c.roomId = r.roomId AND c.status = 'ACTIVE'
WHERE r.status = 'ACTIVE';
GO

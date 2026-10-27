/* =====================================================================
   setup.sql — CSDL "NhaTroSV" (PRJ301 — Quản lý nhà trọ sinh viên)
   ---------------------------------------------------------------------
   Chạy được NHIỀU LẦN: mỗi lần chạy sẽ XOÁ CSDL cũ rồi tạo lại từ đầu
   (dữ liệu tự gõ lúc thử sẽ mất — dữ liệu mẫu chung nằm ở cuối file này).

   Cách chạy:
     • SSMS (Windows): File → Open → setup.sql → Execute (F5).
     • Azure Data Studio / DBeaver (Mac): mở file → Run.
     • Dòng lệnh: sqlcmd -S localhost -U sa -P <mật khẩu> -C -i setup.sql

   File lưu dạng UTF-8 CÓ BOM để SSMS đọc đúng chữ tiếng Việt.
   Nếu thấy chữ "PhÃ²ng" thay vì "Phòng" ⇒ file bị mở sai mã hoá.

   Thứ tự trong file:
     0. Xoá CSDL cũ (nếu có) → tạo mới
     1. Tạo 11 bảng: bảng CHA trước, bảng CON sau
        Account → Tenant → RoomType → Room → UtilityPrice → RentalRequest
        → Contract → MeterReading → Invoice → Payment → MaintenanceRequest
     2. Chỉ mục (index) — gồm 4 chỉ mục UNIQUE CÓ LỌC ("luật không bao giờ phá")
     3. Dữ liệu mẫu
     4. Kiểm tra nhanh

   Quy ước đặt tên (cả nhóm thống nhất):
     • Bảng: số ít, viết hoa chữ đầu (Room). Cột: camelCase (roomCode).
     • Khoá chính: <tênBảng>Id, INT IDENTITY(1,1) (tự tăng từ 1).
     • Ràng buộc: PK_ khoá chính, FK_ khoá ngoại, UQ_ duy nhất, CK_ kiểm tra,
       DF_ mặc định, IX_ chỉ mục thường, UX_ chỉ mục duy nhất.
       Đặt tên rõ ràng để khi lỗi, thông báo của SQL Server nói luôn luật nào bị vi phạm.
   ===================================================================== */

-- Hai dòng SET này BẮT BUỘC với chỉ mục có lọc (filtered index).
-- SSMS và JDBC mặc định đã bật; riêng sqlcmd mặc định TẮT QUOTED_IDENTIFIER
-- ⇒ thiếu dòng này thì sqlcmd báo lỗi 1934 khi INSERT vào bảng Contract.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* ---------------------------------------------------------------------
   0. XOÁ CSDL CŨ → TẠO MỚI
   --------------------------------------------------------------------- */
USE master;   -- phải đứng ở master: không xoá được CSDL mình đang đứng trong
GO
IF DB_ID(N'NhaTroSV') IS NOT NULL
BEGIN
    -- Đá mọi kết nối đang mở (NetBeans, cửa sổ SSMS khác…) rồi mới xoá được
    ALTER DATABASE NhaTroSV SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE NhaTroSV;
END
GO
-- Ghi rõ collation để máy cả nhóm giống hệt nhau (CI = không phân biệt hoa/thường)
CREATE DATABASE NhaTroSV COLLATE SQL_Latin1_General_CP1_CI_AS;
GO
USE NhaTroSV;
GO

/* ---------------------------------------------------------------------
   1. TẠO BẢNG
   Kiểu dữ liệu:
     INT            số nguyên, khoá
     NVARCHAR(n)    chữ tiếng Việt có dấu (INSERT phải viết N'…')
     VARCHAR(n)     chữ không dấu: mã, username, email, trạng thái
     DECIMAL(12,0)  tiền VND (KHÔNG dùng FLOAT — FLOAT làm tròn sai)
     DATE           chỉ ngày;  DATETIME2(0) ngày + giờ, chính xác tới giây
     BIT            đúng/sai (1/0)
   --------------------------------------------------------------------- */

-- 1.1 Account — MỌI người đăng nhập (admin, quản lý, người thuê) chung MỘT bảng, phân biệt bằng role
CREATE TABLE Account (
    accountId    INT IDENTITY(1,1) NOT NULL,
    username     VARCHAR(30)   NOT NULL,
    passwordHash VARCHAR(255)  NOT NULL,          -- chuỗi băm PBKDF2, KHÔNG BAO GIỜ lưu mật khẩu thô
    fullName     NVARCHAR(100) NOT NULL,
    phone        VARCHAR(10)   NULL,
    email        VARCHAR(100)  NULL,
    role         VARCHAR(10)   NOT NULL,
    active       BIT           NOT NULL CONSTRAINT DF_Account_active    DEFAULT 1,   -- 0 = bị khoá
    createdAt    DATETIME2(0)  NOT NULL CONSTRAINT DF_Account_createdAt DEFAULT SYSDATETIME(),
    CONSTRAINT PK_Account          PRIMARY KEY (accountId),
    CONSTRAINT UQ_Account_username UNIQUE (username),
    CONSTRAINT CK_Account_role     CHECK (role IN ('ADMIN', 'MANAGER', 'TENANT')),
    CONSTRAINT CK_Account_username CHECK (LEN(username) BETWEEN 4 AND 30),
    -- Điện thoại: đúng 10 chữ số. '%[^0-9]%' nghĩa là "có ký tự nào KHÔNG phải số"
    CONSTRAINT CK_Account_phone    CHECK (phone IS NULL OR (LEN(phone) = 10 AND phone NOT LIKE '%[^0-9]%')),
    CONSTRAINT CK_Account_email    CHECK (email IS NULL OR email LIKE '%_@_%._%')
);
GO

-- 1.2 Tenant — thông tin riêng của NGƯỜI THUÊ. Quan hệ 1–1 với Account:
--     tenantId vừa là khoá chính, vừa là khoá ngoại trỏ về Account.accountId
CREATE TABLE Tenant (
    tenantId    INT           NOT NULL,
    idCardNo    VARCHAR(12)   NOT NULL,           -- số CCCD 12 chữ số
    school      NVARCHAR(100) NULL,
    dateOfBirth DATE          NULL,
    hometown    NVARCHAR(100) NULL,
    CONSTRAINT PK_Tenant          PRIMARY KEY (tenantId),
    CONSTRAINT FK_Tenant_Account  FOREIGN KEY (tenantId) REFERENCES Account(accountId),
    CONSTRAINT UQ_Tenant_idCardNo UNIQUE (idCardNo),
    CONSTRAINT CK_Tenant_idCardNo CHECK (LEN(idCardNo) = 12 AND idCardNo NOT LIKE '%[^0-9]%')
);
GO

-- 1.3 RoomType — danh mục loại phòng
CREATE TABLE RoomType (
    roomTypeId  INT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(50)  NOT NULL,
    capacity    INT           NOT NULL,           -- số người tối đa
    description NVARCHAR(255) NULL,
    CONSTRAINT PK_RoomType          PRIMARY KEY (roomTypeId),
    CONSTRAINT UQ_RoomType_name     UNIQUE (name),
    CONSTRAINT CK_RoomType_capacity CHECK (capacity BETWEEN 1 AND 8)
);
GO

-- 1.4 Room — từng phòng. KHÔNG có cột "đang có người thuê":
--     điều đó suy ra từ Contract (có hợp đồng ACTIVE hay không) ⇒ một thông tin chỉ lưu một chỗ.
CREATE TABLE Room (
    roomId       INT IDENTITY(1,1) NOT NULL,
    roomCode     VARCHAR(10)   NOT NULL,
    roomTypeId   INT           NOT NULL,
    floor        INT           NOT NULL,
    area         DECIMAL(5,1)  NULL,              -- m², ví dụ 18.5
    monthlyPrice DECIMAL(12,0) NOT NULL,          -- giá niêm yết HIỆN TẠI (hợp đồng chụp lại lúc ký)
    status       VARCHAR(12)   NOT NULL CONSTRAINT DF_Room_status DEFAULT 'ACTIVE',
    imageUrl     VARCHAR(255)  NULL,
    description  NVARCHAR(500) NULL,
    CONSTRAINT PK_Room              PRIMARY KEY (roomId),
    CONSTRAINT UQ_Room_roomCode     UNIQUE (roomCode),
    CONSTRAINT FK_Room_RoomType     FOREIGN KEY (roomTypeId) REFERENCES RoomType(roomTypeId),
    CONSTRAINT CK_Room_floor        CHECK (floor >= 1),
    CONSTRAINT CK_Room_area         CHECK (area IS NULL OR area > 0),
    CONSTRAINT CK_Room_monthlyPrice CHECK (monthlyPrice > 0),
    -- ACTIVE = đang mở cho thuê · MAINTENANCE = đang sửa · HIDDEN = ẩn (thay cho XOÁ)
    CONSTRAINT CK_Room_status       CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'HIDDEN'))
);
GO

-- 1.5 UtilityPrice — bảng giá điện, nước, dịch vụ. Đổi giá = THÊM dòng mới có ngày hiệu lực mới,
--     KHÔNG sửa dòng cũ ⇒ hoá đơn tháng cũ vẫn tính đúng giá lúc đó.
CREATE TABLE UtilityPrice (
    priceId             INT IDENTITY(1,1) NOT NULL,
    electricPrice       DECIMAL(12,0) NOT NULL,   -- đồng / kWh
    waterPrice          DECIMAL(12,0) NOT NULL,   -- đồng / m³
    serviceFeePerPerson DECIMAL(12,0) NOT NULL,   -- đồng / người / tháng (rác, wifi, vệ sinh)
    effectiveFrom       DATE          NOT NULL,   -- áp dụng từ ngày này
    createdAt           DATETIME2(0)  NOT NULL CONSTRAINT DF_UtilityPrice_createdAt DEFAULT SYSDATETIME(),
    CONSTRAINT PK_UtilityPrice               PRIMARY KEY (priceId),
    CONSTRAINT UQ_UtilityPrice_effectiveFrom UNIQUE (effectiveFrom),
    CONSTRAINT CK_UtilityPrice_prices        CHECK (electricPrice >= 0 AND waterPrice >= 0 AND serviceFeePerPerson >= 0)
);
GO

-- 1.6 RentalRequest — yêu cầu thuê phòng do người thuê gửi
--     Trạng thái: PENDING → APPROVED (quản lý duyệt) / REJECTED (quản lý từ chối) / CANCELLED (người thuê tự huỷ)
CREATE TABLE RentalRequest (
    requestId    INT IDENTITY(1,1) NOT NULL,
    tenantId     INT           NOT NULL,
    roomId       INT           NOT NULL,
    moveInDate   DATE          NOT NULL,
    months       INT           NOT NULL,          -- số tháng muốn thuê
    numPeople    INT           NOT NULL,          -- số người ở (≤ sức chứa: kiểm trong Java)
    note         NVARCHAR(500) NULL,              -- ghi chú của người thuê
    status       VARCHAR(10)   NOT NULL CONSTRAINT DF_RentalRequest_status    DEFAULT 'PENDING',
    createdAt    DATETIME2(0)  NOT NULL CONSTRAINT DF_RentalRequest_createdAt DEFAULT SYSDATETIME(),
    decidedAt    DATETIME2(0)  NULL,              -- lúc rời PENDING (duyệt / từ chối / huỷ)
    decidedBy    INT           NULL,              -- quản lý nào duyệt/từ chối (NULL nếu người thuê tự huỷ)
    decisionNote NVARCHAR(255) NULL,              -- lý do từ chối
    CONSTRAINT PK_RentalRequest           PRIMARY KEY (requestId),
    CONSTRAINT FK_RentalRequest_Tenant    FOREIGN KEY (tenantId)  REFERENCES Tenant(tenantId),
    CONSTRAINT FK_RentalRequest_Room      FOREIGN KEY (roomId)    REFERENCES Room(roomId),
    CONSTRAINT FK_RentalRequest_DecidedBy FOREIGN KEY (decidedBy) REFERENCES Account(accountId),
    CONSTRAINT CK_RentalRequest_months    CHECK (months BETWEEN 1 AND 24),
    CONSTRAINT CK_RentalRequest_numPeople CHECK (numPeople BETWEEN 1 AND 8),
    CONSTRAINT CK_RentalRequest_status    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    -- Trạng thái và cột "quyết định" phải khớp nhau:
    CONSTRAINT CK_RentalRequest_decision  CHECK (
           (status = 'PENDING'                 AND decidedAt IS NULL     AND decidedBy IS NULL)
        OR (status IN ('APPROVED', 'REJECTED') AND decidedAt IS NOT NULL AND decidedBy IS NOT NULL)
        OR (status = 'CANCELLED'               AND decidedAt IS NOT NULL AND decidedBy IS NULL))
);
GO

-- 1.7 Contract — hợp đồng thuê: GIAO DỊCH TRUNG TÂM, nối người thuê + phòng + yêu cầu đã duyệt.
--     monthlyRent là giá CHỤP LẠI lúc ký (mai giá phòng tăng, hợp đồng cũ không đổi theo).
--     Trạng thái: ACTIVE → ENDED
CREATE TABLE Contract (
    contractId         INT IDENTITY(1,1) NOT NULL,
    requestId          INT           NOT NULL,    -- duyệt từ yêu cầu nào (UNIQUE: 1 yêu cầu ⇒ tối đa 1 hợp đồng)
    tenantId           INT           NOT NULL,
    roomId             INT           NOT NULL,
    startDate          DATE          NOT NULL,
    endDate            DATE          NOT NULL,    -- ngày hết hạn dự kiến
    monthlyRent        DECIMAL(12,0) NOT NULL,    -- giá thuê chốt lúc ký
    deposit            DECIMAL(12,0) NOT NULL CONSTRAINT DF_Contract_deposit   DEFAULT 0,  -- tiền cọc
    numPeople          INT           NOT NULL,    -- số người ở ⇒ tính phí dịch vụ
    startElectricIndex INT           NOT NULL,    -- chỉ số điện lúc nhận phòng (tháng đầu dùng làm "chỉ số cũ")
    startWaterIndex    INT           NOT NULL,    -- chỉ số nước lúc nhận phòng
    status             VARCHAR(10)   NOT NULL CONSTRAINT DF_Contract_status    DEFAULT 'ACTIVE',
    createdAt          DATETIME2(0)  NOT NULL CONSTRAINT DF_Contract_createdAt DEFAULT SYSDATETIME(),
    endedAt            DATETIME2(0)  NULL,        -- lúc quản lý bấm "Kết thúc hợp đồng"
    CONSTRAINT PK_Contract               PRIMARY KEY (contractId),
    CONSTRAINT FK_Contract_RentalRequest FOREIGN KEY (requestId) REFERENCES RentalRequest(requestId),
    CONSTRAINT FK_Contract_Tenant        FOREIGN KEY (tenantId)  REFERENCES Tenant(tenantId),
    CONSTRAINT FK_Contract_Room          FOREIGN KEY (roomId)    REFERENCES Room(roomId),
    CONSTRAINT UQ_Contract_requestId     UNIQUE (requestId),
    CONSTRAINT CK_Contract_dates         CHECK (endDate > startDate),
    CONSTRAINT CK_Contract_money         CHECK (monthlyRent > 0 AND deposit >= 0),
    CONSTRAINT CK_Contract_numPeople     CHECK (numPeople BETWEEN 1 AND 8),
    CONSTRAINT CK_Contract_startIndex    CHECK (startElectricIndex >= 0 AND startWaterIndex >= 0),
    CONSTRAINT CK_Contract_status        CHECK (status IN ('ACTIVE', 'ENDED')),
    CONSTRAINT CK_Contract_endedAt       CHECK ((status = 'ACTIVE' AND endedAt IS NULL)
                                             OR (status = 'ENDED'  AND endedAt IS NOT NULL))
);
GO

-- 1.8 MeterReading — chỉ số điện nước CUỐI THÁNG của từng PHÒNG (đồng hồ gắn với phòng, không gắn người)
CREATE TABLE MeterReading (
    readingId     INT IDENTITY(1,1) NOT NULL,
    roomId        INT          NOT NULL,
    [year]        INT          NOT NULL,          -- year/month là từ khoá của T-SQL nên bọc [ ] cho chắc
    [month]       INT          NOT NULL,
    electricIndex INT          NOT NULL,          -- số trên đồng hồ điện (kWh)
    waterIndex    INT          NOT NULL,          -- số trên đồng hồ nước (m³)
    recordedAt    DATETIME2(0) NOT NULL CONSTRAINT DF_MeterReading_recordedAt DEFAULT SYSDATETIME(),
    CONSTRAINT PK_MeterReading               PRIMARY KEY (readingId),
    CONSTRAINT FK_MeterReading_Room          FOREIGN KEY (roomId) REFERENCES Room(roomId),
    CONSTRAINT UQ_MeterReading_RoomYearMonth UNIQUE (roomId, [year], [month]),   -- mỗi phòng ghi 1 lần mỗi tháng
    CONSTRAINT CK_MeterReading_period        CHECK ([year] BETWEEN 2020 AND 2100 AND [month] BETWEEN 1 AND 12),
    CONSTRAINT CK_MeterReading_index         CHECK (electricIndex >= 0 AND waterIndex >= 0)
    -- "chỉ số mới ≥ chỉ số tháng trước" là luật GIỮA 2 DÒNG ⇒ kiểm trong Java (BillingService)
);
GO

-- 1.9 Invoice — hoá đơn tháng của một hợp đồng. Mọi khoản tiền là số CHỤP LẠI lúc lập hoá đơn.
--     Trạng thái: UNPAID → PAID (CHỈ IPN của VNPay được chuyển sang PAID).
--     "Quá hạn" KHÔNG phải trạng thái lưu: quá hạn = UNPAID và dueDate < hôm nay (tính lúc truy vấn).
CREATE TABLE Invoice (
    invoiceId    INT IDENTITY(1,1) NOT NULL,
    contractId   INT           NOT NULL,
    priceId      INT           NOT NULL,          -- bảng giá điện nước đã dùng để tính
    [year]       INT           NOT NULL,
    [month]      INT           NOT NULL,
    roomFee      DECIMAL(12,0) NOT NULL,          -- = Contract.monthlyRent
    electricUsed INT           NOT NULL,          -- số kWh = chỉ số mới − chỉ số cũ
    electricFee  DECIMAL(12,0) NOT NULL,          -- = electricUsed × electricPrice
    waterUsed    INT           NOT NULL,          -- số m³
    waterFee     DECIMAL(12,0) NOT NULL,          -- = waterUsed × waterPrice
    serviceFee   DECIMAL(12,0) NOT NULL,          -- = serviceFeePerPerson × Contract.numPeople
    total        DECIMAL(12,0) NOT NULL,          -- = tổng 4 khoản (CHECK bên dưới bắt buộc khớp)
    status       VARCHAR(10)   NOT NULL CONSTRAINT DF_Invoice_status    DEFAULT 'UNPAID',
    dueDate      DATE          NOT NULL,          -- hạn trả: mặc định ngày 10 tháng sau
    createdAt    DATETIME2(0)  NOT NULL CONSTRAINT DF_Invoice_createdAt DEFAULT SYSDATETIME(),
    paidAt       DATETIME2(0)  NULL,              -- CHỈ IPN VNPay ghi cột này
    CONSTRAINT PK_Invoice                   PRIMARY KEY (invoiceId),
    CONSTRAINT FK_Invoice_Contract          FOREIGN KEY (contractId) REFERENCES Contract(contractId),
    CONSTRAINT FK_Invoice_UtilityPrice      FOREIGN KEY (priceId)    REFERENCES UtilityPrice(priceId),
    CONSTRAINT UQ_Invoice_ContractYearMonth UNIQUE (contractId, [year], [month]),   -- 1 hợp đồng 1 hoá đơn mỗi tháng
    CONSTRAINT CK_Invoice_period            CHECK ([year] BETWEEN 2020 AND 2100 AND [month] BETWEEN 1 AND 12),
    CONSTRAINT CK_Invoice_amounts           CHECK (roomFee >= 0 AND electricUsed >= 0 AND electricFee >= 0
                                               AND waterUsed >= 0 AND waterFee >= 0 AND serviceFee >= 0),
    CONSTRAINT CK_Invoice_total             CHECK (total = roomFee + electricFee + waterFee + serviceFee),
    CONSTRAINT CK_Invoice_status            CHECK (status IN ('UNPAID', 'PAID')),
    CONSTRAINT CK_Invoice_paidAt            CHECK ((status = 'UNPAID' AND paidAt IS NULL)
                                                OR (status = 'PAID'   AND paidAt IS NOT NULL))
);
GO

-- 1.10 Payment — mỗi LẦN THỬ thanh toán VNPay là một dòng (thử hỏng rồi thử lại ⇒ nhiều dòng).
--      Trạng thái: PENDING (vừa chuyển sang VNPay) → SUCCESS / FAILED (theo IPN)
CREATE TABLE Payment (
    paymentId        INT IDENTITY(1,1) NOT NULL,
    invoiceId        INT           NOT NULL,
    txnRef           VARCHAR(50)   NOT NULL,      -- vnp_TxnRef mình gửi đi, ví dụ 'INV5-20261003084000'
    amount           DECIMAL(12,0) NOT NULL,      -- số tiền VND thật (gửi VNPay thì nhân 100 thành vnp_Amount)
    status           VARCHAR(10)   NOT NULL CONSTRAINT DF_Payment_status    DEFAULT 'PENDING',
    vnpTransactionNo VARCHAR(20)   NULL,          -- mã giao dịch phía VNPay trả về
    bankCode         VARCHAR(20)   NULL,          -- ví dụ 'NCB'
    responseCode     VARCHAR(5)    NULL,          -- vnp_ResponseCode: '00' thành công, '24' khách huỷ…
    createdAt        DATETIME2(0)  NOT NULL CONSTRAINT DF_Payment_createdAt DEFAULT SYSDATETIME(),
    paidAt           DATETIME2(0)  NULL,          -- lúc IPN xác nhận thành công
    CONSTRAINT PK_Payment         PRIMARY KEY (paymentId),
    CONSTRAINT FK_Payment_Invoice FOREIGN KEY (invoiceId) REFERENCES Invoice(invoiceId),
    CONSTRAINT UQ_Payment_txnRef  UNIQUE (txnRef),
    CONSTRAINT CK_Payment_amount  CHECK (amount > 0),
    CONSTRAINT CK_Payment_status  CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    CONSTRAINT CK_Payment_paidAt  CHECK ((status = 'SUCCESS' AND paidAt IS NOT NULL)
                                      OR (status <> 'SUCCESS' AND paidAt IS NULL))
);
GO

-- 1.11 MaintenanceRequest — sự cố người thuê báo (gắn với HỢP ĐỒNG ⇒ biết phòng nào, ai báo)
--      Trạng thái: OPEN → IN_PROGRESS → DONE
CREATE TABLE MaintenanceRequest (
    ticketId    INT IDENTITY(1,1) NOT NULL,
    contractId  INT            NOT NULL,
    title       NVARCHAR(150)  NOT NULL,
    description NVARCHAR(1000) NULL,
    status      VARCHAR(12)    NOT NULL CONSTRAINT DF_MaintenanceRequest_status    DEFAULT 'OPEN',
    managerNote NVARCHAR(500)  NULL,              -- ghi chú của quản lý (người thuê xem được)
    createdAt   DATETIME2(0)   NOT NULL CONSTRAINT DF_MaintenanceRequest_createdAt DEFAULT SYSDATETIME(),
    resolvedAt  DATETIME2(0)   NULL,              -- ghi khi chuyển sang DONE
    CONSTRAINT PK_MaintenanceRequest          PRIMARY KEY (ticketId),
    CONSTRAINT FK_MaintenanceRequest_Contract FOREIGN KEY (contractId) REFERENCES Contract(contractId),
    CONSTRAINT CK_MaintenanceRequest_status   CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT CK_MaintenanceRequest_resolved CHECK ((status = 'DONE' AND resolvedAt IS NOT NULL)
                                                  OR (status <> 'DONE' AND resolvedAt IS NULL))
);
GO

/* ---------------------------------------------------------------------
   2. CHỈ MỤC (INDEX)
   PRIMARY KEY và UNIQUE TỰ có chỉ mục. Cột KHOÁ NGOẠI thì KHÔNG tự có
   ⇒ tạo cho những cột hay dùng để JOIN / lọc.
   (Invoice.contractId, MeterReading.roomId không cần tạo thêm: chúng đứng ĐẦU
    trong UNIQUE (contractId, year, month) / (roomId, year, month) nên đã có sẵn.)
   --------------------------------------------------------------------- */

-- 2.1 BỐN CHỈ MỤC UNIQUE CÓ LỌC — "luật không bao giờ phá".
--     UNIQUE thường cấm trùng trên MỌI dòng; UNIQUE có lọc chỉ cấm trùng trong các dòng thoả WHERE.

-- (1) Một phòng tại một thời điểm chỉ có MỘT hợp đồng ACTIVE.
--     (UNIQUE(roomId) thường sẽ cấm cả hợp đồng cũ đã ENDED — sai, vì phòng cho thuê nhiều lần.)
--     Chặn được cả trường hợp 2 quản lý cùng bấm "Duyệt" trong cùng một giây.
CREATE UNIQUE INDEX UX_Contract_ActiveRoom   ON Contract(roomId)   WHERE status = 'ACTIVE';
-- (2) Một người thuê chỉ có MỘT hợp đồng ACTIVE.
CREATE UNIQUE INDEX UX_Contract_ActiveTenant ON Contract(tenantId) WHERE status = 'ACTIVE';
-- (3) Một người thuê chỉ có MỘT yêu cầu thuê đang chờ (PENDING) — bấm "Gửi" 2 lần liền cũng không lọt.
CREATE UNIQUE INDEX UX_RentalRequest_PendingTenant ON RentalRequest(tenantId) WHERE status = 'PENDING';
-- (4) Một hoá đơn chỉ có MỘT lần thanh toán SUCCESS — IPN VNPay gọi 2 lần cũng không ghi 2 lần.
CREATE UNIQUE INDEX UX_Payment_SuccessInvoice ON Payment(invoiceId) WHERE status = 'SUCCESS';
GO

-- 2.2 Chỉ mục thường cho các tra cứu chính
CREATE INDEX IX_Room_Status_Price      ON Room(status, monthlyPrice);           -- UC01 tìm phòng theo giá
CREATE INDEX IX_Room_RoomType          ON Room(roomTypeId);                     -- UC01 lọc loại phòng
CREATE INDEX IX_RentalRequest_RoomStat ON RentalRequest(roomId, status);        -- UC09 yêu cầu PENDING của 1 phòng
CREATE INDEX IX_RentalRequest_Tenant   ON RentalRequest(tenantId);              -- UC05 yêu cầu của tôi
CREATE INDEX IX_Contract_Tenant        ON Contract(tenantId);                   -- UC05/UC06 hợp đồng của tôi (cả cũ)
CREATE INDEX IX_Contract_Room          ON Contract(roomId);                     -- lịch sử thuê của 1 phòng
CREATE INDEX IX_Invoice_Status_Due     ON Invoice(status, dueDate);             -- UC14 nợ quá hạn
CREATE INDEX IX_Invoice_PaidAt         ON Invoice(paidAt);                      -- UC14 doanh thu theo tháng
CREATE INDEX IX_Payment_Invoice        ON Payment(invoiceId);                   -- UC07 các lần thử trả của 1 hoá đơn
CREATE INDEX IX_Maintenance_Contract   ON MaintenanceRequest(contractId);       -- UC08 sự cố của tôi
CREATE INDEX IX_Maintenance_Status     ON MaintenanceRequest(status);           -- UC12 lọc theo trạng thái
GO

/* ---------------------------------------------------------------------
   3. DỮ LIỆU MẪU (chung cho cả nhóm)
   "Hôm nay" của bộ dữ liệu: giữa tháng 10/2026.

   ★ MẬT KHẨU THỬ của MỌI tài khoản mẫu:   NhaTro@2026
     Cột passwordHash lưu chuỗi băm theo đúng định dạng của util/PasswordHasher (ASM.4):
         pbkdf2_sha256$<số vòng>$<salt Base64>$<hash Base64>
         - thuật toán PBKDF2WithHmacSHA256, 600000 vòng, salt 16 byte ngẫu nhiên, khoá 256 bit
     Mỗi tài khoản có salt KHÁC nhau ⇒ cùng một mật khẩu nhưng chuỗi băm khác nhau (đó là mục đích của salt).
     Cách sinh lại (Java, khi Khoa đã có PasswordHasher):
         System.out.println(PasswordHasher.hash("NhaTro@2026"));
     Hoặc Python (cho ra đúng định dạng, Java verify được):
         import hashlib,os,base64; s=os.urandom(16)
         k=hashlib.pbkdf2_hmac('sha256',b'NhaTro@2026',s,600000,32)
         print('pbkdf2_sha256$600000$'+base64.b64encode(s).decode()+'$'+base64.b64encode(k).decode())
     Các chuỗi dưới đây ĐÃ được kiểm bằng code verify() của ASM.4 (Java) ⇒ đăng nhập được thật.
   --------------------------------------------------------------------- */

-- 3.1 Tài khoản: 1 admin, 2 quản lý (1 đang bị khoá để thử UC15), 7 người thuê
--     SET IDENTITY_INSERT ON để tự ghi id ⇒ id cố định, cả nhóm nói "tài khoản số 4" là cùng một người.
SET IDENTITY_INSERT Account ON;
INSERT INTO Account (accountId, username, passwordHash, fullName, phone, email, role, active, createdAt) VALUES
 (1,  'admin',     'pbkdf2_sha256$600000$NZ++O9GJkDd5D/JwcnRziQ==$XpTFdHtp1Ks79yzG7R5Om+FFF8o6zFlPrfz/rqIrO9k=', N'Trần Quốc Bảo',   '0912000001', 'admin@nhatrosv.vn',     'ADMIN',   1, '2026-05-01 08:00:00'),
 (2,  'quanly',    'pbkdf2_sha256$600000$IUls0lZl/CAfvEia7e77wA==$4A0uNk8v0tBwyqgqKRB55XgcsQ2W3ciMWbsd09EEHt8=', N'Lê Thị Hoa',      '0912000002', 'hoa.le@nhatrosv.vn',    'MANAGER', 1, '2026-05-01 08:10:00'),
 (3,  'quanly2',   'pbkdf2_sha256$600000$4ppcV0BzbBKnRslIFktDWQ==$n60tD8kkKHesjawulyfw12uMT5u8rbRV/QXEKzLbIi0=', N'Đỗ Văn Nam',      '0912000003', 'nam.do@nhatrosv.vn',    'MANAGER', 0, '2026-05-02 09:00:00'),
 (4,  'an.nguyen', 'pbkdf2_sha256$600000$LrhQcaDV5ZzsmfzJKKGZlw==$ic2JN1yxQEg4NO3HyjVqyo7t/rI4iQ8/MWQxM2c4P5U=', N'Nguyễn Văn An',   '0987000004', 'an.nguyen@gmail.com',   'TENANT',  1, '2026-07-20 20:15:00'),
 (5,  'binh.tran', 'pbkdf2_sha256$600000$QCNo7dfacGHKIrKREFes7A==$SO1Nqs+dcyW0FLdAoJsIAZDm67F3n2VidTOvAod+K58=', N'Trần Thị Bình',   '0987000005', 'binh.tran@gmail.com',   'TENANT',  1, '2026-07-21 19:40:00'),
 (6,  'chi.le',    'pbkdf2_sha256$600000$/cPOScBsjgimC/WER2W3yQ==$wUqhSpXgcnGZc5wbxP20edIZIzQJFggJ1P/7JKe0yr8=', N'Lê Minh Chi',     '0987000006', 'chi.le@gmail.com',      'TENANT',  1, '2026-08-18 21:05:00'),
 (7,  'dung.pham', 'pbkdf2_sha256$600000$50IWfjdRzAG47N0Fr2tZEw==$CvQ+6YQM6rVrQPmLrfqb3RFpkXAmGrUNBCNwLhzpalE=', N'Phạm Tiến Dũng',  '0987000007', 'dung.pham@gmail.com',   'TENANT',  1, '2026-08-19 10:30:00'),
 (8,  'ha.vu',     'pbkdf2_sha256$600000$FFG5+t0FgWxZeTClrYJoIw==$XB9Zs0tjv1srIRoNPXCnVPZrkCvIYJ91eMuQoS/IqYI=', N'Vũ Thu Hà',       '0987000008', 'ha.vu@gmail.com',       'TENANT',  1, '2026-09-10 14:00:00'),
 (9,  'khanh.do',  'pbkdf2_sha256$600000$3d3EXH+kqFVpVrRbwI9wlA==$vhN7XzQwnZEgpFUoobNR+8xhbuD61OPlV7s2zwf7OjE=', N'Đỗ Quốc Khánh',   '0987000009', 'khanh.do@gmail.com',    'TENANT',  1, '2026-05-15 16:20:00'),
 (10, 'lan.hoang', 'pbkdf2_sha256$600000$rEG8lS/eBonwlJgdGj7OSw==$hHOJDN82tFtI4xA4NtLr/DrlqvQzq3ETcIkpORunIb4=', N'Hoàng Ngọc Lan',  '0987000010', 'lan.hoang@gmail.com',   'TENANT',  1, '2026-07-22 22:10:00');
SET IDENTITY_INSERT Account OFF;

-- 3.2 Hồ sơ người thuê (tenantId = accountId của tài khoản TENANT)
INSERT INTO Tenant (tenantId, idCardNo, school, dateOfBirth, hometown) VALUES
 (4,  '001206012345', N'Đại học FPT Hà Nội',              '2006-03-14', N'Hà Nội'),
 (5,  '034306004567', N'Đại học FPT Hà Nội',              '2006-07-02', N'Thái Bình'),
 (6,  '036205007890', N'Đại học Quốc gia Hà Nội',         '2005-11-25', N'Nam Định'),
 (7,  '038206001122', N'Đại học FPT Hà Nội',              '2006-01-09', N'Thanh Hoá'),
 (8,  '025306003344', N'Học viện Công nghệ Bưu chính Viễn thông', '2006-09-30', N'Phú Thọ'),
 (9,  '040205005566', N'Đại học FPT Hà Nội',              '2005-05-18', N'Nghệ An'),
 (10, '001306007788', N'Đại học Bách khoa Hà Nội',        '2006-12-01', N'Hà Nội');

-- 3.3 Loại phòng (loại "Phòng gác lửng" chưa có phòng nào ⇒ thử LEFT JOIN đếm ra 0)
SET IDENTITY_INSERT RoomType ON;
INSERT INTO RoomType (roomTypeId, name, capacity, description) VALUES
 (1, N'Phòng đơn',      1, N'Giường đơn, bàn học, WC riêng'),
 (2, N'Phòng đôi',      2, N'2 giường đơn hoặc 1 giường đôi, WC riêng'),
 (3, N'Phòng 4 người',  4, N'2 giường tầng, ban công, WC riêng'),
 (4, N'Phòng gác lửng', 3, N'Có gác lửng, mới thêm — chưa có phòng');
SET IDENTITY_INSERT RoomType OFF;

-- 3.4 Phòng: 12 phòng, 3 tầng. 9 ACTIVE · 2 MAINTENANCE · 1 HIDDEN (P302: đã từng cho thuê nên ẩn chứ không xoá)
SET IDENTITY_INSERT Room ON;
INSERT INTO Room (roomId, roomCode, roomTypeId, floor, area, monthlyPrice, status, description) VALUES
 (1,  'P101', 1, 1, 18.0, 1800000, 'ACTIVE',      N'Gần cầu thang, mát'),
 (2,  'P102', 2, 1, 22.0, 2500000, 'ACTIVE',      N'Cửa sổ hướng Đông'),
 (3,  'P103', 2, 1, 22.0, 2500000, 'ACTIVE',      N'Mới sơn lại tháng 9'),
 (4,  'P104', 1, 1, 16.5, 1700000, 'MAINTENANCE', N'Đang sửa đường nước'),
 (5,  'P201', 3, 2, 30.0, 3600000, 'ACTIVE',      N'Ban công rộng'),
 (6,  'P202', 1, 2, 18.0, 1900000, 'ACTIVE',      NULL),
 (7,  'P203', 2, 2, 24.0, 2700000, 'ACTIVE',      N'Có điều hoà'),
 (8,  'P204', 3, 2, 32.0, 3800000, 'ACTIVE',      N'Có điều hoà, 2 cửa sổ'),
 (9,  'P301', 1, 3, 18.0, 1900000, 'ACTIVE',      N'Yên tĩnh'),
 (10, 'P302', 2, 3, 24.0, 2700000, 'HIDDEN',      N'Tạm ngừng cho thuê'),
 (11, 'P303', 3, 3, 32.0, 3800000, 'MAINTENANCE', N'Đang chống thấm trần'),
 (12, 'P304', 2, 3, 25.0, 2800000, 'ACTIVE',      N'Góc, nhiều ánh sáng');
SET IDENTITY_INSERT Room OFF;

-- 3.5 Bảng giá: 2 mốc hiệu lực ⇒ hoá đơn tháng 6–8 tính giá cũ, tháng 9 trở đi tính giá mới
SET IDENTITY_INSERT UtilityPrice ON;
INSERT INTO UtilityPrice (priceId, electricPrice, waterPrice, serviceFeePerPerson, effectiveFrom, createdAt) VALUES
 (1, 3500, 15000, 50000, '2026-01-01', '2025-12-20 09:00:00'),
 (2, 3800, 18000, 60000, '2026-09-01', '2026-08-25 09:00:00');
SET IDENTITY_INSERT UtilityPrice OFF;

-- 3.6 Yêu cầu thuê — có đủ 4 trạng thái
--   R1  khánh → P302  APPROVED (hợp đồng C1, đã ENDED)
--   R2  an    → P101  APPROVED (C2)          R3 bình → P102 APPROVED (C3)
--   R4  lan   → P102  REJECTED (tự động từ chối khi R3 được duyệt — cùng phòng)
--   R5  chi   → P201  APPROVED (C4)          R6 dũng → P203 APPROVED (C5)
--   R7  hà    → P202  CANCELLED (người thuê tự huỷ)
--   R8  khánh → P103  PENDING  ┐ HAI yêu cầu chờ CÙNG phòng P103 ⇒ dùng để thử duyệt (UC09)
--   R9  hà    → P103  PENDING  ┘ và thử "2 quản lý cùng bấm Duyệt"
--   R10 lan   → P304  PENDING
SET IDENTITY_INSERT RentalRequest ON;
INSERT INTO RentalRequest (requestId, tenantId, roomId, moveInDate, months, numPeople, note, status, createdAt, decidedAt, decidedBy, decisionNote) VALUES
 (1,  9,  10, '2026-06-01',  2, 2, N'Thuê 2 tháng hè, ở cùng em trai',        'APPROVED',  '2026-05-20 20:00:00', '2026-05-22 09:00:00', 2, NULL),
 (2,  4,  1,  '2026-08-01', 12, 1, N'Em là sinh viên năm 2',                  'APPROVED',  '2026-07-20 20:30:00', '2026-07-25 10:00:00', 2, NULL),
 (3,  5,  2,  '2026-08-01', 12, 2, N'Ở cùng bạn cùng lớp',                    'APPROVED',  '2026-07-21 19:50:00', '2026-07-25 10:05:00', 2, NULL),
 (4,  10, 2,  '2026-08-01',  6, 2, NULL,                                      'REJECTED',  '2026-07-22 22:20:00', '2026-07-25 10:05:00', 2, N'Phòng đã được duyệt cho yêu cầu khác'),
 (5,  6,  5,  '2026-09-01', 10, 3, N'Nhóm 3 bạn cùng trường',                 'APPROVED',  '2026-08-18 21:15:00', '2026-08-20 08:30:00', 2, NULL),
 (6,  7,  7,  '2026-09-01',  6, 2, NULL,                                      'APPROVED',  '2026-08-19 10:40:00', '2026-08-20 08:35:00', 2, NULL),
 (7,  8,  6,  '2026-10-01',  6, 1, NULL,                                      'CANCELLED', '2026-09-10 14:10:00', '2026-09-20 18:00:00', NULL, NULL),
 (8,  9,  3,  '2026-11-01', 12, 2, N'Ở cùng em trai như hè vừa rồi',          'PENDING',   '2026-10-12 21:00:00', NULL, NULL, NULL),
 (9,  8,  3,  '2026-11-01',  6, 1, N'Cần phòng gần trường',                   'PENDING',   '2026-10-14 19:30:00', NULL, NULL, NULL),
 (10, 10, 12, '2026-11-01', 12, 2, N'Muốn xem phòng trước cuối tuần',         'PENDING',   '2026-10-15 22:00:00', NULL, NULL, NULL);
SET IDENTITY_INSERT RentalRequest OFF;

-- 3.7 Hợp đồng: 4 ACTIVE + 1 ENDED
--   C1 có monthlyRent 2.600.000 trong khi phòng P302 nay niêm yết 2.700.000;
--   C4 có 3.500.000 trong khi P201 nay 3.600.000 ⇒ minh hoạ "giá CHỤP LẠI lúc ký".
SET IDENTITY_INSERT Contract ON;
INSERT INTO Contract (contractId, requestId, tenantId, roomId, startDate, endDate, monthlyRent, deposit, numPeople, startElectricIndex, startWaterIndex, status, createdAt, endedAt) VALUES
 (1, 1, 9, 10, '2026-06-01', '2026-08-01', 2600000, 2600000, 2, 1200, 300,  'ENDED',  '2026-05-22 09:00:00', '2026-07-31 18:00:00'),
 (2, 2, 4, 1,  '2026-08-01', '2027-08-01', 1800000, 1800000, 1, 2350, 410,  'ACTIVE', '2026-07-25 10:00:00', NULL),
 (3, 3, 5, 2,  '2026-08-01', '2027-08-01', 2500000, 2500000, 2, 5120, 820,  'ACTIVE', '2026-07-25 10:05:00', NULL),
 (4, 5, 6, 5,  '2026-09-01', '2027-07-01', 3500000, 3500000, 3, 8040, 1215, 'ACTIVE', '2026-08-20 08:30:00', NULL),
 (5, 6, 7, 7,  '2026-09-01', '2027-03-01', 2700000, 2700000, 2, 3105, 640,  'ACTIVE', '2026-08-20 08:35:00', NULL);
SET IDENTITY_INSERT Contract OFF;

-- 3.8 Chỉ số điện nước cuối tháng (tháng 6–7: phòng P302 của C1; tháng 8–9: các phòng đang thuê)
--   P201, P203 ghi tháng 8 = chỉ số lúc bàn giao cho C4, C5 (bằng startElectricIndex/startWaterIndex)
INSERT INTO MeterReading (roomId, [year], [month], electricIndex, waterIndex, recordedAt) VALUES
 (10, 2026, 6, 1330, 309,  '2026-06-30 17:00:00'),
 (10, 2026, 7, 1480, 319,  '2026-07-31 17:00:00'),
 (1,  2026, 8, 2440, 414,  '2026-08-31 17:00:00'),
 (2,  2026, 8, 5300, 829,  '2026-08-31 17:05:00'),
 (5,  2026, 8, 8040, 1215, '2026-08-31 17:10:00'),
 (7,  2026, 8, 3105, 640,  '2026-08-31 17:15:00'),
 (1,  2026, 9, 2535, 418,  '2026-09-30 17:00:00'),
 (2,  2026, 9, 5470, 837,  '2026-09-30 17:05:00'),
 (5,  2026, 9, 8290, 1233, '2026-09-30 17:10:00'),
 (7,  2026, 9, 3245, 652,  '2026-09-30 17:15:00');

-- 3.9 Hoá đơn — mọi con số TÍNH ĐÚNG theo công thức (NT-17), mục 4 kiểm lại bằng SQL:
--   Tổng = tiền phòng + (điện mới − cũ) × giá điện + (nước mới − cũ) × giá nước + phí dịch vụ × số người
--   Ví dụ I5 (An, P101, tháng 9, giá mốc 2): 1.800.000 + 95×3.800 + 4×18.000 + 60.000×1
--                                          = 1.800.000 + 361.000 + 72.000 + 60.000 = 2.293.000
--   Ví dụ I7 (Chi, P201, 3 người, tháng 9): 3.500.000 + 250×3.800 + 18×18.000 + 60.000×3 = 4.954.000
--   Trạng thái: I1–I5, I8 PAID · I6 UNPAID QUÁ HẠN (hạn 10/10) · I7 UNPAID được gia hạn tới 31/10
SET IDENTITY_INSERT Invoice ON;
INSERT INTO Invoice (invoiceId, contractId, priceId, [year], [month], roomFee, electricUsed, electricFee, waterUsed, waterFee, serviceFee, total, status, dueDate, createdAt, paidAt) VALUES
 (1, 1, 1, 2026, 6, 2600000, 130, 455000,  9,  135000, 100000, 3290000, 'PAID',   '2026-07-10', '2026-07-01 08:00:00', '2026-07-06 19:16:00'),
 (2, 1, 1, 2026, 7, 2600000, 150, 525000,  10, 150000, 100000, 3375000, 'PAID',   '2026-08-10', '2026-07-31 16:30:00', '2026-07-31 16:56:00'),
 (3, 2, 1, 2026, 8, 1800000, 90,  315000,  4,  60000,  50000,  2225000, 'PAID',   '2026-09-10', '2026-09-01 08:00:00', '2026-09-05 10:11:00'),
 (4, 3, 1, 2026, 8, 2500000, 180, 630000,  9,  135000, 100000, 3365000, 'PAID',   '2026-09-10', '2026-09-01 08:00:00', '2026-09-09 21:26:00'),
 (5, 2, 2, 2026, 9, 1800000, 95,  361000,  4,  72000,  60000,  2293000, 'PAID',   '2026-10-10', '2026-10-01 08:00:00', '2026-10-03 08:41:00'),
 (6, 3, 2, 2026, 9, 2500000, 170, 646000,  8,  144000, 120000, 3410000, 'UNPAID', '2026-10-10', '2026-10-01 08:00:00', NULL),
 (7, 4, 2, 2026, 9, 3500000, 250, 950000,  18, 324000, 180000, 4954000, 'UNPAID', '2026-10-31', '2026-10-01 08:00:00', NULL),
 (8, 5, 2, 2026, 9, 2700000, 140, 532000,  12, 216000, 120000, 3568000, 'PAID',   '2026-10-10', '2026-10-01 08:00:00', '2026-10-08 19:31:00');
SET IDENTITY_INSERT Invoice OFF;
-- (I2 là hoá đơn tháng cuối của C1: lập lúc trả phòng 31/07, khách trả ngay qua VNPay.)

-- 3.10 Thanh toán VNPay (mỗi lần thử = 1 dòng). txnRef = 'INV<invoiceId>-<yyyyMMddHHmmss>'
--   I4 thử lần 1 khách huỷ (24) rồi lần 2 thành công · I6 thử 1 lần thất bại (51 = không đủ số dư)
--   I7 có 1 lần PENDING (khách mở trang VNPay rồi đóng, chưa có IPN) ⇒ queries-mau.sql thử IPN cho dòng này
SET IDENTITY_INSERT Payment ON;
INSERT INTO Payment (paymentId, invoiceId, txnRef, amount, status, vnpTransactionNo, bankCode, responseCode, createdAt, paidAt) VALUES
 (1, 1, 'INV1-20260706191500', 3290000, 'SUCCESS', '14512001', 'NCB', '00', '2026-07-06 19:15:00', '2026-07-06 19:16:00'),
 (2, 2, 'INV2-20260731165500', 3375000, 'SUCCESS', '14598322', 'NCB', '00', '2026-07-31 16:55:00', '2026-07-31 16:56:00'),
 (3, 3, 'INV3-20260905101000', 2225000, 'SUCCESS', '14630417', 'NCB', '00', '2026-09-05 10:10:00', '2026-09-05 10:11:00'),
 (4, 4, 'INV4-20260908200000', 3365000, 'FAILED',  NULL,       'NCB', '24', '2026-09-08 20:00:00', NULL),
 (5, 4, 'INV4-20260909212500', 3365000, 'SUCCESS', '14652980', 'NCB', '00', '2026-09-09 21:25:00', '2026-09-09 21:26:00'),
 (6, 5, 'INV5-20261003084000', 2293000, 'SUCCESS', '14701153', 'NCB', '00', '2026-10-03 08:40:00', '2026-10-03 08:41:00'),
 (7, 6, 'INV6-20261012220000', 3410000, 'FAILED',  '14735590', 'NCB', '51', '2026-10-12 22:00:00', NULL),
 (8, 7, 'INV7-20261016213000', 4954000, 'PENDING', NULL,       NULL,  NULL, '2026-10-16 21:30:00', NULL),
 (9, 8, 'INV8-20261008193000', 3568000, 'SUCCESS', '14720864', 'NCB', '00', '2026-10-08 19:30:00', '2026-10-08 19:31:00');
SET IDENTITY_INSERT Payment OFF;

-- 3.11 Sự cố — đủ 3 trạng thái (có cả sự cố của hợp đồng đã ENDED: lịch sử vẫn giữ)
SET IDENTITY_INSERT MaintenanceRequest ON;
INSERT INTO MaintenanceRequest (ticketId, contractId, title, description, status, managerNote, createdAt, resolvedAt) VALUES
 (1, 1, N'Ổ cắm cạnh bàn học bị lỏng',  N'Cắm sạc laptop hay bị chập chờn',            'DONE',        N'Đã thay ổ cắm mới',             '2026-06-15 21:00:00', '2026-06-16 10:00:00'),
 (2, 2, N'Bóng đèn nhà vệ sinh bị cháy', NULL,                                          'DONE',        N'Đã thay bóng LED',              '2026-08-20 22:10:00', '2026-08-21 09:30:00'),
 (3, 5, N'Khoá cửa bị kẹt',              N'Phải xoay nhiều lần mới mở được',            'DONE',        N'Đã tra dầu và chỉnh lại ổ khoá','2026-09-12 07:45:00', '2026-09-12 17:00:00'),
 (4, 3, N'Vòi sen rò nước',              N'Nước rỉ liên tục kể cả khi đã khoá',         'IN_PROGRESS', N'Đã hẹn thợ chiều 18/10',        '2026-10-14 20:00:00', NULL),
 (5, 4, N'Điều hoà chảy nước',           N'Nước nhỏ giọt xuống giường tầng dưới',       'OPEN',        NULL,                             '2026-10-16 23:00:00', NULL);
SET IDENTITY_INSERT MaintenanceRequest OFF;
GO

/* ---------------------------------------------------------------------
   4. KIỂM TRA NHANH — chạy xong setup, nhìn kết quả các câu dưới đây.
   Câu nào ghi "phải ra 0 dòng" mà ra dòng ⇒ dữ liệu mẫu có vấn đề.
   --------------------------------------------------------------------- */

-- 4.1 Số dòng mỗi bảng (Room phải ra 12 — NT-84 dùng câu này)
SELECT 'Account' AS bang, COUNT(*) AS soDong FROM Account
UNION ALL SELECT 'Tenant',             COUNT(*) FROM Tenant
UNION ALL SELECT 'RoomType',           COUNT(*) FROM RoomType
UNION ALL SELECT 'Room',               COUNT(*) FROM Room
UNION ALL SELECT 'UtilityPrice',       COUNT(*) FROM UtilityPrice
UNION ALL SELECT 'RentalRequest',      COUNT(*) FROM RentalRequest
UNION ALL SELECT 'Contract',           COUNT(*) FROM Contract
UNION ALL SELECT 'MeterReading',       COUNT(*) FROM MeterReading
UNION ALL SELECT 'Invoice',            COUNT(*) FROM Invoice
UNION ALL SELECT 'Payment',            COUNT(*) FROM Payment
UNION ALL SELECT 'MaintenanceRequest', COUNT(*) FROM MaintenanceRequest;

-- 4.2 Chữ tiếng Việt lưu đúng (phải thấy "Phòng đơn", không phải "Ph?ng ??n")
SELECT roomTypeId, name, capacity FROM RoomType ORDER BY roomTypeId;

-- 4.3 Tình trạng 12 phòng: đang thuê / trống / sửa / ẩn (phải ra: 4 đang thuê, 5 trống, 2 sửa, 1 ẩn)
SELECT r.roomCode, rt.name AS loaiPhong, r.status,
       CASE WHEN c.contractId IS NOT NULL THEN N'Đang thuê: ' + a.fullName
            WHEN r.status = 'ACTIVE'      THEN N'Trống'
            ELSE N'Không cho thuê' END AS tinhTrang
FROM Room r
JOIN RoomType rt     ON rt.roomTypeId = r.roomTypeId
LEFT JOIN Contract c ON c.roomId = r.roomId AND c.status = 'ACTIVE'
LEFT JOIN Account a  ON a.accountId = c.tenantId
ORDER BY r.roomCode;

-- 4.4 Hợp đồng phải KHỚP với yêu cầu đã duyệt (cùng người, cùng phòng, yêu cầu APPROVED) — phải ra 0 dòng
SELECT c.contractId, c.requestId, N'Hợp đồng không khớp yêu cầu' AS loi
FROM Contract c JOIN RentalRequest rr ON rr.requestId = c.requestId
WHERE rr.tenantId <> c.tenantId OR rr.roomId <> c.roomId OR rr.status <> 'APPROVED';

-- 4.5 Tính lại MỌI hoá đơn từ chỉ số điện nước × bảng giá đang hiệu lực
--     Phải ra ĐỦ 8 dòng, cột ketQua toàn chữ "Khớp".
--     (chỉ số cũ = startIndex của hợp đồng nếu là tháng đầu, ngược lại = chỉ số tháng trước của phòng)
SELECT i.invoiceId, i.contractId, i.[year], i.[month], i.total AS tongDaLuu, tinh.tongTinhLai,
       CASE WHEN tinh.tongTinhLai = i.total AND gia.priceId = i.priceId THEN N'Khớp' ELSE N'LỆCH' END AS ketQua
FROM Invoice i
JOIN Contract c           ON c.contractId = i.contractId
LEFT JOIN MeterReading mr ON mr.roomId = c.roomId AND mr.[year] = i.[year] AND mr.[month] = i.[month]
LEFT JOIN MeterReading prev
       ON prev.roomId = c.roomId
      AND prev.[year]  = YEAR (DATEADD(MONTH, -1, DATEFROMPARTS(i.[year], i.[month], 1)))
      AND prev.[month] = MONTH(DATEADD(MONTH, -1, DATEFROMPARTS(i.[year], i.[month], 1)))
OUTER APPLY (SELECT TOP (1) p.priceId, p.electricPrice, p.waterPrice, p.serviceFeePerPerson
             FROM UtilityPrice p
             WHERE p.effectiveFrom <= DATEFROMPARTS(i.[year], i.[month], 1)
             ORDER BY p.effectiveFrom DESC) gia
CROSS APPLY (SELECT
       oldE = CASE WHEN c.startDate >= DATEFROMPARTS(i.[year], i.[month], 1) THEN c.startElectricIndex ELSE prev.electricIndex END,
       oldW = CASE WHEN c.startDate >= DATEFROMPARTS(i.[year], i.[month], 1) THEN c.startWaterIndex    ELSE prev.waterIndex    END) cu
CROSS APPLY (SELECT tongTinhLai = c.monthlyRent
                                + (mr.electricIndex - cu.oldE) * gia.electricPrice
                                + (mr.waterIndex    - cu.oldW) * gia.waterPrice
                                + gia.serviceFeePerPerson * c.numPeople) tinh
ORDER BY i.invoiceId;

-- 4.6 Hoá đơn PAID phải có đúng 1 Payment SUCCESS cùng số tiền; UNPAID thì không có SUCCESS nào — phải ra 0 dòng
SELECT i.invoiceId, i.status, SUM(CASE WHEN p.paymentId IS NULL THEN 0 ELSE 1 END) AS soLanThanhCong
FROM Invoice i
LEFT JOIN Payment p ON p.invoiceId = i.invoiceId AND p.status = 'SUCCESS' AND p.amount = i.total
GROUP BY i.invoiceId, i.status
HAVING (i.status = 'PAID'   AND SUM(CASE WHEN p.paymentId IS NULL THEN 0 ELSE 1 END) <> 1)
    OR (i.status = 'UNPAID' AND SUM(CASE WHEN p.paymentId IS NULL THEN 0 ELSE 1 END) <> 0);

-- 4.7 Chỉ số mới phải ≥ chỉ số tháng trước của cùng phòng — phải ra 0 dòng
SELECT cur.roomId, cur.[year], cur.[month]
FROM MeterReading cur
JOIN MeterReading prev ON prev.roomId = cur.roomId
 AND DATEFROMPARTS(prev.[year], prev.[month], 1) = DATEADD(MONTH, -1, DATEFROMPARTS(cur.[year], cur.[month], 1))
WHERE cur.electricIndex < prev.electricIndex OR cur.waterIndex < prev.waterIndex;

-- 4.8 Mỗi trạng thái có mấy dòng (dữ liệu mẫu phủ đủ mọi trạng thái của sơ đồ trạng thái NT-14)
SELECT 'RentalRequest' AS bang, status, COUNT(*) AS soDong FROM RentalRequest GROUP BY status
UNION ALL SELECT 'Contract',           status, COUNT(*) FROM Contract           GROUP BY status
UNION ALL SELECT 'Invoice',            status, COUNT(*) FROM Invoice            GROUP BY status
UNION ALL SELECT 'Payment',            status, COUNT(*) FROM Payment            GROUP BY status
UNION ALL SELECT 'MaintenanceRequest', status, COUNT(*) FROM MaintenanceRequest GROUP BY status
UNION ALL SELECT 'Room',               status, COUNT(*) FROM Room               GROUP BY status
ORDER BY bang, status;

-- 4.9 Tài khoản mẫu để đăng nhập thử (mật khẩu tất cả: NhaTro@2026)
SELECT accountId, username, fullName, role,
       CASE active WHEN 1 THEN N'Hoạt động' ELSE N'Đã khoá' END AS trangThai
FROM Account ORDER BY accountId;

PRINT N'✔ setup.sql chạy xong — CSDL NhaTroSV đã sẵn sàng.';
GO

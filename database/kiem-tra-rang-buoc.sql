/* =====================================================================
   kiem-tra-rang-buoc.sql — THỬ PHÁ dữ liệu để chứng minh CSDL tự chặn
   Chạy NGAY SAU setup.sql (cần dữ liệu mẫu). Mỗi phép thử cố ý làm sai một luật:
     • SQL Server báo lỗi  ⇒ in "ĐẠT  (bị chặn đúng)"  + số lỗi + thông báo gốc
     • Lệnh lọt qua được   ⇒ in "TRƯỢT (KHÔNG bị chặn!)"  ⇒ thiếu ràng buộc, báo Long
   Mỗi phép thử chạy trong transaction rồi ROLLBACK ⇒ chạy xong dữ liệu mẫu vẫn nguyên.
   Ảnh chụp cho NT-84: chụp kết quả phép thử T3.
   ===================================================================== */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
GO
USE NhaTroSV;
GO

-- Bảng nhỏ ghi kết quả (biến bảng sống trong 1 batch nên dùng bảng tạm #)
IF OBJECT_ID('tempdb..#KetQua') IS NOT NULL DROP TABLE #KetQua;
CREATE TABLE #KetQua (stt INT IDENTITY, phepThu NVARCHAR(120), ketQua NVARCHAR(40), loiSo INT, thongBao NVARCHAR(400));
GO

-- T1. Trùng mã phòng P101 ⇒ phải bị UQ_Room_roomCode chặn (lỗi 2627)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO Room (roomCode, roomTypeId, floor, monthlyPrice) VALUES ('P101', 1, 1, 1500000);
    ROLLBACK;
    INSERT #KetQua VALUES (N'T1 Trùng mã phòng P101', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T1 Trùng mã phòng P101', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T2. Giá phòng âm ⇒ phải bị CK_Room_monthlyPrice chặn (lỗi 547)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO Room (roomCode, roomTypeId, floor, monthlyPrice) VALUES ('P999', 1, 1, -500000);
    ROLLBACK;
    INSERT #KetQua VALUES (N'T2 Giá phòng âm', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T2 Giá phòng âm', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T3. ★ Hợp đồng ACTIVE thứ 2 cho phòng P101 (đang có hợp đồng C2 của An)
--     dùng yêu cầu R10 (của Lan, chưa có hợp đồng) ⇒ phải bị UX_Contract_ActiveRoom chặn (lỗi 2601)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO Contract (requestId, tenantId, roomId, startDate, endDate, monthlyRent, deposit, numPeople, startElectricIndex, startWaterIndex, status)
    VALUES (10, 10, 1, '2026-11-01', '2027-11-01', 1800000, 1800000, 1, 2535, 418, 'ACTIVE');
    ROLLBACK;
    INSERT #KetQua VALUES (N'T3 Hợp đồng ACTIVE thứ 2 cùng phòng', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T3 Hợp đồng ACTIVE thứ 2 cùng phòng', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T4. Hoá đơn cho hợp đồng KHÔNG tồn tại (contractId 9999) ⇒ phải bị FK_Invoice_Contract chặn (lỗi 547)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO Invoice (contractId, priceId, [year], [month], roomFee, electricUsed, electricFee, waterUsed, waterFee, serviceFee, total, dueDate)
    VALUES (9999, 2, 2026, 10, 1000000, 0, 0, 0, 0, 0, 1000000, '2026-11-10');
    ROLLBACK;
    INSERT #KetQua VALUES (N'T4 Hoá đơn cho hợp đồng không tồn tại', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T4 Hoá đơn cho hợp đồng không tồn tại', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- ---------- Các phép thử thêm (không bắt buộc, nhưng nên biết) ----------

-- T5. Một người thuê 2 hợp đồng ACTIVE: An (đang ở P101) thuê thêm P103 ⇒ UX_Contract_ActiveTenant (2601)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO Contract (requestId, tenantId, roomId, startDate, endDate, monthlyRent, deposit, numPeople, startElectricIndex, startWaterIndex)
    VALUES (8, 4, 3, '2026-11-01', '2027-11-01', 2500000, 0, 1, 0, 0);
    ROLLBACK;
    INSERT #KetQua VALUES (N'T5 Một người 2 hợp đồng ACTIVE', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T5 Một người 2 hợp đồng ACTIVE', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T6. Hoá đơn thứ 2 cho cùng hợp đồng C3, cùng tháng 9/2026 ⇒ UQ_Invoice_ContractYearMonth (2627)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO Invoice (contractId, priceId, [year], [month], roomFee, electricUsed, electricFee, waterUsed, waterFee, serviceFee, total, dueDate)
    VALUES (3, 2, 2026, 9, 2500000, 0, 0, 0, 0, 0, 2500000, '2026-10-10');
    ROLLBACK;
    INSERT #KetQua VALUES (N'T6 Hoá đơn trùng tháng', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T6 Hoá đơn trùng tháng', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T7. Ghi chỉ số lần 2 cho P101 tháng 9/2026 ⇒ UQ_MeterReading_RoomYearMonth (2627)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO MeterReading (roomId, [year], [month], electricIndex, waterIndex) VALUES (1, 2026, 9, 2600, 420);
    ROLLBACK;
    INSERT #KetQua VALUES (N'T7 Ghi chỉ số trùng tháng', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T7 Ghi chỉ số trùng tháng', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T8. Tổng hoá đơn KHÔNG bằng tổng các khoản ⇒ CK_Invoice_total (547)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO Invoice (contractId, priceId, [year], [month], roomFee, electricUsed, electricFee, waterUsed, waterFee, serviceFee, total, dueDate)
    VALUES (2, 2, 2026, 10, 1800000, 10, 38000, 1, 18000, 60000, 9999999, '2026-11-10');
    ROLLBACK;
    INSERT #KetQua VALUES (N'T8 Tổng hoá đơn sai', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T8 Tổng hoá đơn sai', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T9. Trạng thái lạ 'DELETED' cho phòng ⇒ CK_Room_status (547)
BEGIN TRY
    BEGIN TRAN;
    UPDATE Room SET status = 'DELETED' WHERE roomCode = 'P202';
    ROLLBACK;
    INSERT #KetQua VALUES (N'T9 Trạng thái phòng không hợp lệ', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T9 Trạng thái phòng không hợp lệ', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T10. Khánh (đang có R8 PENDING) gửi thêm yêu cầu thứ 2 ⇒ UX_RentalRequest_PendingTenant (2601)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO RentalRequest (tenantId, roomId, moveInDate, months, numPeople) VALUES (9, 9, '2026-11-01', 6, 1);
    ROLLBACK;
    INSERT #KetQua VALUES (N'T10 Hai yêu cầu PENDING cùng người', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T10 Hai yêu cầu PENDING cùng người', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T11. IPN ghi SUCCESS lần 2 cho hoá đơn I5 (đã có SUCCESS) ⇒ UX_Payment_SuccessInvoice (2601)
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO Payment (invoiceId, txnRef, amount, status, paidAt) VALUES (5, 'INV5-TRUNG', 2293000, 'SUCCESS', SYSDATETIME());
    ROLLBACK;
    INSERT #KetQua VALUES (N'T11 Hai lần thanh toán SUCCESS 1 hoá đơn', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T11 Hai lần thanh toán SUCCESS 1 hoá đơn', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T12. XOÁ THẬT phòng P302 (đã có yêu cầu + hợp đồng cũ) ⇒ khoá ngoại FK_RentalRequest_Room / FK_Contract_Room chặn (547). Cách đúng: UPDATE status = 'HIDDEN'
BEGIN TRY
    BEGIN TRAN;
    DELETE FROM Room WHERE roomCode = 'P302';
    ROLLBACK;
    INSERT #KetQua VALUES (N'T12 Xoá phòng đã có lịch sử', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T12 Xoá phòng đã có lịch sử', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T13. Xoá loại phòng đang có phòng dùng ("Phòng đơn") ⇒ FK_Room_RoomType chặn (547)
BEGIN TRY
    BEGIN TRAN;
    DELETE FROM RoomType WHERE roomTypeId = 1;
    ROLLBACK;
    INSERT #KetQua VALUES (N'T13 Xoá loại phòng đang dùng', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T13 Xoá loại phòng đang dùng', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- T14. CCCD sai (11 số) ⇒ CK_Tenant_idCardNo (547)
BEGIN TRY
    BEGIN TRAN;
    UPDATE Tenant SET idCardNo = '00120601234' WHERE tenantId = 4;
    ROLLBACK;
    INSERT #KetQua VALUES (N'T14 CCCD không đủ 12 số', N'TRƯỢT (KHÔNG bị chặn!)', NULL, NULL);
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    INSERT #KetQua VALUES (N'T14 CCCD không đủ 12 số', N'ĐẠT (bị chặn đúng)', ERROR_NUMBER(), ERROR_MESSAGE());
END CATCH;
GO

-- KẾT QUẢ
SELECT stt, phepThu, ketQua, loiSo, thongBao FROM #KetQua ORDER BY stt;
SELECT SUM(CASE WHEN ketQua LIKE N'ĐẠT%' THEN 1 ELSE 0 END) AS soDat,
       COUNT(*) AS tongSo FROM #KetQua;

-- Dữ liệu mẫu còn nguyên? (phải ra 12 phòng, 5 hợp đồng, 8 hoá đơn)
SELECT (SELECT COUNT(*) FROM Room) AS soPhong, (SELECT COUNT(*) FROM Contract) AS soHopDong,
       (SELECT COUNT(*) FROM Invoice) AS soHoaDon;

-- Vì sao phải N'…': không có N thì chữ có dấu thành dấu ? (phải thấy cột thieuN bị mất dấu)
SELECT 'Nguyễn Văn An' AS thieuN, N'Nguyễn Văn An' AS coN;
GO

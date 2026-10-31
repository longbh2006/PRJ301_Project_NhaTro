/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import dal.BillingDAO;

import java.util.List;
import model.MeterReading;
import java.sql.SQLException;
import java.sql.Connection;
/**
 *
 * @author Lenovo
 */
public class MeterService {

    public void luuChiSo(List<MeterReading> danhSach) throws SQLException {
        BillingDAO dao = new BillingDAO();
    Connection con = dao.getConnection();

    try {
        // 2. Mở transaction
        con.setAutoCommit(false);

        // 3. Duyệt danh sách và ghi từng chỉ số
        for (MeterReading r : danhSach) {
            dao.insertReading(con, r);
        }

        // 4. Commit toàn bộ
        con.commit();
    } catch (SQLException e) {
        // 5. Rollback khi có lỗi
        try {
            con.rollback();
        } catch (SQLException rbEx) {
            // ghi log, nhưng không làm ảnh hưởng tới exception gốc
        }
        throw e;   // ném lại để xử lý ở tầng gọi
    } finally {
        // 6. Trả Connection về trạng thái mặc định
        try {
            con.setAutoCommit(true);
        } catch (SQLException ex) {
            // ghi log nếu cần
        }
        // **không đóng con** – sẽ được dọn dẹp ở nơi quản lý connection
    }
        
    }
}

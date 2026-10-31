# Chạy project bằng VS Code và Tomcat

Máy đã cài Community Server Connectors 0.26.22 của Red Hat, cùng RSP UI 0.24.13.
Java Extension Pack đã có sẵn. Cấu hình workspace dùng JDK 17, Tomcat 10.1 và
Ant đi kèm NetBeans 17 đang có trên máy Khoa; máy khác cần đổi đường dẫn trong
`.vscode/settings.json` và `.vscode/tasks.json`.

## Thiết lập server một lần

1. Trong VS Code mở thư mục project này. Nếu chưa thấy mục **SERVERS** ở Explorer,
   chạy **Developer: Reload Window** từ `Ctrl+Shift+P`.
2. `Ctrl+Shift+P` → **Servers: Add Local Server...**, chọn thư mục
   `C:\Program Files\Apache Software Foundation\Tomcat 10.1`.
   Nếu cần chọn provider, chọn **Community Server Connector**.
3. Trong **SERVERS**, kiểm tra Tomcat vừa thêm. Đảm bảo không có một Tomcat khác
   đang dùng cùng cổng 8080; dừng instance đó bằng công cụ đã mở nó trước khi chạy.

Không dùng Tomcat 9 cho project này vì source dùng Jakarta Servlet.

## Build và chạy

1. Nhấn `Ctrl+Shift+B` để chạy **NhaTro: Build WAR**. Tác vụ dùng Ant của project,
   xóa output build cũ rồi xuất WAR mới ra `.local/vscode-dist/Project_NhaTro.war`.
2. Trong Explorer, nhấp phải file WAR đó → **Run on Server**, chọn Tomcat vừa thêm.
   Có thể dùng **Debug on Server** để chạy với debugger.
3. Mở `http://localhost:8080/Project_NhaTro/manager/requests` để kiểm tra NT-36.
   Trang yêu cầu session quản lý;
   khi chưa đăng nhập, trả 403 là đúng thiết kế.
4. Sau khi sửa source, build lại rồi **Publish Server (Full)**; nếu WAR chưa được
   nhận lại, dùng **Restart in Run Mode**. Dừng bằng **Stop Server** trong SERVERS.

WAR dùng cấu hình DBContext hiện có tại `web/WEB-INF/ConnectDB.properties` và
driver SQL Server hiện nằm tại `build/web/WEB-INF/lib/mssql-jdbc-13.2.0.jre11.jar`.
Không chia sẻ WAR có chứa cấu hình CSDL cục bộ.

Đã kiểm tra build thành công bằng đúng Ant/JDK và tham số của task vào 29/10/2026;
WAR có controller, service và JSP của NT-36. Đã xác nhận hai extension được cài.
Bước thêm server và chạy qua giao diện extension cần thực hiện trong VS Code;
kết nối điều khiển cửa sổ không khả dụng trong phiên cài đặt này.

Hướng dẫn chính thức: [Application Servers trong VS Code](https://code.visualstudio.com/docs/java/java-tomcat-jetty)
và [Community Server Connectors](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-community-server-connector).

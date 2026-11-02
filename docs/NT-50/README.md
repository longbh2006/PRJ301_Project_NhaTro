# NT-50 — Rà responsive ở 375px

Kiểm tra ngày 31/10/2026 bằng Chrome, trên ứng dụng Tomcat thật và DB local.
Nhánh `nt-50-responsive` được tạo từ `origin/main` tại `8fcd777`.
Checkout: `C:\Users\Khoa\PRJ301_Project_NhaTro_NT50`.

## Kết quả

- 19 trang/trạng thái được đo ở cả viewport 375 × 667 và 1366 × 900:
  **38/38 lần không tràn ngang toàn trang**.
- Điều kiện đo: `document.documentElement.scrollWidth <= document.documentElement.clientWidth`.
  Trên Windows, thanh cuộn dọc có thể chiếm 15px; viewport 375px có vùng nội dung 360px.
- [Số đo từng trang](measurements.json) và [39 ảnh giao diện](screenshots/README.md):
  19 cặp desktop/mobile, thêm một ảnh menu người thuê đang mở.
- Build WAR bằng Ant/JDK 17: `BUILD SUCCESSFUL`; `git diff --check` đạt.

## Những phần đã sửa

- Trang đăng nhập trước đây có `scrollWidth = 410` ở viewport 375px.
  Thay khung cố định và bảng bố trí form bằng layout chung, form Bootstrap một cột,
  nhãn liên kết với input, viewport meta và khung tối đa 420px.
- Header dùng `navbar-expand-lg` và nút hamburger. Dưới 992px, nút mở/đóng
  cả các thao tác tài khoản và sidebar; desktop luôn hiện điều hướng.
- Bọc bảng loại phòng và đơn giá trong `.table-responsive`; bổ sung nhãn vùng
  và khả năng focus bàn phím cho các bảng phòng, phòng trống và yêu cầu thuê.
- Tiêu đề/nút thêm mới và các nút form quản trị xuống dòng khi thiếu chỗ.
- Nội dung dài tự ngắt dòng, ảnh không vượt khung, input được phép co theo cột;
  giảm padding card và tăng font input trên điện thoại.
- Header đăng ký cho phép xuống dòng. Các form nhiều cột đã có `row/col-md-*`
  hoặc CSS grid tự chuyển thành một cột trên màn hình nhỏ.

## Thao tác đã kiểm tra trực tiếp

- Đăng nhập bằng tài khoản mẫu ADMIN và TENANT trong DB local, sau đó đăng xuất.
- Tám trang quản trị: danh sách/thêm/sửa phòng, danh sách/thêm/sửa loại phòng,
  danh sách/thêm bảng giá. Các form hiển thị dữ liệu thật, không gửi lưu/xóa.
- Trang chủ, phòng trống có dữ liệu, chi tiết phòng, đăng nhập, đăng ký.
- Yêu cầu thuê có dữ liệu, chi tiết yêu cầu, lọc trạng thái `PENDING` và trạng thái rỗng.
- Mở form đăng ký thuê trong chi tiết phòng; các trường ngày, số tháng, số người,
  ghi chú và nút gửi nằm trong khung trên điện thoại. Không gửi yêu cầu mới.
- Menu khách và người thuê mở được; đóng menu trả `aria-expanded` về `false`.
- Focus vùng bảng yêu cầu và nhấn ArrowRight: `scrollLeft = 40`, bảng rộng 620px
  cuộn bên trong vùng rộng 342px; toàn trang vẫn có `scrollWidth = clientWidth = 375`.
- TENANT truy cập trang quản trị nhận trang 403; URL không tồn tại nhận trang 404.
- Rà thêm vùng chuyển bố cục quanh 768–1024px trên trang yêu cầu thuê, không tràn ngang.

## Phạm vi chưa kiểm chứng

- Trang lỗi 500 đã rà mã nguồn dùng cùng layout, chưa kích hoạt lỗi server để chụp ảnh.
- Các chức năng hợp đồng, hóa đơn, sự cố, tài khoản và màn hình quản lý chưa có
  controller/view trên `main` tại commit nền; các liên kết chờ triển khai không
  được tính là trang đã kiểm tra. Cần rà tiếp khi các nhánh đó được merge.
- Chưa kiểm tra trên thiết bị iOS vật lý; kết quả trên đây là viewport Chrome.

## Chạy lại trên máy này

Ứng dụng dùng `http://127.0.0.1:18050/Project_NhaTro/`.
Giữ cấu hình DB local trong `web/WEB-INF/ConnectDB.properties` (Git bỏ qua).
Cấu hình build cũ tham chiếu thư viện NetBeans/JSTL ở máy khác; phiên kiểm tra
chỉ ghi đè thuộc tính lúc chạy, không sửa cấu hình chung của repo:

```powershell
$env:JAVA_TOOL_OPTIONS='"-Dlibs.CopyLibs.classpath=C:/Program Files/NetBeans-17/netbeans/java/ant/extra/org-netbeans-modules-java-j2seproject-copylibstask.jar" -Dfile.reference.jakarta.servlet.jsp.jstl-2.0.0.jar=web/WEB-INF/lib/jakarta.servlet.jsp.jstl-3.0.1.jar -Dfile.reference.jakarta.servlet.jsp.jstl-api-2.0.0.jar=web/WEB-INF/lib/jakarta.servlet.jsp.jstl-api-3.0.0.jar'
.\scripts\run-local.ps1 -Port 18050 -JdkPath 'C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot'
Remove-Item Env:JAVA_TOOL_OPTIONS
```

Mở Chrome ở hai kích thước trên, đi qua các URL trong `measurements.json`,
kiểm tra cả menu đóng/mở và cuộn ngang bên trong bảng.
Ảnh chỉ chứa dữ liệu mẫu của ứng dụng; không chia sẻ WAR hoặc cấu hình DB local.

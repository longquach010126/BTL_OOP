# Campus Exchange — Ứng dụng JavaFX

Ứng dụng desktop quản lý và hỗ trợ trao đổi đồ cũ. Java 17+, JavaFX 17.0.10, Maven.
Dựa trên bốn lớp User, Item, Post, ExchangeRequest và Manager đã tích hợp.

## Chạy trên Windows

1. Giải nén **toàn bộ** ZIP, ví dụ vào `C:\Long\CampusExchange_JavaFX`.
2. Có JDK 17 trở lên. Mở PowerShell kiểm tra `java -version`.
3. Nhấp `start.cmd`.

Bộ ZIP đã kèm JavaFX 17.0.10 cho Windows x64 trong `runtime/javafx`. Script dùng javac
để biên dịch rồi mở app, không cần Maven hoặc tải thư viện khi chạy bằng start.cmd.
Đây là mã nguồn chạy bằng JDK, chưa phải bộ cài EXE. Cần Windows x64; runtime kèm theo không dành cho ARM hoặc macOS.

Nếu IntelliJ có JDK nhưng terminal chưa nhận, mở PowerShell và đặt đúng đường dẫn JDK của bạn:

```powershell
$env:JAVA_HOME = 'C:\duong-dan-thuc-te\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\start.cmd
```

Không dùng nguyên đường dẫn ví dụ nếu máy bạn không có thư mục đó.

## Mở bằng IntelliJ IDEA trên Windows

Hướng dẫn từng bước và xử lý lỗi Maven nằm trong [INTELLIJ_WINDOWS.md](INTELLIJ_WINDOWS.md). Tóm tắt: mở `pom.xml` dưới dạng Maven project, chọn JDK 17+, rồi chạy Maven → Plugins → javafx → `javafx:run`. Không chạy trực tiếp `CampusExchangeApp`; `Launcher` là lớp main nếu tạo cấu hình Application.

Không chép đè toàn bộ vào dự án BTL cũ. Hãy chạy độc lập bản này trước và sao lưu/commit BTL.
Tất cả lớp hiện ở default package, không có module-info.java. Cần chuyển package đồng bộ nếu ghép vào dự án có package.

## Các màn hình

1. **Tổng quan**: chỉ số, bài gần đây và nút tạo dữ liệu minh họa khi kho trống.
2. **Người dùng**: thêm, sửa, xóa, tìm theo mã/tên/email. Chặn xóa hồ sơ có liên kết.
3. **Đồ vật**: CRUD, tìm kiếm, lọc trạng thái, chỉ đồ của tôi và tạo bài từ đồ đã chọn.
4. **Bài đăng**: tạo, sửa tiêu đề, đóng, xóa khi chưa có lịch sử; lọc từ khóa, loại, chất lượng, hình thức, trạng thái; chi tiết và gửi yêu cầu.
5. **Yêu cầu trao đổi**: lọc theo người nhận/gửi và trạng thái; chấp nhận, từ chối, hoàn tất, hủy.
6. **Thống kê**: chỉ số, biểu đồ trạng thái yêu cầu và đồ vật theo hình thức.

Trên mỗi bảng: chọn một dòng rồi bấm thao tác. Nhấp đúp bài đăng để xem chi tiết.
Thông báo lỗi nằm trong form khi nhập sai, không tự đóng form.
Giao diện tối thiểu 1040 × 700, có cuộn nội dung cho cửa sổ thấp.

## Đăng ký, đăng nhập và đăng xuất

- Khởi chạy bằng `start.cmd`, chọn **Chưa có tài khoản? Đăng ký** nếu là người dùng mới.
- Tên đăng nhập gồm 3–32 ký tự không dấu: chữ, số, dấu chấm, gạch dưới hoặc gạch ngang. Tên đăng nhập cũng là mã người dùng, không phân biệt hoa thường.
- Điền họ tên, email, số điện thoại và mật khẩu 8–128 ký tự. Sau đăng ký, đăng nhập bằng tài khoản vừa tạo.
- Mỗi tài khoản ứng với đúng một hồ sơ. Đồ vật, bài đăng, yêu cầu gửi đi được gắn với tài khoản đang đăng nhập. Không còn bộ chọn người thao tác.
- Bấm **Đăng xuất** ở góc trên bên phải để kết thúc phiên và trở lại màn hình đăng nhập. Mở lại ứng dụng luôn phải đăng nhập lại.
- Tab **Người dùng** là danh bạ; chỉ có thể sửa hồ sơ của chính mình. Người dùng mới tham gia qua đăng ký.
- Mật khẩu được băm PBKDF2-HMAC-SHA256 với salt ngẫu nhiên riêng, không lưu mật khẩu gốc. Tài khoản được lưu trong `data/exchange.dat` cùng dữ liệu ứng dụng.

### Chuyển đổi dữ liệu cũ

Nếu file dữ liệu chứa hồ sơ chưa có mật khẩu, chủ máy phải đặt mật khẩu riêng cho từng hồ sơ theo hướng dẫn trên màn hình trước khi đăng nhập. Đây là bước thiết lập một lần cho dữ liệu cục bộ trước khi dự án có xác thực, không phải chức năng quên mật khẩu. Không có mật khẩu mặc định. Mã người dùng cũ được giữ nguyên làm tên đăng nhập, giữ liên kết đồ vật, bài đăng và lịch sử yêu cầu. Sau khi thiết lập, không thể đặt lại mật khẩu từ màn hình này.

## Nghiệp vụ và giới hạn

- User.id là giá trị của các trường mang tên Username để tương thích mã gốc.
- Mỗi tài khoản đăng nhập là một người dùng, có quyền theo chủ sở hữu đồ vật và chủ bài.
- Danh bạ người dùng chỉ cho sửa hồ sơ của chính mình; không có vai trò quản trị đặc biệt.
- Item: AVAILABLE → RESERVED → EXCHANGED; Post: OPEN → RESERVED → CLOSED.
- Request: PENDING → ACCEPTED → COMPLETED; PENDING → REJECTED; PENDING/ACCEPTED → CANCELLED.
- Hủy yêu cầu ACCEPTED mở lại bài và giải phóng vật, không phục hồi các yêu cầu đã bị từ chối.
- Chủ bài mới được chấp nhận, từ chối và hoàn tất. Hai bên được hủy trước hoàn tất.
- Chưa lưu giá, thanh toán, chat, vật đối ứng hay chuyển quyền sở hữu sau bàn giao.
- Không gọi trực tiếp setter trạng thái từ controller; dùng Manager để đồng bộ.

## Lưu dữ liệu

Tự lưu sau mỗi thao tác thành công, thêm nút **Lưu dữ liệu**. File `data/exchange.dat`
được đọc khi mở app. Khi ghi lỗi, thay đổi còn ở bộ nhớ và app báo chưa lưu; khi đóng sẽ thử lưu lại.
Nếu file đọc lỗi, app báo lỗi rồi dừng, không ghi đè file cũ.
Chỉ mở snapshot của ứng dụng, một tiến trình mỗi thư mục. Không đồng thời mở console cũ và JavaFX dùng chung file.

Có thể thử sao chép snapshot bộ console đã gửi trước vào `data/exchange.dat` sau khi sao lưu;
bộ JavaFX giữ serialVersionUID và tên lớp cũ, nhưng dữ liệu hỏng hoặc bị sửa thủ công không được cam kết khôi phục.

## Kiểm thử

Nhấp `test.cmd`: chạy IntegrationTest, 38 kiểm tra nghiệp vụ.
Kiểm thử giao diện riêng (cần desktop hiển thị):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run.ps1 -Mode UiTest
```

FxSmokeTest dùng thư mục dữ liệu tạm, tự tương tác form và các nút, tạo ảnh trong ui-check.
Kết quả và ảnh đã kiểm tra nằm trong `verification`. Kiểm thử đã chạy trên Linux với Java/JavaFX 17,
chưa chạy trực tiếp script PowerShell hoặc DLL Windows trong môi trường Windows.
Bản Maven trong IntelliJ cần mạng để tải plugin/dependency; đường chạy Maven chưa kiểm chứng trong môi trường này do Java không phân giải được DNS.
Biên dịch và kiểm thử thành công bằng compiler Java 17 với đúng các module JavaFX 17.0.10.

## Cấu trúc

- `src/main/java`: entity, Manager, Checks, DemoData, CampusExchangeApp, Launcher.
- `src/main/resources/app.css`: giao diện.
- `src/test/java`: IntegrationTest và FxSmokeTest.
- `pom.xml`: phiên bản thư viện và plugin khi mở bằng Maven.
- `runtime/javafx`: thư viện Windows x64 đã kèm, dùng bởi start.cmd.
- `docs`: tài liệu Word.
- `verification`: ảnh chụp app và kết quả kiểm thử.

# Chạy Campus Exchange bằng IntelliJ IDEA trên Windows

## Yêu cầu

- Windows 10/11 64-bit.
- JDK 17 trở lên (khuyến nghị JDK 17).
- IntelliJ IDEA Community hoặc Ultimate.
- Mạng Internet trong lần Maven tải các thư viện lần đầu.

## Mở project

1. Giải nén toàn bộ `CampusExchange_IntelliJ_Windows.zip`.
2. Mở IntelliJ IDEA, chọn **File → Open**.
3. Chọn file `pom.xml` trong thư mục `CampusExchange_JavaFX` đã giải nén và chọn **Open as Project**.
4. Nếu IntelliJ hỏi, chọn **Trust Project** và **Load Maven Project**.
5. Vào **File → Project Structure → Project** và đặt **Project SDK** thành JDK 17 trở lên.
6. Trong cửa sổ Maven, bấm **Reload All Maven Projects** và chờ IntelliJ tải dependency xong.

## Chạy giao diện

Cách khuyến nghị trong IntelliJ:

1. Mở cửa sổ **Maven**.
2. Mở **Plugins → javafx**.
3. Nhấp đúp **javafx:run**.

Hoặc mở Terminal của IntelliJ tại thư mục project và chạy:

```powershell
mvn clean javafx:run
```

Không chạy `CampusExchangeApp` trực tiếp. Nếu muốn tạo cấu hình Application riêng, chọn lớp `Launcher` làm Main class; cấu hình Maven `javafx:run` là cách ổn định hơn vì tự thiết lập JavaFX runtime.

## Nếu IntelliJ không tìm thấy JDK hoặc JavaFX

- Đặt **Project SDK** và **Maven Runner JRE** cùng dùng JDK 17 trở lên.
- Bấm **Reload All Maven Projects**.
- Kiểm tra mạng/proxy nếu Maven không tải được dependency.
- Không thêm `module-info.java`; các lớp hiện dùng default package.

## Chạy độc lập ngoài IntelliJ

`start.cmd` dùng JDK cài trên Windows và các thư viện JavaFX Windows x64 đi kèm trong `runtime/javafx`; không cần Maven cho cách chạy này.

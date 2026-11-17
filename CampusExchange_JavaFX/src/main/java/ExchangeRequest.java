import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExchangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // Các thuộc tính của yêu cầu trao đổi
    private String requestId;
    private String postId;           // ID bài đăng muốn trao đổi
    private String senderUsername;   // Người gửi yêu cầu
    private String receiverUsername; // Người nhận (chủ bài đăng)
    private String status;           // PENDING, ACCEPTED, REJECTED, COMPLETED hoặc CANCELLED
    private String createdAt;        // Thời gian tạo

    // 1. Constructor: Chức năng gửi yêu cầu
    public ExchangeRequest(String requestId, String postId, String senderUsername, String receiverUsername) {
        this.requestId = Checks.id(requestId);
        this.postId = Checks.id(postId);
        this.senderUsername = Checks.id(senderUsername);
        this.receiverUsername = Checks.id(receiverUsername);
        this.status = "PENDING"; // Mặc định khi vừa gửi là chờ duyệt

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.createdAt = dtf.format(LocalDateTime.now());
    }

    // Các phương thức Getters và Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = Checks.id(requestId);
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = Checks.id(postId);
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = Checks.id(senderUsername);
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = Checks.id(receiverUsername);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = Checks.choice(status, "PENDING", "ACCEPTED", "REJECTED", "COMPLETED", "CANCELLED");
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // Các hàm này chỉ chuyển trạng thái entity. Luồng liên lớp phải gọi qua Manager.
    public void acceptRequest() { transition("PENDING", "ACCEPTED"); }
    public void rejectRequest() { transition("PENDING", "REJECTED"); }
    public void completeRequest() { transition("ACCEPTED", "COMPLETED"); }
    public void cancelRequest() {
        if (!"PENDING".equals(status) && !"ACCEPTED".equals(status))
            throw new IllegalStateException("Yêu cầu không thể hủy");
        status = "CANCELLED";
    }
    private void transition(String from, String to) {
        if (!from.equals(status)) throw new IllegalStateException("Trạng thái yêu cầu không hợp lệ");
        status = to;
    }

    // Hiển thị thông tin chi tiết yêu cầu
    public void displayRequestInfo() {
        System.out.println("----------------------------------------");
        System.out.println("Mã yêu cầu       : " + requestId);
        System.out.println("Bài đăng liên quan: " + postId);
        System.out.println("Người gửi        : " + senderUsername);
        System.out.println("Người nhận       : " + receiverUsername);
        System.out.println("Trạng thái       : " + status);
        System.out.println("Thời gian tạo    : " + createdAt);
        System.out.println("----------------------------------------");
    }
}

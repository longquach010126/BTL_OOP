import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Bao gồm:
 *  1. Lớp public Post       : Entity đại diện cho 1 bài đăng
 *  2. Lớp PostManager       : Quản lý danh sách bài đăng & thực thi Tìm kiếm, Lọc, Đăng bài
 */

// ============================================================================
// 1. LỚP THỰC THỂ BÀI ĐĂNG (POST)
// ============================================================================
public class Post implements Serializable {
    private static final long serialVersionUID = 1L;
    private byte[] imageData;

    public byte[] getImageData() { return imageData == null ? null : imageData.clone(); }
    public void setImageData(byte[] data) { imageData = data == null ? null : data.clone(); }

    // Thuộc tính của bài đăng
    private String postId;          // Mã bài đăng (VD: "P001")
    private String title;           // Tiêu đề bài đăng
    private String authorUsername;  // Tên tài khoản / Mã người đăng bài
    private Item item;              // Đồ vật liên kết với bài đăng
    private String status;          // OPEN: nhận yêu cầu, RESERVED: giữ chỗ, CLOSED: hoàn tất
    private String createdAt;       // Thời gian tạo bài đăng (yyyy-MM-dd HH:mm:ss)

    /**
     * Constructor 1: Khởi tạo bài đăng mới (Tự động đặt trạng thái OPEN và thời gian hiện tại)
     */
    public Post(String postId, String title, String authorUsername, Item item) {
        this.postId = Checks.id(postId);
        this.title = title;
        this.authorUsername = Checks.id(authorUsername);
        this.item = item;
        this.status = "OPEN";

        // Tạo chuỗi thời gian đồng bộ định dạng với ExchangeRequest
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.createdAt = dtf.format(LocalDateTime.now());
    }

    /**
     * Constructor 2: Khởi tạo đầy đủ thông tin (Dùng khi nạp dữ liệu cũ)
     */
    public Post(String postId, String title, String authorUsername, Item item, String status, String createdAt) {
        this.postId = Checks.id(postId);
        this.title = title;
        this.authorUsername = Checks.id(authorUsername);
        this.item = item;
        this.status = Checks.choice(status, "OPEN", "RESERVED", "CLOSED");
        this.createdAt = createdAt;
    }

    // --- Getters và Setters ---
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = Checks.id(postId); }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = Checks.id(authorUsername); }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = Checks.choice(status, "OPEN", "RESERVED", "CLOSED");
    }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // --- Phương thức nghiệp vụ của Bài đăng ---

    /**
     * Cập nhật trạng thái bài đăng
     */
    public void updateStatus(String newStatus) { setStatus(newStatus); }

    /**
     * Kiểm tra tính hợp lệ của dữ liệu bài đăng
     */
    public boolean isValid() {
        return postId != null && !postId.trim().isEmpty()
                && title != null && !title.trim().isEmpty()
                && authorUsername != null && !authorUsername.trim().isEmpty()
                && item != null && item.isValid();
    }

    /**
     * Hiển thị thông tin chi tiết bài đăng
     */
    public void displayPostInfo() {
        System.out.println("========================================");
        System.out.println("Mã bài đăng      : " + postId);
        System.out.println("Tiêu đề          : " + title);
        System.out.println("Người đăng       : " + authorUsername);
        System.out.println("Trạng thái       : " + status);
        System.out.println("Thời gian đăng   : " + createdAt);
        if (item != null) {
            System.out.println("--- Thông tin đồ vật ---");
            System.out.println("  + Mã đồ vật    : " + item.getItemId());
            System.out.println("  + Tên đồ vật   : " + item.getName());
            System.out.println("  + Loại (Category): " + item.getCategory());
            System.out.println("  + Tình trạng   : " + item.getCondition());
            System.out.println("  + Hình thức    : " + item.getTransactionType());
            System.out.println("  + Mô tả        : " + item.getDescription());
        } else {
            System.out.println("  + Đồ vật       : (Chưa liên kết)");
        }
        System.out.println("========================================");
    }
}


// ============================================================================
// 2. LỚP QUẢN LÝ BÀI ĐĂNG (POSTMANAGER - Bỏ từ khóa public để để chung trong Post.java)
// ============================================================================
class PostManager implements Serializable {
    private static final long serialVersionUID = 1L;
    // Danh sách lưu trữ tất cả bài đăng
    private List<Post> posts = new ArrayList<>();

    // --- CHỨC NĂNG QUẢN LÝ & ĐĂNG BÀI ---

    /**
     * Tìm bài đăng theo Mã ID
     */
    public Post findPostById(String postId) {
        if (postId == null || postId.trim().isEmpty()) {
            return null;
        }
        String cleanId = postId.trim();
        for (Post post : posts) {
            if (post.getPostId().equalsIgnoreCase(cleanId)) {
                return post;
            }
        }
        return null;
    }

    /**
     * Chức năng Đăng bài mới
     */
    public boolean addPost(Post post) {
        if (post == null || !post.isValid()) {
            System.out.println("-> Lỗi: Thông tin bài đăng hoặc đồ vật không hợp lệ!");
            return false;
        }
        if (findPostById(post.getPostId()) != null) {
            System.out.println("-> Lỗi: Mã bài đăng [" + post.getPostId() + "] đã tồn tại!");
            return false;
        }
        posts.add(post);
        System.out.println("-> Thành công: Đã đăng bài thành công [Mã: " + post.getPostId() + "]!");
        return true;
    }

    /**
     * Chức năng Cập nhật trạng thái bài đăng
     */
    public boolean updatePostStatus(String postId, String newStatus) {
        Post post = findPostById(postId);
        if (post == null) {
            System.out.println("-> Lỗi: Không tìm thấy bài đăng [" + postId + "]!");
            return false;
        }
        try { post.updateStatus(newStatus); return true; }
        catch (IllegalArgumentException ex) { return false; }
    }

    /**
     * Hiển thị tất cả bài đăng
     */
    public void displayAllPosts() {
        if (posts.isEmpty()) {
            System.out.println("-> Danh sách bài đăng rỗng!");
            return;
        }
        System.out.println("\n===== DANH SÁCH BÀI ĐĂNG (" + posts.size() + ") =====");
        for (Post post : posts) {
            post.displayPostInfo();
        }
    }

    // --- CHỨC NĂNG TÌM KIẾM ---

    /**
     * 1. Tìm kiếm theo Tên (Tìm trong Tiêu đề bài đăng HOẶC Tên đồ vật)
     */
    public List<Post> searchByName(String name) {
        List<Post> result = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            System.out.println("-> Lỗi: Vui lòng nhập tên cần tìm!");
            return result;
        }
        String keyword = name.trim().toLowerCase();
        for (Post post : posts) {
            boolean matchTitle = post.getTitle() != null && post.getTitle().toLowerCase().contains(keyword);
            boolean matchItemName = post.getItem() != null && post.getItem().getName() != null 
                                    && post.getItem().getName().toLowerCase().contains(keyword);
            if (matchTitle || matchItemName) {
                result.add(post);
            }
        }
        displaySearchResult("Tên chứa: '" + name + "'", result);
        return result;
    }

    /**
     * 2. Tìm kiếm theo Loại đồ vật (Category)
     */
    public List<Post> searchByCategory(String category) {
        List<Post> result = new ArrayList<>();
        if (category == null || category.trim().isEmpty()) {
            System.out.println("-> Lỗi: Vui lòng nhập loại đồ vật!");
            return result;
        }
        String targetCategory = category.trim();
        for (Post post : posts) {
            if (post.getItem() != null && post.getItem().getCategory() != null) {
                if (post.getItem().getCategory().equalsIgnoreCase(targetCategory)) {
                    result.add(post);
                }
            }
        }
        displaySearchResult("Loại đồ vật: '" + category + "'", result);
        return result;
    }

    /**
     * 3. Tìm kiếm theo Từ khóa tổng hợp (Keyword)
     */
    public List<Post> searchByKeyword(String keyword) {
        List<Post> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            System.out.println("-> Lỗi: Vui lòng nhập từ khóa!");
            return result;
        }
        String key = keyword.trim().toLowerCase();
        for (Post post : posts) {
            boolean inTitle = post.getTitle() != null && post.getTitle().toLowerCase().contains(key);
            boolean inName = post.getItem() != null && post.getItem().getName() != null 
                             && post.getItem().getName().toLowerCase().contains(key);
            boolean inDesc = post.getItem() != null && post.getItem().getDescription() != null 
                             && post.getItem().getDescription().toLowerCase().contains(key);
            if (inTitle || inName || inDesc) {
                result.add(post);
            }
        }
        displaySearchResult("Từ khóa tổng hợp: '" + keyword + "'", result);
        return result;
    }

    // --- CHỨC NĂNG LỌC ---

    /**
     * 4. Lọc theo Tình trạng đồ vật (Condition)
     */
    public List<Post> filterByCondition(String condition) {
        List<Post> result = new ArrayList<>();
        if (condition == null || condition.trim().isEmpty()) {
            System.out.println("-> Lỗi: Vui lòng nhập tình trạng!");
            return result;
        }
        String targetCondition = condition.trim();
        for (Post post : posts) {
            if (post.getItem() != null && post.getItem().getCondition() != null) {
                if (post.getItem().getCondition().equalsIgnoreCase(targetCondition)) {
                    result.add(post);
                }
            }
        }
        displaySearchResult("Tình trạng đồ vật: '" + condition + "'", result);
        return result;
    }

    /**
     * 5. Lọc theo Hình thức trao đổi (TransactionType)
     */
    public List<Post> filterByTransactionType(String transactionType) {
        List<Post> result = new ArrayList<>();
        if (transactionType == null || transactionType.trim().isEmpty()) {
            System.out.println("-> Lỗi: Vui lòng nhập hình thức trao đổi!");
            return result;
        }
        String targetType = Checks.transactionType(transactionType);
        for (Post post : posts) {
            if (post.getItem() != null && post.getItem().getTransactionType() != null) {
                if (post.getItem().getTransactionType().equalsIgnoreCase(targetType)) {
                    result.add(post);
                }
            }
        }
        displaySearchResult("Hình thức trao đổi: '" + transactionType + "'", result);
        return result;
    }

    /**
     * 6. Lọc / Tìm kiếm đa tiêu chí kết hợp (Nâng cao)
     */
    public List<Post> searchAndFilter(String keyword, String category, String condition, 
                                      String transactionType, String postStatus) {
        List<Post> result = new ArrayList<>();

        for (Post post : posts) {
            // Lọc từ khóa
            if (keyword != null && !keyword.trim().isEmpty()) {
                String k = keyword.trim().toLowerCase();
                boolean inTitle = post.getTitle() != null && post.getTitle().toLowerCase().contains(k);
                boolean inName = post.getItem() != null && post.getItem().getName() != null 
                                 && post.getItem().getName().toLowerCase().contains(k);
                boolean inDesc = post.getItem() != null && post.getItem().getDescription() != null 
                                 && post.getItem().getDescription().toLowerCase().contains(k);
                if (!inTitle && !inName && !inDesc) continue;
            }

            // Lọc loại
            if (category != null && !category.trim().isEmpty()) {
                if (post.getItem() == null || post.getItem().getCategory() == null 
                    || !post.getItem().getCategory().equalsIgnoreCase(category.trim())) {
                    continue;
                }
            }

            // Lọc tình trạng
            if (condition != null && !condition.trim().isEmpty()) {
                if (post.getItem() == null || post.getItem().getCondition() == null 
                    || !post.getItem().getCondition().equalsIgnoreCase(condition.trim())) {
                    continue;
                }
            }

            // Lọc hình thức giao dịch
            if (transactionType != null && !transactionType.trim().isEmpty()) {
                if (post.getItem() == null || post.getItem().getTransactionType() == null 
                    || !post.getItem().getTransactionType().equalsIgnoreCase(Checks.transactionType(transactionType))) {
                    continue;
                }
            }

            // Lọc trạng thái bài đăng
            if (postStatus != null && !postStatus.trim().isEmpty()) {
                if (!post.getStatus().equalsIgnoreCase(postStatus.trim())) {
                    continue;
                }
            }

            result.add(post);
        }

        displaySearchResult("Lọc kết hợp nâng cao", result);
        return result;
    }

    // --- PHƯƠNG THỨC TRỢ GIÚP IN KẾT QUẢ ---
    private void displaySearchResult(String criteria, List<Post> resultList) {
        System.out.println("\n>>> KẾT QUẢ THEO [" + criteria + "] (Tìm thấy: " + resultList.size() + " bài)");
        if (resultList.isEmpty()) {
            System.out.println("-> Không tìm thấy bài đăng phù hợp!");
        } else {
            for (Post p : resultList) {
                p.displayPostInfo();
            }
        }
    }

    void removePost(Post post) { posts.remove(post); }

    public List<Post> getPosts() {
        return Collections.unmodifiableList(new ArrayList<>(posts));
    }
}

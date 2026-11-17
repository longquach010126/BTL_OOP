import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Điểm vào nghiệp vụ duy nhất cho Main hoặc controller JavaFX. */
public class Manager implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, User> users = new LinkedHashMap<>();
    private final ItemManager itemManager = new ItemManager();
    private final PostManager postManager = new PostManager();
    private final Map<String, ExchangeRequest> requests = new LinkedHashMap<>();
    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException(message);
    }
    private User user(String id) {
        User u = users.get(Checks.id(id));
        require(u != null, "Không tìm thấy người dùng"); return u;
    }
    private Item item(String id) {
        Item i = itemManager.findItem(Checks.id(id));
        require(i != null, "Không tìm thấy đồ vật"); return i;
    }
    private Post post(String id) {
        Post p = postManager.findPostById(Checks.id(id));
        require(p != null, "Không tìm thấy bài đăng"); return p;
    }
    private ExchangeRequest request(String id) {
        ExchangeRequest r = requests.get(Checks.id(id));
        require(r != null, "Không tìm thấy yêu cầu"); return r;
    }
    public List<User> getUsers() { return Collections.unmodifiableList(new ArrayList<>(users.values())); }
    public List<Item> getItems() { return itemManager.getItems(); }
    public List<Post> getPosts() { return postManager.getPosts(); }
    public List<ExchangeRequest> getRequests() { return Collections.unmodifiableList(new ArrayList<>(requests.values())); }
    public User findUserById(String id) { return users.get(Checks.id(id)); }
    public User register(String username,String name,String email,String phone,String password) {
        require(username!=null && username.trim().matches("[A-Za-z0-9_.-]{3,32}"),"Tên đăng nhập gồm 3–32 chữ cái không dấu, số hoặc . _ -");
        User u=new User(username,name,email,phone,"Thành viên");
        require(u.isValid(),"Hãy điền đầy đủ thông tin");
        require(!users.containsKey(u.getId()),"Tên đăng nhập đã tồn tại");
        u.setPassword(password);addUser(u);return u;
    }
    public User authenticate(String username,String password) {
        User u=username==null?null:users.get(Checks.id(username));
        require(u!=null && u.matchesPassword(password),"Tên đăng nhập hoặc mật khẩu không đúng");return u;
    }
    public boolean hasLegacyUsers() { return users.values().stream().anyMatch(u->!u.hasPassword()); }
    // Only for upgrading local snapshots created before accounts existed.
    public void initializeLegacyPassword(String id,String password) {
        User u=user(id);require(!u.hasPassword(),"Tài khoản đã có mật khẩu");u.setPassword(password);
    }
    public void addUser(User u) {
        require(u != null && u.isValid(), "Thông tin người dùng không hợp lệ");
        require(!users.containsKey(u.getId()), "Mã người dùng trùng"); users.put(u.getId(), u);
    }
    // Cập nhật hồ sơ; giao diện chỉ cho phép sửa hồ sơ của tài khoản đang đăng nhập.
    public void updateUser(String id, String name, String email, String phone, String role) {
        User old = user(id); User next = new User(old.getId(), name, email, phone, role);
        require(next.isValid(), "Thông tin người dùng không hợp lệ");
        old.setName(name); old.setEmail(email); old.setPhone(phone); old.setRole(role);
    }
    public void deleteUser(String id) {
        User u = user(id);
        for (Item i : getItems()) require(!u.getId().equals(i.getOwnerId()), "Người dùng còn sở hữu đồ vật");
        for (Post p : getPosts()) require(!u.getId().equals(p.getAuthorUsername()), "Người dùng có lịch sử bài đăng");
        for (ExchangeRequest r : getRequests()) require(!u.getId().equals(r.getSenderUsername()) && !u.getId().equals(r.getReceiverUsername()), "Người dùng có lịch sử yêu cầu");
        users.remove(u.getId());
    }
    public void addItem(String actorId, Item i) {
        User owner = user(actorId);
        require(i != null && i.isValid(), "Đồ vật không hợp lệ");
        require(itemManager.findItem(i.getItemId()) == null, "Mã đồ vật trùng");
        require("AVAILABLE".equals(i.getStatus()), "Đồ vật mới phải còn sẵn");
        require(i.getOwnerId() == null || owner.getId().equals(i.getOwnerId()), "Sai chủ sở hữu");
        i.setOwnerId(owner.getId()); itemManager.addItem(i);
    }
    private void owner(String actorId, Item i) { require(user(actorId).getId().equals(i.getOwnerId()), "Chỉ chủ đồ vật được thao tác"); }
    public void editItem(String actorId, String itemId, Item next) {
        Item i = item(itemId); owner(actorId, i);
        require("AVAILABLE".equals(i.getStatus()), "Đồ vật đang giữ chỗ hoặc đã giao dịch");
        require(next != null && next.isValid() && i.getItemId().equals(next.getItemId()), "Dữ liệu sửa không hợp lệ");
        itemManager.editItem(i.getItemId(), next);
    }
    public void deleteItem(String actorId, String itemId) {
        Item i = item(itemId); owner(actorId, i);
        for (Post p : getPosts()) require(!p.getItem().getItemId().equals(i.getItemId()), "Đồ vật có lịch sử bài đăng");
        itemManager.deleteItem(i.getItemId(), true);
    }
    public void createPost(String actorId, String postId, String title, String itemId) {
        createPost(actorId, postId, title, itemId, null);
    }
    public void createPost(String actorId, String postId, String title, String itemId, byte[] image) {
        validateImage(image);
        Item i = item(itemId); owner(actorId, i);
        require("AVAILABLE".equals(i.getStatus()), "Đồ vật không còn sẵn");
        for (Post p : getPosts()) require(!p.getItem().getItemId().equals(i.getItemId()) || "CLOSED".equals(p.getStatus()), "Đồ vật đã có bài đang hoạt động");
        Post p = new Post(postId, title, Checks.id(actorId), i);
        p.setImageData(image);
        require(postManager.addPost(p), "Không tạo được bài đăng");
    }
    public List<Post> search(String keyword, String category, String condition, String type, String status) {
        return postManager.searchAndFilter(keyword, category, condition, type, status);
    }
    public void editPost(String actorId, String postId, String title) {
        editPost(actorId, postId, title, post(postId).getImageData());
    }
    public void editPost(String actorId, String postId, String title, byte[] image) {
        validateImage(image);
        Post p=post(postId);
        require(user(actorId).getId().equals(p.getAuthorUsername()), "Chỉ chủ bài được sửa");
        require("OPEN".equals(p.getStatus()) && Checks.filled(title), "Bài phải đang mở và tiêu đề không rỗng");
        p.setTitle(title.trim());
        p.setImageData(image);
    }
    private static void validateImage(byte[] image) {
        if (image == null) return;
        require(image.length > 0 && image.length <= 5 * 1024 * 1024, "Ảnh phải nhỏ hơn hoặc bằng 5 MB");
        try {
            require(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(image)) != null, "File ảnh không hợp lệ");
        } catch (IOException e) { throw new IllegalArgumentException("Không đọc được ảnh", e); }
    }
    public void closePost(String actorId, String postId) {
        Post p=post(postId);
        require(user(actorId).getId().equals(p.getAuthorUsername()), "Chỉ chủ bài được đóng");
        require("OPEN".equals(p.getStatus()), "Chỉ đóng bài đang mở; hãy xử lý giao dịch đang giữ chỗ trước");
        for(ExchangeRequest r:getRequests()) if(r.getPostId().equals(p.getPostId()) && "PENDING".equals(r.getStatus())) r.rejectRequest();
        p.setStatus("CLOSED");
    }
    public void deletePost(String actorId, String postId) {
        Post p=post(postId);
        require(user(actorId).getId().equals(p.getAuthorUsername()), "Chỉ chủ bài được xóa");
        require(!"RESERVED".equals(p.getStatus()), "Bài đang giữ chỗ");
        for(ExchangeRequest r:getRequests()) require(!r.getPostId().equals(p.getPostId()), "Bài có lịch sử yêu cầu, hãy giữ lại hoặc đóng bài");
        postManager.removePost(p);
    }
    public void sendRequest(String actorId, String requestId, String postId) {
        User sender = user(actorId); Post p = post(postId); String id = Checks.id(requestId);
        require(!requests.containsKey(id), "Mã yêu cầu trùng");
        require(!sender.getId().equals(p.getAuthorUsername()), "Không được yêu cầu bài của chính mình");
        require("OPEN".equals(p.getStatus()) && "AVAILABLE".equals(p.getItem().getStatus()), "Bài hoặc đồ vật không còn nhận yêu cầu");
        user(p.getAuthorUsername());
        for (ExchangeRequest r : getRequests()) require(!r.getPostId().equals(p.getPostId()) || !r.getSenderUsername().equals(sender.getId()) || !"PENDING".equals(r.getStatus()), "Đã có yêu cầu đang chờ");
        requests.put(id, new ExchangeRequest(id, p.getPostId(), sender.getId(), p.getAuthorUsername()));
    }
    private Post authorize(String actorId, ExchangeRequest r) {
        Post p = post(r.getPostId());
        require(user(actorId).getId().equals(p.getAuthorUsername()), "Chỉ chủ bài được duyệt hoặc xác nhận hoàn tất");
        require(p.getAuthorUsername().equals(r.getReceiverUsername()), "Người nhận không khớp tác giả"); return p;
    }
    public void acceptRequest(String actorId, String requestId) {
        ExchangeRequest r = request(requestId); Post p = authorize(actorId, r); Item i = p.getItem();
        require("PENDING".equals(r.getStatus()), "Yêu cầu không còn chờ duyệt");
        require("OPEN".equals(p.getStatus()) && "AVAILABLE".equals(i.getStatus()), "Đồ vật không còn sẵn");
        r.acceptRequest(); p.setStatus("RESERVED"); i.setStatus("RESERVED");
        for (ExchangeRequest other : getRequests()) {
            if (other != r && "PENDING".equals(other.getStatus()) && post(other.getPostId()).getItem().getItemId().equals(i.getItemId())) other.rejectRequest();
        }
    }
    public void rejectRequest(String actorId, String requestId) {
        ExchangeRequest r = request(requestId); authorize(actorId, r); r.rejectRequest();
    }
    public void completeRequest(String actorId, String requestId) {
        ExchangeRequest r = request(requestId); Post p = authorize(actorId, r);
        require("ACCEPTED".equals(r.getStatus()) && "RESERVED".equals(p.getStatus()) && "RESERVED".equals(p.getItem().getStatus()), "Giao dịch chưa được giữ chỗ");
        r.completeRequest(); p.getItem().setStatus("EXCHANGED"); p.setStatus("CLOSED");
    }
    public void cancelRequest(String actorId, String requestId) {
        ExchangeRequest r = request(requestId); Post p = post(r.getPostId()); String actor = user(actorId).getId();
        require(actor.equals(r.getSenderUsername()) || actor.equals(p.getAuthorUsername()), "Chỉ hai bên được hủy");
        boolean reserved = "ACCEPTED".equals(r.getStatus()); r.cancelRequest();
        if (reserved) { p.setStatus("OPEN"); p.getItem().setStatus("AVAILABLE"); }
    }
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> s = new LinkedHashMap<>();
        s.put("users", users.size()); s.put("items", itemManager.countItems()); s.put("posts", getPosts().size()); s.put("requests", requests.size());
        for (String status : Arrays.asList("OPEN", "RESERVED", "CLOSED")) {
            int n=0; for(Post p:getPosts()) if(status.equals(p.getStatus())) n++; s.put("post_"+status,n);
        }
        for (String status : Arrays.asList("PENDING", "ACCEPTED", "REJECTED", "COMPLETED", "CANCELLED")) {
            int n=0; for(ExchangeRequest r:getRequests()) if(status.equals(r.getStatus())) n++; s.put("request_"+status,n);
        }
        return Collections.unmodifiableMap(s);
    }
    /** Chỉ mở file dữ liệu do chính ứng dụng tạo. Ghi tạm rồi thay file đích. */
    public void save(Path file) throws IOException {
        Path target=file.toAbsolutePath(); Files.createDirectories(target.getParent());
        Path temp=Files.createTempFile(target.getParent(),"exchange-", ".tmp");
        try {
            try(ObjectOutputStream out=new ObjectOutputStream(Files.newOutputStream(temp))) { out.writeObject(this); }
            try { Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public static Manager load(Path file) throws IOException, ClassNotFoundException {
        try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(file))) {
            in.setObjectInputFilter(ObjectInputFilter.Config.createFilter("maxdepth=30;maxrefs=100000;maxbytes=20000000;Manager;User;Item;Post;ExchangeRequest;ItemManager;PostManager;java.base/*;!*"));
            Object data=in.readObject();
            if(!(data instanceof Manager)) throw new IOException("File không chứa dữ liệu dự án");
            return (Manager)data;
        }
    }
}

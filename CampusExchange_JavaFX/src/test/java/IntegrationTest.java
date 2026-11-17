import java.nio.file.*;
import java.util.*;

/** Chạy bằng java IntegrationTest, không phụ thuộc JUnit. */
public class IntegrationTest {
    private static int passed;
    private static void check(boolean ok, String name) {
        if(!ok) throw new AssertionError(name); passed++; System.out.println("PASS "+name);
    }
    private static void fails(Runnable action, String name) {
        boolean failed=false;
        try { action.run(); } catch(IllegalArgumentException|IllegalStateException|UnsupportedOperationException e) { failed=true; }
        check(failed,name);
    }
    private static Manager fixture() {
        Manager m=new Manager();
        for(String id:Arrays.asList("U01","U02","U03")) m.addUser(new User(id,id,"a@example.com","0900000000","Sinh viên"));
        m.addItem("U01",new Item(" i01 ","Sách Java","Sách","Tốt","Sách học Java","Trao doi"));
        m.createPost("U01"," p01 ","Sách Java","I01"); return m;
    }
    public static void main(String[] args) throws Exception {
        Manager accounts=new Manager();
        User account=accounts.register("alice","Alice","alice@example.com","0901000000","Password123!");
        check(accounts.authenticate(" ALICE ","Password123!")==account,"Đăng nhập đúng tài khoản, không phân biệt hoa thường tên đăng nhập");
        fails(()->accounts.authenticate("alice","wrong"),"Chặn mật khẩu sai");
        fails(()->accounts.authenticate("nobody","Password123!"),"Chặn tài khoản không tồn tại");
        fails(()->accounts.register("ALICE","A","e","p","Password123!"),"Chặn tên đăng nhập trùng");
        fails(()->accounts.register("bob","B","e","p","123"),"Chặn mật khẩu quá ngắn");
        fails(()->accounts.initializeLegacyPassword("alice","NewPassword123!"),"Không ghi đè mật khẩu đã có");
        check(accounts.getUsers().size()==1,"Đăng ký lỗi không tạo hồ sơ thừa");
        Path authFile=Files.createTempFile("campus-auth-",".dat");
        try{accounts.save(authFile);check("ALICE".equals(Manager.load(authFile).authenticate("alice","Password123!").getId()),"Tài khoản đăng nhập được sau khi mở lại");}
        finally{Files.deleteIfExists(authFile);}
        Manager legacy=new Manager();legacy.addUser(new User("OLD","Cũ","e","p","Sinh viên"));
        check(legacy.hasLegacyUsers(),"Nhận biết hồ sơ cũ chưa có mật khẩu");
        fails(()->legacy.authenticate("OLD","Password123!"),"Hồ sơ cũ không có mật khẩu mặc định");
        legacy.initializeLegacyPassword("OLD","Password123!");check(!legacy.hasLegacyUsers()&&legacy.authenticate("OLD","Password123!")!=null,"Chuyển đổi hồ sơ cũ giữ nguyên người dùng");
        Manager m=fixture();
        java.io.ByteArrayOutputStream imageOut=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",imageOut);
        byte[] image=imageOut.toByteArray();
        m.editPost("U01","P01","Sách Java",image);
        fails(()->m.editPost("U02","P01","Sai chủ",image),"Chặn sửa ảnh sai chủ");
        fails(()->m.editPost("U01","P01","Sai ảnh",new byte[]{1,2,3}),"Chặn file không phải ảnh");
        fails(()->m.editPost("U01","P01","Ảnh lớn",new byte[5*1024*1024+1]),"Chặn ảnh vượt 5 MB");
        check(Arrays.equals(image,m.getPosts().get(0).getImageData()),"Ảnh không đổi khi sửa lỗi");
        check("I01".equals(m.getItems().get(0).getItemId()),"Chuẩn hóa mã");
        check("Trao đổi".equals(m.getItems().get(0).getTransactionType()),"Chuẩn hóa hình thức");
        fails(()->m.addUser(new User(" u01 ","X","e","p","r")),"Chặn User trùng mã");
        fails(()->m.addItem("U02",new Item("I02","","Sách","Tốt","Mô tả","Bán")),"Chặn Item sai dữ liệu");
        fails(()->m.createPost("U02","P02","Sai chủ","I01"),"Kiểm tra chủ đồ vật");
        fails(()->m.createPost("U01","P02","Trùng","I01"),"Chặn hai bài cùng đồ vật");
        fails(()->m.sendRequest("U01","R00","P01"),"Chặn tự gửi");
        fails(()->m.sendRequest("U99","R00","P01"),"Chặn User không tồn tại");
        fails(()->m.deleteItem("U01","I01"),"Chặn xóa đồ vật có lịch sử");
        fails(()->m.deleteUser("U01"),"Chặn xóa User có liên kết");
        fails(()->m.getPosts().clear(),"Danh sách bài chỉ đọc");
        fails(()->m.getItems().clear(),"Danh sách vật chỉ đọc");
        m.editItem("U01","I01",new Item("I01","Sách mới","Sách","Tốt","Mô tả","Trao đổi"));
        check("Sách mới".equals(m.getPosts().get(0).getItem().getName()),"Item dùng chung tham chiếu");
        check(m.search("SÁCH",null,null,"Trao doi","OPEN").size()==1,"Tìm kiếm và lọc chuẩn hóa");
        m.sendRequest("U02","R01","P01");m.sendRequest("U03","R02","P01");
        fails(()->m.sendRequest("U02","R03","P01"),"Chặn yêu cầu chờ bị lặp");
        fails(()->m.acceptRequest("U03","R01"),"Chặn người khác duyệt");
        check("PENDING".equals(m.getRequests().get(0).getStatus()),"Duyệt sai không đổi trạng thái");
        m.acceptRequest("U01","R01");
        check("RESERVED".equals(m.getPosts().get(0).getStatus())&&"RESERVED".equals(m.getItems().get(0).getStatus()),"Giữ chỗ bài và vật thật");
        check("REJECTED".equals(m.getRequests().get(1).getStatus()),"Từ chối yêu cầu cạnh tranh");
        fails(()->m.acceptRequest("U01","R02"),"Chặn duyệt yêu cầu đã từ chối");
        fails(()->m.sendRequest("U03","R04","P01"),"Chặn gửi khi giữ chỗ");
        check(m.getStatistics().get("request_COMPLETED")==0,"Chấp nhận chưa phải hoàn tất");
        fails(()->m.completeRequest("U02","R01"),"Chặn hoàn tất sai người");
        m.completeRequest("U01","R01");
        check("CLOSED".equals(m.getPosts().get(0).getStatus())&&"EXCHANGED".equals(m.getItems().get(0).getStatus()),"Hoàn tất đồng bộ");
        check(m.getStatistics().get("request_COMPLETED")==1,"Thống kê hoàn tất");
        fails(()->m.cancelRequest("U01","R01"),"Không hủy giao dịch hoàn tất");
        Path file=Files.createTempFile("exchange-test-",".dat");
        try { m.save(file);Manager loaded=Manager.load(file);
            check(loaded.getStatistics().equals(m.getStatistics()),"Lưu và nạp đúng thống kê");
            check(loaded.getPosts().get(0).getItem()==loaded.getItems().get(0),"Khôi phục tham chiếu chung");
            check(Arrays.equals(image,loaded.getPosts().get(0).getImageData()),"Ảnh còn nguyên sau lưu và mở lại");
        } finally { Files.deleteIfExists(file); }
        Manager cancel=fixture();cancel.sendRequest("U02","R01","P01");cancel.acceptRequest("U01","R01");cancel.cancelRequest("U02","R01");
        check("OPEN".equals(cancel.getPosts().get(0).getStatus())&&"AVAILABLE".equals(cancel.getItems().get(0).getStatus()),"Hủy giải phóng giữ chỗ");
        cancel.sendRequest("U03","R02","P01");cancel.rejectRequest("U01","R02");
        check("OPEN".equals(cancel.getPosts().get(0).getStatus()),"Từ chối không đóng bài");
        PostManager pm=new PostManager();pm.addPost(new Post("P99","Bài","U01",cancel.getItems().get(0)));
        check(!pm.updatePostStatus("P99",""),"Trạng thái rỗng trả false");
        fails(()->cancel.getPosts().get(0).setStatus("ABC"),"Chặn trạng thái tùy ý");
        Manager posts=fixture();
        fails(()->posts.editPost("U02","P01","X"),"Chặn sửa bài sai chủ");
        posts.editPost("U01","P01","Tiêu đề mới");
        check("Tiêu đề mới".equals(posts.getPosts().get(0).getTitle()),"Sửa tiêu đề qua Manager");
        posts.sendRequest("U02","R01","P01");posts.closePost("U01","P01");
        check("U01".equals(posts.getRequests().get(0).getReceiverUsername()),"Yêu cầu gửi đúng người đăng");
        check("CLOSED".equals(posts.getPosts().get(0).getStatus())&&"REJECTED".equals(posts.getRequests().get(0).getStatus()),"Đóng bài từ chối yêu cầu đang chờ");
        fails(()->posts.deletePost("U01","P01"),"Giữ bài có lịch sử yêu cầu");
        Manager deletion=fixture();deletion.deletePost("U01","P01");
        check(deletion.getPosts().isEmpty(),"Xóa bài chưa có yêu cầu");
        deletion.deleteItem("U01","I01");check(deletion.getItems().isEmpty(),"Xóa vật sau xóa bài không lịch sử");
        System.out.println("TOTAL PASSED: "+passed);
    }
}

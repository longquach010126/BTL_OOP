import javafx.application.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.*;

/** Test tương tác JavaFX thực, chạy riêng với dữ liệu tạm. */
public class FxSmokeTest extends Application {
    CampusExchangeApp app;int passed;
    final List<String> screens=Arrays.asList("Trang chủ","Tổng quan","Người dùng","Đồ vật","Yêu cầu trao đổi","Thống kê");
    void check(boolean ok,String name){if(!ok)throw new AssertionError(name);passed++;System.out.println("FX PASS "+name);}
    @Override public void start(Stage stage) throws Exception {
        Path data=Files.createTempDirectory("campus-fx-test-").resolve("test.dat");System.setProperty("campus.data",data.toString());
        Thread.currentThread().setUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});
        app=new CampusExchangeApp();app.start(stage);later(()->{
            check(app.manager.getUsers().isEmpty(),"Khởi động kho trống");
            app.root.applyCss();app.root.layout();
            check(app.root.lookup("#auth-submit")!=null&&app.root.getLeft()==null,"Khởi động ở màn hình đăng nhập");
            snapshot("login.png");
            app.showPage("Trang chủ");check(app.postsGrid==null,"Không mở trang khi chưa đăng nhập");
            register("U001","Người dùng thử");register("U002","Người dùng hai");
            check(app.manager.getUsers().size()==2&&Files.exists(data),"Đăng ký tạo hồ sơ và lưu dữ liệu");
            field("#auth-username","U001");field("#auth-password","SaiMatKhau");click("#auth-submit");
            check(app.root.getLeft()==null,"Sai mật khẩu không mở ứng dụng");
            login("U001");check(app.root.lookup("#actor")==null&&app.root.lookup("#logout")!=null,"Đăng nhập thay bộ chọn người thao tác");
            app.showPage("Người dùng");check(app.root.lookup("#add-user")==null,"Danh bạ không cho tạo hồ sơ bỏ qua đăng ký");
            app.showPage("Đồ vật");form("#add-item",pane->{text(pane,"#item-name","Sách kiểm thử");text(pane,"#item-category","Sách");((TextArea)pane.lookup("#item-description")).setText("Sách học JavaFX");});
            check(app.itemsGrid.getChildren().size()==1,"Lưới đồ vật cập nhật sau thêm");
            check(app.root.lookup("#edit-item-I001")!=null,"Chủ đồ vật thấy thao tác sửa");
            TextField itemSearch=(TextField)app.root.lookup("#item-search");itemSearch.setText("không tồn tại");
            check(app.itemsGrid.getChildren().isEmpty()&&app.root.lookup("#items-empty").isVisible(),"Tìm kiếm kho đồ hiển thị trạng thái rỗng");itemSearch.clear();
            login("U002");app.showPage("Đồ vật");
            check(app.root.lookup("#edit-item-I001")==null&&app.root.lookup("#post-item-I001")==null,"Ẩn thao tác chủ đồ vật khi đổi người dùng");
            ((CheckBox)app.root.lookup("#items-mine")).setSelected(true);check(app.itemsGrid.getChildren().isEmpty(),"Bộ lọc đồ của tôi");
            login("U001");app.showPage("Đồ vật");
            form("#post-item-I001",pane->{
                check(pane.lookup("#choose-post-image") instanceof Button,"Form có nút chọn ảnh");
                check(pane.lookup("#post-image-preview") instanceof javafx.scene.image.ImageView,"Form có xem trước ảnh");
                text(pane,"#post-title","Bài kiểm thử");
            });
            check(app.root.lookup("#post-item-I001").isDisabled()&&app.root.lookup("#delete-item-I001").isDisabled(),"Chặn đăng trùng và xóa đồ vật có lịch sử");
            app.showPage("Trang chủ");
            check(app.postsGrid.getChildren().size()==1,"Tạo Post từ đồ vật trong form");
            check(app.root.lookup("#post-menu-P001")!=null,"Chủ bài thấy menu quản lý");
            app.showPage("Tổng quan");
            check(app.root.lookup("#post-card-P001")!=null&&app.root.lookup("#post-menu-P001")!=null,"Tổng quan dùng cùng thẻ và quyền quản lý bài đăng");
            app.showPage("Trang chủ");
            check(app.root.lookup("#send-request-P001")==null,"Chủ bài không có nút tự gửi");
            TextField search=(TextField)app.root.lookup("#post-search");search.setText("không tồn tại");
            check(app.postsGrid.getChildren().isEmpty()&&app.root.lookup("#posts-empty").isVisible(),"Tìm kiếm hiển thị trạng thái rỗng");search.clear();
            login("U002");app.showPage("Tổng quan");check(app.root.lookup("#post-menu-P001")==null,"Ẩn quản lý bài của người khác");form("#send-request-P001",pane->{});
            check(app.manager.getRequests().size()==1,"Gửi yêu cầu từ thẻ bài đăng");
            check(app.root.lookup("#send-request-P001").isDisabled(),"Tổng quan cập nhật sau khi gửi yêu cầu");
            app.showPage("Trang chủ");
            check(app.root.lookup("#send-request-P001").isDisabled(),"Chặn gửi lặp trên thẻ");
            app.showPage("Yêu cầu trao đổi");
            check(app.requestsList.lookup("#request-card-R001")!=null,"Yêu cầu hiển thị thành thẻ");
            check(app.requestsList.lookup("#accept-request-R001")==null&&app.requestsList.lookup("#cancel-request-R001")!=null,"Người gửi chỉ được hủy yêu cầu chờ");
            ComboBox<String> requestStatus=(ComboBox<String>)app.root.lookup("#request-status");requestStatus.setValue("COMPLETED");
            check(app.requestsList.lookup("#requests-empty")!=null,"Lọc yêu cầu hiển thị trạng thái rỗng");requestStatus.setValue("PENDING");
            login("U001");app.showPage("Yêu cầu trao đổi");
            check(app.requestsList.lookup("#reject-request-R001")!=null&&app.requestsList.lookup("#complete-request-R001")==null,"Chủ bài thấy đúng thao tác chờ duyệt");
            ((Button)app.requestsList.lookup("#accept-request-R001")).fire();
            check("RESERVED".equals(app.manager.getPosts().get(0).getStatus()),"Nút chấp nhận đồng bộ trạng thái");
            check(app.requestsList.lookup("#accept-request-R001")==null&&app.requestsList.lookup("#complete-request-R001")!=null,"Thẻ đã chấp nhận hiện nút hoàn tất");
            Platform.runLater(()->{DialogPane p=dialog();((Button)p.lookupButton(ButtonType.OK)).fire();});((Button)app.requestsList.lookup("#complete-request-R001")).fire();
            check(app.manager.getStatistics().get("request_COMPLETED")==1,"Nút hoàn tất và xác nhận");
            check(app.requestsList.lookup("#cancel-request-R001")==null&&app.requestsList.lookup("#complete-request-R001")==null,"Thẻ hoàn tất không còn thao tác thay đổi");
            app.showPage("Tổng quan");check(app.root.lookup("#post-menu-P001")==null&&app.root.lookup("#send-request-P001")==null,"Tổng quan đồng bộ bài đóng sau hoàn tất");
            try {check(Manager.load(data).getStatistics().get("request_COMPLETED")==1,"Nạp lại file tự lưu");}catch(Exception e){throw new RuntimeException(e);}
            app.manager=DemoData.create();app.logout();app.root.applyCss();app.root.layout();
            check(app.root.lookup("#legacy-save")!=null,"Dữ liệu cũ yêu cầu thiết lập mật khẩu");
            for(int n=0;n<3;n++){field("#legacy-password","TestPass123!");field("#legacy-confirm","TestPass123!");click("#legacy-save");}
            login("U01");
            snapshotPage(0);
        });
    }
    void field(String selector,String value){app.root.applyCss();app.root.layout();((TextField)app.root.lookup(selector)).setText(value);}
    void click(String selector){app.root.applyCss();app.root.layout();((Button)app.root.lookup(selector)).fire();app.root.applyCss();app.root.layout();}
    void register(String id,String name){click("#auth-toggle");field("#auth-username",id);field("#auth-name",name);field("#auth-email",id+"@example.com");field("#auth-phone","0901000000");field("#auth-password","TestPass123!");field("#auth-confirm","TestPass123!");click("#auth-submit");}
    void login(String id){app.logout();app.root.applyCss();app.root.layout();check(app.root.getLeft()==null,"Đăng xuất xóa giao diện phiên trước");field("#auth-username",id);field("#auth-password","TestPass123!");click("#auth-submit");check(app.root.getLeft()!=null,"Đăng nhập thành công "+id);check("Trang chủ".equals(app.page)&&app.postsGrid!=null,"Đăng nhập mở Trang chủ bài đăng");}
    void snapshotPage(int i){if(i==screens.size()){app.stage.setWidth(1040);app.stage.setHeight(700);app.showPage("Trang chủ");later(()->{snapshot("minimum-window.png");check(app.root.getWidth()>=1000,"Cửa sổ kích thước tối thiểu");System.out.println("FX TOTAL PASSED: "+passed);app.dirty=false;app.stage.hide();Platform.exit();});return;}
        ((Button)app.root.lookup("#nav-"+i)).fire();later(()->{check(app.page.equals(screens.get(i)),"Điều hướng "+screens.get(i));snapshot("screen-"+i+".png");snapshotPage(i+1);});}
    void snapshot(String name){try{Files.createDirectories(Paths.get("ui-check"));ImageIO.write(SwingFXUtils.fromFXImage(app.root.snapshot(null,null),null),"png",Paths.get("ui-check",name).toFile());}catch(Exception e){throw new RuntimeException(e);}}
    void form(String trigger,java.util.function.Consumer<DialogPane> fill){Platform.runLater(()->{DialogPane pane=dialog();fill.accept(pane);((Button)pane.lookup("#form-save")).fire();});((Button)app.root.lookup(trigger)).fire();}
    DialogPane dialog(){for(Window w:new ArrayList<>(Window.getWindows()))if(w.isShowing()&&w.getScene()!=null&&w.getScene().getRoot() instanceof DialogPane)return (DialogPane)w.getScene().getRoot();throw new AssertionError("Không thấy Dialog");}
    void text(DialogPane pane,String id,String value){((TextField)pane.lookup(id)).setText(value);}
    void later(Runnable run){PauseTransition p=new PauseTransition(Duration.millis(300));p.setOnFinished(e->Platform.runLater(()->{try{run.run();}catch(Throwable ex){ex.printStackTrace();System.exit(1);}}));p.play();}
    public static void main(String[] args){launch(args);}
}

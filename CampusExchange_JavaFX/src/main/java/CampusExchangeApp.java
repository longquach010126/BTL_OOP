import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;
import javafx.stage.*;
import javafx.util.StringConverter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/** Giao diện JavaFX, gọi Manager cho mọi thay đổi nghiệp vụ. */
public class CampusExchangeApp extends Application {
    Manager manager;
    Stage stage;
    BorderPane root;
    VBox content;
    private User signedInUser;
    Label message=new Label("Sẵn sàng"), saveState=new Label("Chưa có thay đổi");
    final Path dataPath=Paths.get(System.getProperty("campus.data", "data/exchange.dat"));
    final Map<String,Button> navButtons=new LinkedHashMap<>();
    String page="Trang chủ";
    boolean dirty;
    TableView<User> usersTable;
    FlowPane itemsGrid;
    FlowPane postsGrid;
    VBox requestsList;
    static final String ALL="Tất cả";
    @Override public void start(Stage primary) {
        stage=primary;
        try { manager=Files.exists(dataPath)?Manager.load(dataPath):new Manager(); }
        catch(Exception e) { alert(Alert.AlertType.ERROR,"Không thể mở dữ liệu","Không ghi đè dữ liệu cũ. Hãy sao lưu hoặc kiểm tra file:\n"+dataPath.toAbsolutePath()+"\n"+e.getMessage()); Platform.exit(); return; }
        root=new BorderPane();root.getStyleClass().add("app");
        Scene scene=new Scene(root,1280,820);scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setScene(scene);stage.setTitle("Campus Exchange • Đồ cũ, giá trị mới");stage.setMinWidth(1040);stage.setMinHeight(700);
        stage.setOnCloseRequest(e->{if(dirty&&!save(false))e.consume();});
        showAuth(false);stage.show();
    }
    private void showWorkspace(){
        navButtons.clear();root.setLeft(sidebar());content=new VBox(20);content.getStyleClass().add("content");
        ScrollPane scroll=new ScrollPane(content);scroll.setFitToWidth(true);scroll.getStyleClass().add("content-scroll");root.setCenter(scroll);root.setTop(topbar());
        HBox status=new HBox(12,message,new Region(),saveState);HBox.setHgrow(status.getChildren().get(1),Priority.ALWAYS);status.getStyleClass().add("statusbar");root.setBottom(status);
        showPage("Trang chủ");
    }
    private void showAuth(boolean register){
        signedInUser=null;navButtons.clear();root.setLeft(null);root.setTop(null);root.setBottom(null);content=null;
        itemsGrid=null;postsGrid=null;requestsList=null;usersTable=null;
        VBox box=new VBox(12);box.setMaxWidth(440);box.getStyleClass().add("auth-card");
        box.getChildren().addAll(label("CAMPUS EXCHANGE","eyebrow"),label(register?"Tạo tài khoản":"Chào mừng trở lại","page-title"));
        Label error=label("","form-error");
        if(manager.hasLegacyUsers()){
            User legacy=manager.getUsers().stream().filter(u->!u.hasPassword()).findFirst().orElseThrow();
            box.getChildren().addAll(label("Thiết lập tài khoản cho dữ liệu cũ","section-title"),label("Dữ liệu cũ chưa có mật khẩu. Chủ máy cần đặt mật khẩu riêng cho từng hồ sơ trước khi sử dụng. Đây chỉ là bước chuyển đổi lần đầu.","muted"),label(legacy.getId()+" · "+legacy.getName(),"summary"));
            PasswordField password=new PasswordField();password.setId("legacy-password");password.setPromptText("Mật khẩu mới (8–128 ký tự)");
            PasswordField repeat=new PasswordField();repeat.setId("legacy-confirm");repeat.setPromptText("Nhập lại mật khẩu");
            Button setup=button("Lưu mật khẩu và tiếp tục","primary",()->{try{
                if(!password.getText().equals(repeat.getText()))throw new IllegalArgumentException("Mật khẩu nhập lại không khớp");
                if(!legacy.hasPassword())manager.initializeLegacyPassword(legacy.getId(),password.getText());dirty=true;
                if(save(false))showAuth(false);
            }catch(Exception ex){error.setText(ex.getMessage());}});setup.setId("legacy-save");setup.setDefaultButton(true);
            box.getChildren().addAll(password,repeat,error,setup);
        }else{
            TextField username=new TextField();username.setId("auth-username");username.setPromptText("Tên đăng nhập / mã người dùng");
            PasswordField password=new PasswordField();password.setId("auth-password");password.setPromptText("Mật khẩu");
            TextField name=new TextField(),email=new TextField(),phone=new TextField();name.setId("auth-name");email.setId("auth-email");phone.setId("auth-phone");name.setPromptText("Họ và tên");email.setPromptText("Email");phone.setPromptText("Số điện thoại");
            PasswordField repeat=new PasswordField();repeat.setId("auth-confirm");repeat.setPromptText("Nhập lại mật khẩu (tối thiểu 8 ký tự)");
            box.getChildren().add(username);if(register)box.getChildren().addAll(name,email,phone);box.getChildren().add(password);if(register)box.getChildren().add(repeat);
            Button submit=button(register?"Đăng ký":"Đăng nhập","primary",()->{try{
                if(register){
                    if(!password.getText().equals(repeat.getText()))throw new IllegalArgumentException("Mật khẩu nhập lại không khớp");
                    manager.register(username.getText(),name.getText().trim(),email.getText().trim(),phone.getText().trim(),password.getText());dirty=true;
                    if(!save(false)){manager.deleteUser(username.getText());return;}
                    showAuth(false);message.setText("Đăng ký thành công");return;
                }
                signedInUser=manager.authenticate(username.getText(),password.getText());password.clear();showWorkspace();
            }catch(Exception ex){password.clear();repeat.clear();error.setText(ex.getMessage());}});submit.setId("auth-submit");submit.setDefaultButton(true);submit.setMaxWidth(Double.MAX_VALUE);
            Button toggle=button(register?"Đã có tài khoản? Đăng nhập":"Chưa có tài khoản? Đăng ký","link-button",()->showAuth(!register));toggle.setId("auth-toggle");
            box.getChildren().addAll(error,submit,toggle,label("Mỗi tài khoản là một người dùng. Đăng nhập để quản lý đồ và trao đổi.","muted"));
        }
        ScrollPane scroll=new ScrollPane(box);scroll.setFitToWidth(true);scroll.setMaxWidth(480);scroll.setMaxHeight(650);scroll.getStyleClass().add("auth-scroll");
        StackPane center=new StackPane(scroll);center.setPadding(new Insets(30));root.setCenter(center);
    }
    void logout(){if(dirty&&!save(false))return;message.setText("Sẵn sàng");page="Trang chủ";showAuth(false);}
    private VBox sidebar() {
        Label mark=label("CE", "brand-mark");Label brand=label("Campus\nExchange", "brand");
        HBox logo=new HBox(12,mark,brand);logo.setAlignment(Pos.CENTER_LEFT);
        Label group=label("KHÔNG GIAN CỦA BẠN", "nav-caption");
        VBox box=new VBox(10,logo,group);box.getStyleClass().add("sidebar");box.setPrefWidth(224);
        String[] names={"Trang chủ","Tổng quan","Người dùng","Đồ vật","Yêu cầu trao đổi","Thống kê"};
        String[] icons={"⌂","◈","◎","▣","⇄","▥"};
        for(int i=0;i<names.length;i++){String n=names[i];Button b=button(icons[i]+"   "+n,"nav",()->showPage(n));b.setId("nav-"+i);b.setMaxWidth(Double.MAX_VALUE);navButtons.put(n,b);box.getChildren().add(b);}
        Region spacer=new Region();VBox.setVgrow(spacer,Priority.ALWAYS);box.getChildren().add(spacer);
        VBox note=new VBox(8,label("Trao lại giá trị", "sidebar-title"),label("Một món đồ cũ.\nMột khởi đầu mới.","sidebar-note"));note.getStyleClass().add("sidebar-note-box");box.getChildren().add(note);
        box.getChildren().add(label("BÀI TẬP LỚN OOP  /  NHÓM 07","version"));return box;
    }
    private HBox topbar() {
        Label breadcrumb=label("CAMPUS EXCHANGE  /  QUẢN LÝ ĐỒ CŨ", "eyebrow");Region gap=new Region();HBox.setHgrow(gap,Priority.ALWAYS);
        Button save=button("Lưu dữ liệu","secondary",()->save(true));save.setId("save");
        Button logout=button("Đăng xuất","secondary",this::logout);logout.setId("logout");
        HBox bar=new HBox(12,breadcrumb,gap,label(signedInUser.getName()+" ("+signedInUser.getId()+")","muted"),save,logout);bar.setAlignment(Pos.CENTER_LEFT);bar.getStyleClass().add("topbar");return bar;
    }
    void showPage(String name) {
        if(signedInUser==null)return;
        page=name;content.getChildren().clear();
        navButtons.forEach((key,b)->{b.getStyleClass().remove("nav-active");if(key.equals(name))b.getStyleClass().add("nav-active");});
        switch(name){case "Người dùng": usersPage();break;case "Đồ vật":itemsPage();break;case "Trang chủ":postsPage();break;case "Yêu cầu trao đổi":requestsPage();break;case "Thống kê":statsPage();break;default:dashboard();}
    }
    private void heading(String title,String subtitle,Node... actions) {
        VBox text=new VBox(5,label(title,"page-title"),label(subtitle,"muted"));Region gap=new Region();HBox.setHgrow(gap,Priority.ALWAYS);
        HBox row=new HBox(10,text,gap);row.setAlignment(Pos.CENTER_LEFT);row.getChildren().addAll(actions);content.getChildren().add(row);
    }
    private void dashboard() {
        heading("Cùng chia sẻ, cùng tiết kiệm","Theo dõi đồ vật, kết nối nhu cầu và tiếp nối giá trị.",button("+ Đăng bài mới","primary",()->guard(()->postDialog(null))));
        VBox hero=new VBox(10,label("GÓC ĐỒ CŨ SINH VIÊN", "hero-eyebrow"),label("Đồ không còn dùng,\ngiá trị vẫn còn nguyên.","hero-title"),label("Quản lý bài đăng và trao đổi trong một không gian chung.","hero-copy"));
        HBox heroActions=new HBox(10,button("Khám phá bài đăng  →","hero-button",()->showPage("Trang chủ")),button("Thêm đồ vật","hero-secondary",()->guard(()->itemDialog(null))));hero.getChildren().add(heroActions);hero.getStyleClass().add("hero");content.getChildren().add(hero);
        content.getChildren().add(kpis());
        Label recent=label("Bài đăng gần đây", "section-title");content.getChildren().add(recent);
        FlowPane cards=new FlowPane(18,20);cards.setId("recent-posts");cards.setAlignment(Pos.TOP_LEFT);cards.prefWrapLengthProperty().bind(content.widthProperty().subtract(60));
        List<Post> recentPosts=manager.getPosts().stream().sorted(Comparator.comparing(Post::getCreatedAt).thenComparing(Post::getPostId).reversed()).limit(3).collect(Collectors.toList());
        if(recentPosts.isEmpty()) {
            VBox empty=new VBox(10,label("Bắt đầu vòng đời mới cho đồ cũ", "section-title"),label("Thêm đồ vật rồi tạo bài đăng đầu tiên.","muted"),button("Thêm đồ vật","primary",()->guard(()->itemDialog(null))));empty.getStyleClass().add("panel");cards.getChildren().add(empty);
        } else for(Post p:recentPosts)cards.getChildren().add(postCard(p));
        content.getChildren().add(cards);
        if(!recentPosts.isEmpty())content.getChildren().add(button("Xem tất cả bài đăng →","link-button",()->showPage("Trang chủ")));
        content.getChildren().add(label("Bạn đang đăng nhập bằng tài khoản của mình. Chỉ chủ sở hữu được quản lý đồ vật và bài đăng.","footnote"));
    }
    private HBox kpis() {
        Map<String,Integer>s=manager.getStatistics();HBox row=new HBox(14);
        String[] names={"Đồ vật","Bài đang mở","Yêu cầu chờ","Đã hoàn tất"};String[] keys={"items","post_OPEN","request_PENDING","request_COMPLETED"};
        for(int i=0;i<names.length;i++){VBox card=new VBox(8,label(names[i],"muted"),label(s.get(keys[i]).toString(),"metric"));card.getStyleClass().add("metric-card");HBox.setHgrow(card,Priority.ALWAYS);card.setMaxWidth(Double.MAX_VALUE);row.getChildren().add(card);}return row;
    }
    private void usersPage() {
        heading("Người dùng","Các thành viên trong cộng đồng. Bạn chỉ có thể sửa hồ sơ của mình.",button("Sửa hồ sơ của tôi","primary",()->userDialog(signedInUser)));
        usersTable=table();usersTable.setId("users-table");column(usersTable,"Mã",User::getId,80);column(usersTable,"Họ và tên",User::getName,220);column(usersTable,"Email",User::getEmail,250);column(usersTable,"Điện thoại",User::getPhone,150);column(usersTable,"Vai trò",User::getRole,130);
        usersTable.getItems().setAll(manager.getUsers());TextField search=search("Tìm tên, mã hoặc email...");search.textProperty().addListener((o,a,b)->usersTable.getItems().setAll(manager.getUsers().stream().filter(u->matches(b,u.getId(),u.getName(),u.getEmail())).collect(Collectors.toList())));
        panel(search,usersTable);
    }
    private void itemsPage() {
        Button add=button("+ Thêm đồ vật","primary",()->guard(()->itemDialog(null)));add.setId("add-item");heading("Kho đồ vật","Chăm chút kho đồ của bạn, sẵn sàng cho lần trao đổi tiếp theo.",add);
        TextField keyword=search("Tìm tên hoặc mô tả...");keyword.setId("item-search");keyword.setMinWidth(100);
        HBox searchBox=new HBox(10,label("⌕","search-icon"),keyword);searchBox.setAlignment(Pos.CENTER_LEFT);searchBox.getStyleClass().add("market-search");HBox.setHgrow(keyword,Priority.ALWAYS);HBox.setHgrow(searchBox,Priority.ALWAYS);
        CheckBox mine=new CheckBox("Đồ của tôi");mine.setId("items-mine");mine.setMinWidth(100);
        ComboBox<String> state=filter("Trạng thái","AVAILABLE","RESERVED","EXCHANGED");state.setId("item-status");state.setAccessibleText("Trạng thái đồ vật");
        HBox filters=new HBox(16,searchBox,state,mine);filters.setAlignment(Pos.CENTER_LEFT);filters.getStyleClass().add("item-filter-bar");
        itemsGrid=new FlowPane(18,20);itemsGrid.setId("items-grid");itemsGrid.setAlignment(Pos.TOP_LEFT);itemsGrid.prefWrapLengthProperty().bind(content.widthProperty().subtract(60));
        Label count=label("","muted"),empty=label("Chưa có đồ vật phù hợp. Hãy thêm đồ vật hoặc thử bộ lọc khác.","market-empty");empty.setId("items-empty");
        Runnable update=()->{
            List<Item> found=manager.getItems().stream().filter(i->matches(keyword.getText(),i.getName(),i.getDescription())&&(!mine.isSelected()||Objects.equals(currentActor(),i.getOwnerId()))&&filtered(state,i.getStatus())).collect(Collectors.toList());
            itemsGrid.getChildren().clear();for(Item i:found)itemsGrid.getChildren().add(itemCard(i));count.setText(found.size()+" đồ vật");empty.setVisible(found.isEmpty());empty.setManaged(found.isEmpty());
        };
        keyword.textProperty().addListener((o,previous,value)->update.run());mine.selectedProperty().addListener((o,previous,value)->update.run());state.valueProperty().addListener((o,previous,value)->update.run());
        content.getChildren().addAll(filters,count,itemsGrid,empty);update.run();
    }
    private VBox itemCard(Item i){
        VBox card=new VBox();card.setId("item-card-"+i.getItemId());card.getStyleClass().add("market-card");card.setMinWidth(280);card.setPrefWidth(280);card.setMaxWidth(280);
        StackPane media=new StackPane();media.getStyleClass().add("item-media");media.setMinHeight(278);media.setPrefHeight(278);media.setMaxHeight(278);
        byte[] photo=manager.getPosts().stream().filter(p->p.getItem().getItemId().equals(i.getItemId())&&p.getImageData()!=null).reduce((first,last)->last).map(Post::getImageData).orElse(null);
        if(photo!=null)media.getChildren().add(postImage(photo,276,276));else{VBox placeholder=new VBox(8,label("▧","photo-placeholder"),label("Chưa có ảnh bài đăng","muted"));placeholder.setAlignment(Pos.CENTER);media.getChildren().add(placeholder);}
        Label state=badge(i.getStatus());StackPane.setAlignment(state,Pos.TOP_RIGHT);StackPane.setMargin(state,new Insets(12));media.getChildren().add(state);
        Label title=label(i.getName(),"market-title");title.setMinHeight(50);title.setMaxHeight(50);title.setTooltip(new Tooltip(i.getName()));
        Label owner=label("Chủ: "+person(i.getOwnerId()),"muted");owner.setMinHeight(34);
        VBox body=new VBox(8,title,label("Mã: "+i.getItemId()+" · Loại: "+i.getCategory(),"muted"),label("Chất lượng: "+i.getCondition(),"muted"),label("Hình thức: "+i.getTransactionType(),"muted"),owner);body.getStyleClass().add("market-body");
        Button details=button("Chi tiết","secondary",()->itemDetails(i));details.setId("item-details-"+i.getItemId());
        FlowPane actions=new FlowPane(8,8,details);actions.getStyleClass().add("item-actions");
        if(Objects.equals(currentActor(),i.getOwnerId())){
            Button edit=button("Sửa thông tin","secondary",()->guard(()->itemDialog(i)));edit.setId("edit-item-"+i.getItemId());edit.setDisable(!"AVAILABLE".equals(i.getStatus()));if(edit.isDisabled())edit.setTooltip(new Tooltip("Chỉ sửa đồ vật còn sẵn"));actions.getChildren().add(edit);
            boolean history=manager.getPosts().stream().anyMatch(p->p.getItem().getItemId().equals(i.getItemId()));
            boolean active=manager.getPosts().stream().anyMatch(p->p.getItem().getItemId().equals(i.getItemId())&&!"CLOSED".equals(p.getStatus()));
            Button create=button("Tạo bài đăng","primary",()->guard(()->postDialogFor(i)));create.setId("post-item-"+i.getItemId());create.setDisable(!"AVAILABLE".equals(i.getStatus())||active);
            if(create.isDisabled())create.setTooltip(new Tooltip(active?"Đồ vật đã có bài đăng đang hoạt động":"Chỉ đăng đồ vật còn sẵn"));
            Button delete=button("Xóa đồ vật","danger",()->{if(confirm("Xóa đồ vật?",i.getName()))change(()->manager.deleteItem(actor(),i.getItemId()),"Đã xóa đồ vật");});delete.setId("delete-item-"+i.getItemId());delete.setDisable(history);if(history)delete.setTooltip(new Tooltip("Giữ lại đồ vật đã có lịch sử bài đăng"));
            actions.getChildren().addAll(create,delete);
        }
        body.getChildren().add(actions);card.getChildren().addAll(media,body);return card;
    }
    private void postsPage() {
        Button add=button("+ Đăng bài","primary",()->guard(()->postDialog(null)));add.setId("add-post");
        heading("Trang chủ","Khám phá món đồ bạn cần, chia sẻ món đồ bạn có.",add);
        TextField keyword=search("Tìm món đồ, tiêu đề hoặc mô tả…");keyword.setId("post-search");
        keyword.setPrefWidth(350);keyword.setMinWidth(100);
        HBox searchBox=new HBox(10,label("⌕","search-icon"),keyword);searchBox.setAlignment(Pos.CENTER_LEFT);searchBox.getStyleClass().add("market-search");HBox.setHgrow(keyword,Priority.ALWAYS);
        ComboBox<String> category=new ComboBox<>();category.getItems().add(ALL);category.getItems().addAll(manager.getItems().stream().map(Item::getCategory).distinct().sorted().collect(Collectors.toList()));category.setValue(ALL);
        ComboBox<String> type=filter("Hình thức","Bán","Trao đổi","Cho tặng"),status=filter("Trạng thái","OPEN","RESERVED","CLOSED");
        ComboBox<String> condition=new ComboBox<>();condition.getItems().add(ALL);condition.getItems().addAll(manager.getItems().stream().map(Item::getCondition).distinct().sorted().collect(Collectors.toList()));condition.setValue(ALL);
        FlowPane filters=new FlowPane(12,10);filters.getStyleClass().add("market-filters");
        String[] names={"Loại","Hình thức","Chất lượng","Trạng thái"};List<ComboBox<String>> combos=Arrays.asList(category,type,condition,status);
        for(int n=0;n<combos.size();n++){ComboBox<String> c=combos.get(n);c.setPrefWidth(155);c.setId("post-filter-"+n);c.setAccessibleText(names[n]);filters.getChildren().add(new VBox(5,label(names[n],"muted"),c));}
        VBox filterBar=new VBox(14,searchBox,filters);filterBar.getStyleClass().add("market-filter-bar");
        postsGrid=new FlowPane(18,20);postsGrid.setId("posts-grid");postsGrid.setAlignment(Pos.TOP_LEFT);
        // Bind wrapping to the available content width inside the existing page ScrollPane.
        postsGrid.prefWrapLengthProperty().bind(content.widthProperty().subtract(60));
        Label count=label("","muted"),empty=label("Chưa có món đồ phù hợp. Hãy thử từ khóa hoặc bộ lọc khác.","market-empty");empty.setId("posts-empty");
        Runnable update=()->{
            List<Post> found=manager.search(keyword.getText(),criteria(category),criteria(condition),criteria(type),criteria(status));
            postsGrid.getChildren().clear();for(Post p:found)postsGrid.getChildren().add(postCard(p));
            count.setText(found.size()+" bài đăng");empty.setVisible(found.isEmpty());empty.setManaged(found.isEmpty());
        };
        keyword.textProperty().addListener((o,x,y)->update.run());for(ComboBox<String> c:combos)c.setOnAction(e->update.run());
        content.getChildren().addAll(filterBar,count,postsGrid,empty);update.run();
    }
    private VBox postCard(Post p) {
        VBox card=new VBox();card.setId("post-card-"+p.getPostId());card.getStyleClass().add("market-card");card.setPrefWidth(280);card.setMinWidth(280);card.setMaxWidth(280);
        StackPane media=new StackPane();media.getStyleClass().add("market-media");media.setMinHeight(190);media.setPrefHeight(190);media.setMaxHeight(190);
        byte[] bytes=p.getImageData();
        if(bytes==null){VBox placeholder=new VBox(8,label("▧","photo-placeholder"),label("Chưa có ảnh","muted"));placeholder.setAlignment(Pos.CENTER);media.getChildren().add(placeholder);}
        else media.getChildren().add(postImage(bytes,278,188));
        Label state=badge(p.getStatus());StackPane.setAlignment(state,Pos.TOP_LEFT);StackPane.setMargin(state,new Insets(12));media.getChildren().add(state);
        Label title=label(p.getTitle(),"market-title");title.setMinHeight(50);title.setMaxHeight(50);title.setTooltip(new Tooltip(p.getTitle()));
        Label info=label("Mã: "+p.getPostId()+"  ·  "+p.getItem().getCategory(),"muted");
        Label type=label(p.getItem().getTransactionType()+"  ·  "+p.getItem().getCondition(),"market-type");
        Label owner=label("Người đăng: "+person(p.getAuthorUsername()),"muted");owner.setMinHeight(34);
        VBox body=new VBox(8,title,info,type,owner);body.getStyleClass().add("market-body");
        Button details=button("Chi tiết","secondary",()->postDetails(p));details.setId("post-details-"+p.getPostId());
        HBox actions=new HBox(8);actions.setAlignment(Pos.CENTER_LEFT);
        String actor=currentActor();boolean mine=Objects.equals(actor,p.getAuthorUsername());
        boolean pending=manager.getRequests().stream().anyMatch(r->p.getPostId().equals(r.getPostId())&&Objects.equals(actor,r.getSenderUsername())&&"PENDING".equals(r.getStatus()));
        if(actor!=null&&!mine&&"OPEN".equals(p.getStatus())&&"AVAILABLE".equals(p.getItem().getStatus())){
            Button send=button(pending?"Đã gửi yêu cầu":"Gửi yêu cầu","primary",()->guard(()->sendDialog(p)));send.setId("send-request-"+p.getPostId());send.setDisable(pending);send.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(send,Priority.ALWAYS);actions.getChildren().add(send);
        }
        actions.getChildren().add(details);
        if(mine){
            MenuButton menu=new MenuButton("•••");menu.setId("post-menu-"+p.getPostId());menu.setAccessibleText("Quản lý bài đăng "+p.getTitle());menu.getStyleClass().add("card-menu");
            if("OPEN".equals(p.getStatus())){
                MenuItem edit=new MenuItem("Sửa bài đăng");edit.setOnAction(e->guard(()->postDialog(p)));
                MenuItem close=new MenuItem("Đóng bài đăng");close.setOnAction(e->{if(confirm("Đóng bài đăng?","Yêu cầu đang chờ sẽ bị từ chối."))change(()->manager.closePost(actor(),p.getPostId()),"Đã đóng bài");});menu.getItems().addAll(edit,close);
            }
            boolean history=manager.getRequests().stream().anyMatch(r->p.getPostId().equals(r.getPostId()));
            if(!history&&!"RESERVED".equals(p.getStatus())){MenuItem delete=new MenuItem("Xóa bài đăng");delete.setOnAction(e->{if(confirm("Xóa bài đăng?",p.getTitle()))change(()->manager.deletePost(actor(),p.getPostId()),"Đã xóa bài");});menu.getItems().add(delete);}
            if(!menu.getItems().isEmpty()){Region space=new Region();HBox.setHgrow(space,Priority.ALWAYS);actions.getChildren().addAll(space,menu);}
        }
        body.getChildren().add(actions);card.getChildren().addAll(media,body);return card;
    }
    private void requestsPage() {
        heading("Yêu cầu trao đổi","Theo dõi kết nối, xử lý yêu cầu và xác nhận bàn giao.");
        ComboBox<String> scope=filter("Phạm vi","Gửi đến tôi","Tôi đã gửi"),status=filter("Trạng thái","PENDING","ACCEPTED","COMPLETED","REJECTED","CANCELLED");
        scope.setId("request-scope");status.setId("request-status");scope.setAccessibleText("Phạm vi yêu cầu");status.setAccessibleText("Trạng thái yêu cầu");
        FlowPane filters=new FlowPane(20,10,new VBox(6,label("Phạm vi","muted"),scope),new VBox(6,label("Trạng thái","muted"),status));filters.getStyleClass().add("request-filter-bar");
        requestsList=new VBox(16);requestsList.setId("requests-list");requestsList.setFillWidth(true);
        ScrollPane scroll=new ScrollPane(requestsList);scroll.setId("requests-scroll");scroll.setFitToWidth(true);scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);scroll.setPrefViewportHeight(510);scroll.setMinHeight(280);scroll.getStyleClass().add("request-scroll");
        Label count=label("","muted");
        Runnable update=()->{
            List<ExchangeRequest> found=manager.getRequests().stream().filter(r->filtered(status,r.getStatus())&&(ALL.equals(scope.getValue())||("Gửi đến tôi".equals(scope.getValue())?Objects.equals(currentActor(),r.getReceiverUsername()):Objects.equals(currentActor(),r.getSenderUsername())))).collect(Collectors.toList());
            requestsList.getChildren().clear();for(ExchangeRequest r:found)requestsList.getChildren().add(requestCard(r));
            count.setText(found.size()+" yêu cầu");
            if(found.isEmpty()){Label empty=label("Chưa có yêu cầu phù hợp. Hãy thử thay đổi phạm vi hoặc trạng thái.","market-empty");empty.setId("requests-empty");requestsList.getChildren().add(empty);}scroll.setVvalue(0);
        };
        scope.valueProperty().addListener((o,previous,value)->update.run());status.valueProperty().addListener((o,previous,value)->update.run());update.run();
        content.getChildren().addAll(filters,count,scroll,label("Chủ bài duyệt và xác nhận hoàn tất. Hai bên có thể hủy trước khi hoàn tất.","footnote"));
    }
    private VBox requestCard(ExchangeRequest r){
        VBox card=new VBox(14);card.setId("request-card-"+r.getRequestId());card.getStyleClass().add("request-card");
        Label id=label("Yêu cầu #"+r.getRequestId(),"request-id"),date=label(r.getCreatedAt(),"muted");
        Region gap=new Region();HBox.setHgrow(gap,Priority.ALWAYS);
        HBox header=new HBox(12,id,gap,date,badge(r.getStatus()));header.setAlignment(Pos.CENTER_LEFT);header.getStyleClass().add("request-header");
        Post p=manager.getPosts().stream().filter(post->post.getPostId().equals(r.getPostId())).findFirst().orElse(null);
        StackPane thumb=new StackPane();thumb.setMinSize(88,88);thumb.setPrefSize(88,88);thumb.setMaxSize(88,88);thumb.getStyleClass().add("request-thumbnail");
        if(p!=null&&p.getImageData()!=null)thumb.getChildren().add(postImage(p.getImageData(),84,84));else thumb.getChildren().add(label("▧","photo-placeholder"));
        Label title=label(postTitle(r.getPostId()),"request-title");
        FlowPane people=new FlowPane(8,6,label("Người gửi: "+person(r.getSenderUsername()),"muted"),label("➜","request-arrow"),label("Người nhận: "+person(r.getReceiverUsername()),"muted"));
        VBox info=new VBox(10,title,label("Bài đăng #"+r.getPostId()+(p==null?"":" · "+p.getItem().getTransactionType()),"market-type"),people);info.setMinWidth(0);HBox.setHgrow(info,Priority.ALWAYS);
        HBox body=new HBox(16,thumb,info);body.setAlignment(Pos.CENTER_LEFT);
        FlowPane footer=new FlowPane(10,8);footer.setAlignment(Pos.CENTER_RIGHT);footer.getStyleClass().add("request-footer");
        String actor=currentActor();boolean receiver=Objects.equals(actor,r.getReceiverUsername()),sender=Objects.equals(actor,r.getSenderUsername());
        boolean pending="PENDING".equals(r.getStatus()),accepted="ACCEPTED".equals(r.getStatus());
        if((sender||receiver)&&(pending||accepted)){
            Button cancel=button("Hủy yêu cầu","danger",()->{if(confirm("Hủy yêu cầu?","Nếu đang giữ chỗ, bài đăng sẽ được mở lại."))change(()->manager.cancelRequest(actor(),r.getRequestId()),"Đã hủy yêu cầu");});cancel.setId("cancel-request-"+r.getRequestId());footer.getChildren().add(cancel);
        }
        if(receiver&&pending){
            Button reject=button("Từ chối","secondary",()->change(()->manager.rejectRequest(actor(),r.getRequestId()),"Đã từ chối yêu cầu"));reject.setId("reject-request-"+r.getRequestId());
            Button accept=button("Chấp nhận","primary",()->change(()->manager.acceptRequest(actor(),r.getRequestId()),"Đã chấp nhận và giữ chỗ"));accept.setId("accept-request-"+r.getRequestId());footer.getChildren().addAll(reject,accept);
        }
        if(receiver&&accepted){
            Button complete=button("Xác nhận hoàn tất","primary",()->{if(confirm("Xác nhận đã bàn giao?","Chỉ hoàn tất sau khi đã nhận hoặc giao đồ vật. Thao tác này không thể hủy."))change(()->manager.completeRequest(actor(),r.getRequestId()),"Giao dịch đã hoàn tất");});complete.setId("complete-request-"+r.getRequestId());footer.getChildren().add(complete);
        }
        card.getChildren().addAll(header,body);if(!footer.getChildren().isEmpty())card.getChildren().add(footer);return card;
    }
    private void statsPage() {
        heading("Thống kê cộng đồng","Số liệu cập nhật trực tiếp từ dữ liệu đang lưu.");content.getChildren().add(kpis());
        PieChart pie=new PieChart();pie.setTitle("Yêu cầu theo trạng thái");Map<String,Integer> s=manager.getStatistics();
        for(String status:Arrays.asList("PENDING","ACCEPTED","COMPLETED","REJECTED","CANCELLED")){int n=s.get("request_"+status);if(n>0)pie.getData().add(new PieChart.Data(translated(status)+" · "+n,n));}
        pie.setAnimated(false);pie.setLegendVisible(true);pie.setPrefHeight(310);
        CategoryAxis x=new CategoryAxis();NumberAxis y=new NumberAxis();y.setAutoRanging(false);y.setLowerBound(0);y.setUpperBound(Math.max(3,manager.getItems().size()));y.setTickUnit(Math.max(1,Math.ceil(manager.getItems().size()/6.0)));y.setMinorTickVisible(false);y.setLabel("Số lượng");
        BarChart<String,Number> bars=new BarChart<>(x,y);bars.setTitle("Đồ vật theo hình thức");bars.setLegendVisible(false);bars.setAnimated(false);bars.setPrefHeight(310);XYChart.Series<String,Number> data=new XYChart.Series<>();for(String type:Arrays.asList("Bán","Trao đổi","Cho tặng"))data.getData().add(new XYChart.Data<>(type,manager.getItems().stream().filter(i->type.equals(i.getTransactionType())).count()));bars.getData().add(data);
        VBox a=new VBox(pie),b=new VBox(bars);a.getStyleClass().add("panel");b.getStyleClass().add("panel");HBox.setHgrow(a,Priority.ALWAYS);HBox.setHgrow(b,Priority.ALWAYS);a.setMaxWidth(Double.MAX_VALUE);b.setMaxWidth(Double.MAX_VALUE);HBox charts=new HBox(16,a,b);content.getChildren().add(charts);
        panel(label("Đọc đúng kết quả", "section-title"),label("Đã chấp nhận = đang giữ chỗ. Chỉ yêu cầu được xác nhận bàn giao mới tính là đã hoàn tất.","muted"),label("Người dùng: "+s.get("users")+"    ·    Bài đăng: "+s.get("posts")+"    ·    Yêu cầu: "+s.get("requests"),"summary"));
    }
    private void userDialog(User old) {
        if(old==null||!actor().equals(old.getId()))throw new IllegalArgumentException("Chỉ được sửa hồ sơ của mình");
        Form f=new Form(old==null?"Thêm người dùng":"Sửa hồ sơ người dùng");
        TextField id=f.text("Mã người dùng",old==null?nextId("U",manager.getUsers().stream().map(User::getId).collect(Collectors.toList())):old.getId(),"user-id");id.setDisable(old!=null);
        TextField name=f.text("Họ và tên",old==null?"":old.getName(),"user-name"),email=f.text("Email",old==null?"":old.getEmail(),"user-email"),phone=f.text("Điện thoại",old==null?"":old.getPhone(),"user-phone");ComboBox<String> role=f.combo("Vai trò",old==null?"Sinh viên":old.getRole(),"user-role","Sinh viên","Giảng viên","Cán bộ");
        f.submit(()->{if(old==null)manager.addUser(new User(id.getText(),name.getText(),email.getText(),phone.getText(),role.getValue()));else manager.updateUser(old.getId(),name.getText(),email.getText(),phone.getText(),role.getValue());},"Đã lưu người dùng");
    }
    private void itemDialog(Item old) {
        String owner=actor();Form f=new Form(old==null?"Thêm đồ vật":"Sửa thông tin đồ vật");
        TextField id=f.text("Mã đồ vật",old==null?nextId("I",manager.getItems().stream().map(Item::getItemId).collect(Collectors.toList())):old.getItemId(),"item-id");id.setDisable(old!=null);
        TextField name=f.text("Tên đồ vật",old==null?"":old.getName(),"item-name"),cat=f.text("Loại",old==null?"":old.getCategory(),"item-category");
        ComboBox<String> condition=f.combo("Tình trạng",old==null?"Còn tốt":old.getCondition(),"item-condition","Như mới","Còn tốt","Đã qua sử dụng");
        ComboBox<String> type=f.combo("Hình thức",old==null?"Trao đổi":old.getTransactionType(),"item-type","Bán","Trao đổi","Cho tặng");
        TextArea desc=f.area("Mô tả",old==null?"":old.getDescription(),"item-description");
        f.note("Chủ sở hữu khi thêm: "+person(owner));
        f.submit(()->{Item i=new Item(id.getText(),name.getText(),cat.getText(),condition.getValue(),desc.getText(),type.getValue());if(old==null)manager.addItem(owner,i);else manager.editItem(owner,old.getItemId(),i);},"Đã lưu đồ vật");
    }
    private void postDialogFor(Item item) { postDialog(null,item); }
    private void postDialog(Post old) { postDialog(old,null); }
    private void postDialog(Post old,Item preferred) {
        String owner=actor();
        if(old!=null && !owner.equals(old.getAuthorUsername()))throw new IllegalArgumentException("Chỉ chủ bài được sửa");
        Form f=new Form(old==null?"Tạo bài đăng":"Sửa bài đăng");
        TextField id=f.text("Mã bài đăng",old==null?nextId("P",manager.getPosts().stream().map(Post::getPostId).collect(Collectors.toList())):old.getPostId(),"post-id");id.setDisable(old!=null);
        TextField title=f.text("Tiêu đề",old==null?(preferred==null?"":preferred.getName()):old.getTitle(),"post-title");
        ComboBox<Item> item=new ComboBox<>();item.setId("post-item");item.setMaxWidth(Double.MAX_VALUE);item.setConverter(new StringConverter<Item>(){public String toString(Item i){return i==null?"":i.getItemId()+" · "+i.getName();}public Item fromString(String s){return null;}});
        item.getItems().addAll(manager.getItems().stream().filter(i->owner.equals(i.getOwnerId())&&"AVAILABLE".equals(i.getStatus())&&manager.getPosts().stream().noneMatch(p->p.getItem().getItemId().equals(i.getItemId())&&!"CLOSED".equals(p.getStatus()))).collect(Collectors.toList()));
        if(old!=null){item.getItems().setAll(old.getItem());item.setValue(old.getItem());item.setDisable(true);}else if(preferred!=null)item.setValue(preferred);else if(!item.getItems().isEmpty())item.setValue(item.getItems().get(0));
        f.add("Đồ vật của bạn",item);f.note("Mỗi đồ vật chỉ có một bài đăng đang hoạt động.");
        byte[][] photo={old==null?null:old.getImageData()};
        ImageView preview=postImage(photo[0],260,150);preview.setId("post-image-preview");
        Label photoName=label(photo[0]==null?"Chưa chọn ảnh":"Ảnh đã lưu", "muted");
        Button choose=button("Chọn ảnh…","secondary",()->{
            FileChooser picker=new FileChooser();picker.setTitle("Chọn ảnh bài đăng (tối đa 5 MB)");
            picker.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ảnh PNG, JPG, GIF", "*.png","*.jpg","*.jpeg","*.gif"));
            java.io.File file=picker.showOpenDialog(f.dialog.getDialogPane().getScene().getWindow());
            if(file==null)return;
            try{
                if(Files.size(file.toPath())>5*1024*1024)throw new IllegalArgumentException("Ảnh phải nhỏ hơn hoặc bằng 5 MB");
                byte[] bytes=Files.readAllBytes(file.toPath());
                Image image=new Image(new ByteArrayInputStream(bytes),260,150,true,true);
                if(image.isError())throw new IllegalArgumentException("Không đọc được ảnh. Hãy chọn ảnh PNG, JPG hoặc GIF hợp lệ.");
                photo[0]=bytes;preview.setImage(image);photoName.setText(file.getName());f.error.setText("");
            }catch(Exception ex){f.error.setText(ex.getMessage());}
        });choose.setId("choose-post-image");
        Button remove=button("Bỏ ảnh","secondary",()->{photo[0]=null;preview.setImage(null);photoName.setText("Chưa chọn ảnh");});remove.setId("remove-post-image");
        f.add("Ảnh bài đăng",new VBox(8,toolbar(choose,remove),photoName,preview));
        f.submit(()->{if(old!=null)manager.editPost(owner,old.getPostId(),title.getText(),photo[0]);else{if(item.getValue()==null)throw new IllegalArgumentException("Hãy thêm đồ vật còn sẵn trước khi đăng bài");manager.createPost(owner,id.getText(),title.getText(),item.getValue().getItemId(),photo[0]);}},"Đã lưu bài đăng");
    }
    private void sendDialog(Post p) {
        String sender=actor();Form f=new Form("Gửi yêu cầu trao đổi");f.note(p.getTitle()+"\nChủ bài: "+person(p.getAuthorUsername())+"\nNgười gửi: "+person(sender));
        TextField id=f.text("Mã yêu cầu",nextId("R",manager.getRequests().stream().map(ExchangeRequest::getRequestId).collect(Collectors.toList())),"request-id");
        f.note("Hai bên tự thỏa thuận việc bán, tặng hoặc đồ vật đem đổi. Ứng dụng theo dõi yêu cầu và xác nhận bàn giao.");f.submit(()->manager.sendRequest(sender,id.getText(),p.getPostId()),"Đã gửi yêu cầu");
    }
    private void itemDetails(Item i) {
        alert(Alert.AlertType.INFORMATION,"Thông tin đồ vật",i.getName()+"\n\nMã: "+i.getItemId()+"\nChủ sở hữu: "+person(i.getOwnerId())+"\nLoại: "+i.getCategory()+"\nTình trạng: "+i.getCondition()+"\nHình thức: "+i.getTransactionType()+"\nTrạng thái: "+translated(i.getStatus())+"\n\n"+i.getDescription());
    }
    private void postDetails(Post p) {
        Dialog<Void>d=new Dialog<>();d.initOwner(stage);d.setTitle("Chi tiết bài đăng");d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);d.getDialogPane().getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        Item i=p.getItem();VBox box=new VBox(14,label(p.getTitle(),"section-title"),badge(p.getStatus()));
        if(p.getImageData()!=null){ImageView photo=postImage(p.getImageData(),460,280);photo.setId("post-detail-image");box.getChildren().add(photo);}else box.getChildren().add(label("Bài đăng chưa có ảnh","muted"));
        box.getChildren().addAll(label(i.getDescription(),"summary"),label("Mã bài: "+p.getPostId()+"\nChủ bài: "+person(p.getAuthorUsername())+"\nĐồ vật: "+i.getName()+"\nLoại: "+i.getCategory()+"\nTình trạng: "+i.getCondition()+"\nHình thức: "+i.getTransactionType()+"\nNgày đăng: "+p.getCreatedAt(),"muted"));
        Button send=button("Gửi yêu cầu cho người đăng","primary",()->{d.close();guard(()->sendDialog(p));});send.setId("detail-send-request");
        send.setDisable(currentActor()==null||Objects.equals(currentActor(),p.getAuthorUsername())||!"OPEN".equals(p.getStatus()));box.getChildren().add(send);
        box.setPrefWidth(480);box.setPadding(new Insets(10));ScrollPane scroll=new ScrollPane(box);scroll.setFitToWidth(true);scroll.setPrefViewportHeight(540);d.getDialogPane().setContent(scroll);d.showAndWait();
    }
    private static ImageView postImage(byte[] bytes,double width,double height){
        ImageView view=new ImageView();view.setFitWidth(width);view.setFitHeight(height);view.setPreserveRatio(true);
        if(bytes!=null)view.setImage(new Image(new ByteArrayInputStream(bytes),width,height,true,true));return view;
    }
    class Form {
        final Dialog<Void> dialog=new Dialog<>();final GridPane grid=new GridPane();final Label error=label("","form-error");int row;
        Form(String title){dialog.initOwner(stage);dialog.setTitle(title);dialog.getDialogPane().getStylesheets().add(getClass().getResource("/app.css").toExternalForm());dialog.getDialogPane().getStyleClass().add("form-dialog");grid.setHgap(12);grid.setVgap(12);grid.setPadding(new Insets(16,4,12,4));ColumnConstraints left=new ColumnConstraints(125),right=new ColumnConstraints(345);right.setHgrow(Priority.ALWAYS);grid.getColumnConstraints().addAll(left,right);dialog.getDialogPane().setContent(grid);}
        void add(String title,Node field){Label name=label(title,"field-label");grid.add(name,0,row);grid.add(field,1,row++);if(field instanceof Region)((Region)field).setMaxWidth(Double.MAX_VALUE);}
        TextField text(String title,String value,String id){TextField t=new TextField(value);t.setId(id);add(title,t);return t;}
        TextArea area(String title,String value,String id){TextArea a=new TextArea(value);a.setId(id);a.setWrapText(true);a.setPrefRowCount(3);add(title,a);return a;}
        ComboBox<String> combo(String title,String value,String id,String... values){ComboBox<String> c=new ComboBox<>(FXCollections.observableArrayList(values));if(!c.getItems().contains(value))c.getItems().add(value);c.setValue(value);c.setId(id);add(title,c);return c;}
        void note(String text){Label l=label(text,"form-note");grid.add(l,0,row++,2,1);}
        void submit(Runnable action,String success){
            error.setMaxWidth(470);grid.add(error,0,row++,2,1);ButtonType save=new ButtonType("Lưu",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,save);
            Button button=(Button)dialog.getDialogPane().lookupButton(save);button.setId("form-save");button.getStyleClass().add("primary");
            button.addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{action.run();dirty=true;refreshSignedInUser();showPage(page);message.setText(success);save(false);}catch(Exception ex){error.setText(ex.getMessage());e.consume();}});dialog.showAndWait();
        }
    }
    private String currentActor(){return signedInUser==null?null:signedInUser.getId();}
    private String actor(){String id=currentActor();if(id==null)throw new IllegalArgumentException("Hãy đăng nhập trước khi thao tác");return id;}
    private void refreshSignedInUser(){if(signedInUser!=null){signedInUser=manager.findUserById(signedInUser.getId());if(signedInUser!=null)root.setTop(topbar());}}
    private void guard(Runnable action){try{action.run();}catch(Exception e){alert(Alert.AlertType.ERROR,"Không thể thực hiện",e.getMessage());}}
    private void change(Runnable action,String success){guard(()->{action.run();dirty=true;refreshSignedInUser();showPage(page);message.setText(success);save(false);});}
    private boolean save(boolean notify){try{manager.save(dataPath);dirty=false;saveState.setText("Đã lưu lúc "+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));if(notify)message.setText("Đã lưu dữ liệu thành công");return true;}catch(IOException ex){dirty=true;saveState.setText("Chưa lưu được");alert(Alert.AlertType.ERROR,"Không lưu được dữ liệu","Thay đổi vẫn còn trong phiên này. Hãy kiểm tra quyền ghi và bấm Lưu dữ liệu để thử lại.\n"+ex.getMessage());return false;}}
    private boolean confirm(String title,String text){Alert a=new Alert(Alert.AlertType.CONFIRMATION,text,ButtonType.OK,ButtonType.CANCEL);a.initOwner(stage);a.setTitle(title);a.setHeaderText(title);return a.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
    private void alert(Alert.AlertType type,String title,String text){Alert a=new Alert(type);if(stage!=null)a.initOwner(stage);a.setTitle(title);a.setHeaderText(title);a.setContentText(text==null?"Có lỗi xảy ra":text);a.showAndWait();}
    private <T> void selected(TableView<T> table,Consumer<T> action){T value=table.getSelectionModel().getSelectedItem();if(value==null)alert(Alert.AlertType.INFORMATION,"Chọn một dòng","Hãy chọn bản ghi trong bảng trước khi thao tác.");else action.accept(value);}
    private static String nextId(String prefix,List<String> used){for(int n=1;;n++){String id=prefix+String.format(Locale.ROOT,"%03d",n);if(!used.contains(id))return id;}}
    private String person(String id){User u=id==null?null:manager.findUserById(id);return u==null?id:u.getName()+" ("+id+")";}
    private String postTitle(String id){return manager.getPosts().stream().filter(p->p.getPostId().equals(id)).map(Post::getTitle).findFirst().orElse(id);}
    private static String translated(String s){switch(s){case "OPEN":return "Đang mở";case "AVAILABLE":return "Còn sẵn";case "RESERVED":return "Giữ chỗ";case "CLOSED":return "Đã đóng";case "EXCHANGED":return "Đã trao đổi";case "PENDING":return "Chờ duyệt";case "ACCEPTED":return "Đã chấp nhận";case "COMPLETED":return "Hoàn tất";case "REJECTED":return "Từ chối";case "CANCELLED":return "Đã hủy";default:return s;}}
    private static boolean matches(String key,String... values){String k=key==null?"":key.trim().toLowerCase(Locale.ROOT);for(String v:values)if(v!=null&&v.toLowerCase(Locale.ROOT).contains(k))return true;return k.isEmpty();}
    private static boolean filtered(ComboBox<String> filter,String value){return ALL.equals(filter.getValue())||Objects.equals(filter.getValue(),value);}
    private static String criteria(ComboBox<String> filter){return ALL.equals(filter.getValue())?null:filter.getValue();}
    private static ComboBox<String> filter(String prompt,String... options){ComboBox<String> c=new ComboBox<>();c.getItems().add(ALL);c.getItems().addAll(options);c.setValue(ALL);c.setPromptText(prompt);c.setConverter(new StringConverter<String>(){public String toString(String s){return s==null?"":translated(s);}public String fromString(String s){return s;}});c.setPrefWidth(145);return c;}
    private static TextField search(String prompt){TextField t=new TextField();t.setPromptText(prompt);t.setPrefWidth(290);HBox.setHgrow(t,Priority.ALWAYS);return t;}
    private static Label label(String text,String css){Label l=new Label(text);l.getStyleClass().add(css);l.setWrapText(true);return l;}
    private static Label badge(String status){Label l=label(translated(status),"badge");l.getStyleClass().add("status-"+status.toLowerCase(Locale.ROOT));return l;}
    private static Button button(String text,String css,Runnable action){Button b=new Button(text);b.getStyleClass().add(css);b.setOnAction(e->action.run());return b;}
    private static HBox toolbar(Node... nodes){HBox row=new HBox(10,nodes);row.setAlignment(Pos.CENTER_LEFT);for(Node n:nodes)if(n instanceof Label)((Label)n).setMinWidth(Region.USE_PREF_SIZE);return row;}
    private void panel(Node... nodes){VBox box=new VBox(14,nodes);box.getStyleClass().add("panel");content.getChildren().add(box);}
    private static <T> TableView<T> table(){TableView<T> t=new TableView<>();t.setPlaceholder(label("Chưa có dữ liệu phù hợp. Hãy thêm mới hoặc thay đổi bộ lọc.","muted"));t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);t.setPrefHeight(365);t.setMinHeight(250);return t;}
    private static <T> void column(TableView<T> t,String name,Function<T,String> value,double width){TableColumn<T,String> c=new TableColumn<>(name);c.setCellValueFactory(v->new ReadOnlyStringWrapper(value.apply(v.getValue())));c.setPrefWidth(width);c.setMinWidth(65);c.setCellFactory(col->new TableCell<T,String>() { @Override protected void updateItem(String value,boolean empty){super.updateItem(value,empty);setText(empty?null:value);setTooltip(empty||value==null?null:new Tooltip(value));} });t.getColumns().add(c);}
    private static <T> void statusColumn(TableView<T> t,Function<T,String> value){TableColumn<T,String> c=new TableColumn<>("Trạng thái");c.setCellValueFactory(v->new ReadOnlyStringWrapper(value.apply(v.getValue())));c.setPrefWidth(130);c.setMinWidth(120);c.setCellFactory(col->new TableCell<T,String>(){@Override protected void updateItem(String s,boolean empty){super.updateItem(s,empty);setText(null);setGraphic(empty||s==null?null:badge(s));}});t.getColumns().add(c);}
}

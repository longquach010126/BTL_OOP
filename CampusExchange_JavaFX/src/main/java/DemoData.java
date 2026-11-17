public final class DemoData {
    private DemoData() { }
    public static Manager create() {
        Manager m=new Manager();
        m.addUser(new User("U01","Nguyễn Minh Anh","minhanh@example.com","0901000001","Sinh viên"));
        m.addUser(new User("U02","Trần Quốc Bảo","quocbao@example.com","0901000002","Sinh viên"));
        m.addUser(new User("U03","Lê Thu Hà","thuha@example.com","0901000003","Giảng viên"));
        add(m,"U01","I01","Giáo trình lập trình Java","Sách","Còn tốt","Giáo trình có ghi chú, phù hợp môn OOP.","Trao đổi","P01");
        add(m,"U02","I02","Đèn học để bàn","Đồ học tập","Như mới","Đèn sáng 3 chế độ, dùng tốt.","Bán","P02");
        add(m,"U03","I03","Bộ dụng cụ vẽ kỹ thuật","Đồ học tập","Còn tốt","Đầy đủ thước và compa cho sinh viên.","Cho tặng","P03");
        add(m,"U01","I04","Balo laptop 15 inch","Phụ kiện","Còn tốt","Balo sạch, khóa kéo hoạt động tốt.","Cho tặng","P04");
        add(m,"U02","I05","Giá sách mini","Đồ dùng","Như mới","Giá sách gỗ nhỏ cho bàn học.","Trao đổi","P05");
        add(m,"U03","I06","Máy tính cầm tay","Thiết bị","Còn tốt","Máy hoạt động bình thường, có nắp.","Bán","P06");
        m.sendRequest("U02","R01","P01");m.sendRequest("U03","R02","P01");
        m.sendRequest("U01","R03","P02");m.acceptRequest("U02","R03");
        m.sendRequest("U02","R04","P03");m.acceptRequest("U03","R04");m.completeRequest("U03","R04");
        return m;
    }
    private static void add(Manager m,String owner,String id,String name,String cat,String condition,String desc,String type,String pid) {
        m.addItem(owner,new Item(id,name,cat,condition,desc,type));m.createPost(owner,pid,name,id);
    }
}

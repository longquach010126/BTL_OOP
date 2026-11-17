import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import java.util.ArrayList;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    private String ownerId;
    private String status = "AVAILABLE";
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = Checks.id(ownerId); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = Checks.choice(status, "AVAILABLE", "RESERVED", "EXCHANGED"); }
    public Item(String itemId, String name, String category, String condition, String description, String transactionType, String ownerId) {
        this(itemId, name, category, condition, description, transactionType);
        setOwnerId(ownerId);
    }
    // Cac thuoc tinh
    private String itemId;          // Ma 
    private String name;            // Ten 
    private String category;        // Loai 
    private String condition;       // Tinh trang
    private String description;     // Mo ta
    private String transactionType; // Ban, Trao doi, Cho tang

    // Constructor: Khoi tao mot do vat moi
    public Item(String itemId, String name, String category, String condition,
                String description, String transactionType) {
        this.itemId = Checks.id(itemId);
        this.name = name;
        this.category = category;
        this.condition = condition;
        this.description = description;
        this.transactionType = Checks.transactionType(transactionType);
    }
    // Getter: Lay ma
    public String getItemId() {
        return itemId;
    }
    // Getter va Setter
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;// Lay va sua loai do vat
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public String getCondition() {
        return condition;//Lay va sua tinh trang
    }
    public void setCondition(String condition) {
        this.condition = condition;
    }
   
    public String getDescription() {
        return description; //Lay va sua mo ta
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getTransactionType() {
        return transactionType;//Lay va sua hinh thuc giao dich
    }
    public void setTransactionType(String transactionType) {
        this.transactionType = Checks.transactionType(transactionType);
    }

    // Kiem tra hinh thuc giao dich co hop le khong
    public boolean isValidTransactionType() {
        if (transactionType == null) {
            return false;
        }
        String type = transactionType.trim();
        return type.equalsIgnoreCase("Bán")
                || type.equalsIgnoreCase("Trao đổi")
                || type.equalsIgnoreCase("Cho tặng")
                || type.equalsIgnoreCase("Ban")
                || type.equalsIgnoreCase("Trao doi")
                || type.equalsIgnoreCase("Cho tang");
    }

    // Kiem tra cac thong tin bat buoc cua do vat
    public boolean isValid() {
        return itemId != null && !itemId.trim().isEmpty()
                && name != null && !name.trim().isEmpty()
                && category != null && !category.trim().isEmpty()
                && condition != null && !condition.trim().isEmpty()
                && description != null && !description.trim().isEmpty()
                && isValidTransactionType();
    }

    // Hien thi thong tin chi tiet cua mot do vat
    public void displayInfo() {
        System.out.println("------------------------------");
        System.out.println("Ma do vat: " + itemId);
        System.out.println("Ten: " + name);
        System.out.println("Loai: " + category);
        System.out.println("Tinh trang: " + condition);
        System.out.println("Mo ta: " + description);
        System.out.println("Hinh thuc: " + transactionType);
        System.out.println("------------------------------");
    }
}

class ItemManager implements Serializable {
    private static final long serialVersionUID = 1L;
    public List<Item> getItems() { return Collections.unmodifiableList(new ArrayList<>(items)); }
    public int countItems() { return items.size(); } // Lop quan ly do vat
    private ArrayList<Item> items = new ArrayList<>();

    // Tim do vat theo ma
    public Item findItem(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return null;
        }
        for (Item item : items) {
            if (item.getItemId().equalsIgnoreCase(itemId.trim())) {
                return item;
            }
        }
        return null;
    }

    // Them do vat
    public void addItem(Item item) {
        if (item == null || !item.isValid()) {
            System.out.println("Thong tin do vat khong hop le!");
            return;
        }
        if (findItem(item.getItemId()) != null) {
            System.out.println("Ma do vat da ton tai!");
            return;
        }
        items.add(item);
        System.out.println("Them do vat thanh cong!");
    }

    // Sua do vat theo ma
    public void editItem(String itemId, Item newItem) {
        Item oldItem = findItem(itemId);
        if (oldItem == null) {
            System.out.println("Khong tim thay do vat!");
            return;
        }
        if (newItem == null || !newItem.isValid()) {
            System.out.println("Thong tin moi khong hop le!");
            return;
        }
        if (!oldItem.getItemId().equalsIgnoreCase(
                newItem.getItemId().trim())) {
            System.out.println("Khong duoc thay doi ma do vat!");
            return;
        }
        oldItem.setName(newItem.getName());
        oldItem.setCategory(newItem.getCategory());
        oldItem.setCondition(newItem.getCondition());
        oldItem.setDescription(newItem.getDescription());
        oldItem.setTransactionType(newItem.getTransactionType());
        System.out.println("Sua do vat thanh cong!");
    }

    // Xoa do vat theo ma
    public void deleteItem(String itemId, boolean confirm) {
        Item item = findItem(itemId);
        if (item == null) {
            System.out.println("Khong tim thay do vat!");
            return;
        }
        if (!confirm) {
            System.out.println("Da huy thao tac xoa!");
            return;
        }
        items.remove(item);
        System.out.println("Xoa do vat thanh cong!");
    }

    // Hien thi tat ca do vat
    public void displayAll() {
        if (items.isEmpty()) {
            System.out.println("Danh sach do vat rong!");
            return;
        }
        for (Item item : items) {
            item.displayInfo();
        }
    }

    // Tim kiem theo ten
    public void searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("Vui long nhap ten can tim!");
            return;
        }
        boolean found = false;
        for (Item item : items) {
            if (item.getName().toLowerCase()
                    .contains(name.trim().toLowerCase())) {
                item.displayInfo();
                found = true;
            }
        }
        if (!found) {
            System.out.println("Khong tim thay do vat!");
        }
    }

    // Tim kiem theo loai
    public void searchByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            System.out.println("Vui long nhap loai do vat!");
            return;
        }
        boolean found = false;
        for (Item item : items) {
            if (item.getCategory().equalsIgnoreCase(category.trim())) {
                item.displayInfo();
                found = true;
            }
        }
        if (!found) {
            System.out.println("Khong tim thay do vat!");
        }
    }
}

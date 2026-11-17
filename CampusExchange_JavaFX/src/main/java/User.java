import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private byte[] passwordSalt;
    private byte[] passwordHash;
    public boolean hasPassword() { return passwordSalt != null && passwordHash != null; }
    void setPassword(String password) {
        if(password==null || password.length()<8 || password.length()>128)throw new IllegalArgumentException("Mật khẩu cần từ 8 đến 128 ký tự");
        byte[] salt=new byte[16];new java.security.SecureRandom().nextBytes(salt);
        byte[] hash=derivePassword(password,salt);passwordSalt=salt;passwordHash=hash;
    }
    boolean matchesPassword(String password) {
        return hasPassword() && password!=null && password.length()<=128 && java.security.MessageDigest.isEqual(passwordHash,derivePassword(password,passwordSalt));
    }
    private static byte[] derivePassword(String password,byte[] salt) {
        javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(password.toCharArray(),salt,210000,256);
        try { return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch(java.security.GeneralSecurityException e){throw new IllegalStateException("Không thể xử lý mật khẩu",e);}
        finally { spec.clearPassword(); }
    }

    // Ham khoi tao
    public User(String id, String name, String email, String phone, String role){
        this.id=Checks.id(id);
        this.name=name;
        this.email=email;
        this.phone=phone;
        this.role=role;
    }
    public boolean isValid() { return Checks.filled(id) && Checks.filled(name) && Checks.filled(email) && Checks.filled(phone) && Checks.filled(role); }

    // Lay ma nguoi dung
    public String getId(){
        return id;
    }
    //Lay ho ten
    public String getName(){
        return name;
    }
    // Lay email
    public String getEmail(){
        return email;
    }
    //Lay so dien thoai
    public String getPhone(){
        return phone;
    }
    //Lay vai tro
    public String getRole(){
        return role;
    }
    //Sua ho ten
    public void setName(String name){
        this.name=name;
    }
    //Sua email
    public void setEmail(String email){
        this.email=email;
    }
    //Sua so dien thoai
    public void setPhone(String phone){
        this.phone=phone;
    }
    //Sua vai tro
    public void setRole(String role){
        this.role=role;
    }
    //Hien thi thong tin
    @Override
    public String toString(){
        return "Ma: "+id
                + " | Ho ten: " + name
                + " | Email: " + email
                + " | SDT: " + phone
                + " | Vai tro: " + role;
    }
}

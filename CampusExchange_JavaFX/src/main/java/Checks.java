import java.text.Normalizer;
import java.util.Locale;

/** Quy ước chung. Mã người dùng chính là giá trị các trường có hậu tố Username. */
public final class Checks {
    private Checks() { }
    public static boolean filled(String s) { return s != null && !s.trim().isEmpty(); }
    public static String id(String s) {
        if (!filled(s)) throw new IllegalArgumentException("Mã không được rỗng");
        return s.trim().toUpperCase(Locale.ROOT);
    }
    public static String choice(String s, String... allowed) {
        String value = id(s);
        for (String a : allowed) if (a.equals(value)) return value;
        throw new IllegalArgumentException("Trạng thái không hợp lệ: " + s);
    }
    public static String transactionType(String s) {
        if (s == null) return null;
        String value = Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").replace('đ', 'd');
        switch (value) {
            case "ban": return "Bán";
            case "trao doi": return "Trao đổi";
            case "cho tang": return "Cho tặng";
            default: return s.trim();
        }
    }
}

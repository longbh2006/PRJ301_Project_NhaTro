package util;

public final class PasswordHasherTest {

    public static void main(String[] args) {
        String password = "  Mật khẩu thử 123  ";
        String first = PasswordHasher.hash(password);
        String second = PasswordHasher.hash(password);
        check(first.matches("600000:[A-Za-z0-9+/=]+:[A-Za-z0-9+/=]+"), "Stored format");
        check(!first.equals(second), "Random salt per hash");
        check(!first.contains(password), "No plaintext");
        check(PasswordHasher.verify(password, first), "Correct Unicode password");
        check(!PasswordHasher.verify(password.strip(), first), "Password whitespace is significant");
        check(!PasswordHasher.verify("wrong password", first), "Wrong password rejected");
        check(PasswordHasher.verify(password, "pbkdf2_sha256$" + first.replace(':', '$')),
                "Project scaffold format is readable");
        // Vector PBKDF2-HMAC-SHA256 được tính độc lập bằng .NET Rfc2898DeriveBytes.
        check(PasswordHasher.verify("password", "1:MDEyMzQ1Njc4OWFiY2RlZg==:"
                + "751fat1KXRn0p/yStI8jUeqVu5d2QsAHHtTkAQpCy2w="), "Independent known vector");
        String[] invalid = {"", "plaintext", "1::", "-1:a:b", "2147483647:a:b", "1:a:b:c",
                "600000:!:!", first.substring(0, first.length() - 2), "pbkdf2_sha1$" + first};
        for (String value : invalid) {
            check(!PasswordHasher.verify(password, value), "Malformed hash rejected");
        }
        check(!PasswordHasher.verify(password, null), "Null hash rejected");
        check(!PasswordHasher.verify(null, first), "Null password rejected");
        System.out.println("PASS PasswordHasher: salt, Unicode, formats, vector and malformed input");
    }

    private static void check(boolean passed, String description) {
        if (!passed) {
            throw new AssertionError(description);
        }
    }
}

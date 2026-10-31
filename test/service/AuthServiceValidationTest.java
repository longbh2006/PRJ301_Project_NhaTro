package service;

import java.time.LocalDate;
import model.RegistrationForm;

public final class AuthServiceValidationTest {

    public static void main(String[] args) throws Exception {
        RegistrationForm empty = new RegistrationForm(null, null, null, null, null, null, null, null);
        expectInvalid(empty, null, "username", "password", "fullName", "idCardNo");
        expectInvalid(form("abc", "Họ tên", "", "", "012345678901", "", "", ""),
                "12345678", "username");
        expectInvalid(form("a".repeat(31), "Họ tên", "", "", "012345678901", "", "", ""),
                "12345678", "username");
        expectInvalid(form("valid_user", "Họ tên", "", "", "012345678901", "", "", ""),
                "1234567", "password");
        expectInvalid(form("valid_user", "Họ tên", "123", "invalid", "１２３４５６７８９０１２", "", "", ""),
                "12345678", "phone", "email", "idCardNo");
        expectInvalid(form("valid_user", "a".repeat(101), "", "", "012345678901",
                "a".repeat(101), "2026-02-30", "a".repeat(101)), "12345678",
                "fullName", "school", "dateOfBirth", "hometown");
        expectInvalid(form("valid_user", "Họ tên", "", "", "012345678901", "",
                LocalDate.now().plusDays(1).toString(), ""), "12345678", "dateOfBirth");
        System.out.println("PASS AuthService validation: required fields, boundaries, ASCII digits and dates");
    }

    private static RegistrationForm form(String username, String fullName, String phone,
            String email, String idCardNo, String school, String dateOfBirth, String hometown) {
        return new RegistrationForm(username, fullName, phone, email, idCardNo, school, dateOfBirth, hometown);
    }

    private static void expectInvalid(RegistrationForm form, String password, String... fields) throws Exception {
        try {
            new AuthService().register(form, password);
            throw new AssertionError("Invalid data accepted");
        } catch (RegistrationException ex) {
            for (String field : fields) {
                if (!ex.getErrors().containsKey(field)) {
                    throw new AssertionError("Missing validation error: " + field);
                }
            }
        }
    }
}

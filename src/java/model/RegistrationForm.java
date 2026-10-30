package model;

/** Chỉ giữ dữ liệu được phép hiển thị lại; không chứa mật khẩu hay vai trò. */
public final class RegistrationForm {

    private final String username;
    private final String fullName;
    private final String phone;
    private final String email;
    private final String idCardNo;
    private final String school;
    private final String dateOfBirth;
    private final String hometown;

    public RegistrationForm(String username, String fullName, String phone, String email,
            String idCardNo, String school, String dateOfBirth, String hometown) {
        this.username = normalize(username);
        this.fullName = normalize(fullName);
        this.phone = normalize(phone);
        this.email = normalize(email);
        this.idCardNo = normalize(idCardNo);
        this.school = normalize(school);
        this.dateOfBirth = normalize(dateOfBirth);
        this.hometown = normalize(hometown);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getIdCardNo() { return idCardNo; }
    public String getSchool() { return school; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getHometown() { return hometown; }
}

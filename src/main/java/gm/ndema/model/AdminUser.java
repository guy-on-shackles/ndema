package gm.ndema.model;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_users")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String fullName;
    private String password;
    private String accessCode;
    private boolean isSuperAdmin;

    // The institution this officer works for. Super admins have none and see every institution.
    @ManyToOne
    @JoinColumn(name = "institution_id")
    private Institution institution;

    public AdminUser() {}

    public AdminUser(String username, String fullName, String password, String accessCode, boolean isSuperAdmin) {
        this.username = username;
        this.fullName = fullName;
        this.password = password;
        this.accessCode = accessCode;
        this.isSuperAdmin = isSuperAdmin;
    }

    public AdminUser(String username, String fullName, String password, String accessCode, boolean isSuperAdmin, Institution institution) {
        this(username, fullName, password, accessCode, isSuperAdmin);
        this.institution = institution;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean superAdmin) { isSuperAdmin = superAdmin; }
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }
}

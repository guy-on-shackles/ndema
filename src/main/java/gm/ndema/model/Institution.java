package gm.ndema.model;

import jakarta.persistence.*;

@Entity
@Table(name = "institution")
public class Institution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String shortName;
    private String contactEmail;
    private String contactPhone;

    public Institution() {}
    public Institution(String name, String shortName, String contactEmail, String contactPhone) {
        this.name = name;
        this.shortName = shortName;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
}
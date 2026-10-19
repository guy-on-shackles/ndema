package gm.ndema.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_versions")
public class ComplaintVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long complaintId;
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Enums.Category category; // <-- ADD THIS FIELD

    @Enumerated(EnumType.STRING)
    private Enums.Status status;

    private String modifiedByAdmin;
    private LocalDateTime modifiedAt;

    public ComplaintVersion() {}

    // Getters and Setters
    public Long getId() { return id; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ADD THESE CATEGORY GETTER/SETTER METHODS
    public Enums.Category getCategory() { return category; }
    public void setCategory(Enums.Category category) { this.category = category; }

    public Enums.Status getStatus() { return status; }
    public void setStatus(Enums.Status status) { this.status = status; }
    public String getModifiedByAdmin() { return modifiedByAdmin; }
    public void setModifiedByAdmin(String modifiedByAdmin) { this.modifiedByAdmin = modifiedByAdmin; }
    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
}
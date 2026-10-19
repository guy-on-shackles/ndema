package gm.ndema.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint")
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String trackingCode;

    private String title;
    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Enums.Category category;


    @Column(name = "lga")
    private String lga;

    private Double latitude;
    private Double longitude;
    private String landmark;

    @Enumerated(EnumType.STRING)
    private Enums.Channel channel = Enums.Channel.WEB;

    private String reporterPhone;

    @Enumerated(EnumType.STRING)
    private Enums.Status status = Enums.Status.SUBMITTED;

    private Integer urgencyScore = 0;

    @Enumerated(EnumType.STRING)
    private Enums.Urgency urgencyLevel = Enums.Urgency.LOW;

    @ManyToOne
    @JoinColumn(name = "institution_id")
    private Institution institution;

    private Integer confirmations = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;

    @Column(length = 1000)
    private String adminNotes;

    public Complaint() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Enums.Category getCategory() { return category; }
    public void setCategory(Enums.Category category) { this.category = category; }
    public String getLga() { return lga; }
    public void setLga(String lga) { this.lga = lga; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }
    public Enums.Channel getChannel() { return channel; }
    public void setChannel(Enums.Channel channel) { this.channel = channel; }
    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }
    public Enums.Status getStatus() { return status; }
    public void setStatus(Enums.Status status) { this.status = status; }
    public Integer getUrgencyScore() { return urgencyScore; }
    public void setUrgencyScore(Integer urgencyScore) { this.urgencyScore = urgencyScore; }
    public Enums.Urgency getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(Enums.Urgency urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }
    public Integer getConfirmations() { return confirmations; }
    public void setConfirmations(Integer confirmations) { this.confirmations = confirmations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}
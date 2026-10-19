package gm.ndema.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String adminUsername;
    private String actionType;

    @Column(length = 1000)
    private String details;

    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(String adminUsername, String actionType, String details, LocalDateTime timestamp) {
        this.adminUsername = adminUsername;
        this.actionType = actionType;
        this.details = details;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getAdminUsername() { return adminUsername; }
    public String getActionType() { return actionType; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
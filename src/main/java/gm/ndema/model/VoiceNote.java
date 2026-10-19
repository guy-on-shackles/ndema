package gm.ndema.model;

import jakarta.persistence.*;

@Entity
@Table(name = "voice_notes")
public class VoiceNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long complaintId;

    private String contentType;

    @Lob
    @Column(length = 10_000_000)
    private byte[] data;

    public VoiceNote() {}

    public VoiceNote(Long complaintId, String contentType, byte[] data) {
        this.complaintId = complaintId;
        this.contentType = contentType;
        this.data = data;
    }

    public Long getId() { return id; }
    public Long getComplaintId() { return complaintId; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
}

package gm.ndema.repository;

import gm.ndema.model.VoiceNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VoiceNoteRepository extends JpaRepository<VoiceNote, Long> {
    Optional<VoiceNote> findByComplaintId(Long complaintId);
}

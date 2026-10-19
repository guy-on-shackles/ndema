package gm.ndema.repository;

import gm.ndema.model.Complaint;
import gm.ndema.model.Enums;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Optional<Complaint> findByTrackingCode(String trackingCode);
    List<Complaint> findByCategoryAndLga(Enums.Category category, String lga);
}
// ComplaintVersionRepository.java
package gm.ndema.repository;

import gm.ndema.model.ComplaintVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintVersionRepository extends JpaRepository<ComplaintVersion, Long> {
    List<ComplaintVersion> findByComplaintIdOrderByModifiedAtDesc(Long complaintId);
}
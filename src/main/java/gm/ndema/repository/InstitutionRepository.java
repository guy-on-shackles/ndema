package gm.ndema.repository;

import gm.ndema.model.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    Optional<Institution> findByShortName(String shortName);
    Optional<Institution> findByShortNameIgnoreCase(String shortName);
    List<Institution> findAllByOrderByShortNameAsc();
}

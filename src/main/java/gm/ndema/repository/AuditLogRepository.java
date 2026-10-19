// AuditLogRepository.java
package gm.ndema.repository;

import gm.ndema.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
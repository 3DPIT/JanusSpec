package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.domain.ApiDiffLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiDiffLogRepository extends JpaRepository<ApiDiffLog, Long> {
}


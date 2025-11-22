package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.domain.ApiDiffLog;
import blackspring.janusspec.domain.ApiDiffSchema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiDiffSchemaRepository extends JpaRepository<ApiDiffSchema, Long> {
    List<ApiDiffSchema> findByDiffLog(ApiDiffLog diffLog);
    List<ApiDiffSchema> findByDiffLogAndChangeType(ApiDiffLog diffLog, String changeType);
}


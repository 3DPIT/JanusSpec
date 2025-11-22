package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.domain.ApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiEndPointRepository extends JpaRepository<ApiEndpoint, Long> {
}

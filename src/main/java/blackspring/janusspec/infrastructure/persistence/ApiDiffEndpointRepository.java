package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.domain.ApiDiffEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiDiffEndpointRepository extends JpaRepository<ApiDiffEndpoint, Long> {
}


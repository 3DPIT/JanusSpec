package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.domain.ApiEndpoint;
import blackspring.janusspec.domain.SwaggerVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiEndPointRepository extends JpaRepository<ApiEndpoint, Long> {
    List<ApiEndpoint> findBySwaggerVersion(SwaggerVersion swaggerVersion);
}

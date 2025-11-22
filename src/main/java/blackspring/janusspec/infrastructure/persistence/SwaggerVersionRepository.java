package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.domain.SwaggerVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SwaggerVersionRepository extends JpaRepository<SwaggerVersion,Long> {
    Optional<SwaggerVersion> findTopBySwaggerUrlOrderByIdDesc(String url);
    Optional<SwaggerVersion> findTopByServiceNameAndSwaggerUrlOrderByIdDesc(String serviceName, String url);
    Optional<SwaggerVersion> findTopByOrderByIdDesc();
    Optional<SwaggerVersion> findTopByServiceNameOrderByIdDesc(String serviceName);
}

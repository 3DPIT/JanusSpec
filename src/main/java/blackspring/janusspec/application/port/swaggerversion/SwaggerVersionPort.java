package blackspring.janusspec.application.port.swaggerversion;

import blackspring.janusspec.domain.SwaggerVersion;

import java.util.Optional;

public interface SwaggerVersionPort {
    SwaggerVersionRes save(SwaggerVersionReq req);
    Optional<SwaggerVersion> findLatest();
    Optional<SwaggerVersion> findLatestByServiceName(String serviceName);
    Optional<SwaggerVersion> findById(Long id);
}

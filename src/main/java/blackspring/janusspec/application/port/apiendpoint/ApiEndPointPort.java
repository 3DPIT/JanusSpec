package blackspring.janusspec.application.port.apiendpoint;

import blackspring.janusspec.domain.ApiEndpoint;
import blackspring.janusspec.domain.SwaggerVersion;

import java.util.List;

public interface ApiEndPointPort {
    ApiEndPointRes save(ApiEndPointReq req);
    List<ApiEndpoint> findBySwaggerVersion(SwaggerVersion swaggerVersion);
}

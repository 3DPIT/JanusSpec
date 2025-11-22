package blackspring.janusspec.application.port.apiendpoint;

import blackspring.janusspec.domain.SwaggerVersion;
import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;

public record ApiEndPointReq(String serviceName, Long swaggerVersionId, OpenApiSpec openApiSpec) {
}

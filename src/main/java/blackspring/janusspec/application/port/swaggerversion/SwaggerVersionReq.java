package blackspring.janusspec.application.port.swaggerversion;

import blackspring.janusspec.domain.SwaggerVersion;
import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;

public record SwaggerVersionReq(
        String serviceName,
        String swaggerUrl,
        OpenApiSpec openApiSpec
) {
}

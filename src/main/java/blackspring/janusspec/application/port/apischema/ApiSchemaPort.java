package blackspring.janusspec.application.port.apischema;

import blackspring.janusspec.domain.ApiSchema;
import blackspring.janusspec.domain.SwaggerVersion;
import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;

import java.util.List;

public interface ApiSchemaPort {
    void save(SwaggerVersion swaggerVersion, OpenApiSpec openApiSpec);
    List<ApiSchema> findBySwaggerVersion(SwaggerVersion swaggerVersion);
}


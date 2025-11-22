package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.domain.ApiSchema;
import blackspring.janusspec.domain.SwaggerVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiSchemaRepository extends JpaRepository<ApiSchema, Long> {
    List<ApiSchema> findBySwaggerVersion(SwaggerVersion swaggerVersion);
}


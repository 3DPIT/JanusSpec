package blackspring.janusspec.domain;

import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "api_endpoint")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiEndpoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swagger_version_id")
    private SwaggerVersion swaggerVersion;

    @Column(length = 500, nullable = false)
    private String path;

    @Column(name = "http_method", length = 10, nullable = false)
    private String httpMethod;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "operation_id", length = 200)
    private String operationId;

    private boolean deprecated;

    @Column(name = "request_schema_name", length = 200)
    private String requestSchemaName;

    @Column(name = "response_schema_name", length = 200)
    private String responseSchemaName;

    public ApiEndpoint saveApiEndPoint(
            SwaggerVersion swaggerVersion,
            String path,
            String httpMethod,
            String summary,
            String operationId,
            boolean deprecated,
            String requestSchemaName,
            String responseSchemaName) {
        this.swaggerVersion = swaggerVersion;
        this.path = path;
        this.httpMethod = httpMethod;
        this.summary = summary;
        this.operationId= operationId;
        this.deprecated = deprecated;
        this.requestSchemaName = requestSchemaName;
        this.responseSchemaName = responseSchemaName;

        return this;
    }

}
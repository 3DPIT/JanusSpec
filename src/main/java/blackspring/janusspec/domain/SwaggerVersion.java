package blackspring.janusspec.domain;

import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.hash.Hashing;
import jakarta.persistence.*;
import lombok.*;

import java.nio.charset.StandardCharsets;

@Entity
@Table(name = "swagger_version", 
       indexes = {
           @Index(name = "idx_service_url", columnList = "service_name, swagger_url")
       })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwaggerVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="service_name", length = 100, nullable = false)
    private String serviceName;

    @Column(name = "swagger_url", length = 500, nullable = false)
    private String swaggerUrl;

    @Column(name = "version_tag", length = 50, nullable = false)
    private String versionTag;

    @Column(columnDefinition = "TEXT")
    private String rawJson;

    @Column(length =  128)
    private String hash;

    public SwaggerVersion saveSwaggerVersion(String serviceName, String swaggerUrl, OpenApiSpec spec, String checkHash) {

        String hash = Hashing.sha256()
                .hashString(spec.getPaths().toString(), StandardCharsets.UTF_8)
                .toString();

        if(hash.equals(checkHash)){
            return new SwaggerVersion();
        }

        SwaggerVersion version = SwaggerVersion.builder()
                .serviceName(serviceName)
                .swaggerUrl(swaggerUrl)
                .versionTag(spec.getInfo().getVersion())
                .rawJson(spec.getPaths().toString())
                .hash(hash)
                .build();

        return version;
    }
}

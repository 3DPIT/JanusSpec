package blackspring.janusspec.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Table(name = "api_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApiSchema extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swagger_version_id")
    private SwaggerVersion swaggerVersion;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(columnDefinition = "jsonb")
    private String rawSchema;

    @Column(columnDefinition = "jsonb")
    private String properties;
}
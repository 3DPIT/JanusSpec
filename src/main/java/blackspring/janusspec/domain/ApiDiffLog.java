package blackspring.janusspec.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_diff_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApiDiffLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_version_id")
    private SwaggerVersion oldVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_version_id")
    private SwaggerVersion newVersion;

    @Column(columnDefinition = "jsonb")
    private String diffJson;
}

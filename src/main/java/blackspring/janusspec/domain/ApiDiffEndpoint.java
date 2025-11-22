package blackspring.janusspec.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_diff_endpoint")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApiDiffEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diff_log_id")
    private ApiDiffLog diffLog;

    @Column(length = 500)
    private String path;

    @Column(length = 10)
    private String httpMethod;

    @Column(name = "change_type", length = 50)
    private String changeType;   // ADDED, REMOVED, UPDATED

    @Column(columnDefinition = "jsonb")
    private String beforeJson;

    @Column(columnDefinition = "jsonb")
    private String afterJson;
}
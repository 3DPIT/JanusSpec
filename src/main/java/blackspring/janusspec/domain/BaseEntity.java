package blackspring.janusspec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public class BaseEntity {
    @Column(name = "create_at", nullable = false)
    protected LocalDateTime createAt = LocalDateTime.now();
}

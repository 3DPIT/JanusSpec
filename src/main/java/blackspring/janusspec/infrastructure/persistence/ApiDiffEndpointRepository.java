package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.domain.ApiDiffEndpoint;
import blackspring.janusspec.domain.ApiDiffLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiDiffEndpointRepository extends JpaRepository<ApiDiffEndpoint, Long> {
    
    // 특정 DiffLog의 모든 엔드포인트 변경 내역 조회
    List<ApiDiffEndpoint> findByDiffLog(ApiDiffLog diffLog);
    
    // 특정 DiffLog의 특정 타입 변경 내역만 조회
    List<ApiDiffEndpoint> findByDiffLogAndChangeType(ApiDiffLog diffLog, String changeType);
}


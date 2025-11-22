package blackspring.janusspec.application;

import blackspring.janusspec.application.dto.ApiDiffDetailRes;
import blackspring.janusspec.application.dto.ApiDiffLogSummaryRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface GetApiDiff {
    
    /**
     * 전체 변경 이력 조회 (페이징)
     */
    Page<ApiDiffLogSummaryRes> getAllDiffLogs(Pageable pageable);
    
    /**
     * 특정 서비스의 변경 이력 조회 (페이징)
     */
    Page<ApiDiffLogSummaryRes> getDiffLogsByService(String serviceName, Pageable pageable);
    
    /**
     * 특정 Diff Log의 상세 정보 조회
     */
    Optional<ApiDiffDetailRes> getDiffDetail(Long diffLogId);
}


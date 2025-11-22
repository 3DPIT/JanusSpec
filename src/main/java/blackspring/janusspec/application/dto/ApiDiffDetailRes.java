package blackspring.janusspec.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "API 변경 상세 정보")
public record ApiDiffDetailRes(
        @Schema(description = "Diff Log 요약 정보")
        ApiDiffLogSummaryRes summary,
        
        @Schema(description = "추가된 엔드포인트 목록")
        List<ApiEndpointChangeDto> addedEndpoints,
        
        @Schema(description = "삭제된 엔드포인트 목록")
        List<ApiEndpointChangeDto> removedEndpoints,
        
        @Schema(description = "수정된 엔드포인트 목록")
        List<ApiEndpointChangeDto> updatedEndpoints,
        
        @Schema(description = "전체 Diff JSON 요약")
        String diffJsonSummary
) {
}


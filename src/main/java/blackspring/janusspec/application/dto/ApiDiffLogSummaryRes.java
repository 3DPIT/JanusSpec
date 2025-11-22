package blackspring.janusspec.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "API 변경 이력 요약")
public record ApiDiffLogSummaryRes(
        @Schema(description = "Diff Log ID")
        Long diffLogId,
        
        @Schema(description = "서비스명")
        String serviceName,
        
        @Schema(description = "이전 버전 ID")
        Long oldVersionId,
        
        @Schema(description = "이전 버전 태그")
        String oldVersionTag,
        
        @Schema(description = "새 버전 ID")
        Long newVersionId,
        
        @Schema(description = "새 버전 태그")
        String newVersionTag,
        
        @Schema(description = "추가된 API 개수")
        int addedCount,
        
        @Schema(description = "삭제된 API 개수")
        int removedCount,
        
        @Schema(description = "수정된 API 개수")
        int updatedCount,
        
        @Schema(description = "전체 변경 개수")
        int totalChanges,
        
        @Schema(description = "변경 감지 시간")
        LocalDateTime createdAt
) {
}


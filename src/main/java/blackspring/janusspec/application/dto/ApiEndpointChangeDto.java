package blackspring.janusspec.application.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "API 엔드포인트 변경 정보")
public record ApiEndpointChangeDto(
        @Schema(description = "엔드포인트 경로")
        String path,
        
        @Schema(description = "HTTP 메서드")
        String httpMethod,
        
        @Schema(description = "변경 타입 (ADDED, REMOVED, UPDATED)")
        String changeType,
        
        @Schema(description = "변경 전 정보 (JSON)")
        String beforeJson,
        
        @Schema(description = "변경 후 정보 (JSON)")
        String afterJson
) {
    // JSON을 Map으로 변환하는 헬퍼 메서드
    public Map<String, Object> getBeforeData() {
        return parseJson(beforeJson);
    }
    
    public Map<String, Object> getAfterData() {
        return parseJson(afterJson);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return Map.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}


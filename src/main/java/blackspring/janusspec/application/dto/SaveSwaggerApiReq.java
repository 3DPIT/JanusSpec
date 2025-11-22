package blackspring.janusspec.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Swagger API 저장 요청")
public record SaveSwaggerApiReq(
        @Schema(description = "Swagger API 문서 URL", example = "http://localhost:18081/api/v1/users/api-docs", required = true)
        String url,
        
        @Schema(description = "서비스 이름 (선택, null이면 URL에서 자동 추출)", example = "users", required = false)
        String serviceName
) {
    // serviceName이 null이면 URL에서 추출
    public String getServiceName() {
        if (serviceName != null && !serviceName.isBlank()) {
            return serviceName;
        }
        // URL에서 서비스명 추출: /api/v1/{serviceName}/... 패턴
        return extractServiceNameFromUrl(url);
    }
    
    private String extractServiceNameFromUrl(String url) {
        try {
            String[] parts = url.split("/");
            // /api/v1/{serviceName} 패턴 찾기
            for (int i = 0; i < parts.length - 1; i++) {
                if ("api".equals(parts[i]) && i + 2 < parts.length) {
                    if (parts[i + 1].startsWith("v")) { // v1, v2 등
                        return parts[i + 2];
                    }
                }
            }
            // 찾지 못한 경우 기본값
            return "unknown-service";
        } catch (Exception e) {
            return "unknown-service";
        }
    }
}

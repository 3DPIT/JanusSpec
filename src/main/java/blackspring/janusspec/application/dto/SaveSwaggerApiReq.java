package blackspring.janusspec.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

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

			for (int i = 0; i < parts.length; i++) {
				if ("api".equals(parts[i])) {

					// 패턴 1: /api/v1/{serviceName}
					if (i + 2 < parts.length && parts[i + 1].startsWith("v")) {
						return parts[i + 2];
					}

					// 패턴 2: /api/docs/{serviceName}
					if (i + 2 < parts.length && "docs".equals(parts[i + 1])) {
						return parts[i + 2];
					}

					// 일반 패턴: /api/{serviceName}
					if (i + 1 < parts.length && !parts[i + 1].startsWith("v")) {
						return parts[i + 1];
					}
				}
			}

			// 랜덤 unknown 라벨 생성
			return "unknown-service-" + UUID.randomUUID().toString().substring(0, 4);

		} catch (Exception e) {
			return "unknown-service-" + UUID.randomUUID().toString().substring(0, 4);
		}

}

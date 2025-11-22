package blackspring.janusspec.application.dto;

public record SaveSwaggerApiReq(
        String url,
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

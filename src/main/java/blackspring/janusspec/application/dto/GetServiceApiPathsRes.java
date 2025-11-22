package blackspring.janusspec.application.dto;

import java.util.List;

public record GetServiceApiPathsRes(
        String serviceName,
        String versionTag,
        Long swaggerVersionId,
        List<ApiEndpointDto> endpoints
) {
}


package blackspring.janusspec.application.dto;

public record ApiEndpointDto(
        String path,
        String httpMethod,
        String summary,
        String operationId,
        boolean deprecated,
        String requestSchemaName,
        String responseSchemaName
) {
}


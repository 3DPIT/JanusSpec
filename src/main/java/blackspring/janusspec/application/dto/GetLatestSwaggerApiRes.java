package blackspring.janusspec.application.dto;

public record GetLatestSwaggerApiRes(
        Long id,
        String serviceName,
        String swaggerUrl,
        String versionTag,
        String hash
) {
}


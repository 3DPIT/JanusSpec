package blackspring.janusspec.application.port.swaggerversion;

public record SwaggerVersionRes (
        Long swaggerVersionId,
        Long oldVersionId,
        String message,
        boolean checkHash
){
}

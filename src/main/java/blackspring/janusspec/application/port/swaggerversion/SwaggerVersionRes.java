package blackspring.janusspec.application.port.swaggerversion;

public record SwaggerVersionRes (
        Long swaggerVersionId,
        String message,
        boolean checkHash
){
}

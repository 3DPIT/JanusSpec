package blackspring.janusspec.application;

import blackspring.janusspec.application.dto.SaveSwaggerApiReq;
import blackspring.janusspec.application.port.apiendpoint.ApiEndPointPort;
import blackspring.janusspec.application.port.apiendpoint.ApiEndPointReq;
import blackspring.janusspec.application.port.jsonparser.JsonParserPort;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionPort;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionReq;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionRes;
import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SaveSwaggerService implements SaveApiSpec {

    private final SwaggerVersionPort swaggerVersionPort;
    private final ApiEndPointPort endPointPort;
    private final JsonParserPort jsonParserPort;


    @Override
    public String saveServiceApiSpec(SaveSwaggerApiReq req) {
        String[] urlSplit = req.url().split("/");
        OpenApiSpec openApiSpec = jsonParserPort.saveApiSpecAll(req.url());
        SwaggerVersionRes swaggerVersionRes = swaggerVersionPort.save(new SwaggerVersionReq(urlSplit[5], req.url(), openApiSpec));
        if (swaggerVersionRes.checkHash() == false)
            endPointPort.save(new ApiEndPointReq(urlSplit[5], swaggerVersionRes.swaggerVersionId(), openApiSpec));

        return swaggerVersionRes.swaggerVersionId().toString();
    }
}

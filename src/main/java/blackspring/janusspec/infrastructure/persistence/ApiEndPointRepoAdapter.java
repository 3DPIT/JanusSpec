package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.application.port.apiendpoint.ApiEndPointPort;
import blackspring.janusspec.application.port.apiendpoint.ApiEndPointReq;
import blackspring.janusspec.application.port.apiendpoint.ApiEndPointRes;
import blackspring.janusspec.domain.ApiEndpoint;
import blackspring.janusspec.domain.SwaggerVersion;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Iterator;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ApiEndPointRepoAdapter implements ApiEndPointPort {

    private final ApiEndPointRepository apiEndPointRepository;
    private final SwaggerVersionRepository swaggerVersionRepository;

    @Override
    public ApiEndPointRes save(ApiEndPointReq req) {
        Iterator<String> paths = req.openApiSpec().getPaths().fieldNames();
        SwaggerVersion swaggerVersion = swaggerVersionRepository.findById(req.swaggerVersionId()).get();

        while (paths.hasNext()) {

            String path = paths.next();
            JsonNode pathNode = req.openApiSpec().getPaths().get(path);

            // GET/POST/PUT/DELETE 등 HTTP 메소드 루프
            Iterator<String> methods = pathNode.fieldNames();

            while (methods.hasNext()) {
                String httpMethod = methods.next();
                JsonNode methodNode = pathNode.get(httpMethod);

                // summary
                String summary = methodNode.path("summary").asText("");

                // operationId
                String operationId = methodNode.path("operationId").asText("");

                // deprecated
                boolean deprecated = methodNode.path("deprecated").asBoolean(false);

                // request schema
                String requestSchemaName = "";
                JsonNode requestSchemaNode = methodNode
                        .path("requestBody")
                        .path("content")
                        .path("application/json")
                        .path("schema")
                        .path("$ref");

                if (!requestSchemaNode.isMissingNode()) {
                    requestSchemaName = requestSchemaNode.asText("").replace("#/components/schemas/", "");
                }

                // response schema
                String responseSchemaName = "";
                JsonNode responseSchemaNode = methodNode
                        .path("responses")
                        .path("200")
                        .path("content")
                        .path("application/json")
                        .path("schema")
                        .path("$ref");

                if (!responseSchemaNode.isMissingNode()) {
                    responseSchemaName = responseSchemaNode.asText("").replace("#/components/schemas/", "");
                }

                // DB 저장
                ApiEndpoint api = new ApiEndpoint().saveApiEndPoint(
                        swaggerVersion,
                        path,
                        httpMethod,
                        summary,
                        operationId,
                        deprecated,
                        requestSchemaName,
                        responseSchemaName
                );

                apiEndPointRepository.save(api);

                System.out.println("[SAVE] " + httpMethod.toUpperCase() + " " + path +
                        " / summary=" + summary +
                        " / operationId=" + operationId +
                        " / req=" + requestSchemaName +
                        " / resp=" + responseSchemaName);
            }
        }

        return new ApiEndPointRes();
    }

    @Override
    public List<ApiEndpoint> findBySwaggerVersion(SwaggerVersion swaggerVersion) {
        return apiEndPointRepository.findBySwaggerVersion(swaggerVersion);
    }
}

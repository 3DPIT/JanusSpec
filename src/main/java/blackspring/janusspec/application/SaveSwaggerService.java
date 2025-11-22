package blackspring.janusspec.application;

import blackspring.janusspec.application.dto.ApiEndpointDto;
import blackspring.janusspec.application.dto.GetLatestSwaggerApiRes;
import blackspring.janusspec.application.dto.GetServiceApiPathsRes;
import blackspring.janusspec.application.dto.SaveSwaggerApiReq;
import blackspring.janusspec.application.dto.SaveSwaggerApiRes;
import blackspring.janusspec.application.port.apidiff.ApiDiffPort;
import blackspring.janusspec.application.port.apiendpoint.ApiEndPointPort;
import blackspring.janusspec.application.port.apiendpoint.ApiEndPointReq;
import blackspring.janusspec.application.port.jsonparser.JsonParserPort;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionPort;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionReq;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionRes;
import blackspring.janusspec.domain.SwaggerVersion;
import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaveSwaggerService implements SaveApiSpec {

    private final SwaggerVersionPort swaggerVersionPort;
    private final ApiEndPointPort endPointPort;
    private final JsonParserPort jsonParserPort;
    private final ApiDiffPort apiDiffPort;

    @Override
    public SaveSwaggerApiRes saveServiceApiSpec(SaveSwaggerApiReq req) {
        String serviceName = req.getServiceName();
        OpenApiSpec openApiSpec = jsonParserPort.saveApiSpecAll(req.url());
        SwaggerVersionRes swaggerVersionRes = swaggerVersionPort.save(new SwaggerVersionReq(serviceName, req.url(), openApiSpec));
        
        // 해시가 변경된 경우 (API가 변경된 경우)
        if (swaggerVersionRes.checkHash() == false) {
            // 새 버전의 엔드포인트 저장
            endPointPort.save(new ApiEndPointReq(serviceName, swaggerVersionRes.swaggerVersionId(), openApiSpec));
            
            // 이전 버전이 존재하면 diff 저장
            if (swaggerVersionRes.oldVersionId() != null) {
                Optional<SwaggerVersion> oldVersionOpt = swaggerVersionPort.findById(swaggerVersionRes.oldVersionId());
                Optional<SwaggerVersion> newVersionOpt = swaggerVersionPort.findById(swaggerVersionRes.swaggerVersionId());
                
                if (oldVersionOpt.isPresent() && newVersionOpt.isPresent()) {
                    apiDiffPort.saveDiff(oldVersionOpt.get(), newVersionOpt.get());
                }
            }
        }

        return new SaveSwaggerApiRes(swaggerVersionRes.swaggerVersionId().toString(),req.url());
    }

    @Override
    public Optional<GetLatestSwaggerApiRes> getLatestSwaggerApi() {
        return swaggerVersionPort.findLatest()
                .map(swaggerVersion -> new GetLatestSwaggerApiRes(
                        swaggerVersion.getId(),
                        swaggerVersion.getServiceName(),
                        swaggerVersion.getSwaggerUrl(),
                        swaggerVersion.getVersionTag(),
                        swaggerVersion.getHash()
                ));
    }

    @Override
    public Optional<GetServiceApiPathsRes> getServiceApiPaths(String serviceName) {
        return swaggerVersionPort.findLatestByServiceName(serviceName)
                .map(swaggerVersion -> {
                    List<ApiEndpointDto> endpoints = endPointPort.findBySwaggerVersion(swaggerVersion)
                            .stream()
                            .map(endpoint -> new ApiEndpointDto(
                                    endpoint.getPath(),
                                    endpoint.getHttpMethod(),
                                    endpoint.getSummary(),
                                    endpoint.getOperationId(),
                                    endpoint.isDeprecated(),
                                    endpoint.getRequestSchemaName(),
                                    endpoint.getResponseSchemaName()
                            ))
                            .collect(Collectors.toList());

                    return new GetServiceApiPathsRes(
                            swaggerVersion.getServiceName(),
                            swaggerVersion.getVersionTag(),
                            swaggerVersion.getId(),
                            endpoints
                    );
                });
    }
}

package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.application.port.apidiff.ApiDiffPort;
import blackspring.janusspec.domain.ApiDiffEndpoint;
import blackspring.janusspec.domain.ApiDiffLog;
import blackspring.janusspec.domain.ApiEndpoint;
import blackspring.janusspec.domain.SwaggerVersion;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApiDiffRepoAdapter implements ApiDiffPort {

    private final ApiDiffLogRepository apiDiffLogRepository;
    private final ApiDiffEndpointRepository apiDiffEndpointRepository;
    private final ApiEndPointRepository apiEndPointRepository;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApiDiffLog saveDiff(SwaggerVersion oldVersion, SwaggerVersion newVersion) {
        // ApiDiffLog 생성
        ApiDiffLog diffLog = ApiDiffLog.builder()
                .oldVersion(oldVersion)
                .newVersion(newVersion)
                .diffJson(createDiffSummary(oldVersion, newVersion))
                .build();

        ApiDiffLog savedDiffLog = apiDiffLogRepository.save(diffLog);

        // 이전 버전과 새 버전의 엔드포인트 가져오기
        List<ApiEndpoint> oldEndpoints = apiEndPointRepository.findBySwaggerVersion(oldVersion);
        List<ApiEndpoint> newEndpoints = apiEndPointRepository.findBySwaggerVersion(newVersion);

        // 엔드포인트 비교
        Map<String, ApiEndpoint> oldEndpointMap = oldEndpoints.stream()
                .collect(Collectors.toMap(
                        e -> e.getPath() + ":" + e.getHttpMethod(),
                        e -> e
                ));

        Map<String, ApiEndpoint> newEndpointMap = newEndpoints.stream()
                .collect(Collectors.toMap(
                        e -> e.getPath() + ":" + e.getHttpMethod(),
                        e -> e
                ));

        // ADDED: 새 버전에만 있는 엔드포인트
        newEndpointMap.forEach((key, newEndpoint) -> {
            if (!oldEndpointMap.containsKey(key)) {
                ApiDiffEndpoint diffEndpoint = ApiDiffEndpoint.builder()
                        .diffLog(savedDiffLog)
                        .path(newEndpoint.getPath())
                        .httpMethod(newEndpoint.getHttpMethod())
                        .changeType("ADDED")
                        .beforeJson(null)
                        .afterJson(endpointToJson(newEndpoint))
                        .build();
                apiDiffEndpointRepository.save(diffEndpoint);
            }
        });

        // REMOVED: 이전 버전에만 있는 엔드포인트
        oldEndpointMap.forEach((key, oldEndpoint) -> {
            if (!newEndpointMap.containsKey(key)) {
                ApiDiffEndpoint diffEndpoint = ApiDiffEndpoint.builder()
                        .diffLog(savedDiffLog)
                        .path(oldEndpoint.getPath())
                        .httpMethod(oldEndpoint.getHttpMethod())
                        .changeType("REMOVED")
                        .beforeJson(endpointToJson(oldEndpoint))
                        .afterJson(null)
                        .build();
                apiDiffEndpointRepository.save(diffEndpoint);
            }
        });

        // UPDATED: 양쪽 모두에 있지만 내용이 다른 엔드포인트
        oldEndpointMap.forEach((key, oldEndpoint) -> {
            if (newEndpointMap.containsKey(key)) {
                ApiEndpoint newEndpoint = newEndpointMap.get(key);
                if (isEndpointChanged(oldEndpoint, newEndpoint)) {
                    ApiDiffEndpoint diffEndpoint = ApiDiffEndpoint.builder()
                            .diffLog(savedDiffLog)
                            .path(newEndpoint.getPath())
                            .httpMethod(newEndpoint.getHttpMethod())
                            .changeType("UPDATED")
                            .beforeJson(endpointToJson(oldEndpoint))
                            .afterJson(endpointToJson(newEndpoint))
                            .build();
                    apiDiffEndpointRepository.save(diffEndpoint);
                }
            }
        });

        return savedDiffLog;
    }

    private boolean isEndpointChanged(ApiEndpoint oldEndpoint, ApiEndpoint newEndpoint) {
        return !Objects.equals(oldEndpoint.getSummary(), newEndpoint.getSummary()) ||
                !Objects.equals(oldEndpoint.getOperationId(), newEndpoint.getOperationId()) ||
                oldEndpoint.isDeprecated() != newEndpoint.isDeprecated() ||
                !Objects.equals(oldEndpoint.getRequestSchemaName(), newEndpoint.getRequestSchemaName()) ||
                !Objects.equals(oldEndpoint.getResponseSchemaName(), newEndpoint.getResponseSchemaName());
    }

    private String endpointToJson(ApiEndpoint endpoint) {
        try {
            Map<String, Object> endpointData = new HashMap<>();
            endpointData.put("path", endpoint.getPath());
            endpointData.put("httpMethod", endpoint.getHttpMethod());
            endpointData.put("summary", endpoint.getSummary());
            endpointData.put("operationId", endpoint.getOperationId());
            endpointData.put("deprecated", endpoint.isDeprecated());
            endpointData.put("requestSchemaName", endpoint.getRequestSchemaName());
            endpointData.put("responseSchemaName", endpoint.getResponseSchemaName());
            return objectMapper.writeValueAsString(endpointData);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String createDiffSummary(SwaggerVersion oldVersion, SwaggerVersion newVersion) {
        try {
            Map<String, Object> summary = new HashMap<>();
            summary.put("oldVersionId", oldVersion.getId());
            summary.put("newVersionId", newVersion.getId());
            summary.put("oldVersionTag", oldVersion.getVersionTag());
            summary.put("newVersionTag", newVersion.getVersionTag());
            summary.put("serviceName", newVersion.getServiceName());
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{}";
        }
    }
}


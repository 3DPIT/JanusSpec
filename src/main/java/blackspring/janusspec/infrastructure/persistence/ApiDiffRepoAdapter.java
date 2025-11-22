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

        // 통계 정보 수집
        int addedCount = 0;
        int removedCount = 0;
        int updatedCount = 0;

        // ApiDiffLog 생성 (통계 포함)
        ApiDiffLog diffLog = ApiDiffLog.builder()
                .oldVersion(oldVersion)
                .newVersion(newVersion)
                .diffJson(createDiffSummary(oldVersion, newVersion, oldEndpointMap, newEndpointMap))
                .build();

        ApiDiffLog savedDiffLog = apiDiffLogRepository.save(diffLog);

        // ADDED: 새 버전에만 있는 엔드포인트
        for (Map.Entry<String, ApiEndpoint> entry : newEndpointMap.entrySet()) {
            if (!oldEndpointMap.containsKey(entry.getKey())) {
                ApiEndpoint newEndpoint = entry.getValue();
                ApiDiffEndpoint diffEndpoint = ApiDiffEndpoint.builder()
                        .diffLog(savedDiffLog)
                        .path(newEndpoint.getPath())
                        .httpMethod(newEndpoint.getHttpMethod())
                        .changeType("ADDED")
                        .beforeJson(null)
                        .afterJson(endpointToJson(newEndpoint, null))
                        .build();
                apiDiffEndpointRepository.save(diffEndpoint);
                addedCount++;
            }
        }

        // REMOVED: 이전 버전에만 있는 엔드포인트
        for (Map.Entry<String, ApiEndpoint> entry : oldEndpointMap.entrySet()) {
            if (!newEndpointMap.containsKey(entry.getKey())) {
                ApiEndpoint oldEndpoint = entry.getValue();
                ApiDiffEndpoint diffEndpoint = ApiDiffEndpoint.builder()
                        .diffLog(savedDiffLog)
                        .path(oldEndpoint.getPath())
                        .httpMethod(oldEndpoint.getHttpMethod())
                        .changeType("REMOVED")
                        .beforeJson(endpointToJson(oldEndpoint, null))
                        .afterJson(null)
                        .build();
                apiDiffEndpointRepository.save(diffEndpoint);
                removedCount++;
            }
        }

        // UPDATED: 양쪽 모두에 있지만 내용이 다른 엔드포인트
        for (Map.Entry<String, ApiEndpoint> entry : oldEndpointMap.entrySet()) {
            if (newEndpointMap.containsKey(entry.getKey())) {
                ApiEndpoint oldEndpoint = entry.getValue();
                ApiEndpoint newEndpoint = newEndpointMap.get(entry.getKey());
                
                List<String> changedFields = getChangedFields(oldEndpoint, newEndpoint);
                if (!changedFields.isEmpty()) {
                    ApiDiffEndpoint diffEndpoint = ApiDiffEndpoint.builder()
                            .diffLog(savedDiffLog)
                            .path(newEndpoint.getPath())
                            .httpMethod(newEndpoint.getHttpMethod())
                            .changeType("UPDATED")
                            .beforeJson(endpointToJson(oldEndpoint, changedFields))
                            .afterJson(endpointToJson(newEndpoint, changedFields))
                            .build();
                    apiDiffEndpointRepository.save(diffEndpoint);
                    updatedCount++;
                }
            }
        }

        System.out.println("[DIFF SUMMARY] Added: " + addedCount + ", Removed: " + removedCount + ", Updated: " + updatedCount);

        return savedDiffLog;
    }

    /**
     * 두 엔드포인트를 비교하여 변경된 필드 목록 반환
     */
    private List<String> getChangedFields(ApiEndpoint oldEndpoint, ApiEndpoint newEndpoint) {
        List<String> changedFields = new ArrayList<>();
        
        if (!Objects.equals(oldEndpoint.getSummary(), newEndpoint.getSummary())) {
            changedFields.add("summary");
        }
        if (!Objects.equals(oldEndpoint.getOperationId(), newEndpoint.getOperationId())) {
            changedFields.add("operationId");
        }
        if (oldEndpoint.isDeprecated() != newEndpoint.isDeprecated()) {
            changedFields.add("deprecated");
        }
        if (!Objects.equals(oldEndpoint.getRequestSchemaName(), newEndpoint.getRequestSchemaName())) {
            changedFields.add("requestSchemaName");
        }
        if (!Objects.equals(oldEndpoint.getResponseSchemaName(), newEndpoint.getResponseSchemaName())) {
            changedFields.add("responseSchemaName");
        }
        
        return changedFields;
    }

    /**
     * 엔드포인트를 JSON으로 변환 (변경된 필드 정보 포함)
     */
    private String endpointToJson(ApiEndpoint endpoint, List<String> changedFields) {
        try {
            Map<String, Object> endpointData = new LinkedHashMap<>();
            endpointData.put("path", endpoint.getPath());
            endpointData.put("httpMethod", endpoint.getHttpMethod());
            endpointData.put("summary", endpoint.getSummary());
            endpointData.put("operationId", endpoint.getOperationId());
            endpointData.put("deprecated", endpoint.isDeprecated());
            endpointData.put("requestSchemaName", endpoint.getRequestSchemaName());
            endpointData.put("responseSchemaName", endpoint.getResponseSchemaName());
            
            // 변경된 필드 목록 추가 (UPDATED인 경우)
            if (changedFields != null && !changedFields.isEmpty()) {
                endpointData.put("changedFields", changedFields);
            }
            
            return objectMapper.writeValueAsString(endpointData);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Diff 요약 정보 생성 (통계 포함)
     */
    private String createDiffSummary(SwaggerVersion oldVersion, SwaggerVersion newVersion, 
                                     Map<String, ApiEndpoint> oldEndpointMap, Map<String, ApiEndpoint> newEndpointMap) {
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("oldVersionId", oldVersion.getId());
            summary.put("newVersionId", newVersion.getId());
            summary.put("oldVersionTag", oldVersion.getVersionTag());
            summary.put("newVersionTag", newVersion.getVersionTag());
            summary.put("serviceName", newVersion.getServiceName());
            
            // 통계 정보
            int addedCount = 0;
            int removedCount = 0;
            int updatedCount = 0;
            
            // ADDED 계산
            for (String key : newEndpointMap.keySet()) {
                if (!oldEndpointMap.containsKey(key)) {
                    addedCount++;
                }
            }
            
            // REMOVED 계산
            for (String key : oldEndpointMap.keySet()) {
                if (!newEndpointMap.containsKey(key)) {
                    removedCount++;
                }
            }
            
            // UPDATED 계산
            for (String key : oldEndpointMap.keySet()) {
                if (newEndpointMap.containsKey(key)) {
                    ApiEndpoint oldEndpoint = oldEndpointMap.get(key);
                    ApiEndpoint newEndpoint = newEndpointMap.get(key);
                    if (!getChangedFields(oldEndpoint, newEndpoint).isEmpty()) {
                        updatedCount++;
                    }
                }
            }
            
            Map<String, Integer> statistics = new LinkedHashMap<>();
            statistics.put("added", addedCount);
            statistics.put("removed", removedCount);
            statistics.put("updated", updatedCount);
            statistics.put("total", addedCount + removedCount + updatedCount);
            
            summary.put("statistics", statistics);
            summary.put("totalOldEndpoints", oldEndpointMap.size());
            summary.put("totalNewEndpoints", newEndpointMap.size());
            
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{}";
        }
    }
}


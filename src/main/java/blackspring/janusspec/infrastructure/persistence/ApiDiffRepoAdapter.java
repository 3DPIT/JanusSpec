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
                
                Map<String, Map<String, String>> fieldChanges = getDetailedFieldChanges(oldEndpoint, newEndpoint);
                if (!fieldChanges.isEmpty()) {
                    ApiDiffEndpoint diffEndpoint = ApiDiffEndpoint.builder()
                            .diffLog(savedDiffLog)
                            .path(newEndpoint.getPath())
                            .httpMethod(newEndpoint.getHttpMethod())
                            .changeType("UPDATED")
                            .beforeJson(createDetailedBeforeJson(oldEndpoint, fieldChanges))
                            .afterJson(createDetailedAfterJson(newEndpoint, fieldChanges))
                            .build();
                    apiDiffEndpointRepository.save(diffEndpoint);
                    updatedCount++;
                }
            }
        }

        // 상세 로그 출력
        System.out.println("\n========================================");
        System.out.println("📊 API 변경 감지 완료!");
        System.out.println("========================================");
        System.out.println("🆕 추가된 API: " + addedCount + "개");
        System.out.println("🗑️  삭제된 API: " + removedCount + "개");
        System.out.println("✏️  수정된 API: " + updatedCount + "개");
        System.out.println("📈 총 변경 사항: " + (addedCount + removedCount + updatedCount) + "개");
        System.out.println("========================================\n");

        return savedDiffLog;
    }

    /**
     * 두 엔드포인트를 비교하여 변경된 필드의 상세 정보 반환
     * Map<필드명, Map<"before"/"after", 값>>
     */
    private Map<String, Map<String, String>> getDetailedFieldChanges(ApiEndpoint oldEndpoint, ApiEndpoint newEndpoint) {
        Map<String, Map<String, String>> changes = new LinkedHashMap<>();
        
        if (!Objects.equals(oldEndpoint.getSummary(), newEndpoint.getSummary())) {
            Map<String, String> change = new LinkedHashMap<>();
            change.put("before", oldEndpoint.getSummary() != null ? oldEndpoint.getSummary() : "");
            change.put("after", newEndpoint.getSummary() != null ? newEndpoint.getSummary() : "");
            changes.put("summary", change);
        }
        
        if (!Objects.equals(oldEndpoint.getOperationId(), newEndpoint.getOperationId())) {
            Map<String, String> change = new LinkedHashMap<>();
            change.put("before", oldEndpoint.getOperationId() != null ? oldEndpoint.getOperationId() : "");
            change.put("after", newEndpoint.getOperationId() != null ? newEndpoint.getOperationId() : "");
            changes.put("operationId", change);
        }
        
        if (oldEndpoint.isDeprecated() != newEndpoint.isDeprecated()) {
            Map<String, String> change = new LinkedHashMap<>();
            change.put("before", String.valueOf(oldEndpoint.isDeprecated()));
            change.put("after", String.valueOf(newEndpoint.isDeprecated()));
            changes.put("deprecated", change);
        }
        
        if (!Objects.equals(oldEndpoint.getRequestSchemaName(), newEndpoint.getRequestSchemaName())) {
            Map<String, String> change = new LinkedHashMap<>();
            change.put("before", oldEndpoint.getRequestSchemaName() != null ? oldEndpoint.getRequestSchemaName() : "");
            change.put("after", newEndpoint.getRequestSchemaName() != null ? newEndpoint.getRequestSchemaName() : "");
            changes.put("requestSchemaName", change);
        }
        
        if (!Objects.equals(oldEndpoint.getResponseSchemaName(), newEndpoint.getResponseSchemaName())) {
            Map<String, String> change = new LinkedHashMap<>();
            change.put("before", oldEndpoint.getResponseSchemaName() != null ? oldEndpoint.getResponseSchemaName() : "");
            change.put("after", newEndpoint.getResponseSchemaName() != null ? newEndpoint.getResponseSchemaName() : "");
            changes.put("responseSchemaName", change);
        }
        
        return changes;
    }
    
    /**
     * 변경 전 상세 JSON 생성 (변경된 필드만 강조)
     */
    private String createDetailedBeforeJson(ApiEndpoint endpoint, Map<String, Map<String, String>> fieldChanges) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("path", endpoint.getPath());
            data.put("httpMethod", endpoint.getHttpMethod());
            data.put("summary", endpoint.getSummary());
            data.put("operationId", endpoint.getOperationId());
            data.put("deprecated", endpoint.isDeprecated());
            data.put("requestSchemaName", endpoint.getRequestSchemaName());
            data.put("responseSchemaName", endpoint.getResponseSchemaName());
            
            // 변경된 필드 상세 정보
            if (!fieldChanges.isEmpty()) {
                Map<String, String> changedFieldsDetail = new LinkedHashMap<>();
                for (Map.Entry<String, Map<String, String>> entry : fieldChanges.entrySet()) {
                    changedFieldsDetail.put(entry.getKey(), entry.getValue().get("before"));
                }
                data.put("changedFields", changedFieldsDetail);
            }
            
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /**
     * 변경 후 상세 JSON 생성 (변경된 필드만 강조)
     */
    private String createDetailedAfterJson(ApiEndpoint endpoint, Map<String, Map<String, String>> fieldChanges) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("path", endpoint.getPath());
            data.put("httpMethod", endpoint.getHttpMethod());
            data.put("summary", endpoint.getSummary());
            data.put("operationId", endpoint.getOperationId());
            data.put("deprecated", endpoint.isDeprecated());
            data.put("requestSchemaName", endpoint.getRequestSchemaName());
            data.put("responseSchemaName", endpoint.getResponseSchemaName());
            
            // 변경된 필드 상세 정보
            if (!fieldChanges.isEmpty()) {
                Map<String, String> changedFieldsDetail = new LinkedHashMap<>();
                for (Map.Entry<String, Map<String, String>> entry : fieldChanges.entrySet()) {
                    changedFieldsDetail.put(entry.getKey(), entry.getValue().get("after"));
                }
                data.put("changedFields", changedFieldsDetail);
            }
            
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /**
     * 두 엔드포인트를 비교하여 변경된 필드 목록 반환 (통계용)
     */
    private List<String> getChangedFields(ApiEndpoint oldEndpoint, ApiEndpoint newEndpoint) {
        return new ArrayList<>(getDetailedFieldChanges(oldEndpoint, newEndpoint).keySet());
    }

    /**
     * 엔드포인트를 간단한 JSON으로 변환 (ADDED/REMOVED용)
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
            
            // UPDATED 계산 및 상세 정보 수집
            List<Map<String, Object>> updatedDetails = new ArrayList<>();
            for (String key : oldEndpointMap.keySet()) {
                if (newEndpointMap.containsKey(key)) {
                    ApiEndpoint oldEndpoint = oldEndpointMap.get(key);
                    ApiEndpoint newEndpoint = newEndpointMap.get(key);
                    Map<String, Map<String, String>> fieldChanges = getDetailedFieldChanges(oldEndpoint, newEndpoint);
                    if (!fieldChanges.isEmpty()) {
                        updatedCount++;
                        
                        // 변경 상세 정보
                        Map<String, Object> detail = new LinkedHashMap<>();
                        detail.put("path", newEndpoint.getPath());
                        detail.put("method", newEndpoint.getHttpMethod());
                        detail.put("changes", fieldChanges);
                        updatedDetails.add(detail);
                    }
                }
            }
            
            // 추가된 엔드포인트 목록
            List<String> addedPaths = new ArrayList<>();
            for (String key : newEndpointMap.keySet()) {
                if (!oldEndpointMap.containsKey(key)) {
                    ApiEndpoint endpoint = newEndpointMap.get(key);
                    addedPaths.add(endpoint.getHttpMethod().toUpperCase() + " " + endpoint.getPath());
                }
            }
            
            // 삭제된 엔드포인트 목록
            List<String> removedPaths = new ArrayList<>();
            for (String key : oldEndpointMap.keySet()) {
                if (!newEndpointMap.containsKey(key)) {
                    ApiEndpoint endpoint = oldEndpointMap.get(key);
                    removedPaths.add(endpoint.getHttpMethod().toUpperCase() + " " + endpoint.getPath());
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
            
            // 상세 변경 정보
            if (!addedPaths.isEmpty()) {
                summary.put("addedEndpoints", addedPaths);
            }
            if (!removedPaths.isEmpty()) {
                summary.put("removedEndpoints", removedPaths);
            }
            if (!updatedDetails.isEmpty()) {
                summary.put("updatedEndpointsDetails", updatedDetails);
            }
            
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{}";
        }
    }
}


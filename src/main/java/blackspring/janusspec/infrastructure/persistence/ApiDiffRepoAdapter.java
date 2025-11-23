package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.application.port.apidiff.ApiDiffPort;
import blackspring.janusspec.domain.ApiDiffEndpoint;
import blackspring.janusspec.domain.ApiDiffLog;
import blackspring.janusspec.domain.ApiDiffSchema;
import blackspring.janusspec.domain.ApiEndpoint;
import blackspring.janusspec.domain.ApiSchema;
import blackspring.janusspec.domain.SwaggerVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApiDiffRepoAdapter implements ApiDiffPort {

    private final ApiDiffLogRepository apiDiffLogRepository;
    private final ApiDiffEndpointRepository apiDiffEndpointRepository;
    private final ApiDiffSchemaRepository apiDiffSchemaRepository;
    private final ApiEndPointRepository apiEndPointRepository;
    private final ApiSchemaRepository apiSchemaRepository;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApiDiffLog saveDiff(SwaggerVersion oldVersion, SwaggerVersion newVersion) {
        // 이전 버전과 새 버전의 엔드포인트 가져오기
        List<ApiEndpoint> oldEndpoints = apiEndPointRepository.findBySwaggerVersion(oldVersion);
        List<ApiEndpoint> newEndpoints = apiEndPointRepository.findBySwaggerVersion(newVersion);

        // 이전 버전과 새 버전의 스키마 가져오기
        List<ApiSchema> oldSchemas = apiSchemaRepository.findBySwaggerVersion(oldVersion);
        List<ApiSchema> newSchemas = apiSchemaRepository.findBySwaggerVersion(newVersion);

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

        // 스키마 비교
        Map<String, ApiSchema> oldSchemaMap = oldSchemas.stream()
                .collect(Collectors.toMap(
                        ApiSchema::getName,
                        s -> s
                ));

        Map<String, ApiSchema> newSchemaMap = newSchemas.stream()
                .collect(Collectors.toMap(
                        ApiSchema::getName,
                        s -> s
                ));

        // 통계 정보 수집
        int addedCount = 0;
        int removedCount = 0;
        int updatedCount = 0;

        // ApiDiffLog 생성 (통계 포함)
        ApiDiffLog diffLog = ApiDiffLog.builder()
                .oldVersion(oldVersion)
                .newVersion(newVersion)
                .diffJson(createDiffSummary(oldVersion, newVersion, oldEndpointMap, newEndpointMap, oldSchemaMap, newSchemaMap))
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

        // Schema 변경 감지 및 저장
        int schemaAddedCount = 0;
        int schemaRemovedCount = 0;
        int schemaUpdatedCount = 0;

        // ADDED: 새 버전에만 있는 스키마
        for (Map.Entry<String, ApiSchema> entry : newSchemaMap.entrySet()) {
            if (!oldSchemaMap.containsKey(entry.getKey())) {
                ApiSchema newSchema = entry.getValue();
                ApiDiffSchema diffSchema = ApiDiffSchema.builder()
                        .diffLog(savedDiffLog)
                        .schemaName(newSchema.getName())
                        .changeType("ADDED")
                        .beforeJson(null)
                        .afterJson(newSchema.getRawSchema())
                        .build();
                apiDiffSchemaRepository.save(diffSchema);
                schemaAddedCount++;
                System.out.println("[SCHEMA ADDED] " + entry.getKey());
            }
        }

        // REMOVED: 이전 버전에만 있는 스키마
        for (Map.Entry<String, ApiSchema> entry : oldSchemaMap.entrySet()) {
            if (!newSchemaMap.containsKey(entry.getKey())) {
                ApiSchema oldSchema = entry.getValue();
                ApiDiffSchema diffSchema = ApiDiffSchema.builder()
                        .diffLog(savedDiffLog)
                        .schemaName(oldSchema.getName())
                        .changeType("REMOVED")
                        .beforeJson(oldSchema.getRawSchema())
                        .afterJson(null)
                        .build();
                apiDiffSchemaRepository.save(diffSchema);
                schemaRemovedCount++;
                System.out.println("[SCHEMA REMOVED] " + entry.getKey());
            }
        }

        // UPDATED: 양쪽 모두에 있지만 내용이 다른 스키마
        for (Map.Entry<String, ApiSchema> entry : oldSchemaMap.entrySet()) {
            if (newSchemaMap.containsKey(entry.getKey())) {
                ApiSchema oldSchema = entry.getValue();
                ApiSchema newSchema = newSchemaMap.get(entry.getKey());
                
                // 정규화된 rawSchema로 비교 (순서 무관하게 비교)
                String normalizedOldSchema = normalizeSchemaJson(oldSchema.getRawSchema());
                String normalizedNewSchema = normalizeSchemaJson(newSchema.getRawSchema());
                
                if (!Objects.equals(normalizedOldSchema, normalizedNewSchema)) {
                    // Schema 변경 상세 정보 추출
                    Map<String, Map<String, String>> schemaFieldChanges = getDetailedSchemaChanges(oldSchema, newSchema);
                    
                    ApiDiffSchema diffSchema = ApiDiffSchema.builder()
                            .diffLog(savedDiffLog)
                            .schemaName(newSchema.getName())
                            .changeType("UPDATED")
                            .beforeJson(createDetailedSchemaBeforeJson(oldSchema, schemaFieldChanges))
                            .afterJson(createDetailedSchemaAfterJson(newSchema, schemaFieldChanges))
                            .build();
                    apiDiffSchemaRepository.save(diffSchema);
                    schemaUpdatedCount++;
                    System.out.println("[SCHEMA UPDATED] " + entry.getKey());
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
        System.out.println("📦 추가된 Schema: " + schemaAddedCount + "개");
        System.out.println("🗑️  삭제된 Schema: " + schemaRemovedCount + "개");
        System.out.println("✏️  수정된 Schema: " + schemaUpdatedCount + "개");
        System.out.println("📈 총 변경 사항: " + (addedCount + removedCount + updatedCount + schemaAddedCount + schemaRemovedCount + schemaUpdatedCount) + "개");
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
     * 두 스키마를 비교하여 변경된 필드의 상세 정보 반환
     */
    private Map<String, Map<String, String>> getDetailedSchemaChanges(ApiSchema oldSchema, ApiSchema newSchema) {
        Map<String, Map<String, String>> changes = new LinkedHashMap<>();
        
        try {
            // 정규화된 JSON으로 파싱하여 비교
            String normalizedOld = normalizeSchemaJson(oldSchema.getRawSchema());
            String normalizedNew = normalizeSchemaJson(newSchema.getRawSchema());
            
            JsonNode oldJson = objectMapper.readTree(normalizedOld);
            JsonNode newJson = objectMapper.readTree(normalizedNew);
            
            // type 변경 감지
            if (oldJson.has("type") && newJson.has("type")) {
                if (!Objects.equals(oldJson.get("type").asText(), newJson.get("type").asText())) {
                    Map<String, String> change = new LinkedHashMap<>();
                    change.put("before", oldJson.get("type").asText());
                    change.put("after", newJson.get("type").asText());
                    changes.put("type", change);
                }
            }
            
            // properties 변경 감지
            if (oldJson.has("properties") || newJson.has("properties")) {
                JsonNode oldProperties = oldJson.has("properties") ? oldJson.get("properties") : null;
                JsonNode newProperties = newJson.has("properties") ? newJson.get("properties") : null;
                
                Set<String> allPropertyNames = new TreeSet<>();
                if (oldProperties != null && oldProperties.isObject()) {
                    oldProperties.fieldNames().forEachRemaining(allPropertyNames::add);
                }
                if (newProperties != null && newProperties.isObject()) {
                    newProperties.fieldNames().forEachRemaining(allPropertyNames::add);
                }
                
                for (String propName : allPropertyNames) {
                    JsonNode oldProp = (oldProperties != null && oldProperties.has(propName)) ? oldProperties.get(propName) : null;
                    JsonNode newProp = (newProperties != null && newProperties.has(propName)) ? newProperties.get(propName) : null;
                    
                    if (oldProp == null && newProp != null) {
                        // 새로 추가된 property
                        Map<String, String> change = new LinkedHashMap<>();
                        change.put("before", null);
                        change.put("after", newProp.toString());
                        changes.put("property." + propName, change);
                    } else if (oldProp != null && newProp == null) {
                        // 삭제된 property
                        Map<String, String> change = new LinkedHashMap<>();
                        change.put("before", oldProp.toString());
                        change.put("after", null);
                        changes.put("property." + propName, change);
                    } else if (oldProp != null && newProp != null && !oldProp.equals(newProp)) {
                        // 수정된 property
                        Map<String, String> change = new LinkedHashMap<>();
                        change.put("before", oldProp.toString());
                        change.put("after", newProp.toString());
                        changes.put("property." + propName, change);
                    }
                }
            }
            
            // required 필드 변경 감지
            if (oldJson.has("required") || newJson.has("required")) {
                JsonNode oldRequired = oldJson.has("required") ? oldJson.get("required") : null;
                JsonNode newRequired = newJson.has("required") ? newJson.get("required") : null;
                
                if (!Objects.equals(oldRequired, newRequired)) {
                    Map<String, String> change = new LinkedHashMap<>();
                    change.put("before", oldRequired != null ? oldRequired.toString() : "[]");
                    change.put("after", newRequired != null ? newRequired.toString() : "[]");
                    changes.put("required", change);
                }
            }
            
        } catch (Exception e) {
            // JSON 파싱 실패 시 전체 스키마 비교
            if (!Objects.equals(oldSchema.getRawSchema(), newSchema.getRawSchema())) {
                Map<String, String> change = new LinkedHashMap<>();
                change.put("before", oldSchema.getRawSchema());
                change.put("after", newSchema.getRawSchema());
                changes.put("rawSchema", change);
            }
        }
        
        return changes;
    }
    
    /**
     * 변경 전 Schema 상세 JSON 생성
     */
    private String createDetailedSchemaBeforeJson(ApiSchema schema, Map<String, Map<String, String>> fieldChanges) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", schema.getName());
            data.put("rawSchema", schema.getRawSchema());
            
            // 변경된 필드 상세 정보
            if (!fieldChanges.isEmpty()) {
                Map<String, String> changedFieldsDetail = new LinkedHashMap<>();
                for (Map.Entry<String, Map<String, String>> entry : fieldChanges.entrySet()) {
                    String beforeValue = entry.getValue().get("before");
                    changedFieldsDetail.put(entry.getKey(), beforeValue != null ? beforeValue : "");
                }
                data.put("changedFields", changedFieldsDetail);
            }
            
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return schema.getRawSchema();
        }
    }
    
    /**
     * 변경 후 Schema 상세 JSON 생성
     */
    private String createDetailedSchemaAfterJson(ApiSchema schema, Map<String, Map<String, String>> fieldChanges) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", schema.getName());
            data.put("rawSchema", schema.getRawSchema());
            
            // 변경된 필드 상세 정보
            if (!fieldChanges.isEmpty()) {
                Map<String, String> changedFieldsDetail = new LinkedHashMap<>();
                for (Map.Entry<String, Map<String, String>> entry : fieldChanges.entrySet()) {
                    String afterValue = entry.getValue().get("after");
                    changedFieldsDetail.put(entry.getKey(), afterValue != null ? afterValue : "");
                }
                data.put("changedFields", changedFieldsDetail);
            }
            
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return schema.getRawSchema();
        }
    }

    /**
     * Diff 요약 정보 생성 (통계 포함)
     */
    private String createDiffSummary(SwaggerVersion oldVersion, SwaggerVersion newVersion, 
                                     Map<String, ApiEndpoint> oldEndpointMap, Map<String, ApiEndpoint> newEndpointMap,
                                     Map<String, ApiSchema> oldSchemaMap, Map<String, ApiSchema> newSchemaMap) {
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
            
            // Schema 통계 계산
            int schemaAddedCount = 0;
            int schemaRemovedCount = 0;
            int schemaUpdatedCount = 0;
            List<String> addedSchemas = new ArrayList<>();
            List<String> removedSchemas = new ArrayList<>();
            List<String> updatedSchemas = new ArrayList<>();
            
            // ADDED Schema 계산
            for (String key : newSchemaMap.keySet()) {
                if (!oldSchemaMap.containsKey(key)) {
                    schemaAddedCount++;
                    addedSchemas.add(key);
                }
            }
            
            // REMOVED Schema 계산
            for (String key : oldSchemaMap.keySet()) {
                if (!newSchemaMap.containsKey(key)) {
                    schemaRemovedCount++;
                    removedSchemas.add(key);
                }
            }
            
            // UPDATED Schema 계산
            for (String key : oldSchemaMap.keySet()) {
                if (newSchemaMap.containsKey(key)) {
                    ApiSchema oldSchema = oldSchemaMap.get(key);
                    ApiSchema newSchema = newSchemaMap.get(key);
                    if (!Objects.equals(oldSchema.getRawSchema(), newSchema.getRawSchema())) {
                        schemaUpdatedCount++;
                        updatedSchemas.add(key);
                    }
                }
            }
            
            Map<String, Integer> statistics = new LinkedHashMap<>();
            statistics.put("added", addedCount);
            statistics.put("removed", removedCount);
            statistics.put("updated", updatedCount);
            statistics.put("total", addedCount + removedCount + updatedCount);
            
            Map<String, Integer> schemaStatistics = new LinkedHashMap<>();
            schemaStatistics.put("added", schemaAddedCount);
            schemaStatistics.put("removed", schemaRemovedCount);
            schemaStatistics.put("updated", schemaUpdatedCount);
            schemaStatistics.put("total", schemaAddedCount + schemaRemovedCount + schemaUpdatedCount);
            
            summary.put("statistics", statistics);
            summary.put("schemaStatistics", schemaStatistics);
            summary.put("totalOldEndpoints", oldEndpointMap.size());
            summary.put("totalNewEndpoints", newEndpointMap.size());
            summary.put("totalOldSchemas", oldSchemaMap.size());
            summary.put("totalNewSchemas", newSchemaMap.size());
            
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
            
            // Schema 변경 정보
            if (!addedSchemas.isEmpty()) {
                summary.put("addedSchemas", addedSchemas);
            }
            if (!removedSchemas.isEmpty()) {
                summary.put("removedSchemas", removedSchemas);
            }
            if (!updatedSchemas.isEmpty()) {
                summary.put("updatedSchemas", updatedSchemas);
            }
            
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /**
     * Schema JSON을 정규화하여 키 순서를 보장합니다.
     * 같은 내용이라도 항상 같은 문자열이 되도록 보장합니다.
     */
    private String normalizeSchemaJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }
        
        try {
            JsonNode node = objectMapper.readTree(jsonString);
            JsonNode normalized = normalizeJsonNodeRecursive(node, objectMapper);
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            // 파싱 실패 시 원본 반환
            return jsonString;
        }
    }
    
    /**
     * JSON 노드를 재귀적으로 정규화합니다.
     */
    private JsonNode normalizeJsonNodeRecursive(JsonNode node, ObjectMapper mapper) {
        if (node == null || node.isNull()) {
            return node;
        }
        
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            TreeMap<String, JsonNode> sortedMap = new TreeMap<>();
            
            // 모든 필드를 키 순서대로 정렬
            objectNode.fields().forEachRemaining(entry -> {
                // 중첩된 객체도 재귀적으로 정규화
                JsonNode normalizedValue = normalizeJsonNodeRecursive(entry.getValue(), mapper);
                sortedMap.put(entry.getKey(), normalizedValue);
            });
            
            // 정렬된 Map을 다시 JsonNode로 변환
            ObjectNode sortedNode = mapper.createObjectNode();
            sortedMap.forEach(sortedNode::set);
            return sortedNode;
        } else if (node.isArray()) {
            // 배열의 경우 각 요소를 정규화
            com.fasterxml.jackson.databind.node.ArrayNode arrayNode = mapper.createArrayNode();
            node.forEach(element -> {
                arrayNode.add(normalizeJsonNodeRecursive(element, mapper));
            });
            return arrayNode;
        } else {
            // 원시 값은 그대로 반환
            return node;
        }
    }
}


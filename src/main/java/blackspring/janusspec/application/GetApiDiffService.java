package blackspring.janusspec.application;

import blackspring.janusspec.application.dto.ApiDiffDetailRes;
import blackspring.janusspec.application.dto.ApiDiffLogSummaryRes;
import blackspring.janusspec.application.dto.ApiEndpointChangeDto;
import blackspring.janusspec.application.dto.ApiSchemaChangeDto;
import blackspring.janusspec.domain.ApiDiffEndpoint;
import blackspring.janusspec.domain.ApiDiffLog;
import blackspring.janusspec.domain.ApiDiffSchema;
import blackspring.janusspec.infrastructure.persistence.ApiDiffEndpointRepository;
import blackspring.janusspec.infrastructure.persistence.ApiDiffLogRepository;
import blackspring.janusspec.infrastructure.persistence.ApiDiffSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetApiDiffService implements GetApiDiff {

    private final ApiDiffLogRepository apiDiffLogRepository;
    private final ApiDiffEndpointRepository apiDiffEndpointRepository;
    private final ApiDiffSchemaRepository apiDiffSchemaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Page<ApiDiffLogSummaryRes> getAllDiffLogs(Pageable pageable) {
        return apiDiffLogRepository.findAllByOrderByIdDesc(pageable)
                .map(this::convertToSummary);
    }

    @Override
    public Page<ApiDiffLogSummaryRes> getDiffLogsByService(String serviceName, Pageable pageable) {
        return apiDiffLogRepository.findByServiceNameOrderByIdDesc(serviceName, pageable)
                .map(this::convertToSummary);
    }

    @Override
    public Optional<ApiDiffDetailRes> getDiffDetail(Long diffLogId) {
        return apiDiffLogRepository.findById(diffLogId)
                .map(diffLog -> {
                    ApiDiffLogSummaryRes summary = convertToSummary(diffLog);
                    
                    // 모든 변경 엔드포인트 조회
                    List<ApiDiffEndpoint> allEndpoints = apiDiffEndpointRepository.findByDiffLog(diffLog);
                    
                    // 타입별로 분류
                    List<ApiEndpointChangeDto> added = allEndpoints.stream()
                            .filter(e -> "ADDED".equals(e.getChangeType()))
                            .map(this::convertToChangeDto)
                            .collect(Collectors.toList());
                    
                    List<ApiEndpointChangeDto> removed = allEndpoints.stream()
                            .filter(e -> "REMOVED".equals(e.getChangeType()))
                            .map(this::convertToChangeDto)
                            .collect(Collectors.toList());
                    
                    List<ApiEndpointChangeDto> updated = allEndpoints.stream()
                            .filter(e -> "UPDATED".equals(e.getChangeType()))
                            .map(this::convertToChangeDto)
                            .collect(Collectors.toList());
                    
                    // 모든 변경 스키마 조회
                    List<ApiDiffSchema> allSchemas = apiDiffSchemaRepository.findByDiffLog(diffLog);
                    
                    // 타입별로 분류
                    List<ApiSchemaChangeDto> addedSchemas = allSchemas.stream()
                            .filter(s -> "ADDED".equals(s.getChangeType()))
                            .map(this::convertToSchemaChangeDto)
                            .collect(Collectors.toList());
                    
                    List<ApiSchemaChangeDto> removedSchemas = allSchemas.stream()
                            .filter(s -> "REMOVED".equals(s.getChangeType()))
                            .map(this::convertToSchemaChangeDto)
                            .collect(Collectors.toList());
                    
                    List<ApiSchemaChangeDto> updatedSchemas = allSchemas.stream()
                            .filter(s -> "UPDATED".equals(s.getChangeType()))
                            .map(this::convertToSchemaChangeDto)
                            .collect(Collectors.toList());
                    
                    return new ApiDiffDetailRes(
                            summary,
                            added,
                            removed,
                            updated,
                            addedSchemas,
                            removedSchemas,
                            updatedSchemas,
                            diffLog.getDiffJson()
                    );
                });
    }

    private ApiDiffLogSummaryRes convertToSummary(ApiDiffLog diffLog) {
        Map<String, Object> diffJson = parseDiffJson(diffLog.getDiffJson());
        Map<String, Integer> statistics = (Map<String, Integer>) diffJson.getOrDefault("statistics", Map.of());
        
        return new ApiDiffLogSummaryRes(
                diffLog.getId(),
                (String) diffJson.getOrDefault("serviceName", "unknown"),
                diffLog.getOldVersion() != null ? diffLog.getOldVersion().getId() : null,
                (String) diffJson.getOrDefault("oldVersionTag", ""),
                diffLog.getNewVersion() != null ? diffLog.getNewVersion().getId() : null,
                (String) diffJson.getOrDefault("newVersionTag", ""),
                statistics.getOrDefault("added", 0),
                statistics.getOrDefault("removed", 0),
                statistics.getOrDefault("updated", 0),
                statistics.getOrDefault("total", 0),
                diffLog.getCreateAt()
        );
    }

    private ApiEndpointChangeDto convertToChangeDto(ApiDiffEndpoint endpoint) {
        return new ApiEndpointChangeDto(
                endpoint.getPath(),
                endpoint.getHttpMethod(),
                endpoint.getChangeType(),
                endpoint.getBeforeJson(),
                endpoint.getAfterJson()
        );
    }

    private ApiSchemaChangeDto convertToSchemaChangeDto(ApiDiffSchema schema) {
        return new ApiSchemaChangeDto(
                schema.getSchemaName(),
                schema.getChangeType(),
                schema.getBeforeJson(),
                schema.getAfterJson()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDiffJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}


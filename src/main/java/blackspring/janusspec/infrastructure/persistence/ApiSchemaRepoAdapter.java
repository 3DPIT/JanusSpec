package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.application.port.apischema.ApiSchemaPort;
import blackspring.janusspec.domain.ApiSchema;
import blackspring.janusspec.domain.SwaggerVersion;
import blackspring.janusspec.infrastructure.adapter.json.Components;
import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class ApiSchemaRepoAdapter implements ApiSchemaPort {

    private final ApiSchemaRepository apiSchemaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void save(SwaggerVersion swaggerVersion, OpenApiSpec openApiSpec) {
        Components components = openApiSpec.getComponents();
        if (components == null || components.getSchemas() == null) {
            return;
        }

        JsonNode schemas = components.getSchemas();
        Iterator<String> schemaNames = schemas.fieldNames();

        while (schemaNames.hasNext()) {
            String schemaName = schemaNames.next();
            JsonNode schemaNode = schemas.get(schemaName);

            try {
                // JSON 키 순서를 보장하기 위해 정렬된 형태로 직렬화
                String normalizedRawSchema = normalizeJsonNode(schemaNode);
                
                // properties만 추출 (있는 경우) - 정규화된 버전 사용
                String propertiesJson = null;
                if (schemaNode.has("properties")) {
                    JsonNode propertiesNode = schemaNode.get("properties");
                    propertiesJson = normalizeJsonNode(propertiesNode);
                }

                ApiSchema apiSchema = ApiSchema.builder()
                        .swaggerVersion(swaggerVersion)
                        .name(schemaName)
                        .rawSchema(normalizedRawSchema)
                        .properties(propertiesJson)
                        .build();

                apiSchemaRepository.save(apiSchema);

                System.out.println("[SAVE SCHEMA] " + schemaName);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to save schema: " + schemaName + " - " + e.getMessage());
            }
        }
    }

    @Override
    public List<ApiSchema> findBySwaggerVersion(SwaggerVersion swaggerVersion) {
        return apiSchemaRepository.findBySwaggerVersion(swaggerVersion);
    }
    
    /**
     * JSON 노드를 정규화하여 키 순서를 보장합니다.
     * 같은 내용이라도 항상 같은 문자열이 되도록 보장합니다.
     * 중첩된 객체도 재귀적으로 정렬합니다.
     */
    private String normalizeJsonNode(JsonNode node) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode normalized = normalizeJsonNodeRecursive(node, mapper);
            return mapper.writeValueAsString(normalized);
        } catch (Exception e) {
            // 에러 발생 시 원본 toString 사용
            return node.toString();
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


package blackspring.janusspec.domain;

import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.Hashing;
import jakarta.persistence.*;
import lombok.*;

import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

@Entity
@Table(name = "swagger_version", 
       indexes = {
           @Index(name = "idx_service_url", columnList = "service_name, swagger_url")
       })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwaggerVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="service_name", length = 100, nullable = false)
    private String serviceName;

    @Column(name = "swagger_url", length = 500, nullable = false)
    private String swaggerUrl;

    @Column(name = "version_tag", length = 50, nullable = false)
    private String versionTag;

    @Column(columnDefinition = "TEXT")
    private String rawJson;

    @Column(length =  128)
    private String hash;

    public SwaggerVersion saveSwaggerVersion(String serviceName, String swaggerUrl, OpenApiSpec spec, String checkHash) {
        // JSON 키 순서를 보장하기 위해 정렬된 형태로 직렬화
        String normalizedPathsJson = normalizeJsonNode(spec.getPaths());
        
        String hash = Hashing.sha256()
                .hashString(normalizedPathsJson, StandardCharsets.UTF_8)
                .toString();

        if(hash.equals(checkHash)){
            return new SwaggerVersion();
        }

        SwaggerVersion version = SwaggerVersion.builder()
                .serviceName(serviceName)
                .swaggerUrl(swaggerUrl)
                .versionTag(spec.getInfo().getVersion())
                .rawJson(normalizedPathsJson)
                .hash(hash)
                .build();

        return version;
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

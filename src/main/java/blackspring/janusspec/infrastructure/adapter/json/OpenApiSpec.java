package blackspring.janusspec.infrastructure.adapter.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenApiSpec {
    private String openapi;
    private Info info;
    private JsonNode servers;
    private JsonNode security;
    private JsonNode tags;          // tags 정보 (선택적)
    private JsonNode paths;         // 가장 많이 사용
    private Components components;  // 내부에 schemas 있음
}

package blackspring.janusspec.infrastructure.adapter.json;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class OpenApiSpec {
    private String openapi;
    private Info info;
    private JsonNode servers;
    private JsonNode security;
    private JsonNode paths;         // 가장 많이 사용
    private Components components;  // 내부에 schemas 있음
}

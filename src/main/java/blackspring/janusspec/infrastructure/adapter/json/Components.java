package blackspring.janusspec.infrastructure.adapter.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Components {
    private JsonNode schemas;          // 여기를 JsonNode 로 두면 어떤 스키마도 자동 처리됨
    private JsonNode securitySchemes;
}

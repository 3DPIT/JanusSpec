package blackspring.janusspec.infrastructure.adapter.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Info {
    private String title;
    private String description;
    private String version;
    private JsonNode contact;  // contact 정보 (선택적)
}

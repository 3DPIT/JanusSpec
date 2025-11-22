package blackspring.janusspec.application.port.jsonparser;

import blackspring.janusspec.infrastructure.adapter.json.OpenApiSpec;

public interface JsonParserPort {
    OpenApiSpec saveApiSpecAll(String urls);

}

package blackspring.janusspec.infrastructure.adapter.json;

import blackspring.janusspec.application.port.jsonparser.JsonParserPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class JsonParserAdapter implements JsonParserPort {


    @Override
    public OpenApiSpec saveApiSpecAll(String urls) {
        System.out.println(urls);
        try {
            URL url = new URL(urls);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            InputStream is = conn.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(is);

            OpenApiSpec spec = mapper.readValue(json.toPrettyString(), OpenApiSpec.class);

            return spec;
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        }
    }
}

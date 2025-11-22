package blackspring.janusspec.infrastructure.web;

import blackspring.janusspec.application.SaveApiSpec;
import blackspring.janusspec.application.dto.SaveSwaggerApiReq;
import blackspring.janusspec.application.dto.SaveSwaggerApiRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SwaggerController {

    private final SaveApiSpec saveApiSpec;

    @PostMapping("/api/v1/swagger")
    public ResponseEntity<SaveSwaggerApiRes> saveSwaggerApi(@RequestBody SaveSwaggerApiReq req) {
        //"http://3dpit.iptime.org:8000/api/v1/members/api-docs/swagger"
        return ResponseEntity.ok(saveApiSpec.saveServiceApiSpec(req));
    }
}

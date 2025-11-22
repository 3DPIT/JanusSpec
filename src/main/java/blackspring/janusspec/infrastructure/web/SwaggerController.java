package blackspring.janusspec.infrastructure.web;

import blackspring.janusspec.application.SaveApiSpec;
import blackspring.janusspec.application.dto.GetLatestSwaggerApiRes;
import blackspring.janusspec.application.dto.GetServiceApiPathsRes;
import blackspring.janusspec.application.dto.SaveSwaggerApiReq;
import blackspring.janusspec.application.dto.SaveSwaggerApiRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SwaggerController {

    private final SaveApiSpec saveApiSpec;

    @PostMapping("/api/v1/swagger")
    public ResponseEntity<SaveSwaggerApiRes> saveSwaggerApi(@RequestBody SaveSwaggerApiReq req) {
        //"http://3dpit.iptime.org:8000/api/v1/members/api-docs/swagger"
        return ResponseEntity.ok(saveApiSpec.saveServiceApiSpec(req));
    }

    @GetMapping("/api/v1/swagger/latest")
    public ResponseEntity<GetLatestSwaggerApiRes> getLatestSwaggerApi() {
        return saveApiSpec.getLatestSwaggerApi()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/swagger/{serviceName}/paths")
    public ResponseEntity<GetServiceApiPathsRes> getServiceApiPaths(@PathVariable String serviceName) {
        return saveApiSpec.getServiceApiPaths(serviceName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

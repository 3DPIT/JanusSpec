package blackspring.janusspec.infrastructure.web;

import blackspring.janusspec.application.SaveApiSpec;
import blackspring.janusspec.application.dto.GetLatestSwaggerApiRes;
import blackspring.janusspec.application.dto.GetServiceApiPathsRes;
import blackspring.janusspec.application.dto.SaveSwaggerApiReq;
import blackspring.janusspec.application.dto.SaveSwaggerApiRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Swagger API 관리", description = "외부 Swagger API 수집 및 버전 관리 API")
@RestController
@RequiredArgsConstructor
public class SwaggerController {

    private final SaveApiSpec saveApiSpec;

    @Operation(
            summary = "외부 Swagger API 수집 및 저장",
            description = "외부 서비스의 Swagger JSON을 수집하여 저장하고, 이전 버전과 비교하여 변경사항을 추적합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공적으로 저장됨",
                    content = @Content(schema = @Schema(implementation = SaveSwaggerApiRes.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/api/v1/swagger")
    public ResponseEntity<SaveSwaggerApiRes> saveSwaggerApi(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Swagger API URL 및 서비스명",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SaveSwaggerApiReq.class))
            )
            @RequestBody SaveSwaggerApiReq req) {
        return ResponseEntity.ok(saveApiSpec.saveServiceApiSpec(req));
    }

    @Operation(
            summary = "최신 Swagger API 정보 조회",
            description = "가장 최근에 저장된 Swagger API 버전 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetLatestSwaggerApiRes.class))
            ),
            @ApiResponse(responseCode = "404", description = "데이터 없음")
    })
    @GetMapping("/api/v1/swagger/latest")
    public ResponseEntity<GetLatestSwaggerApiRes> getLatestSwaggerApi() {
        return saveApiSpec.getLatestSwaggerApi()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "특정 서비스의 모든 API 경로 조회",
            description = "서비스명으로 해당 서비스의 최신 버전 모든 API 엔드포인트를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetServiceApiPathsRes.class))
            ),
            @ApiResponse(responseCode = "404", description = "서비스를 찾을 수 없음")
    })
    @GetMapping("/api/v1/swagger/{serviceName}/paths")
    public ResponseEntity<GetServiceApiPathsRes> getServiceApiPaths(
            @Parameter(description = "서비스 이름", example = "users", required = true)
            @PathVariable String serviceName) {
        return saveApiSpec.getServiceApiPaths(serviceName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

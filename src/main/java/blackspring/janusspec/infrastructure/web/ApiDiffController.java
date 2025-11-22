package blackspring.janusspec.infrastructure.web;

import blackspring.janusspec.application.GetApiDiff;
import blackspring.janusspec.application.dto.ApiDiffDetailRes;
import blackspring.janusspec.application.dto.ApiDiffLogSummaryRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "API 변경 이력 조회", description = "API 버전 간 변경 이력을 조회하는 API")
@RestController
@RequestMapping("/api/v1/diff")
@RequiredArgsConstructor
public class ApiDiffController {

    private final GetApiDiff getApiDiff;

    @Operation(
            summary = "전체 변경 이력 조회",
            description = "모든 서비스의 API 변경 이력을 최신순으로 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping
    public ResponseEntity<Page<ApiDiffLogSummaryRes>> getAllDiffLogs(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(getApiDiff.getAllDiffLogs(pageable));
    }

    @Operation(
            summary = "특정 서비스의 변경 이력 조회",
            description = "특정 서비스의 API 변경 이력만 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<Page<ApiDiffLogSummaryRes>> getDiffLogsByService(
            @Parameter(description = "서비스 이름", example = "users", required = true)
            @PathVariable String serviceName,
            
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(getApiDiff.getDiffLogsByService(serviceName, pageable));
    }

    @Operation(
            summary = "변경 상세 정보 조회",
            description = "특정 Diff Log의 상세 변경 내역을 조회합니다. 추가/삭제/수정된 모든 엔드포인트의 상세 정보를 포함합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiDiffDetailRes.class))
            ),
            @ApiResponse(responseCode = "404", description = "Diff Log를 찾을 수 없음")
    })
    @GetMapping("/{diffLogId}")
    public ResponseEntity<ApiDiffDetailRes> getDiffDetail(
            @Parameter(description = "Diff Log ID", example = "1", required = true)
            @PathVariable Long diffLogId) {
        
        return getApiDiff.getDiffDetail(diffLogId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}


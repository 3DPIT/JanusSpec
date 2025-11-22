package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionPort;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionReq;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionRes;
import blackspring.janusspec.domain.SwaggerVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SwaggerVersionRepoAdapter implements SwaggerVersionPort {
    private final SwaggerVersionRepository swaggerVersionRepository;

    @Override
    public SwaggerVersionRes save(SwaggerVersionReq req) {
        // 서비스명과 URL로 최신 버전 조회
        SwaggerVersion byServiceAndUrl = swaggerVersionRepository
                .findTopByServiceNameAndSwaggerUrlOrderByIdDesc(req.serviceName(), req.swaggerUrl())
                .orElse(new SwaggerVersion());
        
        SwaggerVersion swaggerVersion = new SwaggerVersion();
        SwaggerVersion checkHash = swaggerVersion.saveSwaggerVersion(
                req.serviceName(), 
                req.swaggerUrl(), 
                req.openApiSpec(), 
                byServiceAndUrl.getHash()
        );

        SwaggerVersion save = swaggerVersionRepository.save(checkHash.getHash()==null ? byServiceAndUrl : checkHash);
        
        Long oldVersionId = (checkHash.getHash() != null && byServiceAndUrl.getId() != null) ? byServiceAndUrl.getId() : null;
    
        return new SwaggerVersionRes(save.getId(), oldVersionId, "성공적으로 저장완료", checkHash.getHash()==null?true:false);
    }

    @Override
    public Optional<SwaggerVersion> findLatest() {
        return swaggerVersionRepository.findTopByOrderByIdDesc();
    }

    @Override
    public Optional<SwaggerVersion> findLatestByServiceName(String serviceName) {
        return swaggerVersionRepository.findTopByServiceNameOrderByIdDesc(serviceName);
    }

    @Override
    public Optional<SwaggerVersion> findById(Long id) {
        return swaggerVersionRepository.findById(id);
    }
}

package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionPort;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionReq;
import blackspring.janusspec.application.port.swaggerversion.SwaggerVersionRes;
import blackspring.janusspec.domain.SwaggerVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SwaggerVersionRepoAdapter implements SwaggerVersionPort {
    private final SwaggerVersionRepository swaggerVersionRepository;

    @Override
    public SwaggerVersionRes save(SwaggerVersionReq req) {
        SwaggerVersion bySwaggerUrl = swaggerVersionRepository.findBySwaggerUrl(req.swaggerUrl()).orElse(new SwaggerVersion());
        SwaggerVersion swaggerVersion = new SwaggerVersion();
        SwaggerVersion checkHash = swaggerVersion.saveSwaggerVersion(req.serviceName(), req.swaggerUrl(), req.openApiSpec(), bySwaggerUrl.getHash());

        SwaggerVersion save = swaggerVersionRepository.save(checkHash.getHash()==null?bySwaggerUrl:checkHash);
    
        return new SwaggerVersionRes(save.getId(),"성공적으로 저장완료",checkHash.getHash()==null?true:false);
    }
}

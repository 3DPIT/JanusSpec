package blackspring.janusspec.application;

import blackspring.janusspec.application.dto.GetLatestSwaggerApiRes;
import blackspring.janusspec.application.dto.GetServiceApiPathsRes;
import blackspring.janusspec.application.dto.SaveSwaggerApiReq;
import blackspring.janusspec.application.dto.SaveSwaggerApiRes;

import java.util.Optional;

public interface SaveApiSpec {
    SaveSwaggerApiRes saveServiceApiSpec(SaveSwaggerApiReq req);
    Optional<GetLatestSwaggerApiRes> getLatestSwaggerApi();
    Optional<GetServiceApiPathsRes> getServiceApiPaths(String serviceName);
}

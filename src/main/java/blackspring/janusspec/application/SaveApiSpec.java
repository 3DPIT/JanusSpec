package blackspring.janusspec.application;

import blackspring.janusspec.application.dto.SaveSwaggerApiReq;
import blackspring.janusspec.application.dto.SaveSwaggerApiRes;

public interface SaveApiSpec {
    SaveSwaggerApiRes saveServiceApiSpec(SaveSwaggerApiReq req);
}

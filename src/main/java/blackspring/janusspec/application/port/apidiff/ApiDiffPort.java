package blackspring.janusspec.application.port.apidiff;

import blackspring.janusspec.domain.ApiDiffLog;
import blackspring.janusspec.domain.SwaggerVersion;

public interface ApiDiffPort {
    ApiDiffLog saveDiff(SwaggerVersion oldVersion, SwaggerVersion newVersion);
}


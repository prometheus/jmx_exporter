package io.prometheus.jmx.test.support;

public class HealthyResponse extends BaseResponse {

    private static final String CONTENT = "Exporter is Healthy.";

    public static final Response RESULT_200 = new BaseResponse().withCode(200).withContent(CONTENT);
    public static final Response RESULT_401 = new BaseResponse().withCode(401);
}

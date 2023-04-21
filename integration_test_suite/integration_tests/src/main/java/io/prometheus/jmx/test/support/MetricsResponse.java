package io.prometheus.jmx.test.support;

public class MetricsResponse extends BaseResponse {

    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    public static final BaseResponse RESULT_200 = new BaseResponse().withCode(200).withContentType(CONTENT_TYPE);
    public static final BaseResponse RESULT_401 = new BaseResponse().withCode(401);

}

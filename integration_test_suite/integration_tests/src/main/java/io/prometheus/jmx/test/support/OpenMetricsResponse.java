package io.prometheus.jmx.test.support;

public class OpenMetricsResponse extends BaseResponse {

    private static String CONTENT_TYPE = "application/openmetrics-text; version=1.0.0; charset=utf-8";

    public static final BaseResponse RESULT_200 = new BaseResponse().withCode(200).withContentType(CONTENT_TYPE);
}

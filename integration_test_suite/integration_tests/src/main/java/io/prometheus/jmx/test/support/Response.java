package io.prometheus.jmx.test.support;

import okhttp3.Headers;

public interface Response {

    /**
     * Method to get the response code
     *
     * @return
     */
    int code();

    /**
     * Method to get the response Headers
     *
     * @return
     */
    Headers headers();

    /**
     * Method to get the response content
     *
     * @return
     */
    String content();

    /**
     * Method to compare whether this Response is equals to another Object
     *
     * @param response
     * @return
     */
    Response isSuperset(Response response);

    /**
     * Method to dispatch the response code to a CodeConsumer
     *
     * @param consume
     * @return
     */
    Response dispatch(CodeConsumer consume);

    /**
     * Method to dispatch the response Headers to a HeadersConsumer
     *
     * @param consumer
     * @return
     */
    Response dispatch(HeadersConsumer consumer);

    /**
     * Method to dispatch the response content to a ContentConsumer
     *
     * @param consumer
     * @return
     */
    Response dispatch(ContentConsumer consumer);
}

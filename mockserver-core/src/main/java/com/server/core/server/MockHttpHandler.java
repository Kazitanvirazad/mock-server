package com.server.core.server;

import com.server.core.constants.Method;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static com.server.core.constants.CommonConstants.DEFAULT_RESPONSE_LENGTH;
import static com.server.core.constants.CommonConstants.METHOD_NOT_ALLOWED_HTTP_CODE;

/**
 * {@link HttpHandler} for handling all the mock server's {@link HttpExchange} request and response
 *
 * @param endpointInitiator {@link EndpointInitiator} data to used for setting {@link HttpExchange} data
 * @author Kazi Tanvir Azad
 */
public record MockHttpHandler(EndpointInitiator endpointInitiator) implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<Method, MethodInitiator> methods = endpointInitiator.getMethods();
        Method inputMethod;
        try (OutputStream outputStream = exchange.getResponseBody()) {
            String exchangeMethod = exchange.getRequestMethod().trim().toUpperCase();
            inputMethod = Method.valueOf(exchangeMethod);
            // checking if exchange has valid http method
            if (methods.containsKey(inputMethod)) {
                MethodInitiator methodInitiator = methods.get(inputMethod);
                //adding headers to the response
                methodInitiator.getHeaders().forEach(header ->
                        exchange.getResponseHeaders().add(header.key(), header.value()));
                byte[] responseBody = methodInitiator.getResponseData();
                long responseLength;
                // getting response content length
                if (ArrayUtils.isNotEmpty(responseBody)) {
                    responseLength = responseBody.length;
                } else {
                    responseLength = DEFAULT_RESPONSE_LENGTH;
                }
                // adding http response code and response content length
                exchange.sendResponseHeaders(methodInitiator.getResponseCode(), responseLength);
                // adding delay
                if (methodInitiator.getDelay() > 0) {
                    try {
                        Thread.sleep(methodInitiator.getDelay());
                    } catch (Exception ignore) {
                    }
                }
                // adding response body
                outputStream.write(responseBody);
            } else {
                // sending method not allowed error
                exchange.sendResponseHeaders(METHOD_NOT_ALLOWED_HTTP_CODE, DEFAULT_RESPONSE_LENGTH);
            }
        } catch (Exception exception) {
            // sending method not allowed error
            exchange.sendResponseHeaders(METHOD_NOT_ALLOWED_HTTP_CODE, DEFAULT_RESPONSE_LENGTH);
        }
    }
}

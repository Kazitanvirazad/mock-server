package com.server.core.function;

import com.server.core.server.EndpointInitiator;
import com.server.core.server.MockHttpHandler;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.util.function.BiConsumer;

/**
 * {@link BiConsumer} for setting {@link HttpServer} context
 *
 * @param httpServer {@link HttpServer} where the context will be created
 * @author Kazi Tanvir Azad
 */
public record EndpointBiConsumer(HttpServer httpServer) implements BiConsumer<String, EndpointInitiator> {
    @Override
    public void accept(String url, EndpointInitiator endpointInitiator) {
        HttpHandler httpHandler = new MockHttpHandler(endpointInitiator);
        this.httpServer.createContext(url, httpHandler);
    }
}

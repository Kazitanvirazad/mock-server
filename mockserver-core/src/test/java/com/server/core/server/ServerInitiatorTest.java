package com.server.core.server;

import com.server.core.config.CommonConfig;
import com.server.core.constants.Method;
import com.server.core.model.data.Server;
import com.server.core.notification.Notification;
import com.server.core.support.ReflectionTestUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;

import static com.server.core.constants.CommonConstants.DEFAULT_RESPONSE_LENGTH;
import static com.server.core.constants.CommonConstants.METHOD_NOT_ALLOWED_HTTP_CODE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServerInitiatorTest {
    private Notification notification;
    private Notification originalNotification;

    @BeforeEach
    void setUp() {
        notification = mock(Notification.class);
        originalNotification = CommonConfig.INSTANCE.notification();
        CommonConfig.INSTANCE.setNotification(notification);
    }

    @AfterEach
    void tearDown() {
        CommonConfig.INSTANCE.setNotification(originalNotification);
    }

    @Test
    void addEndpointOverridesExistingMethodWhenConfirmed() {
        ServerInitiator serverInitiator = new ServerInitiator(8080);
        ObservableSet<String> activeServerIds = FXCollections.observableSet("SER-OLD");
        Server originalServer = server("SER-OLD", Method.GET, "/orders", "old");
        Server replacementServer = server("SER-NEW", Method.GET, "/orders", "new");

        serverInitiator.addEndpoint(originalServer, activeServerIds);
        when(notification.triggerConfirmationPrompt(anyString(), anyString())).thenReturn(true);

        serverInitiator.addEndpoint(replacementServer, activeServerIds);

        EndpointInitiator endpointInitiator = endpoints(serverInitiator).get("/orders");
        assertNotNull(endpointInitiator);
        assertFalse(activeServerIds.contains("SER-OLD"));
        assertArrayEquals("new".getBytes(StandardCharsets.UTF_8),
                endpointInitiator.getMethods().get(Method.GET).getResponseData());
        verify(notification).triggerConfirmationPrompt(anyString(), anyString());
    }

    @Test
    void addEndpointAddsAdditionalMethodAndRejectsOverrideWhenDeclined() {
        ServerInitiator serverInitiator = new ServerInitiator(8080);
        ObservableSet<String> activeServerIds = FXCollections.observableSet();
        Server getServer = server("SER-1", Method.GET, "/orders", "get");
        Server postServer = server("SER-2", Method.POST, "/orders", "post");
        Server duplicateGetServer = server("SER-3", Method.GET, "/orders", "duplicate");

        serverInitiator.addEndpoint(getServer, activeServerIds);
        serverInitiator.addEndpoint(postServer, activeServerIds);
        when(notification.triggerConfirmationPrompt(anyString(), anyString())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> serverInitiator.addEndpoint(duplicateGetServer, activeServerIds));
        assertEquals(2, endpoints(serverInitiator).get("/orders").getMethods().size());
    }

    @Test
    void addEndpointOverridesWithoutRemovingActiveIdWhenPreviousServerIdIsMissing() {
        ServerInitiator serverInitiator = new ServerInitiator(8080);
        ObservableSet<String> activeServerIds = FXCollections.observableSet("SER-OLD");
        EndpointInitiator existingEndpoint = new EndpointInitiator();
        MethodInitiator methodInitiator = new MethodInitiator();
        methodInitiator.setResponseData("old".getBytes(StandardCharsets.UTF_8));
        existingEndpoint.getMethods().put(Method.GET, methodInitiator);
        existingEndpoint.setUrlEndpoint("/orders");
        endpoints(serverInitiator).put("/orders", existingEndpoint);
        when(notification.triggerConfirmationPrompt(anyString(), anyString())).thenReturn(true);

        serverInitiator.addEndpoint(server("SER-NEW", Method.GET, "/orders", "new"), activeServerIds);

        assertTrue(activeServerIds.contains("SER-OLD"));
        assertArrayEquals("new".getBytes(StandardCharsets.UTF_8),
                endpoints(serverInitiator).get("/orders").getMethods().get(Method.GET).getResponseData());
    }

    @Test
    void removeEndpointRestartsServerWhenOtherEndpointsRemain() throws Exception {
        ServerInitiator serverInitiator = new ServerInitiator(8080);
        Server firstServer = server("SER-1", Method.GET, "/orders", "ok");
        Server secondServer = server("SER-2", Method.GET, "/payments", "done");
        HttpServer oldHttpServer = mock(HttpServer.class);
        HttpServer restartedHttpServer = mock(HttpServer.class);

        serverInitiator.addEndpoint(firstServer, FXCollections.observableSet());
        serverInitiator.addEndpoint(secondServer, FXCollections.observableSet());
        ReflectionTestUtil.setField(serverInitiator, "httpServer", oldHttpServer);

        try (MockedStatic<HttpServer> httpServerMock = mockStatic(HttpServer.class)) {
            httpServerMock.when(() -> HttpServer.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0)))
                    .thenReturn(restartedHttpServer);

            serverInitiator.removeEndpoint(firstServer, true);
        }

        verify(oldHttpServer).stop(1);
        verify(restartedHttpServer).createContext(org.mockito.ArgumentMatchers.eq("/payments"),
                org.mockito.ArgumentMatchers.any());
        verify(restartedHttpServer).start();
        assertFalse(serverInitiator.isServerStopped());
    }

    @Test
    void removeEndpointRemovesLastEndpointAndStopsServer() {
        ServerInitiator serverInitiator = new ServerInitiator(8080);
        Server server = server("SER-1", Method.GET, "/orders", "ok");
        HttpServer httpServer = mock(HttpServer.class);

        serverInitiator.addEndpoint(server, FXCollections.observableSet());
        ReflectionTestUtil.setField(serverInitiator, "httpServer", httpServer);

        serverInitiator.removeEndpoint(server, true);

        verify(httpServer).stop(1);
        assertTrue(serverInitiator.isServerStopped());
    }

    @Test
    void removeEndpointIgnoresUnknownEndpointAfterStoppingServer() {
        ServerInitiator serverInitiator = new ServerInitiator(8080);
        HttpServer httpServer = mock(HttpServer.class);
        ReflectionTestUtil.setField(serverInitiator, "httpServer", httpServer);

        serverInitiator.removeEndpoint(server("SER-X", Method.GET, "/missing", "ok"), true);

        verify(httpServer).stop(1);
        assertTrue(serverInitiator.isServerStopped());
    }

    @Test
    void startServerCreatesContextsAndStartsHttpServer() throws Exception {
        ServerInitiator serverInitiator = new ServerInitiator(9090);
        HttpServer httpServer = mock(HttpServer.class);

        serverInitiator.addEndpoint(server("SER-1", Method.GET, "/orders", "ok"), FXCollections.observableSet());
        serverInitiator.addEndpoint(server("SER-2", Method.POST, "/payments", "done"), FXCollections.observableSet());

        try (MockedStatic<HttpServer> httpServerMock = mockStatic(HttpServer.class)) {
            httpServerMock.when(() -> HttpServer.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0)))
                    .thenReturn(httpServer);

            serverInitiator.startServer(true);
        }

        verify(httpServer).createContext(org.mockito.ArgumentMatchers.eq("/orders"),
                org.mockito.ArgumentMatchers.any());
        verify(httpServer).createContext(org.mockito.ArgumentMatchers.eq("/payments"),
                org.mockito.ArgumentMatchers.any());
        verify(httpServer, times(2)).setExecutor(null);
        verify(httpServer).start();
    }

    @Test
    void startServerWithoutEndpointsSkipsContextInitialization() throws Exception {
        ServerInitiator serverInitiator = new ServerInitiator(9090);
        HttpServer httpServer = mock(HttpServer.class);

        try (MockedStatic<HttpServer> httpServerMock = mockStatic(HttpServer.class)) {
            httpServerMock.when(() -> HttpServer.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0)))
                    .thenReturn(httpServer);

            serverInitiator.startServer(true);
        }

        verify(httpServer).start();
        verify(httpServer, times(2)).setExecutor(null);
        org.mockito.Mockito.verify(httpServer, org.mockito.Mockito.never())
                .createContext(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void startServerNotifiesAndThrowsWhenInitializationFails() throws Exception {
        ServerInitiator serverInitiator = new ServerInitiator(9090);
        serverInitiator.addEndpoint(server("SER-1", Method.GET, "/orders", "ok"), FXCollections.observableSet());

        try (MockedStatic<HttpServer> httpServerMock = mockStatic(HttpServer.class)) {
            httpServerMock.when(() -> HttpServer.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0)))
                    .thenThrow(new IOException("boom"));

            RuntimeException actual = assertThrows(RuntimeException.class, () -> serverInitiator.startServer(false));

            assertEquals("java.io.IOException: boom", actual.getMessage());
        }

        verify(notification).triggerErrorNotification("Something went wrong while initializing server", "boom");
    }

    @Test
    void startServerFailsSilentlyWhenRequested() throws Exception {
        ServerInitiator serverInitiator = new ServerInitiator(9090);

        try (MockedStatic<HttpServer> httpServerMock = mockStatic(HttpServer.class)) {
            httpServerMock.when(() -> HttpServer.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0)))
                    .thenThrow(new IOException("boom"));

            assertThrows(RuntimeException.class, () -> serverInitiator.startServer(true));
        }

        verifyNoInteractions(notification);
    }

    @Test
    void restartServerStopsExistingServerAndStartsAgain() throws Exception {
        ServerInitiator serverInitiator = new ServerInitiator(9090);
        HttpServer stoppedHttpServer = mock(HttpServer.class);
        HttpServer restartedHttpServer = mock(HttpServer.class);
        serverInitiator.addEndpoint(server("SER-1", Method.GET, "/orders", "ok"), FXCollections.observableSet());
        ReflectionTestUtil.setField(serverInitiator, "httpServer", stoppedHttpServer);

        try (MockedStatic<HttpServer> httpServerMock = mockStatic(HttpServer.class)) {
            httpServerMock.when(() -> HttpServer.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0)))
                    .thenReturn(restartedHttpServer);

            serverInitiator.restartServer(true);
        }

        verify(stoppedHttpServer).stop(1);
        verify(restartedHttpServer).start();
    }

    @Test
    void mockHttpHandlerWritesResponseAndHeadersForSupportedMethod() throws Exception {
        EndpointInitiator endpointInitiator = new EndpointInitiator();
        MethodInitiator methodInitiator = new MethodInitiator();
        methodInitiator.setResponseCode(202);
        methodInitiator.setDelay(1L);
        methodInitiator.setResponseData("ok".getBytes(StandardCharsets.UTF_8));
        methodInitiator.addHeader(new KeyValue("X-Test", "yes"));
        endpointInitiator.setMethods(new java.util.HashMap<>());
        endpointInitiator.getMethods().put(Method.GET, methodInitiator);

        HttpExchange exchange = mock(HttpExchange.class);
        Headers responseHeaders = new Headers();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(exchange.getRequestMethod()).thenReturn("get");
        when(exchange.getResponseBody()).thenReturn(outputStream);
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);

        httpHandler(endpointInitiator).handle(exchange);

        verify(exchange).sendResponseHeaders(202, 2L);
        assertEquals("yes", responseHeaders.getFirst("X-Test"));
        assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), outputStream.toByteArray());
    }

    @Test
    void mockHttpHandlerReturnsMethodNotAllowedForUnsupportedAndInvalidMethods() throws Exception {
        EndpointInitiator endpointInitiator = new EndpointInitiator();
        MethodInitiator methodInitiator = new MethodInitiator();
        methodInitiator.setResponseCode(200);
        methodInitiator.setResponseData(new byte[0]);
        endpointInitiator.setMethods(new java.util.HashMap<>());
        endpointInitiator.getMethods().put(Method.GET, methodInitiator);

        HttpExchange unsupportedMethodExchange = mock(HttpExchange.class);
        when(unsupportedMethodExchange.getRequestMethod()).thenReturn("POST");
        when(unsupportedMethodExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());

        httpHandler(endpointInitiator).handle(unsupportedMethodExchange);

        verify(unsupportedMethodExchange).sendResponseHeaders(METHOD_NOT_ALLOWED_HTTP_CODE, DEFAULT_RESPONSE_LENGTH);

        HttpExchange invalidMethodExchange = mock(HttpExchange.class);
        when(invalidMethodExchange.getRequestMethod()).thenReturn("CONNECT");
        when(invalidMethodExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());

        httpHandler(endpointInitiator).handle(invalidMethodExchange);

        verify(invalidMethodExchange).sendResponseHeaders(METHOD_NOT_ALLOWED_HTTP_CODE, DEFAULT_RESPONSE_LENGTH);
    }

    @Test
    void mockHttpHandlerUsesDefaultLengthForEmptyResponses() throws Exception {
        EndpointInitiator endpointInitiator = new EndpointInitiator();
        MethodInitiator methodInitiator = new MethodInitiator();
        methodInitiator.setResponseCode(204);
        methodInitiator.setDelay(0L);
        methodInitiator.setResponseData(new byte[0]);
        endpointInitiator.setMethods(new java.util.HashMap<>());
        endpointInitiator.getMethods().put(Method.GET, methodInitiator);

        HttpExchange exchange = mock(HttpExchange.class);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        when(exchange.getResponseHeaders()).thenReturn(new Headers());

        httpHandler(endpointInitiator).handle(exchange);

        verify(exchange).sendResponseHeaders(204, DEFAULT_RESPONSE_LENGTH);
    }

    private static com.sun.net.httpserver.HttpHandler httpHandler(EndpointInitiator endpointInitiator) throws Exception {
        Class<?> handlerClass = Class.forName("com.server.core.server.MockHttpHandler");
        Constructor<?> constructor = handlerClass.getDeclaredConstructor(EndpointInitiator.class);
        constructor.setAccessible(true);
        return (com.sun.net.httpserver.HttpHandler) constructor.newInstance(endpointInitiator);
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<String, EndpointInitiator> endpoints(ServerInitiator serverInitiator) {
        return (java.util.Map<String, EndpointInitiator>) ReflectionTestUtil.getField(serverInitiator, "endPoints");
    }

    private static Server server(String serverId, Method method, String endpoint, String responseData) {
        Server server = new Server();
        server.setServerId(serverId);
        server.setServerName("Server-" + serverId);
        server.setMethod(method);
        server.setUrlEndpoint(endpoint);
        server.setResponseCode(201);
        server.setDelay(25L);
        server.setResponseData(responseData);
        return server;
    }
}

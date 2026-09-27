package com.server.core.server;

import com.server.core.config.CommonConfig;
import com.server.core.constants.Method;
import com.server.core.model.data.Server;
import com.server.core.notification.Notification;
import com.server.core.service.ServerService;
import com.server.core.support.ReflectionTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServerManagerTest {
    private Notification notification;
    private Notification originalNotification;
    private ServerService originalServerService;

    @BeforeEach
    void setUp() {
        notification = mock(Notification.class);
        originalNotification = CommonConfig.INSTANCE.notification();
        CommonConfig.INSTANCE.setNotification(notification);
        originalServerService = (ServerService) ReflectionTestUtil.getField(ServerManager.INSTANCE, "serverService");
        activeServers().clear();
        activeServerIds().clear();
    }

    @AfterEach
    void tearDown() {
        activeServers().clear();
        activeServerIds().clear();
        ReflectionTestUtil.setField(ServerManager.INSTANCE, "serverService", originalServerService);
        CommonConfig.INSTANCE.setNotification(originalNotification);
    }

    @Test
    void startServerRejectsInvalidServer() {
        Server invalidServer = new Server();
        invalidServer.setServerId(null);

        ServerManager.INSTANCE.startServer(invalidServer, false);

        verify(notification).triggerErrorNotification("Invalid Server", """
                Invalid Server selection.
                Try again later.""");
        assertFalse(ServerManager.INSTANCE.hasAnyActiveServer());
    }

    @Test
    void startServerRejectsInvalidServerSilently() {
        Server invalidServer = new Server();
        invalidServer.setServerId(null);

        ServerManager.INSTANCE.startServer(invalidServer, true);

        verifyNoInteractions(notification);
    }

    @Test
    void startServerRejectsAlreadyActiveServer() {
        Server server = server("SER-1", 8080);
        activeServerIds().add("SER-1");

        ServerManager.INSTANCE.startServer(server, false);

        verify(notification).triggerErrorNotification("Server is already running", """
                This server is already running.
                Try staring another server.""");
    }

    @Test
    void startServerRejectsAlreadyActiveServerSilently() {
        Server server = server("SER-1", 8080);
        activeServerIds().add("SER-1");

        ServerManager.INSTANCE.startServer(server, true);

        verifyNoInteractions(notification);
    }

    @Test
    void startServerCreatesNewInitiatorForFreshPort() {
        Server server = server("SER-2", 8181);

        try (MockedConstruction<ServerInitiator> serverInitiatorConstruction = mockConstruction(ServerInitiator.class)) {
            ServerManager.INSTANCE.startServer(server, false);

            ServerInitiator serverInitiator = serverInitiatorConstruction.constructed().getFirst();
            verify(serverInitiator).addEndpoint(server, ServerManager.INSTANCE.getActiveServerIds());
            verify(serverInitiator).startServer(false);
            assertTrue(ServerManager.INSTANCE.isServerActive("SER-2"));
            assertTrue(activeServers().containsKey(8181));
        }
    }

    @Test
    void startServerMarksServerActiveWhenPortEntryContainsNullInitiator() {
        Server server = server("SER-2B", 8182);
        activeServers().put(8182, null);

        ServerManager.INSTANCE.startServer(server, true);

        assertTrue(ServerManager.INSTANCE.isServerActive("SER-2B"));
        verifyNoInteractions(notification);
    }

    @Test
    void startServerRestartsExistingInitiatorForSamePort() {
        Server server = server("SER-3", 8282);
        ServerInitiator serverInitiator = mock(ServerInitiator.class);
        activeServers().put(8282, serverInitiator);

        ServerManager.INSTANCE.startServer(server, false);

        verify(serverInitiator).addEndpoint(server, ServerManager.INSTANCE.getActiveServerIds());
        verify(serverInitiator).restartServer(false);
        assertTrue(ServerManager.INSTANCE.isServerActive("SER-3"));
    }

    @Test
    void startServerSwallowsInitiatorExceptions() {
        Server server = server("SER-3B", 8283);

        try (MockedConstruction<ServerInitiator> ignored = mockConstruction(ServerInitiator.class,
                (mock, context) -> org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                        .when(mock).addEndpoint(server, ServerManager.INSTANCE.getActiveServerIds()))) {
            ServerManager.INSTANCE.startServer(server, false);
        }

        assertFalse(ServerManager.INSTANCE.isServerActive("SER-3B"));
        verifyNoInteractions(notification);
    }

    @Test
    void stopServerRejectsInactiveServer() {
        Server server = server("SER-4", 8383);

        ServerManager.INSTANCE.stopServer(server, false);

        verify(notification).triggerErrorNotification("Server is not running", """
                This server is not running.
                Try stopping a server already running.""");
    }

    @Test
    void stopServerRejectsInvalidServerSilently() {
        Server invalidServer = new Server();
        invalidServer.setServerId(null);

        ServerManager.INSTANCE.stopServer(invalidServer, true);

        verifyNoInteractions(notification);
    }

    @Test
    void stopServerRejectsInactiveServerSilently() {
        ServerManager.INSTANCE.stopServer(server("SER-4B", 8384), true);

        verifyNoInteractions(notification);
    }

    @Test
    void stopServerRemovesTrackedStateAndInitiator() {
        Server server = server("SER-5", 8484);
        ServerInitiator serverInitiator = mock(ServerInitiator.class);
        when(serverInitiator.isServerStopped()).thenReturn(true);
        activeServerIds().add("SER-5");
        activeServers().put(8484, serverInitiator);

        ServerManager.INSTANCE.stopServer(server, false);

        verify(serverInitiator).removeEndpoint(server, false);
        assertFalse(ServerManager.INSTANCE.isServerActive("SER-5"));
        assertFalse(activeServers().containsKey(8484));
    }

    @Test
    void stopServerKeepsInitiatorWhenItIsStillRunning() {
        Server server = server("SER-5B", 8485);
        ServerInitiator serverInitiator = mock(ServerInitiator.class);
        when(serverInitiator.isServerStopped()).thenReturn(false);
        activeServerIds().add("SER-5B");
        activeServers().put(8485, serverInitiator);

        ServerManager.INSTANCE.stopServer(server, true);

        assertFalse(ServerManager.INSTANCE.isServerActive("SER-5B"));
        assertTrue(activeServers().containsKey(8485));
        verifyNoInteractions(notification);
    }

    @Test
    void stopServerLeavesTrackedIdWhenInitiatorEntryIsMissing() {
        Server server = server("SER-5C", 8486);
        activeServerIds().add("SER-5C");

        ServerManager.INSTANCE.stopServer(server, true);

        assertTrue(ServerManager.INSTANCE.isServerActive("SER-5C"));
        verifyNoInteractions(notification);
    }

    @Test
    void stopAllServersStopsOnlyResolvedActiveServers() {
        ServerService serverService = mock(ServerService.class);
        Server server = server("SER-6", 8585);
        ServerInitiator serverInitiator = mock(ServerInitiator.class);
        when(serverInitiator.isServerStopped()).thenReturn(true);
        when(serverService.getServerById("SER-6")).thenReturn(Optional.of(server));
        when(serverService.getServerById("")).thenReturn(Optional.empty());
        ReflectionTestUtil.setField(ServerManager.INSTANCE, "serverService", serverService);
        activeServerIds().addAll(List.of("SER-6", ""));
        activeServers().put(8585, serverInitiator);

        List<String> actual = ServerManager.INSTANCE.stopAllServers(true);

        assertEquals(List.of("SER-6"), actual);
        verify(serverInitiator).removeEndpoint(server, true);
        assertFalse(ServerManager.INSTANCE.isServerActive("SER-6"));
        assertTrue(ServerManager.INSTANCE.hasAnyActiveServer());
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, ServerInitiator> activeServers() {
        return (Map<Integer, ServerInitiator>) ReflectionTestUtil.getField(ServerManager.INSTANCE, "activeServers");
    }

    @SuppressWarnings("unchecked")
    private static javafx.collections.ObservableSet<String> activeServerIds() {
        return (javafx.collections.ObservableSet<String>) ReflectionTestUtil.getField(ServerManager.INSTANCE, "activeServerIds");
    }

    private static Server server(String serverId, int port) {
        Server server = new Server();
        server.setServerId(serverId);
        server.setServerName("Server-" + serverId);
        server.setUrlEndpoint("/orders");
        server.setMethod(Method.GET);
        server.setPort(port);
        server.setResponseCode(200);
        server.setResponseData("ok");
        return server;
    }
}

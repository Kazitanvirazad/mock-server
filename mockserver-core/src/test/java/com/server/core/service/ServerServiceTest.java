package com.server.core.service;

import com.server.core.config.CommonConfig;
import com.server.core.model.data.Server;
import com.server.core.notification.Notification;
import com.server.core.repository.ServerRepository;
import com.server.core.support.ReflectionTestUtil;
import com.server.core.util.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerServiceTest {
    @Mock
    private ServerRepository serverRepository;

    @Mock
    private Notification notification;

    private ServerService serverService;

    @BeforeEach
    void setUp() {
        serverService = new ServerService();
        ReflectionTestUtil.setField(serverService, "serverRepository", serverRepository);
        CommonConfig.INSTANCE.setNotification(notification);
    }

    @Test
    void getServerByIdReturnsRepositoryValue() {
        Server server = server("SER-1", "Orders", "2026-04-04 10:00:00");
        when(serverRepository.getServerById("SER-1")).thenReturn(Optional.of(server));

        Optional<Server> actual = serverService.getServerById("SER-1");

        assertTrue(actual.isPresent());
        assertSame(server, actual.get());
    }

    @Test
    void getServersByCollectionReturnsServersSortedByModifiedOnDescending() {
        Server older = server("SER-1", "Older", "2026-04-04 09:00:00");
        Server newer = server("SER-2", "Newer", "2026-04-04 11:00:00");
        when(serverRepository.getServersByCollectionStream("COL-1")).thenReturn(List.of(older, newer).stream());

        List<Server> actual = serverService.getServersByCollection("COL-1").toList();

        assertIterableEquals(List.of(newer, older), actual);
    }

    @Test
    void createServerReturnsEmptyWhenServerIsNull() {
        assertTrue(serverService.createServer(null).isEmpty());
    }

    @Test
    void createServerThrowsWhenIdGenerationFails() {
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> serverService.createServer(server(null, "Orders", "2026-04-04 10:00:00")));

            assertEquals("UUID7 generate failed while creating Server Id!", exception.getMessage());
            verify(notification).triggerErrorNotification(
                    "Something went wrong while creating Server!",
                    "Please try again later."
            );
        }
    }

    @Test
    void createServerReturnsCreatedServerWhenRepositorySucceeds() {
        Server server = server(null, "Orders", "2026-04-04 10:00:00");
        Server saved = server("SER-10", "Orders", "2026-04-04 12:00:00");
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.of("SER-10"));
            when(serverRepository.createServer(any(Server.class), eq(""), eq(""))).thenReturn(1);
            when(serverRepository.getServerById("SER-10")).thenReturn(Optional.of(saved));

            Optional<Server> actual = serverService.createServer(server);

            assertTrue(actual.isPresent());
            assertSame(saved, actual.get());
            assertEquals("SER-10", server.getServerId());
            verify(notification).triggerInfoNotification("Server Saved!", "Orders saved successfully.");
        }
    }

    @Test
    void createServerReturnsEmptyWhenRepositoryFails() {
        Server server = server(null, "Orders", "2026-04-04 10:00:00");
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.of("SER-11"));
            when(serverRepository.createServer(any(Server.class), eq(""), eq(""))).thenReturn(0);

            Optional<Server> actual = serverService.createServer(server);

            assertTrue(actual.isEmpty());
            verify(notification).triggerErrorNotification("Failed to save server!", "Please try again later.");
        }
    }

    @Test
    void createImportedServerReturnsEmptyWhenServerIsNull() {
        assertTrue(serverService.createImportedServer(null, "COL-1").isEmpty());
    }

    @Test
    void createImportedServerReturnsEmptyWhenIdGenerationFails() {
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.empty());

            Optional<String> actual = serverService.createImportedServer(server(null, "Orders", "2026-04-04 10:00:00"), "COL-1");

            assertTrue(actual.isEmpty());
        }
    }

    @Test
    void createImportedServerReturnsGeneratedIdWhenRepositorySucceeds() {
        Server server = server(null, "Orders", "2026-04-04 10:00:00");
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.of("SER-20"));
            when(serverRepository.createServer(any(Server.class), eq(""), eq(""), eq("COL-1"), eq("SER-20")))
                    .thenReturn(1);

            Optional<String> actual = serverService.createImportedServer(server, "COL-1");

            assertEquals(Optional.of("SER-20"), actual);
        }
    }

    @Test
    void updateServerReturnsEmptyWhenServerIsNull() {
        assertTrue(serverService.updateServer(null).isEmpty());
    }

    @Test
    void updateServerReturnsUpdatedServerWhenRepositorySucceeds() {
        Server server = server("SER-30", "Orders", "2026-04-04 10:00:00");
        Server updated = server("SER-30", "Orders Updated", "2026-04-04 12:00:00");
        when(serverRepository.updateServer(server, "", "")).thenReturn(1);
        when(serverRepository.getServerById("SER-30")).thenReturn(Optional.of(updated));

        Optional<Server> actual = serverService.updateServer(server);

        assertTrue(actual.isPresent());
        assertSame(updated, actual.get());
        verify(notification).triggerInfoNotification("Server Updated!", "Orders updated successfully.");
    }

    @Test
    void updateServerReturnsEmptyWhenRepositoryFails() {
        Server server = server("SER-31", "Orders", "2026-04-04 10:00:00");
        when(serverRepository.updateServer(server, "", "")).thenReturn(0);

        Optional<Server> actual = serverService.updateServer(server);

        assertTrue(actual.isEmpty());
        verify(notification).triggerErrorNotification("Failed to update server!", "Please try again later.");
    }

    @Test
    void deleteServerByIdReturnsBooleanStatus() {
        when(serverRepository.deleteServer("SER-40")).thenReturn(1);
        when(serverRepository.deleteServer("SER-41")).thenReturn(0);

        assertTrue(serverService.deleteServerById("SER-40"));
        assertFalse(serverService.deleteServerById("SER-41"));
    }

    private static Server server(String id, String name, String modifiedOn) {
        Server server = new Server();
        server.setServerId(id);
        server.setServerName(name);
        server.setModifiedOn(Timestamp.valueOf(modifiedOn));
        return server;
    }
}

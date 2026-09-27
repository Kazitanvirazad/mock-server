package com.server.core.util;

import com.server.core.config.CommonConfig;
import com.server.core.constants.Method;
import com.server.core.dto.CollectionDto;
import com.server.core.dto.ServerDto;
import com.server.core.model.data.Collection;
import com.server.core.model.data.Server;
import com.server.core.notification.Notification;
import com.server.core.service.CollectionService;
import com.server.core.service.ServerService;
import com.server.core.support.ReflectionTestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportExportUtilTest {
    private final ServerService serverService = mock(ServerService.class);
    private final CollectionService collectionService = mock(CollectionService.class);
    private final Notification notification = mock(Notification.class);

    private ImportExportUtil importExportUtil;

    @BeforeEach
    void setUp() {
        importExportUtil = new ImportExportUtil();
        ReflectionTestUtil.setField(importExportUtil, "serverService", serverService);
        ReflectionTestUtil.setField(importExportUtil, "collectionService", collectionService);
        CommonConfig.INSTANCE.setNotification(notification);
    }

    @Test
    void exportCollectionWritesJsonFileForDirectoryTarget() throws Exception {
        Collection collection = new Collection("COL-1", "Orders");
        collection.setCreatedOn(Timestamp.valueOf("2026-04-05 10:00:00"));
        when(serverService.getServersByCollection("COL-1")).thenReturn(List.of(new Server()).stream());

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class);
             MockedStatic<Serializer> serializerMock = mockStatic(Serializer.class);
             MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            fileUtilsMock.when(() -> FileUtils.isDirectory(any(File.class))).thenReturn(true);
            serializerMock.when(() -> Serializer.serializeList(any())).thenReturn(Optional.of("[{}]"));
            commonUtilMock.when(() -> CommonUtil.getRandomNumberInRange(999, 3999)).thenReturn(1234);

            importExportUtil.exportCollection(new File("C:/exports"), List.of(collection));

            fileUtilsMock.verify(() -> FileUtils.writeStringToFile(
                    any(File.class), eq("[{}]"), eq(java.nio.charset.StandardCharsets.UTF_8)));
        }
    }

    @Test
    void exportCollectionTriggersNotificationOnFailure() {
        when(serverService.getServersByCollection("COL-1")).thenThrow(new RuntimeException("boom"));
        Collection collection = new Collection("COL-1", "Orders");

        importExportUtil.exportCollection(new File("C:/exports"), List.of(collection));

        verify(notification).triggerErrorNotification("Something went wrong!", "Please try again later");
    }

    @Test
    void importCollectionReturnsFalseWhenDeserializationFails() throws Exception {
        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class);
             MockedStatic<Serializer> serializerMock = mockStatic(Serializer.class)) {
            fileUtilsMock.when(() -> FileUtils.readFileToString(any(File.class), eq(java.nio.charset.StandardCharsets.UTF_8)))
                    .thenReturn("invalid");
            serializerMock.when(() -> Serializer.deSerializeList("invalid", CollectionDto.class))
                    .thenReturn(Optional.empty());

            boolean actual = importExportUtil.importCollection(new File("C:/imports/collection.json"));

            assertFalse(actual);
            verify(notification).triggerErrorNotification("Collection import failed!", "Insufficient input data");
        }
    }

    @Test
    void importCollectionReturnsFalseWhenCollectionListIsEmpty() throws Exception {
        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class);
             MockedStatic<Serializer> serializerMock = mockStatic(Serializer.class)) {
            fileUtilsMock.when(() -> FileUtils.readFileToString(any(File.class), eq(java.nio.charset.StandardCharsets.UTF_8)))
                    .thenReturn("json");
            serializerMock.when(() -> Serializer.deSerializeList("json", CollectionDto.class))
                    .thenReturn(Optional.of(List.of()));

            boolean actual = importExportUtil.importCollection(new File("C:/imports/collection.json"));

            assertFalse(actual);
            verify(notification).triggerErrorNotification("Collection import failed!", "Collection not found in file");
        }
    }

    @Test
    void importCollectionReturnsFalseWhenValidCollectionsAreEmpty() throws Exception {
        CollectionDto collectionDto = new CollectionDto(" ", Timestamp.valueOf("2026-04-05 08:00:00"), null);
        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class);
             MockedStatic<Serializer> serializerMock = mockStatic(Serializer.class)) {
            fileUtilsMock.when(() -> FileUtils.readFileToString(any(File.class), eq(java.nio.charset.StandardCharsets.UTF_8)))
                    .thenReturn("json");
            serializerMock.when(() -> Serializer.deSerializeList("json", CollectionDto.class))
                    .thenReturn(Optional.of(List.of(collectionDto)));

            boolean actual = importExportUtil.importCollection(new File("C:/imports/collection.json"));

            assertFalse(actual);
            verify(notification).triggerErrorNotification("Collection import failed!", "Collections are empty");
        }
    }

    @Test
    void importCollectionRenamesExistingCollectionAndImportsServers() throws Exception {
        ServerDto serverDto = new ServerDto(0L, 8080, 200, "Orders API", "/orders", "{}",
                Method.GET, Timestamp.valueOf("2026-04-05 09:00:00"), null, null);
        CollectionDto collectionDto = new CollectionDto("Orders", Timestamp.valueOf("2026-04-05 08:00:00"),
                List.of(serverDto));
        when(collectionService.getCollectionByName("Orders")).thenReturn(Optional.of(new Collection("OLD", "Orders")));
        when(collectionService.createImportedCollection(any(Collection.class))).thenReturn(Optional.of("COL-NEW"));
        when(serverService.createImportedServer(any(Server.class), eq("COL-NEW"))).thenReturn(Optional.of("SER-NEW"));

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class);
             MockedStatic<Serializer> serializerMock = mockStatic(Serializer.class);
             MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            fileUtilsMock.when(() -> FileUtils.readFileToString(any(File.class), eq(java.nio.charset.StandardCharsets.UTF_8)))
                    .thenReturn("json");
            serializerMock.when(() -> Serializer.deSerializeList("json", CollectionDto.class))
                    .thenReturn(Optional.of(List.of(collectionDto)));
            commonUtilMock.when(() -> CommonUtil.getRandomNumberInRange(999, 5999)).thenReturn(5555);

            boolean actual = importExportUtil.importCollection(new File("C:/imports/collection.json"));

            assertTrue(actual);
            ArgumentCaptor<Collection> collectionCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(collectionService).createImportedCollection(collectionCaptor.capture());
            assertEquals("Orders_5555", collectionCaptor.getValue().getCollectionName());
            verify(serverService).createImportedServer(any(Server.class), eq("COL-NEW"));
        }
    }

    @Test
    void readFileReturnsEmptyByteArrayWhenReadFails() {
        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFileToByteArray(any(File.class)))
                    .thenThrow(new RuntimeException("boom"));

            byte[] actual = importExportUtil.readFile("C:/missing.txt");

            assertArrayEquals(new byte[0], actual);
        }
    }

    @Test
    void exportCollectionSkipsWriteWhenTargetIsNotDirectory() throws Exception {
        Collection collection = new Collection("COL-1", "Orders");
        when(serverService.getServersByCollection("COL-1")).thenReturn(List.<Server>of().stream());

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class);
             MockedStatic<Serializer> serializerMock = mockStatic(Serializer.class)) {
            fileUtilsMock.when(() -> FileUtils.isDirectory(any(File.class))).thenReturn(false);
            serializerMock.when(() -> Serializer.serializeList(any())).thenReturn(Optional.of("[{}]"));

            importExportUtil.exportCollection(new File("C:/exports/file.txt"), List.of(collection));

            fileUtilsMock.verify(() -> FileUtils.writeStringToFile(any(File.class), any(String.class), eq(java.nio.charset.StandardCharsets.UTF_8)),
                    org.mockito.Mockito.never());
        }
    }
}

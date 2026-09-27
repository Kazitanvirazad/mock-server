package com.server.core.service;

import com.server.core.config.CommonConfig;
import com.server.core.model.data.Collection;
import com.server.core.repository.CollectionRepository;
import com.server.core.support.ReflectionTestUtil;
import com.server.core.util.CommonUtil;
import com.server.core.notification.Notification;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {
    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private Notification notification;

    private CollectionService collectionService;

    @BeforeEach
    void setUp() {
        collectionService = new CollectionService();
        ReflectionTestUtil.setField(collectionService, "collectionRepository", collectionRepository);
        CommonConfig.INSTANCE.setNotification(notification);
    }

    @Test
    void getCollectionByIdReturnsRepositoryValue() {
        Collection collection = collection("COL-1", "Orders", "2026-04-04 10:00:00");
        when(collectionRepository.getCollectionById("COL-1")).thenReturn(Optional.of(collection));

        Optional<Collection> actual = collectionService.getCollectionById("COL-1");

        assertTrue(actual.isPresent());
        assertSame(collection, actual.get());
    }

    @Test
    void getCollectionStreamReturnsCollectionsSortedByModifiedOnDescending() {
        Collection older = collection("COL-1", "Older", "2026-04-04 09:00:00");
        Collection newer = collection("COL-2", "Newer", "2026-04-04 11:00:00");
        when(collectionRepository.getCollectionStream()).thenReturn(List.of(older, newer).stream());

        List<Collection> actual = collectionService.getCollectionStream().toList();

        assertIterableEquals(List.of(newer, older), actual);
    }

    @Test
    void createCollectionReturnsEmptyWhenIdGenerationFails() {
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.empty());

            Optional<Collection> actual = collectionService.createCollection("Orders");

            assertTrue(actual.isEmpty());
            verify(notification).triggerErrorNotification(
                    "Something went wrong while saving collection!",
                    "Please try again later."
            );
        }
    }

    @Test
    void createCollectionReturnsCreatedCollectionWhenRepositorySucceeds() {
        Collection saved = collection("COL-10", "Orders", "2026-04-04 11:00:00");
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.of("COL-10"));
            when(collectionRepository.createCollection("COL-10", "Orders")).thenReturn(1);
            when(collectionRepository.getCollectionById("COL-10")).thenReturn(Optional.of(saved));

            Optional<Collection> actual = collectionService.createCollection("Orders");

            assertTrue(actual.isPresent());
            assertSame(saved, actual.get());
            verify(notification).triggerInfoNotification("Collection Saved!", "Orders saved successfully.");
        }
    }

    @Test
    void createCollectionReturnsEmptyWhenRepositoryFails() {
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.of("COL-11"));
            when(collectionRepository.createCollection("COL-11", "Orders")).thenReturn(0);

            Optional<Collection> actual = collectionService.createCollection("Orders");

            assertTrue(actual.isEmpty());
            verify(notification).triggerErrorNotification("Failed to save collection!", "Please try again later.");
        }
    }

    @Test
    void createImportedCollectionReturnsEmptyWhenIdGenerationFails() {
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.empty());

            Optional<String> actual = collectionService.createImportedCollection(new Collection("Imported"));

            assertTrue(actual.isEmpty());
        }
    }

    @Test
    void createImportedCollectionReturnsGeneratedIdWhenRepositorySucceeds() {
        Collection collection = new Collection("Imported");
        try (MockedStatic<CommonUtil> commonUtilMock = mockStatic(CommonUtil.class)) {
            commonUtilMock.when(CommonUtil::generateUniqueAlphanumericId).thenReturn(Optional.of("COL-20"));
            when(collectionRepository.createCollection("COL-20", collection)).thenReturn(1);

            Optional<String> actual = collectionService.createImportedCollection(collection);

            assertEquals(Optional.of("COL-20"), actual);
        }
    }

    @Test
    void editCollectionReturnsUpdatedCollectionWhenRepositorySucceeds() {
        Collection updated = collection("COL-30", "Updated", "2026-04-04 12:00:00");
        when(collectionRepository.editCollection("COL-30", "Updated")).thenReturn(1);
        when(collectionRepository.getCollectionById("COL-30")).thenReturn(Optional.of(updated));

        Optional<Collection> actual = collectionService.editCollection("COL-30", "Updated");

        assertTrue(actual.isPresent());
        assertSame(updated, actual.get());
        verify(notification).triggerInfoNotification("Collection Updated!", "Updated updated successfully.");
    }

    @Test
    void editCollectionReturnsEmptyWhenRepositoryFails() {
        when(collectionRepository.editCollection("COL-31", "Updated")).thenReturn(0);

        Optional<Collection> actual = collectionService.editCollection("COL-31", "Updated");

        assertTrue(actual.isEmpty());
        verify(notification).triggerErrorNotification("Failed to update collection!", "Please try again later.");
    }

    @Test
    void deleteCollectionByIdReturnsBooleanStatus() {
        when(collectionRepository.deleteCollection("COL-40")).thenReturn(1);
        when(collectionRepository.deleteCollection("COL-41")).thenReturn(0);

        assertTrue(collectionService.deleteCollectionById("COL-40"));
        assertFalse(collectionService.deleteCollectionById("COL-41"));
    }

    private static Collection collection(String id, String name, String modifiedOn) {
        Collection collection = new Collection(id, name);
        collection.setModifiedOn(Timestamp.valueOf(modifiedOn));
        return collection;
    }
}

package com.server.app.function;

import com.server.app.model.view.CollectionTableData;
import com.server.app.model.view.ServerTableData;
import com.server.app.service.AppService;
import com.server.core.model.data.Collection;
import com.server.core.server.ServerManager;
import com.server.core.service.CollectionService;
import com.server.core.service.ServerService;
import javafx.collections.FXCollections;
import javafx.scene.control.TableView;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Kazi Tanvir Azad
 */
public record DeleteCollectionConsumer(CollectionService collectionService,
                                       ServerService serverService,
                                       TableView<CollectionTableData> collectionTable,
                                       TableView<ServerTableData> serverTable) implements Consumer<Collection> {

    @Override
    public void accept(Collection collection) {
        serverService.getServersByCollection(collection.getCollectionId())
                .filter(Objects::nonNull)
                .forEach(server -> {
                    if (ServerManager.INSTANCE.isServerActive(server.getServerId())) {
                        ServerManager.INSTANCE.stopServer(server, true);
                    }
                    serverService.deleteServerById(server.getServerId());
                });
        if (collectionService.deleteCollectionById(collection.getCollectionId())) {
            collectionTable.setItems(FXCollections.observableList(AppService.INSTANCE.getTableDataService()
                    .getCollectionTableData()));
            serverTable.getItems().clear();
        }
    }
}

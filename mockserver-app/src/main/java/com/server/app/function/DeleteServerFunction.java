package com.server.app.function;

import com.server.app.model.view.ServerTableData;
import com.server.app.service.AppService;
import com.server.core.model.data.Server;
import com.server.core.server.ServerManager;
import com.server.core.service.ServerService;
import javafx.collections.FXCollections;
import javafx.scene.control.TableView;

import java.util.function.Function;

/**
 * @author Kazi Tanvir Azad
 */
public record DeleteServerFunction(ServerService serverService,
                                   TableView<ServerTableData> serverTable) implements Function<Server, Boolean> {
    @Override
    public Boolean apply(Server server) {
        if (ServerManager.INSTANCE.isServerActive(server.getServerId())) {
            ServerManager.INSTANCE.stopServer(server, true);
        }
        if (serverService.deleteServerById(server.getServerId())) {
            serverTable.setItems(FXCollections.observableList(AppService.INSTANCE.getTableDataService()
                    .getServerTableDataList(server.getCollectionId())));
            return true;
        }
        return false;
    }
}

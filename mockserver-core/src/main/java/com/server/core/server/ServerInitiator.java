package com.server.core.server;

import com.server.core.config.CommonConfig;
import com.server.core.function.EndpointBiConsumer;
import com.server.core.model.data.Server;
import com.sun.net.httpserver.HttpServer;
import javafx.collections.ObservableSet;
import org.apache.commons.collections4.MapUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * @author Kazi Tanvir Azad
 */
public class ServerInitiator {
    private HttpServer httpServer;
    private final int portNumber;
    private final Map<String, EndpointInitiator> endPoints;

    public ServerInitiator(int portNumber) {
        this.endPoints = new HashMap<>();
        this.portNumber = portNumber;
    }

    /**
     * Initialize the {@link HttpServer} with port number
     */
    private void initServer(boolean silent) {
        try {
            // Create and initialize httpServer
            this.httpServer = HttpServer.create(new InetSocketAddress(portNumber), 0);
            httpServer.setExecutor(null);
        } catch (IOException exception) {
            if (!silent) {
                CommonConfig.INSTANCE.notification()
                        .triggerErrorNotification("Something went wrong while initializing server",
                                exception.getMessage());
            }
            throw new RuntimeException(exception);
        }
    }

    /**
     * Adds the {@link Server} to the endPoints map. If there is similar server is already added
     * then overrides the server by taking user's consent
     *
     * @param server          {@link Server} to be added in the {@link EndpointInitiator}
     * @param activeServerIds {@link ObservableSet} of currently server ids
     * @apiNote {@link Server}s are mapped based on the hierarchy of port number > url endpoint > method
     */
    public void addEndpoint(Server server, final ObservableSet<String> activeServerIds) {
        String urlEndpoint = server.getUrlEndpoint();
        EndpointInitiator existingEndpointInitiator = endPoints.getOrDefault(urlEndpoint, null);
        // similar server with same url endpoint and method is already running
        if (Objects.nonNull(existingEndpointInitiator) && existingEndpointInitiator.getMethods().containsKey(server.getMethod())) {
            boolean promptResult = CommonConfig.INSTANCE.notification()
                    .triggerConfirmationPrompt("""
                                    Server with similar Endpoint and
                                    method is already running.""",
                            """
                                    Accept to override the existing
                                    server with the new one.""");
            // User responded 'true' to override the existing server
            if (promptResult) {
                // getting the server to overridden
                MethodInitiator methodInitiatorToOverride = existingEndpointInitiator.getMethods().get(server.getMethod());
                String serverIdToOverride = Objects.nonNull(methodInitiatorToOverride.getServerId()) ?
                        methodInitiatorToOverride.getServerId() : null;
                // override the existing server
                existingEndpointInitiator.addMethod(server);
                // update the active servers set
                if (Objects.nonNull(serverIdToOverride)) {
                    activeServerIds.remove(serverIdToOverride);
                }
            } else {
                // don't do anything as user responded 'false'
                throw new RuntimeException();
            }
        } else if (Objects.nonNull(existingEndpointInitiator)) {
            // similar server with same url endpoint is already running
            existingEndpointInitiator.addMethod(server);
        } else {
            // Create the fresh new server
            EndpointInitiator endpointInitiator = new EndpointInitiator();
            endpointInitiator.setUrlEndpoint(urlEndpoint);
            endpointInitiator.addMethod(server);
            endPoints.put(urlEndpoint, endpointInitiator);
        }
    }

    /**
     * Stops the server and the removes it from the endPoints map and then restarts the server
     *
     * @param server {@link Server} to be removed
     */
    public void removeEndpoint(Server server, boolean silent) {
        stopServer();
        EndpointInitiator existingEndpoints = endPoints.getOrDefault(server.getUrlEndpoint(), null);
        if (Objects.nonNull(existingEndpoints)) {
            existingEndpoints.removeMethod(server.getMethod());
            if (MapUtils.isEmpty(existingEndpoints.getMethods())) {
                endPoints.remove(server.getUrlEndpoint());
            }
        }
        if (MapUtils.isNotEmpty(endPoints)) {
            startServer(silent);
        }
    }

    /**
     * Initialize the {@link HttpServer} with port number, initializes the server context and starts the server
     */
    public void startServer(boolean silent) {
        initServer(silent);
        if (Objects.nonNull(httpServer)) {
            // Add server context logic from 'endPoints'
            initializeServerContext();
            httpServer.setExecutor(null);
            httpServer.start();
        }
    }

    /**
     * Initializes the server context
     */
    private void initializeServerContext() {
        if (MapUtils.isNotEmpty(this.endPoints)) {
            BiConsumer<String, EndpointInitiator> endpointBiConsumer = new EndpointBiConsumer(this.httpServer);
            this.endPoints.forEach(endpointBiConsumer);
        }
    }

    /**
     * Performs server restart
     */
    public void restartServer(boolean silent) {
        stopServer();
        startServer(silent);
    }

    /**
     * Checks if the server is stopped
     *
     * @return {@code boolean} True if server is already stopped, False otherwise
     */
    public boolean isServerStopped() {
        return Objects.isNull(httpServer) && MapUtils.isEmpty(endPoints);
    }

    /**
     * Stops the server
     */
    private void stopServer() {
        if (Objects.nonNull(httpServer)) {
            httpServer.stop(1);
            httpServer = null;
        }
    }
}

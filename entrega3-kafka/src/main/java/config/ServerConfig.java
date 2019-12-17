package config;

import java.util.Map;

public class ServerConfig extends NodeConfig {
    private Integer currentServer;
    private Integer currentServerPort;

    public ServerConfig(Integer numberOfNodes, Integer basePort, Integer portDifference, Integer numberOfReplicas, Map<String, String> hashTable, Integer currentNode) {
        super(numberOfNodes, basePort, portDifference, numberOfReplicas, hashTable, currentNode);
    }

    public Integer getCurrentServerPort() {
        return currentServerPort;
    }

    public void setCurrentServerPort(Integer currentServerPort) {
        this.currentServerPort = currentServerPort;
    }

    public Integer getCurrentServer() {
        return currentServer;
    }

    public void setCurrentServer(Integer currentServer) {
        this.currentServer = currentServer;
    }
}

package config;

import java.util.Map;

public class ClusterConfig {
    private Integer numberOfNodes;
    private Integer basePort;
    private Integer portDifference;
    private Integer numberOfReplicas;
    private Map<String, String> hashTable;

    public ClusterConfig(Integer numberOfNodes, Integer basePort, Integer portDifference, Integer numberOfReplicas) {
        this.numberOfNodes = numberOfNodes;
        this.basePort = basePort;
        this.portDifference = portDifference;
        this.numberOfReplicas = numberOfReplicas;
    }

    public ClusterConfig(Integer numberOfNodes, Integer basePort, Integer portDifference, Integer numberOfReplicas, Map<String, String> hashTable) {
        this.numberOfNodes = numberOfNodes;
        this.basePort = basePort;
        this.portDifference = portDifference;
        this.numberOfReplicas = numberOfReplicas;
        this.hashTable = hashTable;
    }

    public Integer getNumberOfNodes() {
        return numberOfNodes;
    }

    public void setNumberOfNodes(Integer numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    public Integer getBasePort() {
        return basePort;
    }

    public void setBasePort(Integer basePort) {
        this.basePort = basePort;
    }

    public Integer getPortDifference() {
        return portDifference;
    }

    public void setPortDifference(Integer portDifference) {
        this.portDifference = portDifference;
    }

    public Integer getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public void setNumberOfReplicas(Integer numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    public Map<String, String> getHashTable() {
        return hashTable;
    }

    public void setHashTable(Map<String, String> hashTable) {
        this.hashTable = hashTable;
    }
}

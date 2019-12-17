package config;

import java.util.Map;

public class NodeConfig extends ClusterConfig {
    private Integer currentNode;

    public NodeConfig(Integer numberOfNodes, Integer basePort, Integer portDifference, Integer numberOfReplicas, Map<String, String> hashTable) {
        super(numberOfNodes, basePort, portDifference, numberOfReplicas, hashTable);
    }

    public NodeConfig(Integer numberOfNodes, Integer basePort, Integer portDifference, Integer numberOfReplicas, Map<String, String> hashTable, Integer currentNode) {
        super(numberOfNodes, basePort, portDifference, numberOfReplicas, hashTable);
        this.currentNode = currentNode;
    }

    public Integer getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Integer currentNode) {
        this.currentNode = currentNode;
    }
}

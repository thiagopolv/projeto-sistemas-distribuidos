package manager;

import config.ClusterConfig;
import config.NodeConfig;

import java.util.HashMap;
import java.util.Map;

import static util.ConfigProperties.*;

public class ClusterManager {
    private static final Integer NUMBER_OF_NODES = getNumberOfNodes();
    private static final Integer NUMBER_OF_REPLICAS = getNumberOfReplicas();
    private static final Integer BASE_PORT = getBasePort();
    private static final Integer NODE_PORT_DIFFERENCE = getNodePortDifference();
    private static final Integer LAST_BASE_HASH = getLastBaseHash();

    private Map<String, String> generateHashTable() {
        Map<String, String> nodeHashList = new HashMap<>();
        for (int i = 0; i < NUMBER_OF_NODES; i++) {
            String nodeInitialHash = getNodeInitialHash(i);
            nodeHashList.put(String.valueOf(i), nodeInitialHash);
        }
        return nodeHashList;
    }

    private String getNodeInitialHash(int iterator) {
        return Integer.toHexString((LAST_BASE_HASH / NUMBER_OF_NODES * iterator));
    }

    private ClusterConfig getClusterConfig() {
        return new ClusterConfig(NUMBER_OF_NODES, BASE_PORT, NODE_PORT_DIFFERENCE, NUMBER_OF_REPLICAS);
    }

    private NodeConfig getNodeBaseConfig(ClusterConfig clusterConfig) {
        return new NodeConfig(clusterConfig.getNumberOfNodes(), clusterConfig.getBasePort(),
                clusterConfig.getPortDifference(), clusterConfig.getNumberOfReplicas(), clusterConfig.getHashTable());
    }

    private void initNodes(NodeConfig nodeConfig) {
        for (int i = 0; i < NUMBER_OF_NODES; i++) {
            nodeConfig.setCurrentNode(i);
            NodeAdapter adapter = new NodeManager(nodeConfig);
            adapter.start();
        }
    }

    public static void main(String[] args) {

        ClusterManager clusterManager = new ClusterManager();

        ClusterConfig clusterConfig = clusterManager.getClusterConfig();

        clusterConfig.setHashTable(clusterManager.generateHashTable());

        NodeConfig nodeBaseConfig = clusterManager.getNodeBaseConfig(clusterConfig);

        clusterManager.initNodes(nodeBaseConfig);
    }
}
package manager;

import config.NodeConfig;
import config.ServerConfig;
import server.ServerFactory;

public class NodeManager implements NodeAdapter {
    private NodeConfig nodeConfig;

    public NodeManager(NodeConfig nodeConfig) {
        this.nodeConfig = nodeConfig;
    }

    @Override
    public void start() {
        for (int i = 0; i < nodeConfig.getNumberOfReplicas(); i++) {
            ServerConfig serverBaseConfig = getServerBaseConfig();
            Integer currentServerPort =
                    serverBaseConfig.getBasePort() + serverBaseConfig.getPortDifference() *
                            serverBaseConfig.getCurrentNode() + i;
            serverBaseConfig.setCurrentServerPort(currentServerPort);
            ServerFactory serverFactory = new ServerFactory(serverBaseConfig, getServerPort(i));

            Thread thread = new Thread(serverFactory::start);
            thread.setName("node-" + nodeConfig.getCurrentNode() + "--server-" + i + "--port-" + getServerPort(i));
            thread.start();
        }
    }

    @Override
    public Integer getServerPort(int serverPosition) {
        return nodeConfig.getBasePort() + getNodeOffset() + serverPosition;
    }

    @Override
    public Integer getNodeOffset() {
        return nodeConfig.getCurrentNode() * nodeConfig.getPortDifference();
    }

    @Override
    public ServerConfig getServerBaseConfig() {
        return new ServerConfig(nodeConfig.getNumberOfNodes(), nodeConfig.getBasePort(),
                nodeConfig.getPortDifference(), nodeConfig.getNumberOfReplicas(), nodeConfig.getHashTable(),
                nodeConfig.getCurrentNode());
    }
}

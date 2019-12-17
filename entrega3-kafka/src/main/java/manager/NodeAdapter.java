package manager;

import config.ServerConfig;

public interface NodeAdapter {
    void start();

    Integer getServerPort(int serverPosition);

    Integer getNodeOffset();

    ServerConfig getServerBaseConfig();
}

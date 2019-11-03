package server;

import java.util.List;

public class ServerInfo {

    private Integer port;
    private List<Integer> idsInServer;

    public ServerInfo() {
    }

    public ServerInfo(List<Integer> idsInServer) {
        this.idsInServer = idsInServer;
    }

    public ServerInfo(Integer port, List<Integer> idsInServer) {
        this.port = port;
        this.idsInServer = idsInServer;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public List<Integer> getIdsInServer() {
        return idsInServer;
    }

    public void setIdsInServer(List<Integer> idsInServer) {
        this.idsInServer = idsInServer;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "port=" + port +
                ", idsInServer=" + idsInServer +
                '}';
    }
}

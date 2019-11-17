package server;

import java.util.List;
import java.util.Map;

public class ServerInfo {

    private Integer port;
    private List<Integer> idsInServer;
    private Map<String, String> hashTable;

    public ServerInfo() {
    }

    public ServerInfo(Integer port, Map<String, String> hashTable) {
        this.port = port;
        this.hashTable = hashTable;
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

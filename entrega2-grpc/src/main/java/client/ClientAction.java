package client;

import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

public enum ClientAction {
    LIST("LIST"),
    SEND_BID("SEND"),
    DISCONNECT("DISC"),
    CREATE("CREATE"),
    INVALID("");

    private String action;

    ClientAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public static BidiMap<ClientAction, String> getEnumMap() {
        BidiMap<ClientAction, String> enumMap = new DualHashBidiMap<>();
        enumMap.put(LIST, "LIST");
        enumMap.put(SEND_BID, "SEND");
        enumMap.put(DISCONNECT, "DISC");
        enumMap.put(CREATE, "CREATE");
        enumMap.put(INVALID, "");

        return enumMap;
    }

    public static void main(String[] args) {
        System.out.println(ClientAction.valueOf("REF"));

    }
}

package client;

import java.util.function.Function;

public enum ClientAction {
    LIST(ClientService::printAuctions),
    SEND(ClientService::sendBidAndReturnMessage),
    CREATE(ClientService::createAuctionAndSendMessage),
    DISCONNECT(ClientService::sendDisconnectAndSendMessage),
    INVALID(ClientService::sendInvalidActionMessage);

    public final Function<ClientService, Void> clientActionFunction;

    ClientAction(Function<ClientService, Void> clientActionFunction) {
        this.clientActionFunction = clientActionFunction;
    }
}

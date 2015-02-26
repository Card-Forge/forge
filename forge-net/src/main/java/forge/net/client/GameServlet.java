package forge.net.client;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import forge.net.IClientSocket;
import forge.net.NetServer;
import forge.net.NetServer.ClientSocket;

public class GameServlet extends WebSocketServlet {

    private final String prefix;
    private final ClientSocket socket;
    public GameServlet(final NetServer netServer, final String prefix) {
        this.prefix = prefix;
        this.socket = netServer.new ClientSocket();
    }

    public IClientSocket getSocket() {
        return socket;
    }

    @Override
    public WebSocket doWebSocketConnect(final HttpServletRequest request, final String protocol) {
        System.out.printf("Connection from %s recieved%n", request.getRemoteAddr());
        return socket;
    }

}

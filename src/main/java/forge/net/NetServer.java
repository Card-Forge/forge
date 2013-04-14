package forge.net;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;


import forge.error.BugReporter;
import forge.net.client.NetClient;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class NetServer {

    private final Server srv = new Server();
    private final Set<ClientSocket> _openSockets = new CopyOnWriteArraySet<ClientSocket>();
    
    public NetServer() {
        SelectChannelConnector connector= new SelectChannelConnector();
        connector.setPort(81);
        srv.addConnector(connector);
        
        ServletContextHandler context = new ServletContextHandler();
        ServletHolder sh = new ServletHolder(new GameServlet());
        context.addServlet(sh, "/*");
        //context.setContextPath("/");
        srv.setHandler(context);
    }
    
    @SuppressWarnings("serial")
    public class GameServlet extends WebSocketServlet
    {
        @Override
        public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
            System.out.printf("Connection from %s recieved%n", request.getRemoteAddr());
            return new ClientSocket();
        }
    }
    
    
    public class ClientSocket implements WebSocket.OnTextMessage, IClientSocket
    {
        private Connection _connection;
        private IConnectionObserver _client;
        

        @Override
        public void onClose(int closeCode, String message) {
            System.out.println("Lost connection: " + closeCode +  ", " + message);
            _openSockets.remove(_client);
            _client.onConnectionClosed();
        }
        
        public void send(String data)  {
            try {
                _connection.sendMessage(data);
            } catch (IOException e) {
                BugReporter.reportException(e);
            }
        }

        @Override
        public void onMessage(String data) {
            System.out.println("Received: " + data);
            _client.onMessage(data);
        }
        
        public boolean isOpen() {
            return _connection.isOpen();
        }
             
        @Override
        public void onOpen(Connection connection) {
            _connection = connection;
            _client = new NetClient(this);
            _openSockets.add(this);
            send("CardForge server welcomes you.");
        }

        @Override
        public void close(String farewell) {
            _connection.close(1000, farewell);
        }
    }
    
    public void listen() {
        if (!srv.isStarted())
        {
            URI serverUri = null;
            try {
                srv.start();
                Connector connector = srv.getConnectors()[0];
                int port = connector.getLocalPort();
                String host = connector.getHost();
                serverUri = new URI(String.format("ws://%s:%d/", host == null ? "localhost" : host ,port));
            } catch (Exception e) {
                BugReporter.reportException(e);
            }
            
            System.out.println("Server started @ " + serverUri);
        }
        else {
            System.out.println("Server was already started");
        }
    }
    
    public void stop() { 
        try {
            srv.stop();
        } catch (Exception e) {
            BugReporter.reportException(e);
        }
    }
    
}

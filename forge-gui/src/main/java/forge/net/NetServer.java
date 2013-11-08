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

    public int portNumber;
    
    public final int getPortNumber() {
        return portNumber;
    }

    public NetServer() {
        SelectChannelConnector connector= new SelectChannelConnector();
        connector.setMaxIdleTime(1200000); // 20 minutes
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
            _client.onMessage(data);
        }
        
        public boolean isOpen() {
            return _connection.isOpen();
        }
             
        @Override
        public void onOpen(Connection connection) {
            _connection = connection;
            _openSockets.add(this);
            _client = new NetClient(this);
        }

        @Override
        public void close(String farewell) {
            _connection.close(1000, farewell);
        }
    }
    
    public void listen(int port) {
        if (!srv.isStarted())
        {
            portNumber = port;
            URI serverUri = null;
            try {
                Connector connector = srv.getConnectors()[0];
                connector.setPort(portNumber);
                srv.start();

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
            portNumber = -1;
        } catch (Exception e) {
            BugReporter.reportException(e);
        }
    }
    
}

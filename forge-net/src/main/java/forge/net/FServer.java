package forge.net;


public class FServer {
    private FServer() {
        
    }

    private static Lobby lobby = new Lobby();
    
    public static Lobby getLobby() {
        return lobby;
    }
    
    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    private static final NetServer server = new NetServer();
    public static NetServer getServer() {
        // TODO Auto-generated method stub
        return server;
    }

}

package forge.net;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum FServer {
    instance();
    
    private Lobby lobby = null;
    
    public Lobby getLobby() {
        if (lobby == null) {
            lobby = new Lobby();
        }
        return lobby;
    }
    
    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    private final NetServer server = new NetServer();
    public NetServer getServer() {
        // TODO Auto-generated method stub
        return server;
    }

    
}

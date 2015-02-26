package forge.net.game;

public class LoginEvent implements NetEvent {
    private static final long serialVersionUID = -8865183377417377938L;

    private final String username;
    public LoginEvent(final String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}

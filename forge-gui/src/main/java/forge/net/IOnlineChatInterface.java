package forge.net;

public interface IOnlineChatInterface {
    void setGameClient(IRemote iRemote);
    void addMessage(String message);
}

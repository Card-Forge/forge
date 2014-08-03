package forge.game.io;

public interface IGameStateObject {
    void loadState(GameStateDeserializer gsd);
    void saveState(GameStateSerializer gss);
}

package forge.planarconquest;

import forge.planarconquest.ConquestController.GameRunner;

public interface IVCommandCenter {
    void updateCurrentDay();
    void startGame(final GameRunner gameRunner);
}

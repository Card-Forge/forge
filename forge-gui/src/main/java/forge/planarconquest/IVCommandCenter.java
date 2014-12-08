package forge.planarconquest;

import forge.planarconquest.ConquestController.GameRunner;

public interface IVCommandCenter {
    void updateCurrentDay();
    boolean setSelectedCommander(ConquestCommander commander);
    void startGame(final GameRunner gameRunner);
}

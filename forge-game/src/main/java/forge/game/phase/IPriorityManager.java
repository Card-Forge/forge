package forge.game.phase;
import forge.game.player.Player;
/** Manages the priority-passing ring. See PlayerPriority for the concrete implementation. */
public interface IPriorityManager {
    Player getPriorityPlayer();
    void setPriority(Player player);
    void resetPriority();
    boolean isGivingPriority();
    void setGivingPriority(boolean give);
    PriorityResult conductStep();
}

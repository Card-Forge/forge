package forge.game.cost;

import forge.game.player.Player;

public abstract class CostDecisionMakerBase implements ICostVisitor<PaymentDecision> {

    protected final Player player;
    public CostDecisionMakerBase(Player player0) {
        player = player0;
    }
    public Player getPlayer() { return player; }
    public abstract boolean paysRightAfterDecision();
}

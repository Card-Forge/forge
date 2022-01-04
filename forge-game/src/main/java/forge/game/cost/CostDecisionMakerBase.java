package forge.game.cost;

import forge.game.player.Player;

public abstract class CostDecisionMakerBase implements ICostVisitor<PaymentDecision> {

    protected final Player player;
    private boolean effect;
    public CostDecisionMakerBase(Player player0, boolean effect0) {
        player = player0;
        effect = effect0;
    }
    public Player getPlayer() { return player; }
    public abstract boolean paysRightAfterDecision();
    public boolean isEffect() {
        return effect;
    }
}

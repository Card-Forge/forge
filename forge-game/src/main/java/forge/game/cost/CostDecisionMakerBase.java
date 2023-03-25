package forge.game.cost;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public abstract class CostDecisionMakerBase implements ICostVisitor<PaymentDecision> {

    protected final Player player;
    protected final SpellAbility ability;
    protected final Card source;
    private boolean effect;

    public CostDecisionMakerBase(Player player0, boolean effect0, SpellAbility ability0, Card source0) {
        player = player0;
        effect = effect0;
        ability = ability0;
        source = source0;
    }

    public Player getPlayer() { return player; }
    public abstract boolean paysRightAfterDecision();
    public boolean isEffect() {
        return effect;
    }
}

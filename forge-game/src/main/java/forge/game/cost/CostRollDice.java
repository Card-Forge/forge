package forge.game.cost;

import forge.game.ability.effects.RollDiceEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * This is for the "RollDice" Cost
 */
public class CostRollDice extends CostPart {

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    private final String resultSVar;

    /**
     * Instantiates a new cost RollDice.
     *
     * @param amount
     *            the amount
     */
    public CostRollDice(final String amount, final String sides, final String resultSVar, final String description) {
        super(amount, sides, description);
        this.resultSVar = resultSVar;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer) {
        return true;
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("Roll ").append(getAmount());

        if (this.getTypeDescription() == null) {
            sb.append("d").append(getType());
        } else {
            sb.append(" ").append(this.getTypeDescription());
        }

        return sb.toString();
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision pd, SpellAbility sa) {
        int sides = Integer.parseInt(getType());
        int result = RollDiceEffect.rollDiceForPlayer(sa, payer, pd.c, sides);
        sa.setSVar(resultSVar, Integer.toString(result));
        return true;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.spellability.SpellAbility;

import java.util.List;

public class PowerExchangeEffect extends SpellAbilityEffect {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<Card> tgtCards = getTargetCards(sa);


        if (tgtCards.size() == 1) {
            sb.append(sa.getHostCard()).append(" exchanges power with ");
            sb.append(tgtCards.get(0));
        } else if (tgtCards.size() > 1) {
            sb.append(tgtCards.get(0)).append(" exchanges power with ");
            sb.append(tgtCards.get(1));
        }
        sb.append(".");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final Card c1;
        final Card c2;

        final List<Card> tgtCards = getTargetCards(sa);

        if (tgtCards.size() == 1) {
            c1 = source;
            c2 = tgtCards.get(0);
        } else {
            c1 = tgtCards.get(0);
            c2 = tgtCards.get(1);
        }
        if (!c1.isInPlay() || !c2.isInPlay()) {
            return;
        }
        final int power1 = c1.getNetPower();
        final int power2 = c2.getNetPower();

        c1.addTempPowerBoost(power2 - power1);
        c2.addTempPowerBoost(power1 - power2);

        game.fireEvent(new GameEventCardStatsChanged(c1));
        game.fireEvent(new GameEventCardStatsChanged(c2));

        if (!sa.hasParam("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final GameCommand untilEOT = new GameCommand() {

                private static final long serialVersionUID = -4890579038956651232L;

                @Override
                public void run() {
                    c1.addTempPowerBoost(power1 - power2);
                    c2.addTempPowerBoost(power2 - power1);
                    game.fireEvent(new GameEventCardStatsChanged(c1));
                    game.fireEvent(new GameEventCardStatsChanged(c2));
                }
            };
            
            if (sa.hasParam("UntilEndOfCombat")) {
                game.getEndOfCombat().addUntil(untilEOT);
            } else if (sa.hasParam("UntilHostLeavesPlay")) {
                sa.getHostCard().addLeavesPlayCommand(untilEOT);
            } else {
                game.getEndOfTurn().addUntil(untilEOT);
            }
        }
    }

}

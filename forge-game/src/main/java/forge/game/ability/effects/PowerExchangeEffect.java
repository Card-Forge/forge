package forge.game.ability.effects;

import java.util.List;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.perpetual.PerpetualNewPT;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.spellability.SpellAbility;

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
        final boolean perpetual = "Perpetual".equals(sa.getParam("Duration"));
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
        final boolean basePower = sa.hasParam("BasePower");
        final int power1 = basePower ? c1.getCurrentPower() : c1.getNetPower();
        final int power2 = basePower ? c2.getCurrentPower() : c2.getNetPower();

        final long timestamp = game.getNextTimestamp();

        if (perpetual) {
            c1.addPerpetual(new PerpetualNewPT(power2, null), timestamp);
            c2.addPerpetual(new PerpetualNewPT(power1, null), timestamp);
        } else {
            c1.addNewPT(power2, null, timestamp, 0);
            c2.addNewPT(power1, null, timestamp, 0);
        }

        game.fireEvent(new GameEventCardStatsChanged(c1));
        game.fireEvent(new GameEventCardStatsChanged(c2));

        if (!"Permanent".equals(sa.getParam("Duration")) && !perpetual) {
            // If not Permanent, remove Pumped at EOT
            final GameCommand untilEOT = new GameCommand() {

                private static final long serialVersionUID = -4890579038956651232L;

                @Override
                public void run() {
                    c1.removeNewPT(timestamp, 0);
                    c2.removeNewPT(timestamp, 0);
                    game.fireEvent(new GameEventCardStatsChanged(c1));
                    game.fireEvent(new GameEventCardStatsChanged(c2));
                }
            };

            addUntilCommand(sa, untilEOT);
        }
    }

}

package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ControlGainVariantEffect extends SpellAbilityEffect {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getDescription();
    }

    @Override
    public void resolve(SpellAbility sa) {
        // Aminatou, the Fateshifter (multiple players gain control of multiple permanents in an effect)
        // Consider migrating cards with similar effects
        // GainControl embedded in RepeatEach effects don't work well with timestamps
        final Card source = sa.getHostCard();
        final Game game = source.getGame();

        CardCollection tgtCards = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                sa.getParam("AllValid"), source.getController(), source);

        if ("NextPlayerInChosenDirection".equals(sa.getParam("ChangeController")) && (source.getChosenDirection() != null) ) {
            long tStamp = game.getNextTimestamp();
            for (final Player p : game.getPlayers()) {

                CardCollection valid = CardLists.filterControlledBy(tgtCards, game.getNextPlayerAfter(p, source.getChosenDirection()));

                for (Card tgtC : valid) {
                    if (!tgtC.isInPlay() || !tgtC.canBeControlledBy(p)) {
                        continue;
                    }
                    tgtC.setController(p, tStamp);
                    tgtCards.remove(tgtC); // remove from the list if controller is changed
                }
            }
        }
    }

}

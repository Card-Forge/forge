package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class ExploreEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        List<Card> tgt = getTargetCards(sa);

        sb.append(Lang.joinHomogenous(tgt));
        sb.append(" ");
        sb.append(tgt.size() > 1 ? "explore" : "explores");
        sb.append(". ");

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();

        GameEntityCounterTable table = new GameEntityCounterTable();
        final CardZoneTable triggerList = new CardZoneTable();
        for (final Card c : getTargetCards(sa)) {
            // revealed land card
            boolean revealedLand = false;
            final Player pl = c.getController();
            CardCollection top = pl.getTopXCardsFromLibrary(1);
            if (!top.isEmpty()) {
                Card movedCard = null;
                game.getAction().reveal(top, pl, false, Localizer.getInstance().getMessage("lblRevealedForExplore") + " - ");
                final Card r = top.getFirst();
                final Zone originZone = game.getZoneOf(r);
                if (r.isLand()) {
                    movedCard = game.getAction().moveTo(ZoneType.Hand, r, sa);
                    revealedLand = true;
                } else {
                    // TODO find better way to choose optional send away
                    final Card choosen = pl.getController().chooseSingleCardForZoneChange(
                            ZoneType.Graveyard, Lists.newArrayList(ZoneType.Library), sa, top, null,
                            Localizer.getInstance().getMessage("lblPutThisCardToYourGraveyard"), true, pl);
                    if (choosen != null) {
                        movedCard = game.getAction().moveTo(ZoneType.Graveyard, choosen, sa);
                    }
                }

                if (originZone != null && movedCard != null) {
                    triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);
                }
            }
            if (!revealedLand) {
                // need to get newest game state to check
                // if it is still on the battlefield
                // and the timestamp didnt chamge
                Card gamec = game.getCardState(c);
                // if the card is not more in the game anymore
                // this might still return true but its no problem
                if (game.getZoneOf(gamec).is(ZoneType.Battlefield) && gamec.equalsWithTimestamp(c)) {
                    c.addCounter(CounterEnumType.P1P1, 1, pl, sa, true, table);
                }
            }

            // a creature does explore even if it isn't on the battlefield anymore
            game.getTriggerHandler().runTrigger(TriggerType.Explores, AbilityKey.mapFromCard(c), false);
        }
        table.triggerCountersPutAll(game);
        triggerList.triggerChangesZoneAll(game, sa);
    }

}

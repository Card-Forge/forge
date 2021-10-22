package forge.game.ability.effects;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Direction;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

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
        // Multiple players gain control of multiple permanents in an effect
        // GainControl embedded in RepeatEach effects don't work well with timestamps
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        long tStamp = game.getNextTimestamp();
        final String controller = sa.getParam("ChangeController");
        final Map<Player, CardCollection> gainControl = Maps.newHashMap(); // {newController, CardCollection}
        final PlayerCollection players = game.getPlayers();

        int aidx = players.indexOf(sa.getActivatingPlayer());
        if (aidx != -1) {
            Collections.rotate(players, -aidx);
        }

        CardCollection tgtCards = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                sa.getParam("AllValid"), source.getController(), source, sa);

        if ("NextPlayerInChosenDirection".equals(controller) && (source.getChosenDirection() != null) ) {// Aminatou, the Fateshifter
            for (final Player p : players) {
                gainControl.put(p, CardLists.filterControlledBy(tgtCards, game.getNextPlayerAfter(p, source.getChosenDirection())));
            }
        } else if ("CardOwner".equals(controller)) {// Homeward Path, Trostani Discordant etc.
            for (final Player p : players) {
                gainControl.put(p, CardLists.filter(tgtCards, CardPredicates.isOwner(p)));
            }
        } else if ("Random".equals(controller)) {// Scrambleverse
            for (final Card c : tgtCards) {
                final Player p = Aggregates.random(players);
                if (gainControl.containsKey(p)) {
                    gainControl.get(p).add(0, c);
                } else {
                    gainControl.put(p, new CardCollection(c));
                }
            }
        } else if ("ChooseNextPlayerInChosenDirection".equals(controller) && (source.getChosenDirection() != null)) {// Order of Succession
            Player p = sa.getActivatingPlayer();
            do {
                final CardCollection valid = CardLists.filterControlledBy(tgtCards, game.getNextPlayerAfter(p, source.getChosenDirection()));
                final Card c = p.getController().chooseSingleEntityForEffect(valid, sa, " ", null);
                if (c != null) {
                    gainControl.put(p, new CardCollection(c));
                }
                p = game.getNextPlayerAfter(p, source.getChosenDirection());
            } while (!p.equals(sa.getActivatingPlayer()));
        } else if ("ChooseFromPlayerToTheirRight".equals(controller)) {// Inniaz, the Gale Force
            for (final Player p : players) {
                final CardCollection valid = CardLists.filterControlledBy(tgtCards, game.getNextPlayerAfter(p, Direction.Right));
                final Card c = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(valid, sa,
                        "Choose one for the new Controller: " + p.getName(), null);
                if (c != null) {
                    gainControl.put(p, new CardCollection(c));
                }
            }
        }

        for (Map.Entry<Player, CardCollection> e : gainControl.entrySet()) {
            final Player newController = e.getKey();
            for (Card tgtC : e.getValue()) {
                if (!tgtC.isInPlay() || !tgtC.canBeControlledBy(newController)) {
                    continue;
                }
                tgtC.addTempController(newController, tStamp);
            }
        }
    }

}

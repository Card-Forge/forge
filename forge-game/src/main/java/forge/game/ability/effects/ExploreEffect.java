package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.List;
import java.util.Map;

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
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Num", "1"), sa);

        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());

        CardCollectionView tgts = GameActionUtil.orderCardsByTheirOwners(game, getTargetCards(sa), ZoneType.Battlefield, sa);

        for (final Card c : tgts) {
            final Player pl = c.getController();
            for (int i = 0; i < amount; i++) {
                GameEntityCounterTable table = new GameEntityCounterTable();
                final CardZoneTable triggerList = new CardZoneTable();

                if (game.getReplacementHandler().run(ReplacementType.Explore, AbilityKey.mapFromAffected(c))
                        != ReplacementResult.NotReplaced) {
                    continue;
                }

                // revealed land card
                boolean revealedLand = false;
                CardCollection top = pl.getTopXCardsFromLibrary(1);
                if (!top.isEmpty()) {
                    Card movedCard = null;
                    game.getAction().reveal(top, pl, false,
                            Localizer.getInstance().getMessage("lblRevealedForExplore") + " - ");
                    final Card r = top.getFirst();
                    final Zone originZone = game.getZoneOf(r);
                    if (r.isLand()) {
                        movedCard = game.getAction().moveTo(ZoneType.Hand, r, sa, moveParams);
                        revealedLand = true;
                    } else {
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("RevealedCard", r);
                        if (pl.getController().confirmAction(sa, null,
                                Localizer.getInstance().getMessage("lblPutThisCardToYourGraveyard",
                                        CardTranslation.getTranslatedName(r.getName())), r, params))
                            movedCard = game.getAction().moveTo(ZoneType.Graveyard, r, sa, moveParams);
                    }

                    if (originZone != null && movedCard != null) {
                        triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);
                    }
                }
                if (!revealedLand) {
                    // need to get newest game state to check if it is still on the battlefield
                    // and the timestamp didnt change
                    Card gamec = game.getCardState(c);
                    if (gamec.isInPlay() && gamec.equalsWithTimestamp(c)) {
                        c.addCounter(CounterEnumType.P1P1, 1, pl, table);
                    }
                }

                // a creature does explore even if it isn't on the battlefield anymore
                pl.addExploredThisTurn();
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
                if (!top.isEmpty()) runParams.put(AbilityKey.Explored, top.getFirst());
                game.getTriggerHandler().runTrigger(TriggerType.Explores, runParams, false);
                table.replaceCounterEffect(game, sa, true);
                triggerList.triggerChangesZoneAll(game, sa);
            }
        }
    }

}

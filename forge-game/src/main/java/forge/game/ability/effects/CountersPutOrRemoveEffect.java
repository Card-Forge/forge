package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

/**
 * API for adding counters to or subtracting existing counters from a target.
 *
 */
public class CountersPutOrRemoveEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Player pl = !sa.hasParam("DefinedPlayer") ? sa.getActivatingPlayer() :
                AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("DefinedPlayer"), sa).getFirst();
        sb.append(pl.getName());

        if (sa.hasParam("CounterType")) {
            CounterType ctype = CounterType.getType(sa.getParam("CounterType"));
            sb.append(" puts a ").append(ctype.getName());
            sb.append(" counter on or removes a ").append(ctype.getName()).append(" counter from ");
        } else {
            sb.append(" removes a counter from or puts another of those counters on ");
        }

        sb.append(Lang.joinHomogenous(getDefinedCardsOrTargeted(sa))).append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final int counterAmount = AbilityUtils.calculateAmount(source, sa.getParam("CounterNum"), sa);

        if (counterAmount <= 0) {
            return;
        }

        CounterType ctype = null;
        if (sa.hasParam("CounterType")) {
            ctype = CounterType.getType(sa.getParam("CounterType"));
        }

        final Player pl = !sa.hasParam("DefinedPlayer") ? sa.getActivatingPlayer() :
                AbilityUtils.getDefinedPlayers(source, sa.getParam("DefinedPlayer"), sa).getFirst();
        final boolean eachExisting = sa.hasParam("EachExistingCounter");

        GameEntityCounterTable table = new GameEntityCounterTable();

        for (final Card tgtCard : getDefinedCardsOrTargeted(sa)) {
            Card gameCard = game.getCardState(tgtCard, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtCard.equalsWithTimestamp(gameCard)) {
                continue;
            }
            if (!eachExisting && sa.hasParam("Optional") && !pl.getController().confirmAction(sa, null,
                    Localizer.getInstance().getMessage("lblWouldYouLikePutRemoveCounters", ctype.getName(),
                            CardTranslation.getTranslatedName(gameCard.getName())), null)) {
                continue;
            }
            if (!sa.usesTargeting() || gameCard.canBeTargetedBy(sa)) {
                if (gameCard.hasCounters()) {
                    if (eachExisting) {
                        for (CounterType listType : Lists.newArrayList(gameCard.getCounters().keySet())) {
                            addOrRemoveCounter(sa, gameCard, listType, counterAmount, table, pl);
                        }
                    } else {
                        addOrRemoveCounter(sa, gameCard, ctype, counterAmount, table, pl);
                    }
                } else if (!eachExisting && ctype != null) {
                    gameCard.addCounter(ctype, counterAmount, pl, table);
                }
            }
        }
        table.replaceCounterEffect(game, sa, true);
    }

    private void addOrRemoveCounter(final SpellAbility sa, final Card tgtCard, CounterType ctype,
            final int counterAmount, GameEntityCounterTable table, final Player pl) {
        final PlayerController pc = pl.getController();

        Map<String, Object> params = Maps.newHashMap();
        params.put("Target", tgtCard);

        List<CounterType> list = Lists.newArrayList(tgtCard.getCounters().keySet());
        if (ctype != null) {
            list = Lists.newArrayList(ctype);
        }

        String prompt = Localizer.getInstance().getMessage("lblSelectCounterTypeToAddOrRemove");
        CounterType chosenType = pc.chooseCounterType(list, sa, prompt, params);

        params.put("CounterType", chosenType);
        prompt = Localizer.getInstance().getMessage("lblWhatToDoWithTargetCounter",  chosenType.getName()) + " ";
        boolean putCounter = pc.chooseBinary(sa, prompt, BinaryChoiceType.AddOrRemove, params);

        if (putCounter) {
            tgtCard.addCounter(chosenType, counterAmount, pl, table);
        } else {
            tgtCard.subtractCounter(chosenType, counterAmount);
            if (sa.hasParam("RememberRemovedCards")) {
                sa.getHostCard().addRemembered(tgtCard);
            }
        }
    }
}

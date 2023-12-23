package forge.game.ability.effects;

import java.util.EnumSet;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.CardType;
import forge.card.RemoveType;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class AmassEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder("Amass ");
        final Card card = sa.getHostCard();
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);
        final String type = sa.getParam("Type");

        sb.append(CardType.getPluralType(type)).append(" ").append(amount).append(" (Put ");

        sb.append(Lang.nounWithNumeral(amount, "+1/+1 counter"));

        // TODO fix reminder after CR
        sb.append("on an Army you control. If you don't control one, create a 0/0 black " + type + " Army creature token first.)");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();

        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);
        final boolean remember = sa.hasParam("RememberAmass");
        final String type = sa.getParam("Type");

        // create army token if needed
        if (!Iterables.any(activator.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Army"))) {
            CardZoneTable triggerList = new CardZoneTable();
            MutableBoolean combatChanged = new MutableBoolean(false);

            final Card result = TokenInfo.getProtoType("b_0_0_army", sa, activator, false);
            // need to alter the token to add the Type from the Parameter
            result.setCreatureTypes(Lists.newArrayList(type, "Army"));
            result.setName(type + " Army Token");
            result.setTokenSpawningAbility(sa);

            makeTokenTable(makeTokenTableInternal(activator, result, 1), false, triggerList, combatChanged, sa);

            triggerList.triggerChangesZoneAll(game, sa);

            game.fireEvent(new GameEventTokenCreated());

            if (combatChanged.isTrue()) {
                game.updateCombatForView();
                game.fireEvent(new GameEventCombatChanged());
            }
        }

        Map<String, Object> params = Maps.newHashMap();
        params.put("CounterType", CounterType.get(CounterEnumType.P1P1));
        params.put("Amount", 1);

        CardCollectionView tgtCards = CardLists.getType(activator.getCardsIn(ZoneType.Battlefield), "Army");
        tgtCards = pc.chooseCardsForEffect(tgtCards, sa, Localizer.getInstance().getMessage("lblChooseAnArmy"), 1, 1, false, params);

        if (tgtCards.isEmpty()) {
            return;
        }
        GameEntityCounterTable table = new GameEntityCounterTable();
        for (final Card tgtCard : tgtCards) {
            tgtCard.addCounter(CounterEnumType.P1P1, amount, activator, table);
            if (remember) {
                card.addRemembered(tgtCard);
            }
        }
        table.replaceCounterEffect(game, sa, true);
        // change type after counters
        long ts = game.getNextTimestamp();
        for (final Card tgtCard : tgtCards) {
            tgtCard.addChangedCardTypes(CardType.parse(type, true), null, false, EnumSet.noneOf(RemoveType.class), ts, 0, true, false);
        }
    }

}

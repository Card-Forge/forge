package forge.game.ability.effects;

import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class EmpowerJaceEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder("Empower Jace ");
        final Card card = sa.getHostCard();
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);

        sb.append(amount).append(" (Put ");

        sb.append(Lang.nounWithNumeral(amount, "+1/+1 counter"));

        // TODO fix reminder after CR
        sb.append(" on a Jace token you control.");
        sb.append(" If you don't control one, first create a blue Jace planeswalker token with \"-1: Surveil 1.\" and \"-3: Draw a card.\")");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final Player p = getTargetPlayers(sa).getFirst();
        if (p == null) {
            return;
        }
        final int amount = AbilityUtils.calculateAmount(source, sa.getParamOrDefault("Num", "1"), sa);

        if (!p.getCardsIn(ZoneType.Battlefield).anyMatch(CardPredicates.isType("Jace").and(CardPredicates.TOKEN))) {
            CardZoneTable triggerList = new CardZoneTable();
            MutableBoolean combatChanged = new MutableBoolean(false);

            Card result = TokenInfo.getProtoType("u_empower_jace", sa, p, false);
            result.setTokenSpawningAbility(sa);

            makeTokenTable(makeTokenTableInternal(p, result, 1), false, triggerList, combatChanged, sa);

            triggerList.triggerChangesZoneAll(game, sa);

            game.fireEvent(new GameEventTokenCreated());

            if (combatChanged.isTrue()) {
                game.updateCombatForView();
                game.fireEvent(new GameEventCombatChanged());
            }
        }

        CardCollectionView tgtCards = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Jace").and(CardPredicates.TOKEN));
        if (tgtCards.isEmpty()) {
            return;
        }

        Map<String, Object> params = Maps.newHashMap();
        params.put("CounterType", CounterEnumType.LOYALTY);
        params.put("Amount", amount);
        Card tgt = p.getController().chooseSingleEntityForEffect(tgtCards, sa, Localizer.getInstance().getMessage("lblChooseaCard"), false, params);

        GameEntityCounterTable table = new GameEntityCounterTable();
        tgt.addCounter(CounterEnumType.LOYALTY, amount, p, table);
        table.replaceCounterEffect(game, sa);
    }
}

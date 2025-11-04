package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.card.TokenCreateTable;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class EndureEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();

        List<Card> tgt = getTargetCards(sa);

        sb.append(Lang.joinHomogenous(tgt));
        sb.append(" ");
        sb.append(tgt.size() > 1 ? "endure" : "endures");

        int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Num", "1"), sa);

        sb.append(" ").append(amount);
        sb.append(". ");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        String num = sa.getParamOrDefault("Num", "1");
        int amount = AbilityUtils.calculateAmount(host, num, sa);

        if (amount < 1) {
            return;
        }

        GameEntityCounterTable table = new GameEntityCounterTable();
        TokenCreateTable tokenTable = new TokenCreateTable();
        for (final Card c : GameActionUtil.orderCardsByTheirOwners(game, getTargetCards(sa), ZoneType.Battlefield, sa)) {
            final Player pl = c.getController();

            Card gamec = game.getCardState(c, null);

            Map<String, Object> params = Maps.newHashMap();
            params.put("RevealedCard", c);
            params.put("Amount", amount);
            if (gamec != null && gamec.isInPlay() && gamec.equalsWithGameTimestamp(c) && gamec.canReceiveCounters(CounterEnumType.P1P1)
                    && pl.getController().confirmAction(sa, null,
                            Localizer.getInstance().getMessage("lblEndureAction", c.getTranslatedName(), amount),
                            gamec, params)) {
                gamec.addCounter(CounterEnumType.P1P1, amount, pl, table);
            } else {
                final Card result = TokenInfo.getProtoType("w_x_x_spirit", sa, pl, false);

                // set PT
                result.setBasePowerString(num);
                result.setBasePower(amount);
                result.setBaseToughnessString(num);
                result.setBaseToughness(amount);

                tokenTable.put(pl, result, 1);
            }
        }
        table.replaceCounterEffect(game, sa, true);

        if (!tokenTable.isEmpty()) {
            CardZoneTable triggerList = new CardZoneTable();
            MutableBoolean combatChanged = new MutableBoolean(false);
            makeTokenTable(tokenTable, false, triggerList, combatChanged, sa);

            triggerList.triggerChangesZoneAll(game, sa);

            game.fireEvent(new GameEventTokenCreated());

            if (combatChanged.isTrue()) {
                game.updateCombatForView();
                game.fireEvent(new GameEventCombatChanged());
            }
        }
    }

}

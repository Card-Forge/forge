package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class IncubateEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (!StringUtils.isNumeric(sa.getParam("Amount"))) { // non-numeric too easy to miscalc, default to SpellDesc
            String desc = sa.getParamOrDefault("SpellDescription", "Please add SpellDescription for non-numeric");
            int idx = desc.indexOf("(");
            if (idx > 0) { //trim reminder text from StackDesc
                desc = desc.substring(0, desc.indexOf("(") - 1);
            }
            return desc;
        }

        final StringBuilder sb = new StringBuilder("Incubate ");
        final Card card = sa.getHostCard();
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Amount", "1"), sa);
        final int times = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Times", "1"), sa);

        sb.append(amount);
        if (times > 1) {
            sb.append(" ").append(times == 2 ? "twice" : Lang.nounWithNumeral(amount, "times"));
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final int times = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Times", "1"), sa);
        sa.putParam("WithCountersType", "P1P1");
        sa.putParam("WithCountersAmount", sa.getParamOrDefault("Amount", "1"));

        for (final Player p : getTargetPlayers(sa)) {
            for (int i = 0; i < times; i++) {
                CardZoneTable triggerList = new CardZoneTable();
                MutableBoolean combatChanged = new MutableBoolean(false);

                makeTokenTable(makeTokenTableInternal(p, "incubator_c_0_0_a_phyrexian", 1, sa), false,
                        triggerList, combatChanged, sa);

                triggerList.triggerChangesZoneAll(game, sa);
                triggerList.clear();

                game.fireEvent(new GameEventTokenCreated());

                if (combatChanged.isTrue()) {
                    game.updateCombatForView();
                    game.fireEvent(new GameEventCombatChanged());
                }
            }
        }
    }
}

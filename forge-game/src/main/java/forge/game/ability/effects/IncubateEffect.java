package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

import org.apache.commons.lang3.mutable.MutableBoolean;

public class IncubateEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder("Incubate ");
        final Card card = sa.getHostCard();
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Amount", "1"), sa);
        final int times = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Times", "1"), sa);

        sb.append(amount);
        if (times > 1) {
            sb.append(" ").append(times == 2 ? "twice" : Lang.nounWithNumeral(amount, "times"));
        }
        sb.append(".").append(" (Create an Incubator token with ");
        sb.append(Lang.nounWithNumeral(amount, "+1/+1 counter"));
        sb.append(" on it and \"{2}: Transform this artifact.\" It transforms into a 0/0 Phyrexian artifact creature.)");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();
        final String amtString = sa.getParamOrDefault("Amount", "1");
        final int times = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Times", "1"), sa);

        // create incubator token
        CardZoneTable triggerList = new CardZoneTable();
        MutableBoolean combatChanged = new MutableBoolean(false);

        sa.putParam("WithCountersType", "P1P1");
        sa.putParam("WithCountersAmount", amtString);

        makeTokenTable(makeTokenTableInternal(activator, "incubator", times, sa), false,
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

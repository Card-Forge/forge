package forge.game.trigger;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class TriggerBecomeMonarch extends Trigger {

    public TriggerBecomeMonarch(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        final Card host = this.getHostCard();
        final Game game = host.getGame();
        if (hasParam("ValidPlayer")) {
            if (!matchesValid(runParams.get(AbilityKey.Player), getParam("ValidPlayer").split(","),
                    host)) {
                return false;
            }
        }

        if (hasParam("BeginTurn")) {
            if (!matchesValid(game.getMonarchBeginTurn(), getParam("BeginTurn").split(","),
                    host)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblPlayer")).append(": ").append(sa.getTriggeringObject(AbilityKey.Player)).append(", ");
        return sb.toString();
    }
}

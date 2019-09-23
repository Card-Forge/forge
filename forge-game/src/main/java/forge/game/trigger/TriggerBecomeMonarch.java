package forge.game.trigger;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TriggerBecomeMonarch extends Trigger {

    public TriggerBecomeMonarch(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        final Card host = this.getHostCard();
        final Game game = host.getGame();
        if (this.mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(runParams2.get("Player"), this.mapParams.get("ValidPlayer").split(","),
                    host)) {
                return false;
            }
        }

        if (this.mapParams.containsKey("BeginTurn")) {
            if (!matchesValid(game.getMonarchBeginTurn(), this.mapParams.get("BeginTurn").split(","),
                    host)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObjectsFrom(this, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(sa.getTriggeringObject(AbilityKey.Player)).append(", ");
        return sb.toString();
    }
}

package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TriggerEnteredRoom extends Trigger {

    public TriggerEnteredRoom(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidCard", runParams.get(AbilityKey.Card))) {
            return false;
        }

        if (!matchesValidParam("ValidRoom", runParams.get(AbilityKey.RoomName))) {
            return false;
        }

        return true;
    }

    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.RoomName);
    }

    public String getImportantStackObjects(SpellAbility sa) {
        Object roomName = sa.getTriggeringObject(AbilityKey.RoomName);
        if (roomName != null) {
            StringBuilder sb = new StringBuilder("Room: ");
            sb.append(roomName);
            return sb.toString();
        }
        return "";
    }
}

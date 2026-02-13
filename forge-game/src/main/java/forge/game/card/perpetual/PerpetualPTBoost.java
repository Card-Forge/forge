package forge.game.card.perpetual;

import forge.game.card.Card;
import forge.game.staticability.StaticAbility;

public record PerpetualPTBoost(Integer power, Integer toughness) implements PerpetualInterface {
    @Override
    public StaticAbility createEffect(Card c) {
        StringBuilder sb = new StringBuilder("Mode$ Continuous | AffectedDefined$ Self | EffectZone$ All ");
        if (power != null) {
            sb.append("| AddPower$ ").append(power);
        }
        if (toughness != null) {
            sb.append("| AddToughness$ ").append(toughness);
        }
        return StaticAbility.create(sb.toString(), c, c.getCurrentState(), true);
    }
}

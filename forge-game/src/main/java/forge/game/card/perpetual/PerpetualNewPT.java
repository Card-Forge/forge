package forge.game.card.perpetual;

import forge.game.card.Card;
import forge.game.staticability.StaticAbility;

public record PerpetualNewPT(Integer power, Integer toughness) implements PerpetualInterface {
    @Override
    public StaticAbility createEffect(Card c) {
        StringBuilder sb = new StringBuilder("Mode$ Continuous | AffectedDefined$ Self | EffectZone$ All ");
        if (power != null) {
            sb.append("| SetPower$ ").append(power);
        }
        if (toughness != null) {
            sb.append("| SetToughness$ ").append(toughness);
        }
        return StaticAbility.create(sb.toString(), c, c.getCurrentState(), true);
    }
}

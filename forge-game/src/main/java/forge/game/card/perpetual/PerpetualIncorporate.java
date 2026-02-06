package forge.game.card.perpetual;

import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.staticability.StaticAbility;

public record PerpetualIncorporate(ManaCost incorporate) implements PerpetualInterface {
    @Override
    public StaticAbility createEffect(Card c) {
        String s = "Mode$ Continuous | AffectedDefined$ Self | EffectZone$ All | Incorporate$ " + incorporate.getShortString();
        return StaticAbility.create(s, c, c.getCurrentState(), true);
    }
}

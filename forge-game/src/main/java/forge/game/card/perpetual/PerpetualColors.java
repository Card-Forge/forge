package forge.game.card.perpetual;

import java.util.stream.Collectors;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.card.Card;
import forge.game.staticability.StaticAbility;

public record PerpetualColors(ColorSet colors, boolean overwrite) implements PerpetualInterface {
    @Override
    public StaticAbility createEffect(Card c) {
        StringBuilder sb = new StringBuilder("Mode$ Continuous | AffectedDefined$ Self | EffectZone$ All | ");
        sb.append( overwrite ? "SetColor" : "AddColor").append("$ ");
        sb.append(colors.stream().map(MagicColor.Color::getName).collect(Collectors.joining(",")));

        return StaticAbility.create(sb.toString(), c, c.getCurrentState(), true);
    }
}

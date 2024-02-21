package forge.game.ability.effects;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class ManifestEffect extends ManifestBaseEffect {

    protected String getDefaultMessage() {
        return Localizer.getInstance().getMessage("lblChooseCardToManifest");
    }
    protected Card internalEffect(Card c, Player p, SpellAbility sa, Map<AbilityKey, Object> moveParams) {
        final Card source = sa.getHostCard();
        Card rem = c.manifest(p, sa, moveParams);
        if (rem != null && sa.hasParam("RememberManifested") && rem.isManifested()) {
            source.addRemembered(rem);
        }
        return rem;
    }
}

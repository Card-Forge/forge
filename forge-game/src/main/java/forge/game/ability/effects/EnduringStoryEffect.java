package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

public class EnduringStoryEffect extends SpellAbilityEffect {

    /** Artifacts, legendaries and/or Sagas - a permanent that is more than one of these still counts once - i.e. historic. */
    public static int countStoried(final Player p) {
        return CardLists.count(p.getCardsIn(ZoneType.Battlefield), Card::isHistoric);
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        List<Player> tgt = getTargetPlayers(sa);

        sb.append(Lang.joinHomogenous(tgt));
        sb.append(tgt.size() > 1 ? " have" : " has");
        sb.append(" an enduring story. ");

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        for (Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            // Player needs three or more artifacts, legendaries and/or Sagas on the battlefield
            if (countStoried(p) >= 3) {
                p.setEnduringStory(true, sa.getOriginalHost().getSetCode());
            }
        }
    }
}

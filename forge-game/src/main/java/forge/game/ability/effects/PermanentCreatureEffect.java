package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PermanentCreatureEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Player p = sa.getActivatingPlayer();
        sa.getHostCard().setController(p, 0);
        final Card c = p.getGame().getAction().moveTo(p.getZone(ZoneType.Battlefield), sa.getHostCard());
        sa.setHostCard(c);
    }

    @Override
    public String getStackDescription(final SpellAbility sa) {
        final Card sourceCard = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();
        sb.append(sourceCard.getName()).append(" - Creature ").append(sourceCard.getNetPower());
        sb.append(" / ").append(sourceCard.getNetToughness());
        return sb.toString();
    }
}

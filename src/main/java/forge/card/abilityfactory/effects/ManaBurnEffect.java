package forge.card.abilityfactory.effects;

import java.util.List;

import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;


// TakeDamage = will mana burnt be removed from life
// May also add filters on colors
public class ManaBurnEffect extends SpellEffect {

    @Override
    public void resolve(SpellAbility sa) {
        
        List<Player> targets = getDefinedPlayersBeforeTargetOnes(sa);
        for(Player p : targets) {
            int taken = p.getManaPool().clearPool(false);
            if ( "True".equalsIgnoreCase(sa.getParam("TakeDamage")) )
                p.addDamage(taken, sa.getSourceCard());
        }
        
    }

}

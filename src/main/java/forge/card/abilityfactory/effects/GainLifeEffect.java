package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Map;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class GainLifeEffect extends SpellEffect {

    // *************************************************************************
    // ************************ EXCHANGE LIFE **********************************
    // *************************************************************************
    
        
    
    // *************************************************************************
    // ************************* LOSE LIFE *************************************
    // *************************************************************************
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        }
        else {
            final String conditionDesc = params.get("ConditionDescription");
            if (conditionDesc != null) {
                sb.append(conditionDesc).append(" ");
            }
    
            ArrayList<Player> tgtPlayers;
    
            final Target tgt = sa.getTarget();
            if (tgt != null && !params.containsKey("Defined")) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
    
            for (final Player player : tgtPlayers) {
                sb.append(player).append(" ");
            }
    
            sb.append("gains ").append(amount).append(" life.");
        }
    
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {

    
        final int lifeAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.gainLife(lifeAmount, sa.getSourceCard());
            }
        }
    }
    
}
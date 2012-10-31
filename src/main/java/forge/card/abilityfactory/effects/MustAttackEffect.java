package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Map;

import forge.Card;
import forge.GameEntity;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class MustAttackEffect extends SpellEffect { 
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final StringBuilder sb = new StringBuilder();
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
    
        // end standard pre-
    
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        String defender = null;
        if (params.get("Defender").equals("Self")) {
            defender = host.toString();
        } else {
            // TODO - if more needs arise in the future
        }
    
        for (final Player player : tgtPlayers) {
            sb.append("Creatures ").append(player).append(" controls attack ");
            sb.append(defender).append(" during his or her next turn.");
        }
    
        // begin standard post-
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                GameEntity entity;
                if (params.get("Defender").equals("Self")) {
                    entity = sa.getSourceCard();
                } else {
                    entity = p.getOpponent();
                }
                // System.out.println("Setting mustAttackEntity to: "+entity);
                p.setMustAttackEntity(entity);
            }
        }

    } // mustAttackResolve()

    // **************************************************************
    // ********************* RemoveFromCombat ***********************
    // **************************************************************

}
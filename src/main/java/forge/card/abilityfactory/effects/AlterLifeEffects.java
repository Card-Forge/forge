package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AlterLifeEffects {

    public static class PoisonEffect extends SpellEffect {

        /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        public void resolve(Map<String, String> params, SpellAbility sa) {
            final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("Num"), sa);
        
            ArrayList<Player> tgtPlayers;
        
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        
            for (final Player p : tgtPlayers) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    p.addPoisonCounters(amount, sa.getSourceCard());
                }
            }
        }

        /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        public String getStackDescription(Map<String, String> params, SpellAbility sa) {
            final StringBuilder sb = new StringBuilder();
            final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("Num"), sa);
        
            if (!(sa instanceof AbilitySub)) {
                sb.append(sa.getSourceCard()).append(" - ");
            } else {
                sb.append(" ");
            }
        
            final String conditionDesc = params.get("ConditionDescription");
            if (conditionDesc != null) {
                sb.append(conditionDesc).append(" ");
            }
        
            ArrayList<Player> tgtPlayers;
        
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        
            if (tgtPlayers.size() > 0) {
                final Iterator<Player> it = tgtPlayers.iterator();
                while (it.hasNext()) {
                    final Player p = it.next();
                    sb.append(p);
                    if (it.hasNext()) {
                        sb.append(", ");
                    } else {
                        sb.append(" ");
                    }
                }
            }
        
            sb.append("get");
            if (tgtPlayers.size() < 2) {
                sb.append("s");
            }
            sb.append(" ").append(amount).append(" poison counter");
            if (amount != 1) {
                sb.append("s.");
            } else {
                sb.append(".");
            }
        
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                sb.append(abSub.getStackDescription());
            }
        
            return sb.toString();
        } 
    
    }
    
    public static class SetLifeEffect extends SpellEffect {

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
                    p.setLife(lifeAmount, sa.getSourceCard());
                }
            }
        }

        /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        public String getStackDescription(Map<String, String> params, SpellAbility sa) {
            final StringBuilder sb = new StringBuilder();
            final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);
        
            if (!(sa instanceof AbilitySub)) {
                sb.append(sa.getSourceCard()).append(" -");
            } else {
                sb.append(" ");
            }
        
            final String conditionDesc = params.get("ConditionDescription");
            if (conditionDesc != null) {
                sb.append(conditionDesc).append(" ");
            }
        
            ArrayList<Player> tgtPlayers;
        
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        
            for (final Player player : tgtPlayers) {
                sb.append(player).append(" ");
            }
        
            sb.append("life total becomes ").append(amount).append(".");
        
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                sb.append(abSub.getStackDescription());
            }
        
            return sb.toString();
        }
    
    }

    public static class ExchangeLifeEffect extends SpellEffect {

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
            final Player activatingPlayer = sa.getActivatingPlayer();
        
            if (sa instanceof AbilitySub) {
                sb.append(" ");
            } else {
                sb.append(sa.getSourceCard()).append(" -");
            }
        
            ArrayList<Player> tgtPlayers;
        
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        
            if (tgtPlayers.size() == 1) {
                sb.append(activatingPlayer).append(" exchanges life totals with ");
                sb.append(tgtPlayers.get(0));
            } else if (tgtPlayers.size() > 1) {
                sb.append(tgtPlayers.get(0)).append(" exchanges life totals with ");
                sb.append(tgtPlayers.get(1));
            }
            sb.append(".");
        
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
            final Card source = sa.getSourceCard();
            Player p1;
            Player p2;
        
            ArrayList<Player> tgtPlayers;
        
            final Target tgt = sa.getTarget();
            if ((tgt != null) && !params.containsKey("Defined")) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        
            if (tgtPlayers.size() == 1) {
                p1 = sa.getActivatingPlayer();
                p2 = tgtPlayers.get(0);
            } else {
                p1 = tgtPlayers.get(0);
                p2 = tgtPlayers.get(1);
            }
        
            final int life1 = p1.getLife();
            final int life2 = p2.getLife();
        
            if ((life1 > life2) && p1.canLoseLife()) {
                final int diff = life1 - life2;
                p1.loseLife(diff, source);
                p2.gainLife(diff, source);
            } else if ((life2 > life1) && p2.canLoseLife()) {
                final int diff = life2 - life1;
                p2.loseLife(diff, source);
                p1.gainLife(diff, source);
            } else {
                // they are equal, so nothing to do
            }
        
        }
        
    }
    
    public static class GainLifeEffect extends SpellEffect {

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

    
    public static class LoseLifeEffect extends SpellEffect {

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
        
            final String conditionDesc = params.get("ConditionDescription");
            if (conditionDesc != null) {
                sb.append(conditionDesc).append(" ");
            }
        
            ArrayList<Player> tgtPlayers;
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        
            for (final Player player : tgtPlayers) {
                sb.append(player).append(" ");
            }
        
            sb.append("loses ").append(amount).append(" life.");
        
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

            int lifeLost = 0;
        
            final int lifeAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("LifeAmount"), sa);
        
            ArrayList<Player> tgtPlayers;
        
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        
            for (final Player p : tgtPlayers) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    lifeLost += p.loseLife(lifeAmount, sa.getSourceCard());
                }
            }
            sa.getSourceCard().setSVar("AFLifeLost", "Number$" + Integer.toString(lifeLost));
        }
        
    }

}

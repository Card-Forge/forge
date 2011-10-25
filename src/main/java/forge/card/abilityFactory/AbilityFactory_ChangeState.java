package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Player;
import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

/** 
 * AbilityFactory for abilities that cause cards to change states.
 *
 */
public class AbilityFactory_ChangeState {

    public static SpellAbility getChangeStateAbility(final AbilityFactory AF) {
        SpellAbility ret = new Ability_Activated(AF.getHostCard() ,AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = -1083427558368639457L;

            @Override
            public String getStackDescription() {
                return changeStateStackDescription(AF,this);
            }
            
            @Override
            public void resolve() {
                changeStateResolve(AF,this);
            }
        };
        
        return ret;
    }
    
    public static SpellAbility getChangeStateSpell(final AbilityFactory AF) {
        SpellAbility ret = new Spell(AF.getHostCard()) {
            private static final long serialVersionUID = -7506856902233086859L;

            @Override
            public String getStackDescription() {
                return changeStateStackDescription(AF,this);
            }
            
            @Override
            public void resolve() {
                changeStateResolve(AF,this);
            }
        };
        
        return ret;
    }
    
    public static SpellAbility getChangeStateDrawback(final AbilityFactory AF) {
        Ability_Sub ret = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {

            private static final long serialVersionUID = -3793247725721587468L;

            @Override
            public String getStackDescription() {
                return changeStateStackDescription(AF,this);
            }
            
            @Override
            public boolean chkAI_Drawback() {
                
                //Gross generalization, but this always considers alternate states more powerful
                if (AF.getHostCard().isInAlternateState()) {
                   return false; 
                }
                
                return true;
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                if(!mandatory && AF.getHostCard().isInAlternateState()) {
                    return false;
                }
                
                return true;
            }

            @Override
            public void resolve() {
                changeStateResolve(AF,this);
            }
            
        };
        
        return ret;
    }
    
    private static String changeStateStackDescription(AbilityFactory AF, SpellAbility sa) {
        Map<String,String> params = AF.getMapParams();
        
        StringBuilder sb = new StringBuilder();
        Card host = AF.getHostCard();

        String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null)
            sb.append(conditionDesc).append(" ");

        ArrayList<Card> tgtCards;

        Target tgt = AF.getAbTgt();
        if (tgt != null)
            tgtCards = tgt.getTargetCards();
        else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (sa instanceof Ability_Sub)
            sb.append(" ");
        else
            sb.append(host).append(" - ");

        if(params.containsKey("Flip")) {
            sb.append("Flip");
        }
        else {
            sb.append("Transform ");           
        }
 

        Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            Card tgtC = it.next();
            if (tgtC.isFaceDown()) sb.append("Morph ").append("(").append(tgtC.getUniqueNumber()).append(")");
            else sb.append(tgtC);

            if (it.hasNext()) sb.append(", ");
        }
        sb.append(".");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }
    
    private static void changeStateResolve(AbilityFactory AF, SpellAbility sa) {
        
        ArrayList<Card> tgtCards;
        
        if (AF.getAbTgt() != null) {
            tgtCards = AF.getAbTgt().getTargetCards();
        }
        else {
            tgtCards = AbilityFactory.getDefinedCards(AF.getHostCard(), AF.getMapParams().get("Defined"), sa);
        }
        
        for (Card tgt : tgtCards) {
            if(AF.getAbTgt() != null) {
                if(!CardFactoryUtil.canTarget(AF.getHostCard(), tgt)) {
                    continue;
                }
            }
            tgt.changeState();
        }
        
    }
    
    ////////////////////////////////////////////////
    //                  changeStateAll            //
    ////////////////////////////////////////////////
    
    public static SpellAbility getChangeStateAllAbility(final AbilityFactory AF) {
        SpellAbility ret = new Ability_Activated(AF.getHostCard(),AF.getAbCost(),AF.getAbTgt()) {

            private static final long serialVersionUID = 7841029107610111992L;

            @Override
            public String getStackDescription() {
                return changeStateAllStackDescription(AF,this);
            }
            
            @Override
            public void resolve() {
                changeStateAllResolve(AF,this);
            }
            
        };
        
        return ret;
    }
    
    public static SpellAbility getChangeStateAllSpell(final AbilityFactory AF) {
        SpellAbility ret = new Spell(AF.getHostCard()) {

            private static final long serialVersionUID = 4217632586060204603L;

            @Override
            public String getStackDescription() {
                return changeStateAllStackDescription(AF,this);
            }
            
            @Override
            public void resolve() {
                changeStateAllResolve(AF,this);
            }
        };
        
        return ret;
    }
    
    public static SpellAbility getChangeStateAllDrawback(final AbilityFactory AF) {
        Ability_Sub ret = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {

            private static final long serialVersionUID = 4047514893482113436L;

            @Override
            public String getStackDescription() {
                return changeStateAllStackDescription(AF,this);
            }
            
            @Override
            public boolean chkAI_Drawback() {
                
                //Gross generalization, but this always considers alternate states more powerful
                if (AF.getHostCard().isInAlternateState()) {
                   return false; 
                }
                
                return true;
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return true;
            }

            @Override
            public void resolve() {
                changeStateAllResolve(AF,this);
            }
            
        };
        
        return ret;
    }
    
    private static void changeStateAllResolve(AbilityFactory AF,SpellAbility sa) {
        HashMap<String, String> params = AF.getMapParams();

        Card card = sa.getSourceCard();
        
        Target tgt = AF.getAbTgt();
        Player targetPlayer = null;
        if (tgt != null)
            targetPlayer = tgt.getTargetPlayers().get(0);

        String Valid = "";

        if (params.containsKey("ValidCards"))
            Valid = params.get("ValidCards");

        // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_ to use the X variable
        // We really need a better solution to this
        if (Valid.contains("X"))
            Valid = Valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(card, "X", sa)));

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        
        if (targetPlayer != null) {
            list = list.getController(targetPlayer);
        }

        list = AbilityFactory.filterListByType(list, Valid, sa);

        boolean remChanged = params.containsKey("RememberChanged");
        if (remChanged)
            card.clearRemembered();

        for (int i = 0; i < list.size(); i++)
            if (list.get(i).changeState())
                card.addRemembered(list.get(i));
    }
    
    private static String changeStateAllStackDescription(final AbilityFactory AF, final SpellAbility sa) {
      
        Card host = AF.getHostCard();
        Map<String,String> params = AF.getMapParams();
        StringBuilder sb = new StringBuilder();
        
        if (sa instanceof Ability_Sub)
            sb.append(" ");
        else
            sb.append(host).append(" - ");

        if(params.containsKey("Flip")) {
            sb.append("Flip");
        }
        else {
            sb.append("Transform ");           
        }
 
        sb.append(" permanents.");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }
}

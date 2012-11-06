package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.abilityfactory.ai.CharmAi;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class CharmEffect extends SpellEffect {
    
    public static List<AbilitySub> makePossibleOptions(final SpellAbility sa, final Map<String, String> params) {
        final Card source = sa.getSourceCard();
    
        final String[] saChoices = params.get("Choices").split(",");
        List<AbilitySub> choices = new ArrayList<AbilitySub>();
        for (final String saChoice : saChoices) {
            final String ab = source.getSVar(saChoice);
            final AbilityFactory charmAF = new AbilityFactory();
            choices.add((AbilitySub)charmAF.getAbility(ab, source));
        }
        return choices;
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        // all chosen modes have been chained as subabilities to this sa.
        // so nothing to do in this resolve
    }


    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        // nothing stack specific for Charm
        
        return sb.toString();
    }
    
    public static void makeChoices(SpellAbility sa) {
        AbilityFactory af = sa.getAbilityFactory();
        final Map<String, String> params = af.getMapParams();
        //this resets all previous choices
        sa.setSubAbility(null);
        
        final int num = Integer.parseInt(params.containsKey("CharmNum") ? params.get("CharmNum") : "1");
        final int min = params.containsKey("MinCharmNum") ? Integer.parseInt(params.get("MinCharmNum")) : num;
        final List<AbilitySub> choices = makePossibleOptions(sa, params);
        
        List<AbilitySub> chosen = null;
        
        Player activator = sa.getActivatingPlayer();
        if ( activator.isHuman() )
        {
            chosen = new ArrayList<AbilitySub>();
            for (int i = 0; i < num; i++) {
                AbilitySub a;
                if (i < min) {
                    a = GuiChoose.one("Choose a mode", choices);
                } else {
                    a = GuiChoose.oneOrNone("Choose a mode", choices);
                }
                if (null == a) {
                    break;
                }

                choices.remove(a);
                chosen.add(a);
            }
        }
        else 
            chosen = CharmAi.chooseOptionsAi(activator, true, choices, num, min);
        
        chainAbilities(sa, chosen);
    }

    private static void chainAbilities(SpellAbility sa, List<AbilitySub> chosen) {
        SpellAbility saDeepest = sa;
        while( saDeepest.getSubAbility() != null)
            saDeepest = saDeepest.getSubAbility();
        
        for(AbilitySub sub : chosen){
            saDeepest.setSubAbility(sub);
            sub.setActivatingPlayer(saDeepest.getActivatingPlayer());
            sub.setParent(saDeepest);
            
            // to chain the next one
            saDeepest = sub;
        }
    }
    
    

} 
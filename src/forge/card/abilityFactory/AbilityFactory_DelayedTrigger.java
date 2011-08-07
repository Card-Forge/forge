package forge.card.abilityFactory;

import forge.AllZone;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 5/18/11
 * Time: 8:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class AbilityFactory_DelayedTrigger {
    public static Ability_Sub getDrawback(final AbilityFactory AF)
    {
        final Ability_Sub drawback = new Ability_Sub(AF.getHostCard(),AF.getAbTgt()) {
			private static final long serialVersionUID = 6192972525033429820L;

			@Override
            public boolean chkAI_Drawback() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void resolve() {
                doResolve(AF,this);
            }
        };

        return drawback;
    }

    private static void doResolve(AbilityFactory AF,SpellAbility SA)
    {
        HashMap<String,String> mapParams = AF.getMapParams();

        if(mapParams.containsKey("Cost"))
            mapParams.remove("Cost");

        if(mapParams.containsKey("SpellDescription"))
        {
            mapParams.put("TriggerDescription",mapParams.get("SpellDescription"));
            mapParams.remove("SpellDescription");
        }

        Trigger delTrig = TriggerHandler.parseTrigger(mapParams,AF.getHostCard());

        AllZone.TriggerHandler.registerDelayedTrigger(delTrig);

        if(AF.hasSubAbility())
            if(SA.getSubAbility() != null)
                SA.getSubAbility().resolve();
    }
}

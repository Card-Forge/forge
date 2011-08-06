package forge.card.abilityFactory;

import forge.AllZone;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

public class AbilityFactory_Clash {

    public static SpellAbility getAbility(final AbilityFactory AF)
    {
        final SpellAbility abClash = new Ability_Activated(AF.getHostCard(),AF.getAbCost(),AF.getAbTgt()) {
			private static final long serialVersionUID = -8019637116128196248L;

			@Override
            public boolean canPlayAI()
            {
                return true;
            }

            @Override
            public boolean canPlay()
            {
                return true;
            }

            @Override
            public boolean doTrigger(boolean mandatory)
            {
                return true;
            }

            @Override
            public String getStackDescription()
            {
                return "Clash with an opponent.";
            }

            @Override
            public void resolve()
            {
                clashResolve(AF,this);
            }
        };

        return abClash;
    }

    public static SpellAbility getSpell(final AbilityFactory AF)
    {
        final SpellAbility spClash = new Spell(AF.getHostCard(),AF.getAbCost(),AF.getAbTgt()) {
			private static final long serialVersionUID = -4991665176268317172L;

			@Override
            public boolean canPlayAI()
            {
                return true;
            }

            @Override
            public boolean canPlay()
            {
                return true;
            }

            @Override
            public boolean doTrigger(boolean mandatory)
            {
                return true;
            }

            @Override
            public String getStackDescription()
            {
                return "Clash with an opponent.";
            }

            @Override
            public void resolve()
            {
                clashResolve(AF,this);
            }
        };

        return spClash;
    }

    public static SpellAbility getDrawback(final AbilityFactory AF)
    {
        final SpellAbility dbClash = new Ability_Sub(AF.getHostCard(),AF.getAbTgt()) {
			private static final long serialVersionUID = -3850086157052881360L;

			@Override
            public boolean canPlayAI()
            {
                return true;
            }

            @Override
            public boolean canPlay()
            {
                return true;
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(boolean mandatory)
            {
                return true;
            }

            @Override
            public String getStackDescription()
            {
                return " Clash with an opponent.";
            }

            @Override
            public void resolve() {
                clashResolve(AF,this);
            }
        };

        return dbClash;
    }

    private static void clashResolve(final AbilityFactory AF,final SpellAbility SA)
    {
        AbilityFactory AF_Outcomes = new AbilityFactory();
        boolean victory = AF.getHostCard().getController().clashWithOpponent(AF.getHostCard());

        if(victory)
        {
                if(AF.getMapParams().containsKey("WinSubAbility"))
                {
                    SpellAbility win = AF_Outcomes.getAbility(AF.getHostCard().getSVar(AF.getMapParams().get("WinSubAbility")),AF.getHostCard());
                    win.setActivatingPlayer(AF.getHostCard().getController());
                    ((Ability_Sub)win).setParent(SA);

                    win.resolve();
                }

                //Run triggers
                HashMap<String,Object> runParams = new HashMap<String,Object>();
                runParams.put("Player",AF.getHostCard().getController());
                runParams.put("Won","True");
                AllZone.TriggerHandler.runTrigger("Clashed",runParams);
        }
        else
        {
                if(AF.getMapParams().containsKey("OtherwiseSubAbility"))
                {
                    SpellAbility otherwise = AF_Outcomes.getAbility(AF.getHostCard().getSVar(AF.getMapParams().get("OtherwiseSubAbility")),AF.getHostCard());
                    otherwise.setActivatingPlayer(AF.getHostCard().getController());
                    ((Ability_Sub)otherwise).setParent(SA);

                    otherwise.resolve();
                }
                //Run triggers
                HashMap<String,Object> runParams = new HashMap<String,Object>();
                runParams.put("Player",AF.getHostCard().getController());
                runParams.put("Won","False");
                AllZone.TriggerHandler.runTrigger("Clashed",runParams);
        }

        //Oldstyle drawbacks shouldn't be necessary anymore?
        if(AF.hasSubAbility())
            if (SA.getSubAbility() != null)
                SA.getSubAbility().resolve();
    }

}

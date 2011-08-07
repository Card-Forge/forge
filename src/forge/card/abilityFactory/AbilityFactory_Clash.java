package forge.card.abilityFactory;

import forge.AllZone;
import forge.Card;
import forge.GameActionUtil;
import forge.Player;
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

        //Run triggers
        HashMap<String,Object> runParams = new HashMap<String,Object>();
        runParams.put("Player",AF.getHostCard().getController());

        if(victory)
        {
                if(AF.getMapParams().containsKey("WinSubAbility"))
                {
                    SpellAbility win = AF_Outcomes.getAbility(AF.getHostCard().getSVar(AF.getMapParams().get("WinSubAbility")),AF.getHostCard());
                    win.setActivatingPlayer(AF.getHostCard().getController());
                    ((Ability_Sub)win).setParent(SA);

                    win.resolve();
                }
                runParams.put("Won","True");
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
                runParams.put("Won","False");
        }

        AllZone.TriggerHandler.runTrigger("Clashed",runParams);
    }
    
    // *************************************************************************
    // ************************* FlipACoin *************************************
    // *************************************************************************

    public static SpellAbility getAbilityFlip(final AbilityFactory af) {
    	final SpellAbility abFlip = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -8293336773930687488L;

			@Override
    		public boolean canPlayAI() {
    			return true;
    		}
			
    		@Override
    		public boolean doTrigger(boolean mandatory) {
    			return true;
    		}

    		@Override
    		public String getStackDescription() {
    			return flipGetStackDescription(af, this);
    		}

    		@Override
    		public void resolve() {
    			flipResolve(af, this);
    		}
    	};

    	return abFlip;
    }

    public static SpellAbility getSpellFlip(final AbilityFactory af) {
    	final SpellAbility spFlip = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -4402144245527547151L;

			@Override
    		public boolean canPlayAI() {
    			return true;
    		}

    		@Override
    		public boolean canPlay() {
    			return true;
    		}

    		@Override
    		public boolean doTrigger(boolean mandatory) {
    			return true;
    		}

    		@Override
    		public String getStackDescription() {
    			return flipGetStackDescription(af, this);
    		}

    		@Override
    		public void resolve() {
    			flipResolve(af, this);
    		}
    	};

    	return spFlip;
    }

    public static SpellAbility getDrawbackFlip(final AbilityFactory af) {
    	final SpellAbility dbFlip = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = 8581978154811461324L;

			@Override
    		public boolean canPlayAI() {
    			return true;
    		}

    		@Override
    		public boolean canPlay() {
    			return true;
    		}

    		@Override
    		public boolean chkAI_Drawback() {
    			return true;
    		}

    		@Override
    		public boolean doTrigger(boolean mandatory) {
    			return true;
    		}

    		@Override
    		public String getStackDescription() {
    			return flipGetStackDescription(af, this);
    		}

    		@Override
    		public void resolve() {
    			flipResolve(af, this);
    		}
    	};

    	return dbFlip;
    }
    
    private static String flipGetStackDescription(AbilityFactory af, SpellAbility sa) {
    	HashMap<String,String> params = af.getMapParams();
    	Card host = af.getHostCard();
    	Player player = params.containsKey("OpponentCalls") ? host.getController().getOpponent() : host.getController();
    	
    	StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard()).append(" - ");
		else
			sb.append(" ");

		sb.append(player).append(" flips a coin.");

		Ability_Sub abSub = sa.getSubAbility();
		if(abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
    }

    private static void flipResolve(final AbilityFactory af, final SpellAbility sa) {
    	HashMap<String,String> params = af.getMapParams();
    	Card host = af.getHostCard();
    	Player player = host.getController();

    	AbilityFactory AF_Outcomes = new AbilityFactory();
    	boolean victory = GameActionUtil.flipACoin(player, sa.getSourceCard());

    	//Run triggers
    	//HashMap<String,Object> runParams = new HashMap<String,Object>();
    	//runParams.put("Player", player);

    	if(victory) {
    		if(params.containsKey("WinSubAbility")) {
    			SpellAbility win = AF_Outcomes.getAbility(host.getSVar(params.get("WinSubAbility")), host);
    			win.setActivatingPlayer(player);
    			((Ability_Sub)win).setParent(sa);

    			win.resolve();
    		}
    		//runParams.put("Won","True");
    	}
    	else {
    		if(params.containsKey("LoseSubAbility")) {
    			SpellAbility lose = AF_Outcomes.getAbility(host.getSVar(params.get("LoseSubAbility")), host);
    			lose.setActivatingPlayer(player);
    			((Ability_Sub)lose).setParent(sa);

    			lose.resolve();
    		}
    		//runParams.put("Won","False");
    	}

    	//AllZone.TriggerHandler.runTrigger("FlipsACoin",runParams);
    }

}//end class AbilityFactory_Clash

package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

public class AbilityFactory_Animate {

	//**************************************************************
	// *************************** Animate *************************
	//**************************************************************

	public static SpellAbility createAbilityAnimate(final AbilityFactory AF) {
		final SpellAbility abAnimate = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 1938171749867735155L;
			final AbilityFactory af = AF;

			public boolean canPlayAI() {
				return animateCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				animateResolve(af, this);
			}

			public String getStackDescription() {
				return animateStackDescription(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return animateTriggerAI(af, this, mandatory);
			}
		};
		return abAnimate;
	}

	public static SpellAbility createSpellAnimate(final AbilityFactory AF) {
		final SpellAbility spAnimate = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -4047747186919390147L;
			final AbilityFactory af = AF;

			public boolean canPlayAI() {
				return animateCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				animateResolve(af, this);
			}

			public String getStackDescription() {
				return animateStackDescription(af, this);
			}
		};
		return spAnimate;
	}

	public static SpellAbility createDrawbackAnimate(final AbilityFactory AF) {
		final SpellAbility dbAnimate = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = -8659938411460952874L;
			final AbilityFactory af = AF;

			@Override
			public void resolve() {
				animateResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return animatePlayDrawbackAI(af, this);
			}

			public String getStackDescription() {
				return animateStackDescription(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return animateTriggerAI(af, this, mandatory);
			}
		};
		return dbAnimate;
	}

	private static String animateStackDescription(final AbilityFactory af, SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		//TODO - add support for X
		//String powerString = params.get("Power");
		//String toughnessString = params.get("Toughness");
		int power = Integer.parseInt(params.get("Power"));
		int toughness = Integer.parseInt(params.get("Toughness"));
		boolean permanent = params.containsKey("Permanent") ? true : false;
		ArrayList<String> types = new ArrayList<String>(Arrays.asList(params.get("Types").split(",")));
		final ArrayList<String> keywords = new ArrayList<String>();
		if(params.containsKey("Keywords")) keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
		ArrayList<String> colors = new ArrayList<String>();
		if(params.containsKey("Colors")) colors.addAll(Arrays.asList(params.get("Colors").split(",")));

		StringBuilder sb = new StringBuilder();

		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(sa.getSourceCard().getName()).append(" - ");

		Target tgt = af.getAbTgt();
		ArrayList<Card> tgts;
		if (tgt != null)
			tgts = tgt.getTargetCards();
		else
			tgts = AbilityFactory.getDefinedCards(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);

		for(Card c : tgts) {
			sb.append(c).append(" ");
		}
		sb.append("become");
		if(tgts.size() == 1) sb.append("s a");
		sb.append(" ").append(power).append("/").append(toughness);
		if(colors.size() > 0) sb.append(" ");
		for(int i = 0; i < colors.size(); i++) {
			sb.append(colors.get(i));
			if(i < (colors.size() - 1)) sb.append(" and ");
		}
		sb.append(" ");
		for(int i = types.size() - 1; i >= 0; i--) {
			sb.append(types.get(i));
			sb.append(" ");
		}
		if(keywords.size() > 0) sb.append("with ");
		for(int i = 0; i < keywords.size(); i++) {
			sb.append(keywords.get(i));
			if(i < (keywords.size() - 1)) sb.append(" and ");
		}
		//sb.append(types)
		//sb.append(keywords)
		//sb.append(triggers)
		if(!permanent) sb.append(" until end of turn.");
		else sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null)
			sb.append(abSub.getStackDescription());

		return sb.toString();
	}

	private static boolean animateCanPlayAI(final AbilityFactory af, SpellAbility sa) {
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		boolean useAbility = true;
		
		//TODO - add some kind of check to answer "Am I going to attack with this?"
		//TODO - add some kind of check for during human turn to answer "Can I use this to block something?"
		
		//don't activate during main2
		if(AllZone.Phase.getPhase().equals(Constant.Phase.Main2))
        	return false;
		
		if(null == tgt) {
			ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, af.getMapParams().get("Defined"), sa);
			
			boolean bFlag = false;
			for(Card c : defined) {
				bFlag |= (!c.isCreature() && !c.isTapped());
			}
			
			if (!bFlag)	// All of the defined stuff is animated, not very useful
				return false;
		}
		else{
			tgt.resetTargets();
			useAbility &= animateTgtAI(af, sa);
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	useAbility &= subAb.chkAI_Drawback();
		
		return useAbility;
	}// end animateCanPlayAI()

	private static boolean animatePlayDrawbackAI(final AbilityFactory af, SpellAbility sa) {
		// AI should only activate this during Human's turn
		boolean chance = animateTgtAI(af, sa);

		// todo: restrict the subAbility a bit

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();

		return chance;
	}

	private static boolean animateTriggerAI(final AbilityFactory af, SpellAbility sa, boolean mandatory) {
		if(!ComputerUtil.canPayCost(sa))	// If there is a cost payment
			return false;

		boolean chance = animateTgtAI(af, sa);

		// Improve AI for triggers. If source is a creature with:
		// When ETB, sacrifice a creature. Check to see if the AI has something to sacrifice

		// Eventually, we can call the trigger of ETB abilities with not mandatory as part of the checks to cast something


		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();

		return chance || mandatory;
	}

	private static boolean animateTgtAI(AbilityFactory af, SpellAbility sa) {
		//This is reasonable for now.  Kamahl, Fist of Krosa and a sorcery or two are the only things
		//that animate a target.  Those can just use SVar:RemAIDeck:True until this can do a reasonalbly
		//good job of picking a good target
		return false;
	}

	private static void animateResolve(final AbilityFactory af, final SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card source = sa.getSourceCard();
		String db = params.get("SubAbility");

		//AF specific params
		int power = Integer.parseInt(params.get("Power"));
		int toughness = Integer.parseInt(params.get("Toughness"));
		boolean permanent = params.containsKey("Permanent") ? true : false;
		ArrayList<String> types = new ArrayList<String>(Arrays.asList(params.get("Types").split(",")));
		final ArrayList<String> keywords = new ArrayList<String>();
		if(params.containsKey("Keywords")) keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
		ArrayList<String> colors = new ArrayList<String>();
		if(params.containsKey("Colors")) colors.addAll(Arrays.asList(params.get("Colors").split(",")));

		String colorDesc = "";
		for(String col : colors) {
			if(col.equals("White")) {
				colorDesc += "W";
			}
			else if(col.equals("Blue")) {
				colorDesc += "U";
			}
			else if(col.equals("Black")) {
				colorDesc += "B";
			}
			else if(col.equals("Red")) {
				colorDesc += "R";
			}
			else if(col.equals("Green")) {
				colorDesc += "G";
			}
			else if(col.equals("Colorless")) {
				colorDesc = "C";
			}
		}
		final String finalDesc = colorDesc;

		Target tgt = af.getAbTgt();
		ArrayList<Card> tgts;
		if (tgt != null)
			tgts = tgt.getTargetCards();
		else
			tgts = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
		for(final Card c : tgts){
			//final ArrayList<Card_Color> originalColors = c.getColor();
			final ArrayList<String> originalTypes = c.getType();
			final long timestamp = doAnimate(c, power, toughness, types, finalDesc, keywords);

			final Command unactivate = new Command() {
				private static final long serialVersionUID = -5861759814760561373L;

				public void execute() {
					doUnanimate(c, originalTypes, finalDesc, keywords, timestamp);
				}
			};

			if(!permanent) AllZone.EndOfTurn.addUntil(unactivate);
		}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
				abSub.resolve();
			}
			else
				CardFactoryUtil.doDrawBack(db, 0, source.getController(), source.getController().getOpponent(), source.getController(), source, tgts.get(0), sa);
		}
	}

	private static long doAnimate(Card c, int power, int toughness, ArrayList<String> types, String colors, ArrayList<String> keywords) {
		c.setBaseAttack(power);
		c.setBaseDefense(toughness);

		for(String r : types) {
			// if the card doesn't have that type, add it
			if (!c.getType().contains(r))
				c.addType(r);
		}
		for(String k : keywords) {
			//this maybe should just blindly add since multiple instances of a keyword sometimes have effects
			//practically, this shouldn't matter though, and will display more cleanly
			if(!c.getIntrinsicKeyword().contains(k))
				c.addIntrinsicKeyword(k);	
		}

		long timestamp = c.addColor(colors, c, false, true);
		return timestamp;
	}

	private static void doUnanimate(Card c, ArrayList<String> originalTypes, String colorDesc, ArrayList<String> originalKeywords, long timestamp) {
		c.setBaseAttack(0);
		c.setBaseDefense(0);

		c.clearAllTypes();
		for(String type : originalTypes) {
			c.addType(type);
		}

		c.removeColor(colorDesc, c, false, timestamp);

		for(String k : originalKeywords) {
			//TODO - may want to look at saving off intrinsic and extrinsic separately and add back that way
			c.removeIntrinsicKeyword(k);
		}

		//any other unanimate cleanup
		c.unEquipAllCards();
	}

}//end class AbilityFactory_Animate
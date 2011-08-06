package forge;

import java.util.ArrayList;
import java.util.HashMap;

public class AbilityFactory_Mana {
	// ****************************** MANA ************************
	public static SpellAbility createAbilityMana(final AbilityFactory AF, final String produced){
		final Ability_Mana abMana = new Ability_Mana(AF.getHostCard(), AF.getAbCost(), produced){
			private static final long serialVersionUID = -1933592438783630254L;
			
			final AbilityFactory af = AF;
			
			public boolean canPlayAI()
			{
				return manaCanPlayAI(af);
			}
			
			@Override
			public void resolve() {
				manaResolve(this, af);
			}
			
		};
		return abMana;
	}
	
	public static SpellAbility createSpellMana(final AbilityFactory AF, final String produced){
		final SpellAbility spMana = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			// To get the mana to resolve properly, we need the spell to contain an Ability_Mana
			Ability_Cost tmp = new Ability_Cost("0", AF.getHostCard().getName(), false);
			Ability_Mana tmpMana = new Ability_Mana(AF.getHostCard(), tmp, produced){
				private static final long serialVersionUID = 1454043766057140491L;
				
			};
			
			public boolean canPlayAI()
			{
				return manaCanPlayAI(af);
			}
			
			@Override
            public String getStackDescription(){
            // when getStackDesc is called, just build exactly what is happening
                return manaStackDescription(tmpMana, af);
            }
			
			@Override
			public void resolve() {
				manaResolve(tmpMana, af);
			}
			
		};
		return spMana;
	}
	
	// Mana never really appears as a Drawback
	public static Ability_Sub createDrawbackMana(final AbilityFactory AF, final String produced){
		final Ability_Sub dbMana = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			// To get the mana to resolve properly, we need the spell to contain an Ability_Mana
			Ability_Cost tmp = new Ability_Cost("0", AF.getHostCard().getName(), false);
			Ability_Mana tmpMana = new Ability_Mana(AF.getHostCard(), tmp, produced){
				private static final long serialVersionUID = 1454043766057140491L;
				
			};
			
			@Override
            public String getStackDescription(){
            // when getStackDesc is called, just build exactly what is happening
                return manaStackDescription(tmpMana, af);
            }
			
			@Override
			public void resolve() {
				manaResolve(tmpMana, af);
			}

			@Override
			public boolean chkAI_Drawback() {
				// todo: AI shouldn't use this until he has a mana pool
				return false;
			}
			
		};
		return dbMana;
	}
	
	public static boolean manaCanPlayAI(final AbilityFactory af){
		// AI cannot use this properly until he has a ManaPool
		return false;
	}
	
	public static String manaStackDescription(Ability_Mana abMana, AbilityFactory af){
		StringBuilder sb = new StringBuilder();
		sb.append("Add ").append(generatedMana(abMana, af)).append(" to your mana pool.");

		if (abMana.getSubAbility() != null)
			sb.append(abMana.getSubAbility().getStackDescription());
		
		return sb.toString();
	}
	
	public static void manaResolve(Ability_Mana abMana, AbilityFactory af){
		// Spells are not undoable
		abMana.undoable = af.isAbility() && abMana.isUndoable();

		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		
		abMana.produceMana(generatedMana(abMana, af));
		
		// convert these to SubAbilities when appropriate		
		if (params.containsKey("Stuck")){
			abMana.undoable = false;
			card.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
		}
		
		String deplete = params.get("Deplete");
		if (deplete != null){
			int num = card.getCounters(Counters.getType(deplete));
			if (num == 0){
				abMana.undoable = false;
				AllZone.GameAction.sacrifice(card);
			}
		}
		
		doDrawback(af, abMana, card);
	}
	
	private static String generatedMana(Ability_Mana abMana, AbilityFactory af){
		// Calculate generated mana here for stack description and resolving
		HashMap<String,String> params = af.getMapParams();
		int amount = params.containsKey("Amount") ? AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), abMana) : 1;

		String baseMana = abMana.mana();

		if (params.containsKey("Bonus")){
			// For mana abilities that get a bonus
			// Bonus currently MULTIPLIES the base amount. Base Amounts should ALWAYS be Base
			int bonus = 0;
			if (params.get("Bonus").equals("UrzaLands")){
				if (hasUrzaLands(abMana.getActivatingPlayer()))
					bonus = Integer.parseInt(params.get("BonusProduced"));
			}

			amount += bonus;
		}

		StringBuilder sb = new StringBuilder();
		if (amount == 0)
			sb.append("0");
		else{
			try{
				// if baseMana is an integer(colorless), just multiply amount and baseMana
				int base = Integer.parseInt(baseMana);
				sb.append(base*amount);
			}
			catch(NumberFormatException e){
				for(int i = 0; i < amount; i++){
					if (i != 0)
						sb.append(" ");
					sb.append(baseMana);
				}
			}
		}
		return sb.toString();
	}
	
	// ****************************** MANAREFLECTED ************************
	public static SpellAbility createAbilityManaReflected(final AbilityFactory AF, final String produced){
		final Ability_Mana abMana = new Ability_Mana(AF.getHostCard(), AF.getAbCost(), produced){
			private static final long serialVersionUID = -1933592438783630254L;
			
			final AbilityFactory af = AF;

			public boolean canPlayAI()
			{
				return manaReflectedCanPlayAI(af);
			}
			
			@Override
			public void resolve() {
				manaReflectedResolve(this, af);
			}
			
		};
		abMana.setReflectedMana(true);
		return abMana;
	}
	
	public static SpellAbility createSpellManaReflected(final AbilityFactory AF, final String produced){
		// No Spell has Reflected Mana, but might as well put it in for the future
		final SpellAbility spMana = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			// To get the mana to resolve properly, we need the spell to contain an Ability_Mana
			Ability_Cost tmp = new Ability_Cost("0", AF.getHostCard().getName(), false);
			Ability_Mana tmpMana = new Ability_Mana(AF.getHostCard(), tmp, produced){
				private static final long serialVersionUID = 1454043766057140491L;

				// todo: maybe add can produce here, so old AI code can use reflected mana?
			};
			//tmpMana.setReflectedMana(true);
			
			public boolean canPlayAI()
			{
				return manaReflectedCanPlayAI(af);
			}
			
			@Override
			public void resolve() {
				manaReflectedResolve(tmpMana, af);
			}
			
		};
		return spMana;
	}
	
	public static boolean manaReflectedCanPlayAI(final AbilityFactory af){
		// AI cannot use this properly until he has a ManaPool
		return false;
	}
	
	public static void manaReflectedResolve(Ability_Mana abMana, AbilityFactory af){
		// Spells are not undoable
		abMana.undoable = af.isAbility() && abMana.isUndoable();

		Card card = af.getHostCard();
		
		ArrayList<String> colors = reflectableMana(abMana, af, new ArrayList<String>(), new ArrayList<Card>());
		
		String generated = generatedReflectedMana(abMana, af, colors);
		
		if (abMana.getCanceled()){
			abMana.undo();
			return;
		}

		abMana.produceMana(generated);

		doDrawback(af, abMana, card);
	}
	
	// add Colors and 
	private static ArrayList<String> reflectableMana(Ability_Mana abMana, AbilityFactory af, ArrayList<String> colors, ArrayList<Card> parents){
		// Here's the problem with reflectable Mana. If more than one is out, they need to Reflect each other, 
		// so we basically need to have a recursive list that send the parents so we don't infinite recurse. 
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		
		if (!parents.contains(card))
			parents.add(card);
		
		String colorOrType = params.get("ColorOrType"); // currently Color or Type, Type is colors + colorless
		String validCard = params.get("Valid"); 

		String reflectProperty = params.get("ReflectProperty");	// Produce (Reflecting Pool) or Is (Meteor Crater)

		int maxChoices = 5;	// Color is the default colorOrType
		if (colorOrType.equals("Type"))
			maxChoices++;
		
		CardList cards = null;
		if (validCard.equals("Sacrificed")){
			cards = abMana.getSacrificedCost();
		}
		else{
			cards = AllZoneUtil.getCardsInPlay().getValidCards(validCard, abMana.getActivatingPlayer(), card);
		}

		// remove anything cards that is already in parents
		for(Card p : parents)
			if (cards.contains(p))
				cards.remove(p);
		
		if (cards.size() == 0)
			return colors;
			
		if (reflectProperty.equals("Is")){ // Meteor Crater
			colors = hasProperty(maxChoices, cards, colors);
		}
		else if (reflectProperty.equals("Produce")){
			ArrayList<Ability_Mana> abilities = new ArrayList<Ability_Mana>();
			for(Card c : cards){
				abilities.addAll(c.getManaAbility());
			}
			// currently reflected mana will ignore other reflected mana abilities
			
			ArrayList<Ability_Mana> reflectAbilities =  new ArrayList<Ability_Mana>();
			
			for(Ability_Mana ab : abilities){
				if (maxChoices == colors.size())
					break;
				
				if (ab.isReflectedMana()){
					if (!parents.contains(ab.getSourceCard())){
						// Recursion! 
						reflectAbilities.add(ab);
						parents.add(ab.getSourceCard());
					}
					continue;
				}
				colors = canProduce(maxChoices, ab, colors);
				if (!parents.contains(ab.getSourceCard()))
					parents.add(ab.getSourceCard());
			}
			
			for(Ability_Mana ab : reflectAbilities){
				if (maxChoices == colors.size())
					break;
				
				colors = reflectableMana(ab, ab.getAbilityFactory(), colors, parents);
			}
		}
		
		return colors;
	}
	
	private static ArrayList<String> hasProperty(int maxChoices, CardList cards, ArrayList<String> colors){
		for(Card c:cards) {
			// For each card, go through all the colors and if the card is that color, add
			for(String col : Constant.Color.onlyColors){
				if(c.isColor(col) && !colors.contains(col)){
					colors.add(col);
					if (colors.size() == maxChoices)
						break;
				}
			}
		}
		return colors;
	}
	
	private static ArrayList<String> canProduce(int maxChoices, Ability_Mana ab, ArrayList<String> colors){
		for(String col : Constant.Color.onlyColors){
			String s = Input_PayManaCostUtil.getShortColorString(col);
			if(ab.canProduce(s) && !colors.contains(col))
				colors.add(col);
		}

		if (maxChoices == 6 && ab.canProduce("1") && !colors.contains(Constant.Color.Colorless))
			colors.add(Constant.Color.Colorless);

		return colors;
	}

	private static String generatedReflectedMana(Ability_Mana abMana, AbilityFactory af, ArrayList<String> colors){
		// Calculate generated mana here for stack description and resolving
		HashMap<String,String> params = af.getMapParams();
		int amount = params.containsKey("Amount") ? AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), abMana) : 1;

		String baseMana = "";
		
		if (colors.size() == 0)
			return "0";
		else if (colors.size() == 1)
			baseMana = Input_PayManaCostUtil.getShortColorString(colors.get(0));
		else{
			if (abMana.getActivatingPlayer().isHuman()){
				Object o = AllZone.Display.getChoiceOptional("Select Mana to Produce", colors.toArray());
				if (o == null) {
					// User hit cancel
					abMana.setCanceled(true); 
					return "";
				} 
				else {
					baseMana = Input_PayManaCostUtil.getShortColorString((String) o);
				}
			}
			else{
				// AI doesn't really have anything here yet
			}
		}

		StringBuilder sb = new StringBuilder();
		if (amount == 0)
			sb.append("0");
		else{
			try{
				// if baseMana is an integer(colorless), just multiply amount and baseMana
				int base = Integer.parseInt(baseMana);
				sb.append(base*amount);
			}
			catch(NumberFormatException e){
				for(int i = 0; i < amount; i++){
					if (i != 0)
						sb.append(" ");
					sb.append(baseMana);
				}
			}
		}
		return sb.toString();
	}
	
	// *************** Utility Functions **********************
	
	public static void doDrawback(AbilityFactory af, Ability_Mana abMana, Card card){
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		
		// if mana production has any type of SubAbility, undoable=false
		if (af.hasSubAbility()){
			abMana.undoable = false;
			Ability_Sub abSub = abMana.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			else{
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, abMana);
			}
		}
	}
	
	private static boolean hasUrzaLands(Player p){
		CardList landsControlled = AllZoneUtil.getPlayerCardsInPlay(p);
		
		return (landsControlled.containsName("Urza's Mine") && landsControlled.containsName("Urza's Tower") &&
			landsControlled.containsName("Urza's Power Plant"));
	}
}

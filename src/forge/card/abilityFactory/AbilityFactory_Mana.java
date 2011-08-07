package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Counters;
import forge.Player;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;
import forge.gui.input.Input_PayManaCostUtil;

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
				manaResolve(this, af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				// TODO Auto-generated method stub
				return false;
			}
			
		};
		return abMana;
	}
	
	public static SpellAbility createSpellMana(final AbilityFactory AF, final String produced){
		final SpellAbility spMana = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			// To get the mana to resolve properly, we need the spell to contain an Ability_Mana
			Cost tmp = new Cost("0", AF.getHostCard().getName(), false);
			Ability_Mana tmpMana = new Ability_Mana(AF.getHostCard(), tmp, produced){
				private static final long serialVersionUID = 1454043766057140491L;

				@Override
				public boolean doTrigger(boolean mandatory) {
					// TODO Auto-generated method stub
					return false;
				}
				
			};
			
			public boolean canPlayAI()
			{
				return manaCanPlayAI(af);
			}
			
			@Override
            public String getStackDescription(){
            // when getStackDesc is called, just build exactly what is happening
                return manaStackDescription(tmpMana, af, this);
            }
			
			@Override
			public void resolve() {
				manaResolve(tmpMana, af, this);
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
			Cost tmp = new Cost("0", AF.getHostCard().getName(), false);
			Ability_Mana tmpMana = new Ability_Mana(AF.getHostCard(), tmp, produced){
				private static final long serialVersionUID = 1454043766057140491L;

				@Override
				public boolean doTrigger(boolean mandatory) {
					// TODO Auto-generated method stub
					return false;
				}
				
			};
			
			@Override
            public String getStackDescription(){
            // when getStackDesc is called, just build exactly what is happening
                return manaStackDescription(tmpMana, af, this);
            }
			
			@Override
			public void resolve() {
				manaResolve(tmpMana, af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				// TODO Auto-generated method stub
				return false;
			}
			
		};
		return dbMana;
	}
	
	public static boolean manaCanPlayAI(final AbilityFactory af){
		// AI cannot use this properly until he has a ManaPool
		return false;
	}
	
	public static String manaStackDescription(Ability_Mana abMana, AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		
		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(af.getHostCard()).append(" - ");
		
		sb.append("Add ").append(generatedMana(abMana, af, sa)).append(" to your mana pool.");

		if (abMana.getSubAbility() != null)
			sb.append(abMana.getSubAbility().getStackDescription());
		
		return sb.toString();
	}
	
	public static void manaResolve(Ability_Mana abMana, AbilityFactory af, SpellAbility sa){
		// Spells are not undoable
		abMana.setUndoable(af.isAbility() && abMana.isUndoable());

		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		
		ArrayList<Player> tgtPlayers;
		
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
		
		for(Player player : tgtPlayers)
			abMana.produceMana(generatedMana(abMana, af, sa), player);
		
		// convert these to SubAbilities when appropriate		
		if (params.containsKey("Stuck")){
			abMana.setUndoable(false);
			card.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
		}
		
		String deplete = params.get("Deplete");
		if (deplete != null){
			int num = card.getCounters(Counters.getType(deplete));
			if (num == 0){
				abMana.setUndoable(false);
				AllZone.GameAction.sacrifice(card);
			}
		}
		
		doDrawback(af, abMana, card);
	}
	
	private static String generatedMana(Ability_Mana abMana, AbilityFactory af, SpellAbility sa){
		// Calculate generated mana here for stack description and resolving
		HashMap<String,String> params = af.getMapParams();
		int amount = params.containsKey("Amount") ? AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), sa) : 1;

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
		
		try{
			if (params.get("Amount") != null && amount != Integer.parseInt(params.get("Amount")))
				abMana.setUndoable(false);
			}
		catch(NumberFormatException n){
			abMana.setUndoable(false);
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

			@Override
			public boolean doTrigger(boolean mandatory) {
				// TODO Auto-generated method stub
				return false;
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
			Cost tmp = new Cost("0", AF.getHostCard().getName(), false);
			Ability_Mana tmpMana = new Ability_Mana(AF.getHostCard(), tmp, produced){
				private static final long serialVersionUID = 1454043766057140491L;

				@Override
				public boolean doTrigger(boolean mandatory) {
					// TODO Auto-generated method stub
					return false;
				}

				// TODO: maybe add can produce here, so old AI code can use reflected mana?
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
		HashMap<String,String> params = af.getMapParams();
		abMana.setUndoable(af.isAbility() && abMana.isUndoable());

		Card card = af.getHostCard();
		
		ArrayList<String> colors = reflectableMana(abMana, af, new ArrayList<String>(), new ArrayList<Card>());
		
		ArrayList<Player> tgtPlayers;
		
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(abMana.getSourceCard(), params.get("Defined"), abMana);
		
		for(Player player : tgtPlayers) {
			String generated = generatedReflectedMana(abMana, af, colors, player);
		
			if (abMana.getCanceled()){
				abMana.undo();
				return;
			}

			abMana.produceMana(generated, player);
		}

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
		
		// Reuse AF_Defined in a slightly different way
		if (validCard.startsWith("Defined.")){
			cards = new CardList();
			for(Card c : AbilityFactory.getDefinedCards(card, validCard.replace("Defined.", ""), (SpellAbility)abMana))
				cards.add(c);
		}
		else{
			cards = AllZoneUtil.getCardsInPlay().getValidCards(validCard, abMana.getActivatingPlayer(), card);
		}

		// remove anything cards that is already in parents
		for(Card p : parents)
			if (cards.contains(p))
				cards.remove(p);
		
		if (cards.size() == 0 && !reflectProperty.equals("Produced"))
			return colors;
			
		if (reflectProperty.equals("Is")){ // Meteor Crater
			colors = hasProperty(maxChoices, cards, colors);
		}
		else if (reflectProperty.equals("Produced")){
			String producedColors = (String)abMana.getTriggeringObject("Produced");
			for(String col : Constant.Color.onlyColors){
				String s = Input_PayManaCostUtil.getShortColorString(col);
				if(producedColors.contains(s) && !colors.contains(col))
					colors.add(col);
			}
			if (maxChoices == 6 && producedColors.contains("1") && !colors.contains(Constant.Color.Colorless))
				colors.add(Constant.Color.Colorless);
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

	private static String generatedReflectedMana(Ability_Mana abMana, AbilityFactory af, ArrayList<String> colors, Player player){
		// Calculate generated mana here for stack description and resolving
		HashMap<String,String> params = af.getMapParams();
		int amount = params.containsKey("Amount") ? AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), abMana) : 1;

		String baseMana = "";
		
		if (colors.size() == 0)
			return "0";
		else if (colors.size() == 1)
			baseMana = Input_PayManaCostUtil.getShortColorString(colors.get(0));
		else{
			if (player.isHuman()){
				Object o = GuiUtils.getChoiceOptional("Select Mana to Produce", colors.toArray());
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
				baseMana = Input_PayManaCostUtil.getShortColorString(colors.get(0));
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
		
		// if mana production has any type of SubAbility, undoable=false
		if (af.hasSubAbility())  {
			abMana.setUndoable(false);
			Ability_Sub abSub = abMana.getSubAbility();
			AbilityFactory.resolve(abSub);
		}
	}
	
	private static boolean hasUrzaLands(Player p){
		CardList landsControlled = AllZoneUtil.getPlayerCardsInPlay(p);
		
		return (landsControlled.containsName("Urza's Mine") && landsControlled.containsName("Urza's Tower") &&
			landsControlled.containsName("Urza's Power Plant"));
	}
}

package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.ComputerUtil;
import forge.Counters;
import forge.MyRandom;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;

public class AbilityFactory_PermanentState {
	// ****************************************
	// ************** Untap *******************
	// ****************************************
	public static SpellAbility createAbilityUntap(final AbilityFactory AF){
		final SpellAbility abUntap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return untapStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return untapCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				untapResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return untapTrigger(af,this, mandatory);
			}
			
		};
		return abUntap;
	}
	
	public static SpellAbility createSpellUntap(final AbilityFactory AF){
		final SpellAbility spUntap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return untapStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return untapCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				untapResolve(af, this);
			}
			
		};
		return spUntap;
	}
	
	public static SpellAbility createDrawbackUntap(final AbilityFactory AF){
		final SpellAbility dbUntap = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return untapStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				untapResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return untapPlayDrawbackAI(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return untapTrigger(af, this, mandatory);
			}
			
		};
		return dbUntap;
	}

	public static String untapStackDescription(AbilityFactory af, SpellAbility sa){
		// when getStackDesc is called, just build exactly what is happening
		StringBuilder sb = new StringBuilder();
		final HashMap<String, String> params = af.getMapParams();
		Card hostCard = sa.getSourceCard();

		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(sa.getSourceCard()).append(" - ");

		sb.append("Untap ");

		if (params.containsKey("UntapUpTo")) {
			sb.append("up to ").append(params.get("Amount")).append(" ");
			sb.append(params.get("UntapType")).append("s");
		} 
		else {
			ArrayList<Card> tgtCards;
			Target tgt = af.getAbTgt();
			if (tgt != null)
				tgtCards = tgt.getTargetCards();
			else {
				tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
			}

			Iterator<Card> it = tgtCards.iterator();
			while (it.hasNext()) {
				sb.append(it.next());
				if (it.hasNext())
					sb.append(", ");
			}
		}
		sb.append(".");

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			sb.append(subAb.getStackDescription());

		return sb.toString();
	}
	
	public static boolean untapCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
    	if (af.getAbCost().getAddCounter())
    		if (af.getAbCost().getCounterType().equals(Counters.M1M1))
            	return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		Random r = MyRandom.random;
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed()+1);
		
		if (tgt == null){
			if (sa.getSourceCard().isUntapped())
				return false;
		}
		else{
			if (!untapPrefTargeting(tgt, af, sa, false))
				return false;
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	public static boolean untapTrigger(AbilityFactory af, SpellAbility sa, boolean mandatory){
		HashMap<String,String> params = af.getMapParams();
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = sa.getTarget();
		
		if (tgt == null){
			if (mandatory)
				return true;
			
			// TODO: use Defined to determine, if this is an unfavorable result
			ArrayList<Card> pDefined = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
			if (pDefined != null && pDefined.get(0).isUntapped())
				return false;
			
			return true;
		}
		else{
			if (untapPrefTargeting(tgt, af, sa, mandatory)){
				return true;
			}
			else if (mandatory){
				// not enough preferred targets, but mandatory so keep going:
				return untapUnpreferredTargeting(af, sa, mandatory);
			}
		}
		
		return false;
	}
	
	public static boolean untapPlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		Target tgt = af.getAbTgt();

		boolean randomReturn = true;
		
		if (tgt == null){
			// who cares if its already untapped, it's only a subability?
		}
		else{
			if (!untapPrefTargeting(tgt, af, sa, false))
				return false;
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	public static boolean untapPrefTargeting(Target tgt, AbilityFactory af, SpellAbility sa, boolean mandatory){
		Card source = sa.getSourceCard();
		
		CardList untapList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
		untapList = untapList.getTargetableCards(source);
		untapList = untapList.getValidCards(tgt.getValidTgts(), source.getController(), source);
		
		untapList = untapList.filter(AllZoneUtil.tapped);
		// filter out enchantments and planeswalkers, their tapped state doesn't matter.
		String[] tappablePermanents = {"Creature", "Land", "Artifact"}; 
		untapList = untapList.getValidCards(tappablePermanents, source.getController(), source);

		if (untapList.size() == 0)
			return false;
		
		while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
			Card choice = null;
			
			if (untapList.size() == 0){
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			if (untapList.getNotType("Creature").size() == 0)
        		choice = CardFactoryUtil.AI_getBestCreature(untapList); //if only creatures take the best
        	else
        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(untapList, af.getHostCard(), false);
			
			if (choice == null){	// can't find anything left
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			untapList.remove(choice);
			tgt.addTarget(choice);
		}
		return true;
	}
	
	public static boolean untapUnpreferredTargeting(AbilityFactory af, SpellAbility sa, boolean mandatory){
		Card source = sa.getSourceCard();
		Target tgt = sa.getTarget();
		
		CardList list = AllZoneUtil.getCardsInPlay();
		
		list = list.getValidCards(tgt.getValidTgts(), source.getController(), source);
		list = list.getTargetableCards(source);
		
		// filter by enchantments and planeswalkers, their tapped state doesn't matter.
		String[] tappablePermanents = {"Enchantment", "Planeswalker"}; 
		CardList tapList = list.getValidCards(tappablePermanents, source.getController(), source);

		if (untapTargetList(source, tgt, af, sa, mandatory, tapList))
			return true;
		
		// try to just tap already tapped things
		tapList = list.filter(AllZoneUtil.untapped);
		
		if (untapTargetList(source, tgt, af, sa, mandatory, tapList))
			return true;
		
		// just tap whatever we can
		tapList = list;
		
		if (untapTargetList(source, tgt, af, sa, mandatory, tapList))
			return true;
		
		return false;
	}
	
	public static boolean untapTargetList(Card source, Target tgt, AbilityFactory af, SpellAbility sa, boolean mandatory, CardList tapList){
		for(Card c : tgt.getTargetCards())
			tapList.remove(c);
		
		if (tapList.size() == 0)
			return false;
		
		while(tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)){ 
			Card choice = null;
			
			if (tapList.size() == 0){
				if (tgt.getNumTargeted() < tgt.getMinTargets(source, sa) || tgt.getNumTargeted() == 0){
					if (!mandatory)
						tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			if (tapList.getNotType("Creature").size() == 0)
        		choice = CardFactoryUtil.AI_getBestCreature(tapList); //if only creatures take the best
        	else
        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(tapList, af.getHostCard(), false);
			
			if (choice == null){	// can't find anything left
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					if (!mandatory)
						tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			tapList.remove(choice);
			tgt.addTarget(choice);
		}
		
		return true;
	}
	
	public static void untapResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		Target tgt = af.getAbTgt();
		ArrayList<Card> tgtCards = null;
		
		if (params.containsKey("UntapUpTo"))
			chooseUntapUpTo(af, sa, params);
		else{	
			if (tgt != null)
				tgtCards = tgt.getTargetCards();
			else{
				tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
			}
	
			for(Card tgtC : tgtCards){
				if (AllZoneUtil.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC)))
					tgtC.untap();
			}
		}
	}
	
	public static void chooseUntapUpTo(AbilityFactory af, SpellAbility sa, HashMap<String,String> params){
		int num = Integer.parseInt(params.get("Amount"));
		String valid = params.get("UntapType");
		
		Player activatingPlayer = sa.getActivatingPlayer();
		
		// Reuse existing UntapUpTo Input
    	if (activatingPlayer.isHuman()) 
    		AllZone.InputControl.setInput(CardFactoryUtil.input_UntapUpToNType(num, valid));
    	else{
            CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
            list = list.getType(valid);
            list = list.filter(AllZoneUtil.tapped);
            
            int count = 0;
            while(list.size() != 0 )
            for(int i = 0; i < num && i < list.size(); i++){

            	Card c = CardFactoryUtil.AI_getBestLand(list);
                c.untap();
                list.remove(c);
                count++;
            }
    	}
	}
	
	// ****************************************
	// ************** Tap *********************
	// ****************************************
	
	public static SpellAbility createAbilityTap(final AbilityFactory AF){
		final SpellAbility abTap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return tapCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				tapResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return tapTrigger(af, this, mandatory);
			}
			
		};
		return abTap;
	}
	
	public static SpellAbility createSpellTap(final AbilityFactory AF){
		final SpellAbility spTap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return tapCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				tapResolve(af, this);
			}
			
		};
		return spTap;
	}
	
	public static SpellAbility createDrawbackTap(final AbilityFactory AF){
		final SpellAbility dbTap = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				tapResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return tapPlayDrawbackAI(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return tapTrigger(af, this, mandatory);
			}
			
		};
		return dbTap;
	}
	
	public static String tapStackDescription(AbilityFactory af, SpellAbility sa){
		// when getStackDesc is called, just build exactly what is happening
		StringBuilder sb = new StringBuilder();
		final HashMap<String, String> params = af.getMapParams();
		Card hostCard = sa.getSourceCard();
		 
		 if (sa instanceof Ability_Sub)
			 sb.append(" ");
		 else
			 sb.append(sa.getSourceCard()).append(" - ");
		 
		 sb.append("Tap ");
		 
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
		}
		
		Iterator<Card> it = tgtCards.iterator();
		while(it.hasNext()) {
			sb.append(it.next());
			if(it.hasNext()) sb.append(", ");
		}
		
		sb.append(".");

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	sb.append(subAb.getStackDescription());
		
		 return sb.toString();
	}
	
	public static boolean tapCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		HashMap<String,String> params = af.getMapParams();
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		Random r = MyRandom.random;
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		if (tgt == null){
			ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
			
			boolean bFlag = false;
			for(Card c : defined)
				bFlag |= c.isUntapped();
			
			if (!bFlag)	// All of the defined stuff is tapped, not very useful
				return false;
		}
		else{
			tgt.resetTargets();
			if (!tapPrefTargeting(source, tgt, af, sa, false))
				return false;
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	public static boolean tapTrigger(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = sa.getTarget();
		Card source = sa.getSourceCard();
		
		if (tgt == null){
			if (mandatory)
				return true;
			
			// TODO: use Defined to determine, if this is an unfavorable result
			
			return true;
		}
		else{
			if (tapPrefTargeting(source, tgt, af, sa, mandatory)){
				return true;
			}
			else if (mandatory){
				// not enough preferred targets, but mandatory so keep going:
				return tapUnpreferredTargeting(af, sa, mandatory);
			}
		}
		
		return false;
	}
	
	public static boolean tapPlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		boolean randomReturn = true;
		
		if (tgt == null){
			// either self or defined, either way should be fine
		}
		else{
			// target section, maybe pull this out?
			tgt.resetTargets();
			if (!tapPrefTargeting(source, tgt, af, sa, false))
				return false;
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	public static boolean tapPrefTargeting(Card source, Target tgt, AbilityFactory af, SpellAbility sa, boolean mandatory){
		CardList tapList = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		tapList = tapList.filter(AllZoneUtil.untapped);
		tapList = tapList.getValidCards(tgt.getValidTgts(), source.getController(), source);
		// filter out enchantments and planeswalkers, their tapped state doesn't matter.
		String[] tappablePermanents = {"Creature", "Land", "Artifact"}; 
		tapList = tapList.getValidCards(tappablePermanents, source.getController(), source);
		tapList = tapList.getTargetableCards(source);

		if (tapList.size() == 0)
			return false;
		
		while(tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)){ 
			Card choice = null;
			
			if (tapList.size() == 0){
				if (tgt.getNumTargeted() < tgt.getMinTargets(source, sa) || tgt.getNumTargeted() == 0){
					if (!mandatory)
						tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			if (tapList.getNotType("Creature").size() == 0)
        		choice = CardFactoryUtil.AI_getBestCreature(tapList); //if only creatures take the best
        	else
        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(tapList, af.getHostCard(), false);
			
			if (choice == null){	// can't find anything left
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					if (!mandatory)
						tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			tapList.remove(choice);
			tgt.addTarget(choice);
		}
		
		return true;
	}
	
	public static boolean tapUnpreferredTargeting(AbilityFactory af, SpellAbility sa, boolean mandatory){
		Card source = sa.getSourceCard();
		Target tgt = sa.getTarget();		
		
		CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(tgt.getValidTgts(), source.getController(), source);
		list = list.getTargetableCards(source);
		
		// filter by enchantments and planeswalkers, their tapped state doesn't matter.
		String[] tappablePermanents = {"Enchantment", "Planeswalker"}; 
		CardList tapList = list.getValidCards(tappablePermanents, source.getController(), source);

		if (tapTargetList(af, sa, tapList, mandatory))
			return true;
		
		// try to just tap already tapped things
		tapList = list.filter(AllZoneUtil.tapped);
		
		if (tapTargetList(af, sa, tapList, mandatory))
			return true;
		
		// just tap whatever we can
		tapList = list;
		
		if (tapTargetList(af, sa, tapList, mandatory))
			return true;
		
		return false;
	}
	
	public static boolean tapTargetList(AbilityFactory af, SpellAbility sa, CardList tapList, boolean mandatory){
		Card source = sa.getSourceCard();
		Target tgt = sa.getTarget();
		
		for(Card c : tgt.getTargetCards())
			tapList.remove(c);
		
		if (tapList.size() == 0)
			return false;
		
		while(tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)){ 
			Card choice = null;
			
			if (tapList.size() == 0){
				if (tgt.getNumTargeted() < tgt.getMinTargets(source, sa) || tgt.getNumTargeted() == 0){
					if (!mandatory)
						tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			if (tapList.getNotType("Creature").size() == 0)
        		choice = CardFactoryUtil.AI_getBestCreature(tapList); //if only creatures take the best
        	else
        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(tapList, af.getHostCard(), false);
			
			if (choice == null){	// can't find anything left
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					if (!mandatory)
						tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			tapList.remove(choice);
			tgt.addTarget(choice);
		}
		
		return true;
	}
	
	public static void tapResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
		}

		for(Card tgtC : tgtCards){
			if (AllZoneUtil.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC)))
				tgtC.tap();
		}
	}
	
	// ****************************************
	// ************** UntapAll *****************
	// ****************************************
	public static SpellAbility createAbilityUntapAll(final AbilityFactory AF){
		final SpellAbility abUntap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8914852730903389831L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return untapAllStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return untapAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				untapAllResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return untapAllTrigger(af, this, mandatory);
			}

		};
		return abUntap;
	}

	public static SpellAbility createSpellUntapAll(final AbilityFactory AF){
		final SpellAbility spUntap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5713174052551899363L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription() {
				return untapAllStackDescription(af, this);
			}

			public boolean canPlayAI() {
				return untapAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				untapAllResolve(af, this);
			}

		};
		return spUntap;
	}
	
	public static SpellAbility createDrawbackUntapAll(final AbilityFactory af) {
		final SpellAbility dbUntapAll = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = -5187900994680626766L;

			@Override
			public String getStackDescription(){
				return untapAllStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				untapAllResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return untapAllPlayDrawbackAI(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return untapAllPlayDrawbackAI(af, this);
			}
			
		};
		return dbUntapAll;
	}
	
	private static boolean untapAllPlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		return true;
	}

	private static void untapAllResolve(final AbilityFactory af, final SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();

		String Valid = "";

		if(params.containsKey("ValidCards")) 
			Valid = params.get("ValidCards");

		CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(Valid.split(","), card.getController(), card);

		for(int i = 0; i < list.size(); i++) list.get(i).untap();
	}

	private static boolean untapAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		/*
		 * All cards using this currently have SVar:RemAIDeck:True
		 */
		return false;
	}

	public static boolean untapAllTrigger(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		if (mandatory)
			return true;
		
		
		return false;
	}
	
	private static String untapAllStackDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		// when getStackDesc is called, just build exactly what is happening
		StringBuilder sb = new StringBuilder();

		if (sa instanceof Ability_Sub) {
			sb.append(" ");
			sb.append("Untap all valid cards.");
		}
		else {
			sb.append(sa.getSourceCard()).append(" - ");
			sb.append(params.get("SpellDescription"));
		}

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			sb.append(subAb.getStackDescription());

		return sb.toString();
	}
	
	// ****************************************
	// ************** TapAll *****************
	// ****************************************
	public static SpellAbility createAbilityTapAll(final AbilityFactory AF){
		final SpellAbility abUntap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -2095140656782946737L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return tapAllStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return tapAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				tapAllResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return tapAllTrigger(af, this, mandatory);
			}

		};
		return abUntap;
	}

	public static SpellAbility createSpellTapAll(final AbilityFactory AF){
		final SpellAbility spUntap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -62401571838950166L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription() {
				return tapAllStackDescription(af, this);
			}

			public boolean canPlayAI() {
				return tapAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				tapAllResolve(af, this);
			}

		};
		return spUntap;
	}

	public static SpellAbility createDrawbackTapAll(final AbilityFactory AF){
		final SpellAbility dbTap = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapAllStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				tapAllResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return tapAllPlayDrawbackAI(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return tapAllPlayDrawbackAI(af, this);
			}
			
		};
		return dbTap;
	}
	
	private static void tapAllResolve(final AbilityFactory af, final SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();

		String Valid = "";

		if(params.containsKey("ValidCards")) 
			Valid = params.get("ValidCards");

		CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(Valid.split(","), card.getController(), card);

		for(int i = 0; i < list.size(); i++) list.get(i).tap();
	}

	private static boolean tapAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		// If tapping all creatures do it either during declare attackers of AIs turn
		// or during upkeep/begin combat?
		
		
		if (!ComputerUtil.canPayCost(sa))
			return false;

		Card source = sa.getSourceCard();
		HashMap<String,String> params = af.getMapParams();

		String valid = "";
		if(params.containsKey("ValidCards")) 
			valid = params.get("ValidCards");
		
		
		CardList validTappables = getTapAllTargets(valid, source);

		Random r = MyRandom.random;
		boolean rr = false;
		if (r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed()))
			rr = true;

		if(validTappables.size() > 0) {
			CardList human = validTappables.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getController().isHuman();
				}
			});
			CardList compy = validTappables.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getController().isHuman();
				}
			});
			if(human.size() > compy.size()) {
				return rr;
			}
		}
		return false;
	}

	private static CardList getTapAllTargets(String valid, Card source) {
		CardList tmpList = AllZoneUtil.getCardsInPlay();
		tmpList = tmpList.getValidCards(valid, source.getController(), source);
		tmpList = tmpList.getTargetableCards(source);
		tmpList = tmpList.filter(AllZoneUtil.untapped);
		return tmpList;
	}


	private static String tapAllStackDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		// when getStackDesc is called, just build exactly what is happening
		StringBuilder sb = new StringBuilder();

		if (sa instanceof Ability_Sub) {
			sb.append(" ");
			sb.append("Tap all valid cards.");
		}
		else {
			sb.append(sa.getSourceCard()).append(" - ");
			sb.append(params.get("SpellDescription"));
		}

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			sb.append(subAb.getStackDescription());

		return sb.toString();
	}
	
	public static boolean tapAllTrigger(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		if (mandatory)
			return true;
		
		Card source = sa.getSourceCard();
		HashMap<String,String> params = af.getMapParams();

		String valid = "";
		if(params.containsKey("ValidCards")) 
			valid = params.get("ValidCards");
		
		CardList validTappables = getTapAllTargets(valid, source);

		Random r = MyRandom.random;
		boolean rr = false;
		if (r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed()))
			rr = true;

		if(validTappables.size() > 0) {
			CardList human = validTappables.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getController().isHuman();
				}
			});
			CardList compy = validTappables.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getController().isHuman();
				}
			});
			if(human.size() > compy.size()) {
				return rr;
			}
		}
		
		return false;
	}
	
	private static boolean tapAllPlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		return true;
	}
	
	
	// ****************************************
	// ************** Tap or Untap ************
	// ****************************************
	
	public static SpellAbility createAbilityTapOrUntap(final AbilityFactory AF){
		final SpellAbility abTapOrUntap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4713183763302932079L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapOrUntapStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return tapOrUntapCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				tapOrUntapResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return tapOrUntapTrigger(af, this, mandatory);
			}
			
		};
		return abTapOrUntap;
	}
	
	public static SpellAbility createSpellTapOrUntap(final AbilityFactory AF){
		final SpellAbility spTapOrUntap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -8870476840484788521L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapOrUntapStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return tapOrUntapCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				tapOrUntapResolve(af, this);
			}
			
		};
		return spTapOrUntap;
	}
	
	public static SpellAbility createDrawbackTapOrUntap(final AbilityFactory AF){
		final SpellAbility dbTapOrUntap = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -8282868583712773337L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapOrUntapStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				tapOrUntapResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return tapOrUntapPlayDrawbackAI(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return tapOrUntapTrigger(af, this, mandatory);
			}
			
		};
		return dbTapOrUntap;
	}
	
	private static String tapOrUntapStackDescription(AbilityFactory af, SpellAbility sa){
		// when getStackDesc is called, just build exactly what is happening
		StringBuilder sb = new StringBuilder();
		
		HashMap<String,String> params = af.getMapParams();

		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(sa.getSourceCard()).append(" - ");

		sb.append("Tap or untap ");

		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
		}

		Iterator<Card> it = tgtCards.iterator();
		while(it.hasNext()) {
			sb.append(it.next());
			if(it.hasNext()) sb.append(", ");
		}

		sb.append(".");

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			sb.append(subAb.getStackDescription());

		return sb.toString();
	}
	
	private static boolean tapOrUntapCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		HashMap<String,String> params = af.getMapParams();
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		Random r = MyRandom.random;
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		if (tgt == null){
			//assume we are looking to tap human's stuff
			//TODO - check for things with untap abilities, and don't tap those.
			ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
			
			boolean bFlag = false;
			for(Card c : defined)
				bFlag |= c.isUntapped();
			
			if (!bFlag)	// All of the defined stuff is tapped, not very useful
				return false;
		}
		else{
			tgt.resetTargets();
			if (!tapPrefTargeting(source, tgt, af, sa, false))
				return false;
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	private static boolean tapOrUntapTrigger(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = sa.getTarget();
		Card source = sa.getSourceCard();
		
		if (tgt == null){
			if (mandatory)
				return true;
			
			// TODO: use Defined to determine if this is an unfavorable result
			
			return true;
		}
		else{
			if (tapPrefTargeting(source, tgt, af, sa, mandatory)){
				return true;
			}
			else if (mandatory){
				// not enough preferred targets, but mandatory so keep going:
				return tapUnpreferredTargeting(af, sa, mandatory);
			}
		}
		
		return false;
	}
	
	private static boolean tapOrUntapPlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		boolean randomReturn = true;
		
		if (tgt == null){
			// either self or defined, either way should be fine
		}
		else{
			// target section, maybe pull this out?
			tgt.resetTargets();
			if (!tapPrefTargeting(source, tgt, af, sa, false))
				return false;
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	private static void tapOrUntapResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
		}

		for(Card tgtC : tgtCards){
			if (AllZoneUtil.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC))){
				String[] tapOrUntap = new String[] {"Tap", "Untap"};
				Object z = GuiUtils.getChoiceOptional("Tap or Untap "+tgtC+"?", tapOrUntap);
				if(null == z) continue;
				boolean tap = (z.equals("Tap")) ? true : false;
				
				if(tap) tgtC.tap();
				else tgtC.untap();
			}
		}
	}
	
	//Phasing? Something else? Who knows!
	
}// end of AbilityFactory_PermanentState class

package forge.card.abilityFactory;

import java.util.HashMap;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Command;
import forge.ComputerUtil;
import forge.Counters;
import forge.MyRandom;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

public class AbilityFactory_Token extends AbilityFactory {
	private AbilityFactory AF = null;
	
	private String tokenAmount;
	private String tokenName;
	private String[] tokenTypes;
	private String tokenOwner;
	private String[] tokenColors;
	private String[] tokenKeywords;
	private String tokenPower;
	private String tokenToughness;
	private String tokenImage;
	private String[] tokenAbilities;
	private String[] tokenTriggers;
	private String[] tokenSVars;
	private boolean tokenTapped;
	private boolean tokenAttacking;
    
	public AbilityFactory_Token(final AbilityFactory af) {
		AF = af;
		
		HashMap<String,String> mapParams = af.getMapParams();
		String image;
		String[] keywords;
		
		if(mapParams.containsKey("TokenKeywords")) {
			keywords = mapParams.get("TokenKeywords").split("<>");
		}
		else {
			keywords = new String[0];
		}
		
		if(mapParams.containsKey("TokenImage")) {
			image = mapParams.get("TokenImage");
		}
		else {
			image = "";
		}
		
		if(mapParams.containsKey("TokenTapped")) {
			tokenTapped = mapParams.get("TokenTapped").equals("True");
		}
		else
		{
			tokenTapped = false;
		}
		if(mapParams.containsKey("TokenAttacking")) {
			tokenAttacking = mapParams.get("TokenAttacking").equals("True");
		}
		else
		{
			tokenAttacking = false;
		}
		
		if(mapParams.containsKey("TokenAbilities"))
		{
			tokenAbilities = mapParams.get("TokenAbilities").split(",");
		}
		else
		{
			tokenAbilities = null;
		}
		if(mapParams.containsKey("TokenTriggers"))
		{
			tokenTriggers = mapParams.get("TokenTriggers").split(",");
		}
		else
		{
			tokenTriggers = null;
		}
		if(mapParams.containsKey("TokenSVars"))
		{
			tokenSVars = mapParams.get("TokenSVars").split(",");
		}
		else
		{
			tokenSVars = null;
		}
		
		tokenAmount = mapParams.get("TokenAmount");
		tokenPower = mapParams.get("TokenPower");
		tokenToughness = mapParams.get("TokenToughness");
		tokenName = mapParams.get("TokenName");
		tokenTypes = mapParams.get("TokenTypes").split(",");
		tokenColors = mapParams.get("TokenColors").split(",");
		tokenKeywords = keywords;
		tokenImage = image;
		if(mapParams.containsKey("TokenOwner"))
			tokenOwner = mapParams.get("TokenOwner");
		else tokenOwner = "You";
	}
	
	public SpellAbility getAbility()
	{
		
		
		final SpellAbility abToken = new Ability_Activated(AF.getHostCard(),AF.getAbCost(),AF.getAbTgt())
		{
			private static final long serialVersionUID = 8460074843405764620L;

			@Override
			public boolean canPlayAI() {
				return tokenCanPlayAI(this);
			}
			
			@Override
			public void resolve() {
				doResolve(this);
            	AF.getHostCard().setAbilityUsed(AF.getHostCard().getAbilityUsed() + 1);
			}
			
			@Override
			public String getStackDescription() {
				return doStackDescription(this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return tokenDoTriggerAI(this, mandatory);
			}
		};
		
		return abToken;
	}
	
	public SpellAbility getSpell()
	{		
		final SpellAbility spToken = new Spell(AF.getHostCard(),AF.getAbCost(),AF.getAbTgt())
		{
			private static final long serialVersionUID = -8041427947613029670L;

			@Override
			public boolean canPlayAI() {
				return tokenCanPlayAI(this);
			}
			
			@Override
			public void resolve() {
				doResolve(this);
			}
			
			@Override
			public String getStackDescription() {
				return doStackDescription(this);
			}
		};
		
		return spToken;
	}
	
	public SpellAbility getDrawback()
    {
		final SpellAbility dbDealDamage = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 7239608350643325111L;

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public String getStackDescription() {
				return doStackDescription(this);
			}

			@Override
			public void resolve() {
				doResolve(this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return tokenDoTriggerAI(this, mandatory);
			}

		}; // Spell

		return dbDealDamage;
	}
	
	private boolean tokenCanPlayAI(SpellAbility sa){
		Cost cost = sa.getPayCosts();
		if (!ComputerUtil.canPayCost(sa))	// If there is a cost payment it's usually not mandatory
			return false;

		for(String type : tokenTypes){
			if (type.equals("Legendary")){
				// Don't kill AIs Legendary tokens
				if (AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer, tokenName).size() > 0)
					return false;
				
				/*// Kill Human's Legendary tokens
				if (AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer, tokenName).size() > 0)
					return true;*/ // Never use "return true" in canPlayAI
			}
		}
		
		
		if (AbilityFactory.playReusable(sa))
			return true;

		// todo: if i don't have enough blockers and my token can block one of the unblocked creatures
		// create it after attackers are declared
		//if (AllZone.Phase.is(Constant.Phase.Combat_Declare_Attackers_InstantAbility, AllZone.HumanPlayer))
		//	return true;

		// prevent run-away activations - first time will always return true
		Random r = MyRandom.random;
		final Card source = sa.getSourceCard();
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());

		Target tgt = sa.getTarget();
		if (tgt != null){
			tgt.resetTargets();
			if (tgt.canOnlyTgtOpponent())
				tgt.addTarget(AllZone.HumanPlayer);
			else
				tgt.addTarget(AllZone.ComputerPlayer);		
		}

		if (cost != null){
			// AI currently disabled for these costs
			if (cost.getSubCounter()){
				if (cost.getCounterType().equals(Counters.P1P1)) {
					// A card has a 25% chance per counter to be able to pass through here
					// 4+ counters will always pass. 0 counters will never
					int currentNum = source.getCounters(cost.getCounterType());
					double percent = .25 * (currentNum / cost.getCounterNum());
					if (percent <= r.nextFloat())
						return false;
				}
			}
		}
		
		if (tokenAmount.equals("X") && source.getSVar(tokenAmount).equals("Count$xPaid")){
			// Set PayX here to maximum value.
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(xPay));
		}
		
		return ((r.nextFloat() < .6667) && chance);
	}
	
	private boolean tokenDoTriggerAI(SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		return true;
	}
	
	private String doStackDescription(SpellAbility sa) {
		
		int finalPower = AbilityFactory.calculateAmount(AF.getHostCard(), tokenPower, sa);
		
		int finalToughness = AbilityFactory.calculateAmount(AF.getHostCard(), tokenToughness, sa);
		
		int finalAmount = AbilityFactory.calculateAmount(AF.getHostCard(), tokenAmount, sa);
		
		StringBuilder sb = new StringBuilder();
		
		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(AF.getHostCard().getName()).append(" - ");
		
		sb.append("Put (").append(finalAmount).append(") ").append(finalPower).append("/").append(finalToughness);
		sb.append(" ").append(tokenName).append(" token");
		if(finalAmount != 1) sb.append("s");
		sb.append(" onto the battlefield");
		
		if(tokenOwner.equals("Opponent")) {
			sb.append(" under your opponent's control.");
		}
		else {
			sb.append(".");
		}
		
        if (sa.getSubAbility() != null){
     	   sb.append(sa.getSubAbility().getStackDescription());
        }
		
		return sb.toString();
	}
	
	private void doResolve(SpellAbility sa) {
		String imageName = "";
		Player controller;
		String cost = "";
		//Construct colors
		String colorDesc = "";
		for(String col : tokenColors) {
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
		if(tokenImage.equals("")) {			
			
			imageName += colorDesc + " " + tokenPower + " " + tokenToughness + " " + tokenName;
		}
		else {
			imageName = tokenImage;
		}
		System.out.println("AF_Token imageName = " + imageName);
		
		for(char c : colorDesc.toCharArray()) {
			cost += c + ' ';
		}
		
		cost = colorDesc.replace('C', '1').trim();
		
		controller = AbilityFactory.getDefinedPlayers(AF.getHostCard(),tokenOwner,sa).get(0);

		int finalPower = AbilityFactory.calculateAmount(AF.getHostCard(), tokenPower, sa);
		int finalToughness = AbilityFactory.calculateAmount(AF.getHostCard(), tokenToughness, sa);
		int finalAmount = AbilityFactory.calculateAmount(AF.getHostCard(), tokenAmount, sa);
		
		for(int i=0;i<finalAmount;i++) {
			CardList tokens = CardFactoryUtil.makeToken(tokenName, imageName, controller, cost, tokenTypes, finalPower, finalToughness, tokenKeywords);
			
			//Grant abilities
			if(tokenAbilities != null)
			{
				AbilityFactory af = new AbilityFactory();
				for(String s : tokenAbilities)
				{
					String actualAbility = AF.getHostCard().getSVar(s);
					for(Card c : tokens)
					{
						SpellAbility grantedAbility = af.getAbility(actualAbility, c);
						c.addSpellAbility(grantedAbility);
					}
				}
			}
			
			//Grant triggers
			if(tokenTriggers != null)
			{
				
				for(String s : tokenTriggers)
				{
					String actualTrigger = AF.getHostCard().getSVar(s);
					
					for(final Card c : tokens)
					{
						//Needs to do some voodoo when the token disappears to remove the triggers at the same time.
						Command LPCommand = new Command() {

							private static final long serialVersionUID = -9007707442828928732L;

							public void execute() {
								AllZone.TriggerHandler.removeAllFromCard(c);
							}
							
						};
						c.addLeavesPlayCommand(LPCommand);
						Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c);
						c.addTrigger(parsedTrigger);
						AllZone.TriggerHandler.registerTrigger(parsedTrigger);
					}
				}
			}
			
			//Grant SVars
			if(tokenSVars != null)
			{
				for(String s : tokenSVars)
				{
					String actualSVar = AF.getHostCard().getSVar(s);
					for(Card c : tokens)
					{
						c.setSVar(s, actualSVar);
					}
				}
			}
			
			for(Card c : tokens)
			{
				if(tokenTapped)
				{
					c.tap();
				}
				if(tokenAttacking)
				{
					AllZone.Combat.addAttacker(c);
				}
			}
		}
		
		if (AF.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			else{
               CardFactoryUtil.doDrawBack(AF.getMapParams().get("SubAbility"), finalAmount, AF.getHostCard().getController(),
            		   AF.getHostCard().getController().getOpponent(), null, AF.getHostCard(), null, sa);
			}
		}
	}
}

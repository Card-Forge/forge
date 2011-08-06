
package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_DealDamage {
	private AbilityFactory AF = null;

	private String damage;

	public AbilityFactory_DealDamage(AbilityFactory newAF)
	{
		AF = newAF;

		damage = AF.getMapParams().get("NumDmg");

		// Note: TgtOpp should not be used, Please use ValidTgts$ Opponent instead
	}

	public SpellAbility getAbility() {
		final SpellAbility abDamage = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -7560349014757367722L;

			@Override
			public boolean canPlay() {
				return super.canPlay();
			}

			@Override
			public boolean canPlayAI() {
				return doCanPlayAI(this);

			}

			@Override
			public String getStackDescription() {
				return damageStackDescription(AF, this);
			}

			@Override
			public void resolve() {
				doResolve(this);
				AF.getHostCard().setAbilityUsed(AF.getHostCard().getAbilityUsed() + 1);

			}
		};// Ability_Activated

		return abDamage;
	}

	public SpellAbility getAbilityDamageAll(){

		final SpellAbility abDamageAll = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1831356710492849854L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return damageAllStackDescription(af, this);
			}

			@Override
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}

			@Override
			public boolean canPlayAI() {
				return damageAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				damageAllResolve(af, this);
			}

		};
		return abDamageAll;
	}

	public SpellAbility getSpell() {
		final SpellAbility spDealDamage = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 7239608350643325111L;

			@Override
			public boolean canPlay() {
				return super.canPlay();
			}

			@Override
			public boolean canPlayAI() {
				return doCanPlayAI(this);

			}

			@Override
			public String getStackDescription() {
				return damageStackDescription(AF, this);
			}

			@Override
			public void resolve() {
				doResolve(this);

			}

		}; // Spell

		return spDealDamage;
	}

	public SpellAbility getSpellDamageAll(){
		final SpellAbility spDamageAll = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8004957182752984818L;
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();

			@Override
			public String getStackDescription(){
				if(params.containsKey("SpellDescription"))
					return AF.getHostCard().getName() + " - " + params.get("SpellDescription");
				else
					return damageAllStackDescription(af, this);
			}

			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}

			public boolean canPlayAI()
			{
				return damageAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				damageAllResolve(af, this);
			}

		};
		return spDamageAll;
	}

	public SpellAbility getDrawback() {
		final SpellAbility dbDealDamage = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 7239608350643325111L;

			@Override
			public boolean chkAI_Drawback() {
				// Make sure there is a valid target
				return damageTargetAI(this);
			}

			@Override
			public String getStackDescription() {
				return damageStackDescription(AF, this);
			}

			@Override
			public void resolve() {
				doResolve(this);
			}

		}; // Drawback

		return dbDealDamage;
	}

	public SpellAbility getDrawbackDamageAll(){
		final SpellAbility dbDamageAll = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -6169562107675964474L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return damageAllStackDescription(af, this);
			}

			@Override
			public void resolve() {
				damageAllResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				//check AI life before playing this drawback?
				return true;
			}

		};
		return dbDamageAll;
	}

	private int getNumDamage(SpellAbility saMe) {
		return AbilityFactory.calculateAmount(saMe.getSourceCard(), damage, saMe);
	}

	private boolean shouldTgtP(int d, final boolean noPrevention) {
		int restDamage = d;

		if (!noPrevention)
			restDamage = AllZone.HumanPlayer.staticDamagePrevention(restDamage, AF.getHostCard(), false);

		if (restDamage == 0) return false;

		PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
		CardList hand = new CardList(compHand.getCards());

		if(AF.isSpell() && hand.size() > 7) // anti-discard-at-EOT
		return true;

		if(AllZone.HumanPlayer.getLife() - restDamage < 10) // if damage from this spell would drop the human to less than 10 life
			return true;

		return false;
	}

	private Card chooseTgtC(final int d, final boolean noPrevention) {
		CardList hPlay = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		hPlay = hPlay.getValidCards(AF.getAbTgt().getValidTgts(), AllZone.ComputerPlayer, AF.getHostCard());

		hPlay = hPlay.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				int restDamage = d;
				if (!noPrevention)
					restDamage = c.staticDamagePrevention(d,AF.getHostCard(),false);
				// will include creatures already dealt damage
				return c.getKillDamage() <= restDamage && CardFactoryUtil.canTarget(AF.getHostCard(), c)
				&& !c.getKeyword().contains("Indestructible") && !(c.getSVar("SacMe").length() > 0);
			}
		});

		if(hPlay.size() > 0) {
			Card best = CardFactoryUtil.AI_getBestCreature(hPlay);
			return best;
		}

		return null;
	}

	private boolean doCanPlayAI(SpellAbility saMe)
	{           
		int dmg = getNumDamage(saMe);
		boolean rr = AF.isSpell();

		// temporarily disabled until better AI
		if (AF.getAbCost().getSacCost())    {
			if(AllZone.HumanPlayer.getLife() - dmg > 0) // only if damage from this ability would kill the human
				return false;
		}
		if (AF.getAbCost().getSubCounter())  {
			// +1/+1 counters only if damage from this ability would kill the human, otherwise ok
			if(AllZone.HumanPlayer.getLife() - dmg > 0 && AF.getAbCost().getCounterType().equals(Counters.P1P1))
				return false;
		}
		if (AF.getAbCost().getLifeCost())    {
			if(AllZone.HumanPlayer.getLife() - dmg > 0) // only if damage from this ability would kill the human
				return false;
		}

		if (!ComputerUtil.canPayCost(saMe))
			return false;

		// TODO handle proper calculation of X values based on Cost

		// todo: this should only happen during Players EOT or if Stuffy is going to die
		if(AF.getHostCard().equals("Stuffy Doll")) {
			return true;
		}

		if (AF.isAbility())
		{
			Random r = new Random(); // prevent run-away activations
			if(r.nextFloat() <= Math.pow(.6667, AF.getHostCard().getAbilityUsed()))
				rr = true;
		}

		boolean bFlag = damageTargetAI(saMe);
		if (!bFlag)
			return false;

		Ability_Sub subAb = saMe.getSubAbility();
		if (subAb != null)
			rr &= subAb.chkAI_Drawback();
		return rr;

	}

	private boolean damageTargetAI(SpellAbility saMe) {
		int dmg = getNumDamage(saMe);
		Target tgt = AF.getAbTgt();
		HashMap<String,String> params = AF.getMapParams();

		boolean noPrevention = params.containsKey("NoPrevention");

		if (tgt == null){
			// todo: Improve circumstances where the Defined Damage is unwanted
			ArrayList<Object> objects = AbilityFactory.getDefinedObjects(saMe.getSourceCard(), params.get("Defined"), saMe);

			for(Object o : objects){
				if (o instanceof Card){
					//Card c = (Card)o;
				}
				else if (o instanceof Player){
					Player p = (Player)o;
					if (p.isComputer() && dmg >= p.getLife())	// Damage from this spell will kill me
						return false;
				}			
			}
			return true;
		}


		tgt.resetTargets();

		// target loop
		while (tgt.getNumTargeted() < tgt.getMaxTargets(saMe.getSourceCard(), saMe)) {
			// TODO: Consider targeting the planeswalker
			if (tgt.canTgtCreatureAndPlayer()) {

				if (shouldTgtP(dmg,noPrevention)) {
					tgt.addTarget(AllZone.HumanPlayer);
					continue;
				}

				Card c = chooseTgtC(dmg,noPrevention);
				if (c != null) {
					tgt.addTarget(c);
					continue;
				}
			}

			if (tgt.canTgtPlayer()) {
				tgt.addTarget(AllZone.HumanPlayer);
				continue;
			}

			if (tgt.canTgtCreature()) {
				Card c = chooseTgtC(dmg,noPrevention);
				if (c != null) {
					tgt.addTarget(c);
					continue;
				}
			}
			// fell through all the choices, no targets left?
			if (tgt.getNumTargeted() < tgt.getMinTargets(saMe.getSourceCard(), saMe)
					|| tgt.getNumTargeted() == 0) {
				tgt.resetTargets();
				return false;
			} else {
				// todo is this good enough? for up to amounts?
				break;
			}
		}
		return true;
	}

	private String damageStackDescription(AbilityFactory af, SpellAbility sa){
		// when damageStackDescription is called, just build exactly what is happening
		StringBuilder sb = new StringBuilder();
		String name = af.getHostCard().toString();
		int dmg = getNumDamage(sa);

		ArrayList<Object> tgts;
		if(sa.getTarget() == null) 
			tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		else 
			tgts = sa.getTarget().getTargets();

		if (!(sa instanceof Ability_Sub))
			sb.append(name).append(" - ");

		sb.append("Deals ").append(dmg).append(" damage to ");

		for(int i = 0; i < tgts.size(); i++){
			if (i != 0)
				sb.append(" ");

			Object o = tgts.get(0);
			if (o instanceof Card || o instanceof Player)
				sb.append(o.toString());
		}

		sb.append(". ");

		if (sa.getSubAbility() != null){
			sb.append(sa.getSubAbility().getStackDescription());
		}

		return sb.toString();
	}

	private void doResolve(SpellAbility saMe)
	{
		int dmg = getNumDamage(saMe);
		HashMap<String,String> params = AF.getMapParams();

		boolean noPrevention = params.containsKey("NoPrevention");

		ArrayList<Object> tgts;
		if(saMe.getTarget() == null) 
			tgts = AbilityFactory.getDefinedObjects(saMe.getSourceCard(), params.get("Defined"), saMe);
		else 
			tgts = saMe.getTarget().getTargets();

		boolean targeted = (AF.getAbTgt() != null);

		for(Object o : tgts){
			if (o instanceof Card){
				Card c = (Card)o;
				if(AllZone.GameAction.isCardInPlay(c) && (!targeted || CardFactoryUtil.canTarget(AF.getHostCard(), c))) {
					if (noPrevention)
						c.addDamageWithoutPrevention(dmg, AF.getHostCard());
					else
						c.addDamage(dmg, AF.getHostCard());
				}

			}
			else if (o instanceof Player){
				Player p = (Player) o;
				if (!targeted || p.canTarget(AF.getHostCard())) {
					if (noPrevention)
						p.addDamageWithoutPrevention(dmg, AF.getHostCard());
					else
						p.addDamage(dmg, AF.getHostCard());
				}
			}
		}

		if (AF.hasSubAbility()){
			Ability_Sub abSub = saMe.getSubAbility();
			if (abSub != null){
				abSub.resolve();
			}
			else{
				Object obj = tgts.get(0);

				Player pl = null;
				Card c = null;

				if (obj instanceof Card){
					c = (Card)obj;
					pl = c.getController();
				}
				else{
					pl = (Player)obj;
				}
				CardFactoryUtil.doDrawBack(params.get("SubAbility"), dmg, AF.getHostCard().getController(),
						AF.getHostCard().getController().getOpponent(),   pl, AF.getHostCard(), c, saMe);
			}
		}
	}

	private void damageAllResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		Card card = sa.getSourceCard();

		int dmg = getNumDamage(sa);

		String valid = "";
		String players = "";

		if(params.containsKey("ValidCards")) 
			valid = params.get("ValidCards");
		if(params.containsKey("ValidPlayers"))
			players = params.get("ValidPlayers");

		CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(valid.split(","), card.getController(), card);

		for(Card c:list) c.addDamage(dmg, card);

		if(players.equals("Each")) {
			for(Player p:AllZoneUtil.getPlayersInGame()) {
				p.addDamage(dmg, card);
			}
		}
		else if(players.equals("EachOpponent")) {
			for(Player p:AllZoneUtil.getOpponents(card.getController())) p.addDamage(dmg, card);
		}
		else if(players.equals("Self"))
			card.getController().addDamage(dmg, card);
		else {
			//anything else to go here?
		}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
				abSub.resolve();
			}
			else
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
		}
	}

	private String damageAllStackDescription(final AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		String name = af.getHostCard().getName();
		HashMap<String,String> params = af.getMapParams();
		String desc = "";
		if(params.containsKey("ValidDescription")) 
			desc = params.get("ValidDescription");
		int dmg = getNumDamage(sa);

		sb.append(name).append(" - Deals "+dmg+" to "+desc);

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private boolean damageAllCanPlayAI(final AbilityFactory af, final SpellAbility sa){
    		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
    		Random r = new Random();
    		Ability_Cost abCost = sa.getPayCosts();
    		final Card source = sa.getSourceCard();
    		final HashMap<String,String> params = af.getMapParams();
    		String numDmg = params.get("NumDmg");
    		final int dmg = getNumDamage(sa); 
    		String validC = "";
    		String validP = "";
    		final int maxX = ComputerUtil.getAvailableMana().size() - CardUtil.getConvertedManaCost(source);

    		if(params.containsKey("ValidCards")) 
    			validC = params.get("ValidCards");
    		if(params.containsKey("ValidPlayers"))
    			validP = params.get("ValidPlayers");

    		CardList humanlist = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
    		CardList computerlist = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);

    		humanlist = humanlist.getValidCards(validC.split(","), source.getController(), source);
    		computerlist = computerlist.getValidCards(validC.split(","), source.getController(), source);
    		
    		CardListFilter filterX = new CardListFilter(){
    			public boolean addCard(Card c)
    			{
    				return CardFactoryUtil.canDamage(source, c) && c.staticDamagePrevention(maxX, source, false) >= c.getKillDamage();
    			}
    		};
    		
    		CardListFilter filter = new CardListFilter(){
    			public boolean addCard(Card c)
    			{
    				return CardFactoryUtil.canDamage(source, c) && c.staticDamagePrevention(dmg, source, false) >= c.getKillDamage();
    			}
    		};

    		humanlist = humanlist.getNotKeyword("Indestructible");
    		computerlist = computerlist.getNotKeyword("Indestructible");
    		
    		//TODO: X may be something different than X paid
    		if(numDmg.equals("X")) {
    			humanlist = humanlist.filter(filterX);
    			computerlist = computerlist.filter(filterX);
    		}
    		else {
    			humanlist = humanlist.filter(filter);
    			computerlist = computerlist.filter(filter);
    		}
    		
    		
    		//abCost stuff that should probably be centralized...
    		if (abCost != null){
    			// AI currently disabled for some costs
    			if (abCost.getSacCost()){ 
    				//OK
    			}
    			if (abCost.getLifeCost()){
    				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
    					return false;
    			}
    			if (abCost.getDiscardCost()) //OK

    			if (abCost.getSubCounter()){
    					// OK
    			}
    		}

    		if (!ComputerUtil.canPayCost(sa))
    			return false;
    		/////
    		
    		//Don't kill yourself
    		if (validP.contains("Each") 
    				&& AllZone.ComputerPlayer.getLife() <= AllZone.ComputerPlayer.staticDamagePrevention(dmg, source, false))
				return false;
    		
    		//TODO: X may be something different than X paid
			if ((validP.contains("Each") || validP.contains("EachOpponent")) && numDmg.equals("X") 
					&& AllZone.HumanPlayer.getLife() <= AllZone.HumanPlayer.staticDamagePrevention(maxX, source, false))
					return true;
    		
    		//if we can kill human, do it
    		if((validP.contains("Each") || validP.contains("EachOpponent")) 
    				&& AllZone.HumanPlayer.getLife() <= AllZone.HumanPlayer.staticDamagePrevention(dmg, source, false))
    			return true;

    		 // prevent run-away activations - first time will always return true
    		 boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());

    		 // evaluate both lists and pass only if human creatures are more valuable
			 if(CardFactoryUtil.evaluateCreatureList(computerlist) + 200 >= CardFactoryUtil.evaluateCreatureList(humanlist))
				 return false;

    		 Ability_Sub subAb = sa.getSubAbility();
    		 if (subAb != null)
    		 	chance &= subAb.chkAI_Drawback();

    		 return ((r.nextFloat() < .6667) && chance);
	}
}

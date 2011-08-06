package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_ZoneAffecting {
	public static SpellAbility createAbilityDraw(final AbilityFactory AF){
		final SpellAbility abDraw = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return drawStackDescription(af, this);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();		
			}
			
			public boolean canPlayAI()
			{
				return drawCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				drawResolve(af, this);
			}
			
		};
		return abDraw;
	}
	
	public static SpellAbility createSpellDraw(final AbilityFactory AF){
		final SpellAbility spDraw = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return drawStackDescription(af, this);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return drawCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				drawResolve(af, this);
			}
			
		};
		return spDraw;
	}
	
	public static SpellAbility createDrawbackDraw(final AbilityFactory AF){
		final SpellAbility dbDraw = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return drawStackDescription(af, this);
			}

			@Override
			public void resolve() {
				drawResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return drawTargetAI(af, this);
			}
			
		};
		return dbDraw;
	}
	
	public static String drawStackDescription(AbilityFactory af, SpellAbility sa){
		Player player = af.getAbTgt() == null ? sa.getActivatingPlayer() : sa.getTargetPlayer(); 
		StringBuilder sb = new StringBuilder();
		
		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		
		sb.append(player.toString());
		sb.append(" draws (");
		sb.append(AbilityFactory.calculateAmount(sa.getSourceCard(), af.getMapParams().get("NumCards"), sa));
		sb.append(").");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
        	sb.append(abSub.getStackDescription());
        }
		
		return sb.toString();
	}
	
	public static boolean drawCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		Ability_Cost abCost = af.getAbCost();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) {
				if (abCost.getCounterType().equals(Counters.P1P1)) return false; // Other counters should be used 
			}
			
		}
			
		boolean bFlag = drawTargetAI(af, sa);
		
		if (!bFlag)
			return false;
		
		if (tgt != null){
			ArrayList<Player> players = tgt.getTargetPlayers();
			if (players.size() > 0 && players.get(0).equals(AllZone.HumanPlayer))
				return true;
		}
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		// some other variables here, like handsize vs. maxHandSize

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		return randomReturn;
	}
	
    public static boolean drawTargetAI(AbilityFactory af, SpellAbility sa) {
        Target tgt = af.getAbTgt();
        HashMap<String,String> params = af.getMapParams();
        
        int computerHandSize = AllZoneUtil.getCardsInZone(Constant.Zone.Hand, AllZone.ComputerPlayer).size();
        int humanLibrarySize = AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.HumanPlayer).size();
        int computerLibrarySize = AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.ComputerPlayer).size();
        int computerMaxHandSize = AllZone.ComputerPlayer.getMaxHandSize();
        
        // todo: handle deciding what X would be around here for Braingeyser type cards
        int numCards = 1;
        if (params.containsKey("NumCards"))
        	numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
        	
        
        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();
            
            if (!AllZone.HumanPlayer.cantLose() && numCards >= humanLibrarySize) {
                // Deck the Human? DO IT!
                tgt.addTarget(AllZone.HumanPlayer);
                return true;
            }
            
            if (numCards >= computerLibrarySize) {
                // Don't deck your self
                return false;
            }
            
            if (computerHandSize + numCards > computerMaxHandSize) {
                // Don't draw too many cards and then risk discarding cards at EOT
                return false;
            }
            
            tgt.addTarget(AllZone.ComputerPlayer);
        }
        else {
            // ability is not targeted
            if (numCards >= computerLibrarySize) {
                // Don't deck yourself
                return false;
            }
            if (computerHandSize + numCards > computerMaxHandSize) {
                // Don't draw too many cards and then risk discarding cards at EOT
                return false;
            }
        }
        return true;
    }// drawTargetAI()
	
	public static void drawResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		
		Card source = sa.getSourceCard();
		int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else{
			tgtPlayers = new ArrayList<Player>();
			tgtPlayers.add(sa.getActivatingPlayer());
		}
		
		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard())){
				if (params.containsKey("NextUpkeep"))
					for(int i = 0; i < numCards; i++)
						p.addSlowtripList(source);
				else
					p.drawCards(numCards);		
				
			}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
	        else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), tgtPlayers.get(0), source, null, sa);
	        }
		}
	}
	
	
	// ******************** MILL ****************************
	
	public static SpellAbility createAbilityMill(final AbilityFactory AF){
		final SpellAbility abMill = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return millStackDescription(this, af);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();		
			}
			
			public boolean canPlayAI()
			{
				return millCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				millResolve(af, this);
			}
			
		};
		return abMill;
	}
	
	public static SpellAbility createSpellMill(final AbilityFactory AF){
		final SpellAbility spMill = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return millStackDescription(this, af);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return millCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				millResolve(af, this);
			}
			
		};
		return spMill;
	}
	
	public static SpellAbility createDrawbackMill(final AbilityFactory AF){
		final SpellAbility dbMill = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return millStackDescription(this, af);
			}
			
			public boolean canPlayAI()
			{
				return millCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				millResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return millTargetAI(af, this);
			}
			
		};
		return dbMill;
	}
	
	public static String millStackDescription(SpellAbility sa, AbilityFactory af){
		// when getStackDesc is called, just build exactly what is happening
		Player player = af.getAbTgt() == null ? sa.getActivatingPlayer() : sa.getTargetPlayer(); 
		StringBuilder sb = new StringBuilder();
		
		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		
		sb.append("Mills ");
		int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), af.getMapParams().get("NumCards"), sa);
		sb.append(numCards);
		sb.append(" Card(s) from ");
		sb.append(player.toString());
		sb.append("'s library.");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
        	sb.append(abSub.getStackDescription());
        }
		
		return sb.toString();
	}
	
	public static boolean millCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		

		Card source = sa.getSourceCard();
		Ability_Cost abCost = af.getAbCost();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) {
				if (abCost.getCounterType().equals(Counters.P1P1)) return false; // Other counters should be used 
			}
			
		}
		
		boolean bFlag = millTargetAI(af, sa);
		if (!bFlag)
			return false;
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		// some other variables here, like deck size, and phase and other fun stuff

		return randomReturn;
	}
	
	public static boolean millTargetAI(AbilityFactory af, SpellAbility sa){
		Target tgt = af.getAbTgt();
		HashMap<String,String> params = af.getMapParams();
		
		if (tgt != null){
			tgt.resetTargets();
			
			int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
			
			CardList pLibrary = AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.HumanPlayer);
			
			if (pLibrary.size() == 0)	// deck already empty, no need to mill
				return false;
			
			if (numCards >= pLibrary.size()){
				// Can Mill out Human's deck? Do it!
				tgt.addTarget(AllZone.HumanPlayer);
				return true;
			}
			
			// Obscure case when you know what your top card is so you might? want to mill yourself here
			// if (AI wants to mill self)
			// tgt.addTarget(AllZone.ComputerPlayer);
			// else
			tgt.addTarget(AllZone.HumanPlayer);
		}
		return true;
	}
	
	public static void millResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		
		Card source = sa.getSourceCard();

		int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else{
			tgtPlayers = new ArrayList<Player>();
			tgtPlayers.add(sa.getActivatingPlayer());
		}
		
		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard()))
				p.mill(numCards);	

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), tgtPlayers.get(0), source, null, sa);
			}
		}
	}
	
	//////////////////////
	//
	//Discard stuff
	//
	//////////////////////
	
	//NumCards - the number of cards to be discarded (may be integer or X)
	//Opponent - set to True if "target opponent" - hopefully will be obsolete soon
	//Mode	- the mode of discard - should match spDiscard
	//				-Random
	//				-TgtChoose
	//				-RevealYouChoose
	//				-Hand
	//DiscardValid - a ValidCards syntax for acceptable cards to discard
	//UnlessType - a ValidCards expression for "discard x unless you discard a ..."
	//TODO - possibly add an option for EachPlayer$True - Each player discards a card (Rix Maadi, Slivers, 
	//			Delirium Skeins, Strong Arm Tactics, Wheel of Fortune, Wheel of Fate
	
	//Examples:
	//A:SP$Discard | Cost$B | Tgt$TgtP | NumCards$2 | Mode$Random | SpellDescription$<...>
	//A:AB$Discard | Cost$U | Opponent$True | Mode$RevealYouChoose | NumCards$X | SpellDescription$<...>
	
	public static SpellAbility createAbilityDiscard(final AbilityFactory AF) {
		final SpellAbility abDraw = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 4348585353456736817L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription() {
				// when getStackDesc is called, just build exactly what is happening
				return discardStackDescription(af, this);
			}
			
			@Override
			public boolean canPlay() {
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			@Override
			public boolean canPlayAI() {
				discardTargetAI(af);
				return discardCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				discardResolve(af, this);
			}
			
		};
		return abDraw;
	}

	public static SpellAbility createSpellDiscard(final AbilityFactory AF) {
		final SpellAbility spDraw = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 4348585353456736817L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription() {
				// when getStackDesc is called, just build exactly what is happening
				return discardStackDescription(af, this);
			}
			
			@Override
			public boolean canPlay() {
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			@Override
			public boolean canPlayAI() {
				discardTargetAI(af);
				return discardCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				discardResolve(af, this);
			}
			
		};
		return spDraw;
	}
	
	public static SpellAbility createDrawbackDiscard(final AbilityFactory AF) {
		final SpellAbility dbDraw = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 4348585353456736817L;
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription() {
				// when getStackDesc is called, just build exactly what is happening
				return discardStackDescription(af, this);
			}
			
			@Override
			public boolean canPlay() {
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			@Override
			public boolean canPlayAI() {
				discardTargetAI(af);
				return discardCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				discardResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				discardTargetAI(af);
				return discardCheckDrawbackAI(af, this);
			}
			
		};
		return dbDraw;
	}
	
	
	private static void discardResolve(final AbilityFactory af, final SpellAbility sa){
		Card source = sa.getSourceCard();
		HashMap<String,String> params = af.getMapParams();

		boolean opp = params.containsKey("Opponent");
		String mode = params.get("Mode");

		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else{
			tgtPlayers = new ArrayList<Player>();
			
			Player activator = sa.getActivatingPlayer();

			/*
			 * This may need to check that the opponent can be targeted.
			 * I think, ideally, this is handled by something like ValidTgts$Player.Opponent
			 * actually, canTarget is checked slightly below here...
			 */
			if (opp)
				tgtPlayers.add(activator.getOpponent());
			else
				tgtPlayers.add(activator);
		}

		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard())) {	
				if(mode.equals("Hand")) {
					p.discardHand(sa);
				}
				else {
					int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
					if(mode.equals("Random")) {
						p.discardRandom(numCards, sa);
					}
					if(mode.equals("TgtChoose")) {
						if(params.containsKey("UnlessType")) {
							p.discardUnless(numCards, params.get("UnlessType"), sa);
						}
						else p.discard(numCards, sa);
					}

					if(mode.equals("RevealYouChoose")) {
						PlayerZone pzH = AllZone.getZone(Constant.Zone.Hand, p);
						if(pzH.size() != 0) {
							CardList dPHand = new CardList(pzH.getCards());
							CardList dPChHand = new CardList(dPHand.toArray());

							if (params.containsKey("DiscardValid")) {	// Restrict card choices
								String[] dValid = params.get("DiscardValid").split(",");
								dPChHand = dPHand.getValidCards(dValid,source.getController(),source);
							}

							if(source.getController().isComputer()){
								//AI
								for(int i = 0; i < numCards; i++) {
									if (dPChHand.size() > 0){
										CardList dChoices = new CardList();
										if(params.containsKey("DiscardValid")) {
											String dValid = params.get("DiscardValid");
											if (dValid.contains("Creature") && !dValid.contains("nonCreature")) {
												Card c = CardFactoryUtil.AI_getBestCreature(dPChHand);
												if (c!=null)
													dChoices.add(CardFactoryUtil.AI_getBestCreature(dPChHand));
											}
										}


										CardListUtil.sortByTextLen(dPChHand);
										dChoices.add(dPChHand.get(0));

										CardListUtil.sortCMC(dPChHand);
										dChoices.add(dPChHand.get(0));

										Card dC = dChoices.get(CardUtil.getRandomIndex(dChoices));
										dPChHand.remove(dC);

										CardList dCs = new CardList();
										dCs.add(dC);
										AllZone.Display.getChoiceOptional("Computer has chosen", dCs.toArray());

										AllZone.ComputerPlayer.discard(dC, sa);
									}
								}
							}
							else {
								//human
								AllZone.Display.getChoiceOptional("Revealed computer hand", dPHand.toArray());

								for(int i = 0; i < numCards; i++) {
									if (dPChHand.size() > 0) {
										Card dC = AllZone.Display.getChoice("Choose a card to be discarded", dPChHand.toArray());

										dPChHand.remove(dC);
										AllZone.HumanPlayer.discard(dC, sa);
									}
								}
							}
						}
					}
				}
			}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
				abSub.resolve();
			}
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), source.getController(), source, null, sa);
			}
		}
	}
	
	private static String discardStackDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String mode = params.get("Mode");
		Player player;
		if(af.getAbTgt() == null) {
			player = sa.getActivatingPlayer();
			
			if(params.containsKey("Opponent")) {
				player = player.getOpponent();
			}
		}
		else {
			player = sa.getTargetPlayer();
		}
		StringBuilder sb = new StringBuilder();
		
		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		
		sb.append(player.toString());
		
		if(mode.equals("RevealYouChoose")) sb.append(" reveals his or her hand.");
		if(mode.equals("RevealYouChoose")) sb.append("  You choose (");
		else sb.append(" discards (");
		
		if(mode.equals("Hand")) {
			sb.append("Hand");
		}
		else sb.append(af.getMapParams().get("NumCards"));
			
		sb.append(")");
		if(mode.equals("RevealYouChoose")) sb.append(" to discard");
		
		if(mode.equals("Random"))
			sb.append(" at random.");
		else sb.append(".");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
        	sb.append(abSub.getStackDescription());
        }
		
		return sb.toString();
	}
	
	private static boolean discardCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		Ability_Cost abCost = af.getAbCost();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) {
				if (abCost.getCounterType().equals(Counters.P1P1)) return false; // Other counters should be used 
			}
			
		}
			/*
		///////////////////////////////////////////////////
		//copied from spDiscard
		
		int nCards = Integer.parseInt(params.get("NumCards"));;
    	
    	CardList humanHand = AllZoneUtil.getPlayerHand(AllZone.HumanPlayer);
    	int numHHand = humanHand.size();
    	
    	if (numHHand >= nCards)
    	{
    		if (Tgt)
    			setTargetPlayer(AllZone.HumanPlayer);
    		
    		return true;
    	}
    		
    	return false;
    	
    	//end copied
		
		if (!bFlag)
			return false;
		*/
		if (tgt != null){
			ArrayList<Player> players = tgt.getTargetPlayers();
			if (players.size() > 0 && players.get(0).equals(AllZone.HumanPlayer))
				return true;
		}
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		// some other variables here, like handsize vs. maxHandSize

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		return randomReturn;
	}
	
	private static boolean discardTargetAI(AbilityFactory af) {
		Target tgt = af.getAbTgt();
		if(tgt!= null) {
			tgt.addTarget(AllZone.HumanPlayer);
			return true;
		}
		return false;
	}// discardTargetAI()
	
	
	private static boolean discardCheckDrawbackAI(AbilityFactory af, Ability_Sub subAb) {
		// Drawback AI improvements
		// if parent draws cards, make sure cards in hand + cards drawn > 0
		return true;
	}// discardCheckDrawbackAI()
}


package forge;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import forge.properties.ForgeProps;
import forge.properties.NewConstants.LANG.GameAction.GAMEACTION_TEXT;

public class GameAction {
    //  private StaticEffects staticEffects = new StaticEffects();
	
	//private CardList          humanList;
    //private CardList          computerList;
    
    //private boolean           fantasyQuest     = false;
    
    public void resetActivationsPerTurn(){
    	CardList all = AllZoneUtil.getCardsInGame();
    	
        // Reset Activations per Turn
        for(Card card : all){
        	for(SpellAbility sa : card.getSpellAbility())
        		sa.getRestrictions().resetTurnActivations();
        }
    }
    
    public Card moveTo(PlayerZone zone, Card c) {
        //c = getCurrentCard(c); - breaks things, seems to not be needed
        //not 100% sure though, this might be broken
        

        //here I did a switcheroo: remove card from zone first, 
        //THEN do copyCard, to ensure leavesPlay() effects will execute.
        
        PlayerZone p = AllZone.getZone(c);
        //like if a Sorcery was resolved and needs to be put in the graveyard
        //used by Input_Instant
        if(p != null) p.remove(c);
        
        //hack: do not reset unearthed
        boolean unearthed = false;
        if (c.isUnearthed())
        	unearthed = true;
        
        //create new Card, which resets stats and anything that might have changed during play
        if(!c.isToken()) c = AllZone.CardFactory.copyCard(c);
        
        if (unearthed)
        	c.setUnearthed(true);
        
        zone.add(c);
        return c;
    }
    
    //card can be anywhere like in Hand or in Play
    public void moveToGraveyard(Card c) {
    	if (AllZoneUtil.isCardInPlay("Leyline of the Void", c.getOwner().getOpponent())) {
    		moveTo(AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner()), c);
    	}
    	else {
    		//must put card in OWNER's graveyard not controller's
    		PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getOwner());
    		moveTo(grave, c);
    	}
    }
    
    public void moveToHand(Card c) {
        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
        moveTo(hand, c);
    }
    
    public void moveToPlay(Card c) {
        PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
        moveTo(play, c);
    }
    
    public void moveToTopOfLibrary(Card c) {
        PlayerZone p = AllZone.getZone(c);
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, c.getOwner());
        
        if(p != null) p.remove(c);
        if(!c.isToken()) c = AllZone.CardFactory.copyCard(c);
        
        library.add(c, 0);
    }
    
    public void moveToLibrary(Card c) {
    	moveToTopOfLibrary(c);
    }
    
    /**
     * moves a card from whichever Zone it's in to the bottom of its owner's library
     * 
     * @param c the card to move
     */
    public void moveToBottomOfLibrary(Card c) {
    	PlayerZone p = AllZone.getZone(c);
    	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, c.getOwner());
    	if( p != null ) p.remove(c);
    	if(!c.isToken()) lib.add(c);
    }
    /*
    @Deprecated
    public void discardRandom(Player player, SpellAbility sa) {
    	player.discardRandom(sa);
    	/*
        Card[] c = AllZone.getZone(Constant.Zone.Hand, player).getCards();
        if(c.length != 0) discard(CardUtil.getRandom(c), sa);
        *
    }*/
    /*
    @Deprecated
    public void mill(Player player, int n)
    {
    	player.mill(n);
    	/*
    	CardList lib = AllZoneUtil.getPlayerCardsInLibrary(player);
		
        int max = Math.min(n, lib.size());
        
        for(int i = 0; i < max; i++) {
            AllZone.GameAction.moveToGraveyard(lib.get(i));
        }
        *
    }
    */
    /*
    public void discard(Card c, SpellAbility sa)
    {
    	if (sa!= null)
    	{
    		;
    	}
    	
    	AllZone.GameAction.CheckWheneverKeyword(c,"DiscardsCard",null);
        discard_nath(c);
        discard_megrim(c);
        if(AllZoneUtil.getPlayerCardsInPlay(c.getOwner(), "Necropotence").size() > 0){	// necro disrupts madness
        	exile(c);
        	return;
        }
        discard_madness(c);
	        if ((c.getKeyword().contains("If a spell or ability an opponent controls causes you to discard CARDNAME, put it onto the battlefield instead of putting it into your graveyard.") ||
	        	c.getKeyword().contains("If a spell or ability an opponent controls causes you to discard CARDNAME, put it onto the battlefield with two +1/+1 counters on it instead of putting it into your graveyard."))	
	        	&& !c.getController().equals(sa.getSourceCard().getController()))
	        	discard_PutIntoPlayInstead(c);
	        else if (c.getKeyword().contains("If a spell or ability an opponent controls causes you to discard CARDNAME, return it to your hand."))
	        		;
	    	else
	        	moveToGraveyard(c);
    	}
    */
    /*
    @Deprecated
    public void discardRandom(Player player, int numDiscard, SpellAbility sa) {
    	player.discardRandom(numDiscard, sa);
    	/*
        for(int i = 0; i < numDiscard; i++) {
            Card[] c = AllZone.getZone(Constant.Zone.Hand, player).getCards();
            if(c.length != 0) discard(CardUtil.getRandom(c), sa);
        }
    }*/
    
    /*
    public void discard(Player player, int numDiscard, SpellAbility sa) {
        if(player.isHuman()) AllZone.InputControl.setInput(CardFactoryUtil.input_discard(numDiscard, sa));
        else {
            for(int i = 0; i < numDiscard; i++)
                AI_discard(sa);
        }
    }
    */
    /*
    @Deprecated
    public void discardUnless(Player player, int numDiscard, String uType, SpellAbility sa) {
        if(player.isHuman()) AllZone.InputControl.setInput(CardFactoryUtil.input_discardNumUnless(
                numDiscard, uType, sa));
        else AI_discardNumUnless(numDiscard, uType, sa);
    }
    */
    /*
    public void discardHand(Player player, SpellAbility sa) {
        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
        CardList list = new CardList(hand.getCards());
        discardRandom(player, list.size(), sa);
    }
    */
    
    public boolean AI_discardNumType(int numDiscard, String[] uTypes, SpellAbility sa) {
        CardList hand = new CardList();
        hand.addAll(AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer).getCards());
        CardList tHand = hand.getValidCards(uTypes,sa.getActivatingPlayer(),sa.getSourceCard());
        
        if(tHand.size() >= numDiscard) {
            CardListUtil.sortCMC(tHand);
            tHand.reverse();
            for(int i = 0; i < numDiscard; i++)
            	tHand.get(i).getController().discard(tHand.get(i), sa);
            return true;
        }
        return false;
    }
    /*
    public void AI_discardNum(int numDiscard, SpellAbility sa) {
        for(int i = 0; i < numDiscard; i++)
            AI_discard(sa);
    }*/
    /*
    @Deprecated
    public void AI_discardNumUnless(int numDiscard, String uType, SpellAbility sa) {
        CardList hand = new CardList();
        hand.addAll(AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer).getCards());
        CardList tHand = hand.getType(uType);
        
        if(tHand.size() > 0) {
            CardListUtil.sortCMC(tHand);
            tHand.reverse();
            tHand.get(0).getController().discard(tHand.get(0), sa);  //this got changed to doDiscard basically
            return;
        }
        for(int i = 0; i < numDiscard; i++)
            AllZone.ComputerPlayer.discard(sa);
    }
    */
    /*
    public void AI_discard(SpellAbility sa) {
        CardList hand = new CardList();
        hand.addAll(AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer).getCards());
        
        if(hand.size() > 0) {
            CardList blIP = new CardList();
            blIP.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
            blIP = blIP.getType("Basic");
            if(blIP.size() > 5) {
                CardList blIH = hand.getType("Basic");
                if(blIH.size() > 0) {
                    discard(blIH.get(CardUtil.getRandomIndex(blIH)), sa);
                    return;
                }
                
                CardListUtil.sortAttackLowFirst(hand);
                CardListUtil.sortNonFlyingFirst(hand);
                discard(hand.get(0), sa);
                return;
            } else {
                CardListUtil.sortCMC(hand);
                discard(hand.get(0), sa);
                return;
            }
        }
    }
    */
    /*
    public void handToLibrary(Player player, int numToLibrary, String libPos) {
        if(player.isHuman()) {
            if(libPos.equals("Top") || libPos.equals("Bottom")) libPos = libPos.toLowerCase();
            else {
                Object o = new Object();
                String s = "card";
                if(numToLibrary > 1) s += "s";
                
                o = AllZone.Display.getChoice("Do you want to put the " + s
                        + " on the top or bottom of your library?", new Object[] {"top", "bottom"});
                libPos = o.toString();
            }
            AllZone.InputControl.setInput(CardFactoryUtil.input_putFromHandToLibrary(libPos, numToLibrary));
        } else {
            for(int i = 0; i < numToLibrary; i++) {
                if(libPos.equals("Top") || libPos.equals("Bottom")) libPos = libPos.toLowerCase();
                else {
                    Random r = new Random();
                    if(r.nextBoolean()) libPos = "top";
                    else libPos = "bottom";
                }
                AI_handToLibrary(libPos);
            }
        }
    }
    
    public void AI_handToLibrary(String libPos) {
        CardList hand = new CardList();
        hand.addAll(AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer).getCards());
        
        CardList blIP = new CardList();
        blIP.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
        blIP = blIP.getType("Basic");
        if(blIP.size() > 5) {
            CardList blIH = hand.getType("Basic");
            if(blIH.size() > 0) {
                Card card = blIH.get(CardUtil.getRandomIndex(blIH));
                AllZone.Computer_Hand.remove(card);
                if(libPos.equals("top")) AllZone.Computer_Library.add(card, 0);
                else AllZone.Computer_Library.add(card);
                return;
            }
            
            CardListUtil.sortAttackLowFirst(hand);
            CardListUtil.sortNonFlyingFirst(hand);
            if(libPos.equals("top")) AllZone.Computer_Library.add(hand.get(0), 0);
            else AllZone.Computer_Library.add(hand.get(0));
            AllZone.Computer_Hand.remove(hand.get(0));
            return;
        } else {
            CardListUtil.sortCMC(hand); 
            if(libPos.equals("top")) AllZone.Computer_Library.add(hand.get(0), 0);
            else AllZone.Computer_Library.add(hand.get(0));
            AllZone.Computer_Hand.remove(hand.get(0));
            return;
        }
    }
    */
    
    
    public void discard_nath(Card discardedCard) {
        final Player owner = discardedCard.getOwner();
        final Player opponent = owner.getOpponent();
        
        PlayerZone opponentZone = AllZone.getZone(Constant.Zone.Play, opponent);
        CardList opponentList = new CardList(opponentZone.getCards());
        
        for(int i = 0; i < opponentList.size(); i++) {
            Card card = opponentList.get(i);
            if(card.getName().equals("Nath of the Gilt-Leaf")) {
                Card c = new Card();
                
                c.setOwner(card.getController());
                c.setController(card.getController());
                
                c.setName("Elf Warrior");
                c.setImageName("G 1 1 Elf Warrior");
                c.setManaCost("G");
                c.setToken(true);
                
                c.addType("Creature");
                c.addType("Elf");
                c.addType("Warrior");
                c.setBaseAttack(1);
                c.setBaseDefense(1);
                
                opponentZone.add(c);
            }
        }
    }
    
    public void discard_PutIntoPlayInstead(Card c)
    {
    	/*
    	if (c.getName().equals("Dodecapod"))
    		c.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(c, Counters.P1P1, 2));
        */
    	PlayerZone hand = AllZone.getZone(c);
    	PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
    	//moveTo(play, c);
    	hand.remove(c);
    	play.add(c);
    	if (c.getName().equals("Dodecapod"))
    		c.setCounter(Counters.P1P1, 2, false);
    }
    
    public void discard_megrim(Card c) {
        /* 
         * Whenever an opponent discards a card, Megrim deals 2 damage to that player.
        */
    	final Player owner = c.getOwner();  //discarded card owner
        final Player opponent = owner.getOpponent();  //check this for Megrim
        CardList megrims = AllZoneUtil.getPlayerCardsInPlay(opponent, "Megrim");
        for(Card megrim:megrims) {
        	final Card thisMegrim = megrim;
        	final Ability ability = new Ability(megrim, "0") {
        		@Override
        		public void resolve() {
        			owner.addDamage(2, thisMegrim);
        		}
        	};
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(megrim.getName()).append(" - deals 2 damage to ").append(owner);
        	ability.setStackDescription(sb.toString());
        	
        	AllZone.Stack.add(ability);
        }
    }
    
    public void discard_madness(Card c) {
    	// Whenever a card with madness is discarded, you may cast it for it's madness cost
    	if (!c.hasMadness())
    		return;

    	final Card madness = c;
    	final Ability cast = new Ability(madness, madness.getMadnessCost()) {
    		@Override
    		public void resolve() {
    			//moveToHand(madness);
    			if (madness.getOwner().isHuman())
    				AllZone.Human_Graveyard.remove(madness);
    			else
    				AllZone.Computer_Graveyard.remove(madness);
    			playCardNoCost(madness);
    			System.out.println("Madness cost paid");
    		}
    	};
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append(madness.getName()).append(" - Cast via Madness");
    	cast.setStackDescription(sb.toString());
    	
    	final Ability activate = new Ability(madness, "0") {
    		@Override
    		public void resolve() {
    			// pay madness cost here.
    			if (cast.getManaCost().equals("0"))
    				AllZone.Stack.add(cast);
    			else if (madness.getOwner().isHuman())
    				AllZone.InputControl.setInput(new Input_PayManaCost(cast));
    			else 	// computer will ALWAYS pay a madness cost if he has the mana.
    				ComputerUtil.playStack(cast);	
    		}
    	};
    	
    	StringBuilder sbAct = new StringBuilder();
    	sbAct.append(madness.getName()).append(" - Discarded. Pay Madness Cost?");
    	activate.setStackDescription(sbAct.toString());
        
    	AllZone.Stack.add(activate);
    }
    
    public boolean checkEndGameSate(){
        // Win / Lose
    	boolean humanWins = false;
    	boolean computerWins = false;
    	Player computer = AllZone.ComputerPlayer;
    	Player human = AllZone.HumanPlayer;
    	
    	int gameWon = Constant.Runtime.WinLose.getWin();
    	
    	if (human.hasWon()){	// Winning Conditions can be worth more than losing conditions
    		// Human wins
    		humanWins = true;
    		if (human.getAltWin()){
	        	Constant.Runtime.WinLose.setWinMethod(gameWon, human.getWinCondition());
    		}
	        Constant.Runtime.WinLose.addWin();
    	}
    	
    	else if (computer.hasLost()){
    		// Human wins
    		humanWins = true;
    		
    		if (computer.getAltLose()){
	        	Constant.Runtime.WinLose.setWinMethod(gameWon, computer.getLoseCondition());
    		}
	        Constant.Runtime.WinLose.addWin();
    	}
    	

    	if (computer.hasWon() || human.hasLost()){
    		if (humanWins){
    			// both players won/lost at the same time.
    			// TODO: Handle a Draw here
    		}
    		
    		// Computer wins
    		computerWins = true;
    		Constant.Runtime.WinLose.addLose();
    	}
    	
		return humanWins || computerWins;
    }
    
    
    public void checkStateEffects() {
    	// sol(10/29) added for Phase updates, state effects shouldn't be checked during Spell Resolution
    	if (AllZone.InputControl.getResolving()) 
    		return;
    	
        JFrame frame = (JFrame) AllZone.Display;
        if(!frame.isDisplayable()) return;

        if(checkEndGameSate()) {
            frame.dispose();
            if (!Constant.Quest.fantasyQuest[0])
            	new Gui_WinLose();
            else
            	new Gui_WinLose(Constant.Quest.humanList[0], Constant.Quest.computerList[0],
            			Constant.Quest.humanLife[0], Constant.Quest.computerLife[0]);
            return;
        }
        //do this twice, sometimes creatures/permanents will survive when they shouldn't
        for (int q=0;q<2;q++)
        {
        	//int size = AllZone.StaticEffects.getStateBasedMap().keySet().size();
        	//Object[] arr = AllZone.StaticEffects.getStateBasedMap().keySet().toArray();
        	
	        //card state effects like Glorious Anthem
	        for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
	            Command com = GameActionUtil.commands.get(effect);
	            com.execute();
	        }
	        
	        GameActionUtil.executeCardStateEffects();
	        GameActionUtil.StaticEffectKeyword.execute();
	        GameActionUtil.stPump.execute();
	        
	        //System.out.println("checking state effects");
	        ArrayList<Card> creature = PlayerZoneUtil.getCardType(AllZone.Computer_Play, "Creature");
	        creature.addAll(PlayerZoneUtil.getCardType(AllZone.Human_Play, "Creature"));
	        
	        Card c;
	        Iterator<Card> it = creature.iterator();
	        
	        while(it.hasNext()) {
	            c = it.next();
	            
	            if(c.isEquipped()) {
	                for(int i = 0; i < c.getEquippedBy().size(); i++) {
	                    Card equipment = c.getEquippedBy().get(i);
	                    if(!AllZone.GameAction.isCardInPlay(equipment)) {
	                        equipment.unEquipCard(c);
	                    }
	                }
	            }//if isEquipped()
	            
	            if(c.getNetDefense() <= c.getDamage() && !c.getKeyword().contains("Indestructible")) {
	                destroy(c);
	                AllZone.Combat.removeFromCombat(c); //this is untested with instants and abilities but required for First Strike combat phase
	            }
	
	            else if(c.getNetDefense() <= 0) {
	                destroy(c);
	                AllZone.Combat.removeFromCombat(c);
	            }
	            
	        }//while it.hasNext()
	        
	
	        ArrayList<Card> enchantments = PlayerZoneUtil.getCardType(AllZone.Computer_Play, "Enchantment");
	        enchantments.addAll(PlayerZoneUtil.getCardType(AllZone.Human_Play, "Enchantment"));
	        
	        Iterator<Card> iterate = enchantments.iterator();
	        while(iterate.hasNext()) {
	            c = iterate.next();
	            
	            if(c.isAura()) {
	                for(int i = 0; i < c.getEnchanting().size(); i++) {
	                    Card perm = c.getEnchanting().get(i);
	                    if(!AllZone.GameAction.isCardInPlay(perm)
	                            || CardFactoryUtil.hasProtectionFrom(c, perm)
	                            || ((c.getKeyword().contains("Enchant creature") || c.getKeyword().contains("Enchant tapped creature") ) 
	                               && !perm.getType().contains("Creature"))
	                            || (c.getKeyword().contains("Enchant tapped creature") && perm.isUntapped() ) ) {
	                        c.unEnchantCard(perm);
	                        destroy(c);
	                    }
	                }
	            }//if isAura
	            
	        }//while iterate.hasNext()
	        
	        //Make sure all equipment stops equipping previously equipped creatures that have left play.
	        ArrayList<Card> equip = PlayerZoneUtil.getCardType(AllZone.Computer_Play, "Equipment");
	        equip.addAll(PlayerZoneUtil.getCardType(AllZone.Human_Play, "Equipment"));
	        
	        Iterator<Card> iter = equip.iterator();
	        while(iter.hasNext()) {
	            c = iter.next();
	            if(c.isEquipping()) {
	                Card equippedCreature = c.getEquipping().get(0);
	                if(!AllZone.GameAction.isCardInPlay(equippedCreature)) c.unEquipCard(equippedCreature);
	            }
	        }//while iter.hasNext()
        }//for q=0;q<2
        
        destroyLegendaryCreatures();
        destroyPlaneswalkers();
    }//checkStateEffects()
    

    private void destroyPlaneswalkers() {
        //get all Planeswalkers
        CardList list = AllZoneUtil.getTypeInPlay("Planeswalker");
        
        Card c;
        for(int i = 0; i < list.size(); i++) {
            c = list.get(i);
            
            if(c.getCounters(Counters.LOYALTY) <= 0) AllZone.GameAction.moveToGraveyard(c);
            
            String subtype = c.getType().get(c.getType().size() - 1);
            CardList cl = list.getType(subtype);
            
            if(cl.size() > 1) {
                for(Card crd:cl) {
                    AllZone.GameAction.moveToGraveyard(crd);
                }
            }
        }
        
    }
    
    private void destroyLegendaryCreatures() {
        //ArrayList<Card> a = PlayerZoneUtil.getCardType(AllZone.Human_Play, "Legendary");
        //a.addAll(PlayerZoneUtil.getCardType(AllZone.Computer_Play, "Legendary"));  
        CardList a = AllZoneUtil.getTypeInPlay("Legendary");

        while(!a.isEmpty() && !AllZoneUtil.isCardInPlay("Mirror Gallery")) {
            //ArrayList<Card> b = getCardsNamed(a, (a.get(0)).getName());
        	CardList b = AllZoneUtil.getCardsInPlay(a.get(0).getName());
            a.remove(0);
            if(1 < b.size()) {
                for(int i = 0; i < b.size(); i++)
                    AllZone.GameAction.sacrificeDestroy(b.get(i));            
            }
        }
    }//destroyLegendaryCreatures()
    
    //ArrayList search is all Card objects, returns ArrayList of Cards
    public ArrayList<Card> getCardsNamed(ArrayList<Card> search, String name) {
        ArrayList<Card> a = new ArrayList<Card>();
        Card c[] = CardUtil.toCard(search);
        
        for(int i = 0; i < c.length; i++) {
            if(c[i].getName().equals(name)) a.add(c[i]);
        }
        return a;
    }
    
    public void sacrifice(Card c) {
        sacrificeDestroy(c);
    }
    
    public void destroyNoRegeneration(Card c) {
        if(!AllZone.GameAction.isCardInPlay(c) || c.getKeyword().contains("Indestructible")) return;
        
        if (c.isEnchanted())
        {
        	CardList list = new CardList(c.getEnchantedBy().toArray());
        	list = list.filter(new CardListFilter()
        	{
        		public boolean addCard(Card crd)
        		{
        			return crd.getKeyword().contains("Totem armor");
        		}
        	});
        	CardListUtil.sortCMC(list);
        	
        	if (list.size() != 0)
        	{
        		final Card crd;
	        	if (list.size() == 1)
	        	{
	        		crd = list.get(0);
	        	}
	        	else {
	        		if (c.getController().isHuman())
	        			crd = AllZone.Display.getChoiceOptional("Select totem armor to destroy", list.toArray());
	        		else 
	        			crd = list.get(0);
	        	}
	        	
	        	final Card card = c;
	        	Ability_Static ability = new Ability_Static(crd, "0")
	        	{
	        		public void resolve()
	        		{
	        			destroy(crd);
	    	        	card.setDamage(0);
	    	        	
	        		}
	        	};
	        	
	        	StringBuilder sb = new StringBuilder();
	        	sb.append(crd).append(" - Totem armor: destroy this aura.");
	        	ability.setStackDescription(sb.toString());
	        	
	        	AllZone.Stack.add(ability);
	        	return;
        	}
        }//totem armor
        
        sacrificeDestroy(c);
    }
    
    // Whenever Keyword
    public void checkWheneverKeyword(Card Triggering_Card,String Event, Object[] Custom_Parameters) {
    	checkStateEffects();
    	
        PlayerZone Hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
        PlayerZone Cplay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
        PlayerZone Hgrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer);
        PlayerZone Cgrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
        PlayerZone HRFG = AllZone.getZone(Constant.Zone.Removed_From_Play, AllZone.HumanPlayer);
        PlayerZone CRFG = AllZone.getZone(Constant.Zone.Removed_From_Play, AllZone.ComputerPlayer);
        
 		CardList Cards_WithKeyword = new CardList();
        Cards_WithKeyword.add(new CardList(Hplay.getCards()));
        Cards_WithKeyword.add(new CardList(Cplay.getCards()));
        Cards_WithKeyword.add(new CardList(Hgrave.getCards()));
        Cards_WithKeyword.add(new CardList(Cgrave.getCards()));
        Cards_WithKeyword.add(new CardList(HRFG.getCards()));
        Cards_WithKeyword.add(new CardList(CRFG.getCards()));
 		Cards_WithKeyword = Cards_WithKeyword.filter(new CardListFilter() {
             public boolean addCard(Card c) {
                 if(c.getKeyword().toString().contains("WheneverKeyword")) return true;
                 return false;
             }
         });
 		
 		if(!Cards_WithKeyword.contains(Triggering_Card)) Cards_WithKeyword.add(Triggering_Card);
 		
 		boolean Triggered = false;
 		for(int i = 0; i < Cards_WithKeyword.size() ; i++) {	
 		if(Triggered == false) {	
 			Card card = Cards_WithKeyword.get(i);
 		        ArrayList<String> a = card.getKeyword();
 		        int WheneverKeywords = 0;
 		        int WheneverKeyword_Number[] = new int[a.size()];
 		        for(int x = 0; x < a.size(); x++)
 		            if(a.get(x).toString().startsWith("WheneverKeyword")) {
 		            	WheneverKeyword_Number[WheneverKeywords] = x;
 		            	WheneverKeywords = WheneverKeywords + 1;
 		            }
 		        for(int CKeywords = 0; CKeywords < WheneverKeywords; CKeywords++) {
 		    		if(Triggered == false) {	
                 String parse = card.getKeyword().get(WheneverKeyword_Number[CKeywords]).toString();                
                 String k[] = parse.split(":");
                 if((k[1].contains(Event))) {
                	 runWheneverKeyword(Triggering_Card, Event, Custom_Parameters); // Beached
                	 Triggered = true;
                 }
 		        }
 		        }
 		}
 		}	
    }
	static boolean MultiTarget_Cancelled = false;
    public void runWheneverKeyword(Card c, String Event, Object[] Custom_Parameters) { 
    	/**
    	 * Custom_Parameters Info: 
    	 * For GainLife		: Custom_Parameters[0] = Amount of Life Gained
    	 * For DealsDamage	: Custom_Parameters[0] = Player Target
    	 * 					: Custom_Parameters[2] = Damage Source
    	 * For DrawCard		: Custom_Parameters[0] = Initiating Player
    	 */
		final Card F_TriggeringCard = c;
		final String[] Custom_Strings = new String[10];
		int Custom_Strings_Count = 0;
		boolean Stop = false;
		for(int i = 0; i < Custom_Strings.length; i++) Custom_Strings[i] = "Null";
        PlayerZone Hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
        PlayerZone Cplay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
        PlayerZone Hgrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer);
        PlayerZone Cgrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
        PlayerZone HRFG = AllZone.getZone(Constant.Zone.Removed_From_Play, AllZone.HumanPlayer);
        PlayerZone CRFG = AllZone.getZone(Constant.Zone.Removed_From_Play, AllZone.ComputerPlayer);
        
 		CardList Cards_WithKeyword = new CardList();
        Cards_WithKeyword.add(new CardList(Hplay.getCards()));
        Cards_WithKeyword.add(new CardList(Cplay.getCards()));
        Cards_WithKeyword.add(new CardList(Hgrave.getCards()));
        Cards_WithKeyword.add(new CardList(Cgrave.getCards()));
        Cards_WithKeyword.add(new CardList(HRFG.getCards()));
        Cards_WithKeyword.add(new CardList(CRFG.getCards()));
 		Cards_WithKeyword = Cards_WithKeyword.filter(new CardListFilter() {
             public boolean addCard(Card c) {
                 if(c.getKeyword().toString().contains("WheneverKeyword")) return true;
                 return false;
             }
         });
 		
 		if(!Cards_WithKeyword.contains(c)) Cards_WithKeyword.add(c);
 		
 		for(int i = 0; i < Cards_WithKeyword.size() ; i++) {	
 			Card card = Cards_WithKeyword.get(i);
 			final Card F_card = card;
 		        ArrayList<String> a = card.getKeyword();
 		        int WheneverKeywords = 0;
 		        int WheneverKeyword_Number[] = new int[a.size()];
 		        for(int x = 0; x < a.size(); x++)
 		            if(a.get(x).toString().startsWith("WheneverKeyword")) {
 		            	WheneverKeyword_Number[WheneverKeywords] = x;
 		            	WheneverKeywords = WheneverKeywords + 1;
 		            }
 		        for(int CKeywords = 0; CKeywords < WheneverKeywords; CKeywords++) {
                 String parse = card.getKeyword().get(WheneverKeyword_Number[CKeywords]).toString();                
                 String k[] = parse.split(":");
                 final String F_k[] = k;
                 // Conditions
                 if((k[1].contains(Event)))
                		 {      
                    if(k[1].contains("DealsDamage")) {
                    	boolean Nullified = true;
                        String DamageTakerParse = k[1];                
                        String DamageTaker[] = DamageTakerParse.split("/");
                        for(int z = 0; z < DamageTaker.length - 1; z++) {
                        	if(DamageTaker[z + 1].equals("Opponent") && Custom_Parameters[0].equals(card.getController().getOpponent())) Nullified = false;  
                        }
                        if(Nullified == true) k[4] = "Null";   
                        }
                    if(k[1].contains("CastSpell")) {
                    	boolean Nullified = true;              	
                        String SpellControllerParse = k[1];                
                        String SpellController[] = SpellControllerParse.split("/");
                        for(int z = 0; z < SpellController.length - 1; z++) {
                        	if(SpellController[z + 1].equals("Controller") && (c.getController()).equals(card.getController())) Nullified = false;  
                        	if(SpellController[z + 1].equals("Opponent") && (c.getController()).equals(card.getController().getOpponent())) Nullified = false;
                        	if(SpellController[z + 1].equals("Any")) Nullified = false;
                        }
                        if(Nullified == true) k[4] = "Null";   
                        }
                    if(k[1].contains("PlayLand")) {
                    	boolean Nullified = true;              	
                        String SpellControllerParse = k[1];                
                        String SpellController[] = SpellControllerParse.split("/");
                        for(int z = 0; z < SpellController.length - 1; z++) {
                        	if(SpellController[z + 1].equals("Controller") && (c.getController()).equals(card.getController())) Nullified = false;  
                        	if(SpellController[z + 1].equals("Opponent") && (c.getController()).equals(card.getController().getOpponent())) Nullified = false;
                        	if(SpellController[z + 1].equals("Any")) Nullified = false;
                        }
                        if(Nullified == true) k[4] = "Null";   
                        }
                    int Initiator_Conditions = 1;
                    String ConditionsParse = k[2];                
                    String Conditions[] = ConditionsParse.split("!");
                    Initiator_Conditions = Conditions.length;
                        for(int y = 0; y < Initiator_Conditions; y++) {
                            if(Conditions[y].contains("Self") && !Conditions[y].contains("ControllingPlayer_Self")) {
                            	if(!card.equals(c)) k[4] = "Null";    
                                }
                            if(Conditions[y].contains("ControllingPlayer_Self")) {
                            	if(!card.getController().equals(Custom_Parameters[0])) k[4] = "Null";    
                                }
                            if(Conditions[y].contains("ControllingPlayer_Opponent")) {
                            	// Special Case for Draw Card
                            	if(Event.equals("DrawCard")) {
                            		if(!card.getController().equals(((Player) Custom_Parameters[0]).getOpponent())) k[4] = "Null"; 	
                            	} else if(!card.getController().equals(c.getController().getOpponent())) k[4] = "Null";    
                                }
                                
                            if(Conditions[y].contains("Enchanted_Creature")) {
                            	if(((Card)Custom_Parameters[2]).getEnchantedBy().contains(card) == false) k[4] = "Null";    
                                }
                            if(Conditions[y].contains("Equipped_Creature")) {
                            	if(((Card)Custom_Parameters[2]).getEquippedBy().contains(card) == false) k[4] = "Null";    
                                }
                           	if(Conditions[y].contains("Type") && !Conditions[y].contains("OneTypeOfMany")) {
                                String TypeParse = Conditions[y];                
                                String Type[] = TypeParse.split("/");
                                for(int z = 0; z < Type.length - 1; z++) if(!c.isType(Type[z + 1])) k[4] = "Null";     		
                                     	}
                           	if(Conditions[y].contains("OneTypeOfMany")) {
                           		boolean Nullified = true;
                                String TypeParse = Conditions[y];                
                                String Type[] = TypeParse.split("/");
                                for(int z = 0; z < Type.length - 1; z++) if(c.isType(Type[z + 1])) Nullified = false;   
                                if(Nullified == true) k[4] = "Null";
                                     	}
                           	if(Conditions[y].contains("Color")) {
                                String ColorParse = Conditions[y];                
                                String Color[] = ColorParse.split("/");
                                for(int z = 0; z < Color.length - 1; z++) if(!CardUtil.getColors(c).contains(Color[z + 1])) k[4] = "Null";     		
                                     	}
                            }
                    // Zone Condition
                		String Zones = k[3];
                        PlayerZone[] Required_Zones = new PlayerZone[1];
                        	if(Zones.equals("Hand")) Required_Zones[0] = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        	if(Zones.equals("Graveyard")) Required_Zones[0] = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                        	if(Zones.equals("Play") || Zones.equals("Any")) Required_Zones[0] = AllZone.getZone(Constant.Zone.Play, card.getController());
                        	if(Zones.contains("Library")) Required_Zones[0] = AllZone.getZone(Constant.Zone.Library, card.getController());
                        	if(Zones.contains("Exiled")) Required_Zones[0] = AllZone.getZone(Constant.Zone.Removed_From_Play, card.getController());
                        //	if(Zones.contains("Sideboard")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Sideboard, card.getController());
                        	final PlayerZone Required_Zone = Required_Zones[0]; 
                        	final String F_Zones = Zones;
                    // Special Conditions   
                        int Special_Conditions = 1;
                        String Special_ConditionsParse = k[8];                
                        String Special_Condition[] = Special_ConditionsParse.split("!");
                        Special_Conditions = Special_Condition.length;
                            for(int y = 0; y < Special_Conditions; y++) {
                            	
                  	if(Special_Condition[y].contains("Initiator - Other than Self")) {
                 		if(card.equals(c)) k[4] = "Null";      		
                 	}
                  	if(Special_Condition[y].contains("Initiator - OwnedByController")) {
                 		if(!c.getController().equals(card.getController())) k[4] = "Null";      		
                 	}
                  	if(Special_Condition[y].contains("Initiator - Has Keyword")) {
                  		boolean Nullified = true;
                        String KeywordParse = Special_Condition[y];                
                        String Keyword[] = KeywordParse.split("/");
                        for(int z = 0; z < Keyword.length - 1; z++) if((c.getKeyword()).contains(Keyword[z + 1])) Nullified = false;  
                        if(Nullified == true) k[4] = "Null";
                 	}
                  	if(Special_Condition[y].contains("ControllerUpkeep")) {
                        if(!AllZone.Phase.isPlayerTurn(card.getController())) k[4] = "Null";	
                 	}
                  	if(Special_Condition[y].contains("ControllerEndStep")) {
                        if(!AllZone.Phase.isPlayerTurn(card.getController())) k[4] = "Null";	
                 	}
                  	if(Special_Condition[y].contains("MoreCardsInHand")) {
                        if(((AllZone.getZone(Constant.Zone.Hand, card.getController())).getCards()).length 
                        		<= (AllZone.getZone(Constant.Zone.Hand, card.getController().getOpponent())).getCards().length) k[4] = "Null";
                 	}
                  	if(Special_Condition[y].contains("SearchType")) {
                    	for(int TypeRestrict = 0; TypeRestrict < (Special_Condition[y].split("/")).length - 1; TypeRestrict ++) {
                        	Custom_Strings[Custom_Strings_Count] = "Type" + (Special_Condition[y].split("/"))[TypeRestrict + 1];
                    	Custom_Strings_Count++;
                    	}
                 	}
                  	if(Special_Condition[y].contains("SearchColor")) {
                    	for(int ColorRestrict = 0; ColorRestrict < (Special_Condition[y].split("/")).length - 1; ColorRestrict ++) {
                        	Custom_Strings[Custom_Strings_Count] = "Color" + (Special_Condition[y].split("/"))[ColorRestrict + 1];
                    	Custom_Strings_Count++;
                    	}
                 	}
                  	if(Special_Condition[y].contains("Suspended")) {
                 		if(!card.hasSuspend()) k[4] = "Null";      		
                 	}  	
                    }
                            
                  	// Mana Cost (if Any)
            		String ManaCost = "0";
            		if(k[7].contains("PayMana")) {
                        String PayAmountParse = k[7];                
                        ManaCost = PayAmountParse.split("/")[1];
            		}
            		
                  	// Targets
            		           		
                    int Target_Conditions = 1;
                    String TargetParse = k[5];                
                    String Targets[] = TargetParse.split("!");
                    Target_Conditions = Targets.length;
                    Player TargetPlayer[] = new Player[Target_Conditions];
                    Card TargetCard[] = new Card[Target_Conditions];
                        for(int y = 0; y < Target_Conditions; y++) {       			
    				if(Targets[y].equals("ControllingPlayer_Self")) TargetPlayer[y] = card.getController();
    				if(Targets[y].equals("ControllingPlayer_Opponent")) TargetPlayer[y] = card.getController().getOpponent();
    				if(Targets[y].equals("ControllingPlayer_Initiator")) TargetPlayer[y] = F_TriggeringCard.getController();  				  				
    				if(Targets[y].equals("Self")) TargetCard[y] = F_card;
    				if(Targets[y].equals("Initiating_Card")) TargetCard[y] = c; 
                        }

                        final Player[] F_TargetPlayer = TargetPlayer;
                        final Card[] F_TargetCard = TargetCard;
                     //   JOptionPane.showMessageDialog(null, Targets, "", JOptionPane.INFORMATION_MESSAGE);
                        
    				// Effects
                        
                        int Effects = 1;
                        String EffectParse = k[4];                
                        String Effect[] = EffectParse.split("!");
                        Effects = Effect.length;
                        final Command[] Command_Effects = new Command[Effects];
                        final Command[] CommandExecute = new Command[1];
                        String StackDescription = F_card + " - ";                    
                        final int[] Effects_Count = new int[1];   
                        
                   		final Ability Ability = new Ability(card, ManaCost) {
                			@Override
                			public void resolve() {
                				for(int Commands = 0; Commands < Command_Effects.length; Commands++) Whenever_ManaPaid(F_card, F_k, Command_Effects[Commands], this);
                			}
                		};
                        
                   		final Spell Spell = new Spell(card) {
							private static final long serialVersionUID = -4909393989689642952L;

							@Override
                			public void resolve() {
                				for(int Commands = 0; Commands < Command_Effects.length; Commands++) Whenever_ManaPaid(F_card, F_k, Command_Effects[Commands], this);
                			}
                		};

                		SpellAbility[] SpellAbility = new SpellAbility[1];
                		if(k[1].equals("ActualSpell")) SpellAbility[0] = Spell;
                		else SpellAbility[0] = Ability;
                		
                		final SpellAbility F_SpellAbility = SpellAbility[0];
                		
                		if(k[7].contains("Choice_Instant") && k[4] != "Null") {
                	    	if(card.getController().equals(AllZone.HumanPlayer)) {
                	        	Object[] possibleValues = {"Yes", "No"};
                	        	Object q = JOptionPane.showOptionDialog(null, "Activate - " + card.getName(),card.getName() + " Ability", 
                	        			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                	        			null, possibleValues, possibleValues[0]);
                	              if(q.equals(1)) {
                	            	  Stop = true;
                	                }
                	    	}
                		}
                		if(Stop == false) { 
                		
                        for(int y = 0; y < Effects; y++) {  
                        	// Variables
                           String AmountParse = Effect[y];                          
                           String[] S_Amount = AmountParse.split("/");
                           int[] I_Amount = new int[S_Amount.length - 1];
                           int Multiple_Targets = 1;
                           
                           for(int b = 0; b < S_Amount.length - 1; b++) {
                           if(S_Amount[b+1].equals("Toughness")) I_Amount[b] = F_TriggeringCard.getNetDefense();
                           else if(S_Amount[b+1].equals("Power")) I_Amount[b] = F_TriggeringCard.getNetAttack();
                           else if(S_Amount[b+1].equals("Life_Gained")) I_Amount[b] = ((Integer)Custom_Parameters[0]);
                           else if(S_Amount[b+1].contains("ControlledAmountType")) {
                        	   final String[] TypeSplit =  AmountParse.split("/");
                        		CardList Cards_WithAllTypes = new CardList();
                        		Cards_WithAllTypes.add(new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards()));
                        		Cards_WithAllTypes = Cards_WithAllTypes.filter(new CardListFilter() {
                                     public boolean addCard(Card c) {
                                    	 for(int z = 0; z < TypeSplit.length - 1; z++)
                                         if(c.isType(TypeSplit[z + 1])) return true;
                                         return false;
                                     }
                                 });
                        		I_Amount[b] = Cards_WithAllTypes.size();
                           }
                           else if(!S_Amount[0].equals("KeywordPumpEOT")&& !S_Amount[1].contains("ControlledAmountType")) I_Amount[b] = Integer.valueOf(S_Amount[b+1]);
                           
                           // NOTE: Multiple Targets and Groups of Integers is not supported
                           
                           if(k[8].contains("MultipleTargets")) {
                           	Multiple_Targets = I_Amount[0];
                           	I_Amount[0] = 1;
                           }
                           } // For
                           // Input for Targets
                           
                           final int F_Multiple_Targets = Multiple_Targets; 
                       	   final Object[] Targets_Multi = new Object[Multiple_Targets];
                           final int[] index = new int[1];
                           final int[] F_Amount = I_Amount;
                           final String[] F_S_Amount = S_Amount;
                           final int F_Target = Effects_Count[0];

                		
                           final Command MultiTargetsCommand = new Command() {
                               private static final long serialVersionUID = -83034517601871955L;
                               
                               public void execute() {
                               	MultiTarget_Cancelled = false;
                                   for(int i = 0; i < F_Multiple_Targets; i++) {
                                  	 AllZone.InputControl.setInput(CardFactoryUtil.input_MultitargetCreatureOrPlayer(F_SpellAbility , i , F_Amount[0]*F_Multiple_Targets,new Command() {
                                  	
                                        private static final long serialVersionUID = -328305150127775L;
                                        
                                        public void execute() {
                                       	 Targets_Multi[index[0]] = F_SpellAbility.getTargetPlayer(); 
                                       	 if(Targets_Multi[index[0]] == null) Targets_Multi[index[0]] = F_SpellAbility.getTargetCard();
                                            index[0]++;  
                                            if(F_Multiple_Targets == 1) AllZone.Stack.updateObservers();
                                       	 }
                                    }));
                                   } 
                               	AllZone.Stack.add(F_SpellAbility);
                               }
                           };
                           
                           final Command InputCommand = new Command() {
                               private static final long serialVersionUID = -83034517601871955L;
                               
                               public void execute() {             						
                   		            String WhichInput = F_k[5].split("/")[1]; 
                   		            if(WhichInput.equals("Creature")) 
                   		            	if(F_card.getController().equals(AllZone.HumanPlayer))
                   		            		AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreature(F_SpellAbility, GetTargetsCommand));
                   		            	else {
                   		            		CardList PossibleTargets = new CardList();	
                   		            		PossibleTargets.addAll(AllZone.Human_Play.getCards());
                   		            		PossibleTargets.addAll(AllZone.Computer_Play.getCards());
                   		            		PossibleTargets = PossibleTargets.getType("Creature");
                   		            		if(Whenever_AI_GoodEffect(F_k)) {
                   		            			PossibleTargets = PossibleTargets.filter(new CardListFilter() {
                                                    public boolean addCard(Card c) {
                                                        if(c.getController().equals(AllZone.ComputerPlayer)) return true;
                                                        return false;
                                                    }
                                        		});
                   		            			if(PossibleTargets.size() > 0) {
                   		            			Targets_Multi[index[0]] = CardFactoryUtil.AI_getBestCreature(PossibleTargets,F_card);
                   		            			AllZone.Stack.add(F_SpellAbility);              		            			 
                   		            			}
                   		            			index[0]++;
                   		            		} else {
                   		            			PossibleTargets = PossibleTargets.filter(new CardListFilter() {
                                                    public boolean addCard(Card c) {
                                                        if(c.getController().equals(AllZone.HumanPlayer)) return true;
                                                        return false;
                                                    }
                                        		});
                   		            			if(PossibleTargets.size() > 0) {
                       		            		Targets_Multi[index[0]] = CardFactoryUtil.AI_getBestCreature(PossibleTargets,F_card);
                       		            		AllZone.Stack.add(F_SpellAbility);              		            			 
                       		            		}
                       		            		index[0]++; 
                   		            		}
                           		      		
                   		            	}
                   		            if(WhichInput.equals("Player")) 
                   		            	if(F_card.getController().equals(AllZone.HumanPlayer))
                   		            		AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(F_SpellAbility, GetTargetsCommand));
                   		            	else {
                   		            		if(Whenever_AI_GoodEffect(F_k)) {
                   		            			Targets_Multi[index[0]] = AllZone.ComputerPlayer;
                   		            			if(Targets_Multi[index[0]] != null) AllZone.Stack.add(F_SpellAbility);
                   		            			index[0]++; 
                   		            		}
                   		            		 else {
                   		            			 Targets_Multi[index[0]] = AllZone.HumanPlayer;
                   		            		     if(Targets_Multi[index[0]] != null) AllZone.Stack.add(F_SpellAbility);
                   		            			 index[0]++; 
                   		            		 }
                           		      		
                   		            	}
                   		            if(WhichInput.contains("Specific")) {
                   		      		CardList Cards_inPlay = new CardList();
                   		      		Cards_inPlay.addAll(AllZone.Human_Play.getCards());
                   		      		Cards_inPlay.addAll(AllZone.Computer_Play.getCards());
                   		      		final String[] Specific = F_k[5].split("/");
                   		      		final int[] Restriction_Count = new int[1]; 
                   		      		for(int i = 0; i < Specific.length - 2;i++) {
                   		      			if(Specific[i+2].contains("Type.") && !Specific[i+2].contains("NonType.")) {
                   		      			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                                                public boolean addCard(Card c) {
                                                    if(c.isType(Specific[Restriction_Count[0] + 2].replaceFirst("Type.", ""))) return true;
                                                    return false;
                                                }
                                    		});
                   		      			}
                   		      			if(Specific[i+2].contains("NonType.")) {
                       		      			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                                                    public boolean addCard(Card c) {
                                                        if(!c.isType(Specific[Restriction_Count[0] + 2].replaceFirst("NonType.", ""))) return true;
                                                        return false;
                                                    }
                                        		});
                       		      			}
                   		      			if(Specific[i+2].contains("Color.") && !Specific[i+2].contains("NonColor.")) {
                       		      			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                                                    public boolean addCard(Card c) {
                                                        if(CardUtil.getColors(c).contains(Specific[Restriction_Count[0] + 2].replaceFirst("Color.", ""))) return true;
                                                        return false;
                                                    }
                                        		});	
                       		      			}
                   		      			if(Specific[i+2].contains("NonColor.")) {
                       		      			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                                                    public boolean addCard(Card c) {
                                                        if(!CardUtil.getColors(c).contains(Specific[Restriction_Count[0] + 2].replaceFirst("NonColor.", ""))) return true;
                                                        return false;
                                                    }
                                        		});	
                       		      			}
                   		      			if(Specific[i+2].equals("NotSelf")) {
                       		      			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                                                    public boolean addCard(Card c) {
                                                        if(!c.equals(F_card)) return true;
                                                        return false;
                                                    }
                                        		});
                       		      			}
                   		      		Restriction_Count[0]++;
                   		      		}
                   		            	if(F_card.getController().equals(AllZone.HumanPlayer))
                   		            		AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(F_SpellAbility, Cards_inPlay, "Select a Valid Card", GetTargetsCommand, true, true));
                   		            	else {
                   		            		if(Whenever_AI_GoodEffect(F_k)) {
                   		            			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                                                    public boolean addCard(Card c) {
                                                        if(c.getController().equals(AllZone.ComputerPlayer)) return true;
                                                        return false;
                                                    }
                                        		});
                		            			if(Cards_inPlay.size() > 0) {
                           		            		Targets_Multi[index[0]] = CardFactoryUtil.AI_getBestCreature(Cards_inPlay,F_card);
                           		            		AllZone.Stack.add(F_SpellAbility);              		            			 
                           		            		}
                           		            		index[0]++; 
                   		            		} else {
                   		            			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                                                    public boolean addCard(Card c) {
                                                        if(c.getController().equals(AllZone.HumanPlayer)) return true;
                                                        return false;
                                                    }
                                        		});
                  		            			if(Cards_inPlay.size() > 0) {
                           		            		Targets_Multi[index[0]] = CardFactoryUtil.AI_getBestCreature(Cards_inPlay,F_card);
                           		            		AllZone.Stack.add(F_SpellAbility);              		            			 
                           		            		}
                           		            		index[0]++;                  		            			
                   		            		}
                           		      		
                   		            	}
                   		            }	
                   		            
                   		         AllZone.Stack.updateObservers();
                               }
                               
                   		   final Command GetTargetsCommand = new Command() {
                                  	
                                        private static final long serialVersionUID = -328305150127775L;
                                        
                                        public void execute() {
                                       	 Targets_Multi[index[0]] = F_SpellAbility.getTargetPlayer(); 
                                       	 if(Targets_Multi[index[0]] == null) Targets_Multi[index[0]] = F_SpellAbility.getTargetCard();
                                       	if(F_k[8].contains("AttachTarget")) F_card.attachCard((Card) Targets_Multi[index[0]]);
                                            index[0]++;  
                                            if(F_Multiple_Targets == 1) AllZone.Stack.updateObservers();                                       
                                      	 }
                           };
                               
                           };
                           
                           if(k[8].contains("MultipleTargets")) CommandExecute[0] = MultiTargetsCommand;
                           else if(k[8].contains("SingleTarget")) CommandExecute[0] = MultiTargetsCommand;
                           else if(k[5].contains("NormalInput")) CommandExecute[0] = InputCommand;
                           else {
                        	   if(F_TargetPlayer[y] != null) Targets_Multi[index[0]] = F_TargetPlayer[y];
                        	   if(F_TargetCard[y] != null) Targets_Multi[index[0]] = F_TargetCard[y];
                        	   index[0]++; 
                        	   CommandExecute[0] = Command.Blank;
                           }
                    
                           // Null
                   		if(Effect[y].equals("Null")) {
                        Command_Effects[F_Target] = Command.Blank;      
                   		}
                   		
                		// +1 +1 Counters
                		if(Effect[y].contains("+1+1 Counters")) {

                			Command Proper_resolve = new Command() {
                            	private static final long serialVersionUID = 151367344511590317L;

                    			public void execute() {
                    				if(Whenever_Go(F_card,F_k) == true) {
                    					CardList All = Check_if_All_Targets(F_card, F_k);
                    					if(All.size() > 0) {
                    						for(int i = 0; i < All.size(); i++) {
                            					if(AllZone.GameAction.isCardInZone(All.get(i),Required_Zone) || F_Zones.equals("Any")) 
                            						All.get(i).addCounter(Counters.P1P1, F_Amount[0]);	
                    						}
                    					}
                    					if(AllZone.GameAction.isCardInZone(F_TargetCard[F_Target],Required_Zone) || F_Zones.equals("Any")) 
                    						F_TargetCard[F_Target].addCounter(Counters.P1P1, F_Amount[0]);
                    				}
                    				};
                		};
                        Command_Effects[F_Target] = Proper_resolve;      
                        if(Check_if_All_Targets(F_card, F_k).size() > 0) StackDescription = StackDescription + "all specified permanents get" + F_Amount[0] + " +1/+1 counters";
                        else StackDescription = StackDescription +  F_TargetCard[y] + " gets " + F_Amount[0] + " +1/+1 counters";
                	}
                		
                		// CustomCounters.(What Counter)/Amount
                		if(Effect[y].contains("CustomCounter")) {

                			Command Proper_resolve = new Command() {
                            	private static final long serialVersionUID = 151367344511590317L;

                    			public void execute() {
                    				if(Whenever_Go(F_card,F_k) == true) {
                    					String PossibleCounter = (F_k[4].split("/")[0]).replaceFirst("CustomCounter.", "");
                    					Counters Counter = null;
                    					for(int i = 0; i < Counters.values().length ; i++) {
                    						if(Counters.values()[i].toString().equals(PossibleCounter)) 
                    							Counter = Counters.values()[i];
                    					}
                    					CardList All = Check_if_All_Targets(F_card, F_k);
                    					if(All.size() > 0) {
                    						for(int i = 0; i < All.size(); i++) {
                            					if(AllZone.GameAction.isCardInZone(All.get(i),Required_Zone) || F_Zones.equals("Any")) 
                            						All.get(i).addCounter(Counter, F_Amount[0]);	
                    						}
                    					}
                    					if(AllZone.GameAction.isCardInZone(F_TargetCard[F_Target],Required_Zone) || F_Zones.equals("Any")) 
                    						F_TargetCard[F_Target].addCounter(Counter, F_Amount[0]);
                    				}
                    				};
                		};
                        Command_Effects[F_Target] = Proper_resolve;      
                        if(Check_if_All_Targets(F_card, F_k).size() > 0) StackDescription = StackDescription + "all specified permanents get" + F_Amount[0] + " +1/+1 counters";
                        else StackDescription = StackDescription +  F_TargetCard[y] + " gets " + F_Amount[0] + 
                        " " + (F_k[4].split("/")[0]).replaceFirst("CustomCounter.", "") + " "+((F_Amount[0]>1)?"counters":"counter");
                	}
                		
                		// StatsPumpEOT/Power/Toughness
                		if(Effect[y].contains("StatsPumpEOT")) {

                			Command Proper_resolve = new Command() {
                            	private static final long serialVersionUID = 151367344511590317L;

                    			public void execute() {
                                    final Command untilEOT = new Command() {
                                        private static final long serialVersionUID = 1497565871061029469L;
                                        
                                        public void execute() {                                       	
                                            if(AllZone.GameAction.isCardInPlay(F_card)) {
                                            	F_TargetCard[F_Target].addTempAttackBoost(- F_Amount[0]);
                                            	F_TargetCard[F_Target].addTempDefenseBoost(- F_Amount[1]);
                                            }
                                        }
                                        }; //Command
                                        
                    				if(Whenever_Go(F_card,F_k) == true) {
                    					CardList All = Check_if_All_Targets(F_card, F_k);
                    					if(All.size() > 0) {
                    						for(int i = 0; i < All.size(); i++) { 
                                                F_TargetCard[F_Target].addTempAttackBoost(F_Amount[0]);
                                                F_TargetCard[F_Target].addTempDefenseBoost(F_Amount[1]);
                                                AllZone.EndOfTurn.addUntil(untilEOT);	
                    						}
                    					}
                    					else if(AllZone.GameAction.isCardInZone(F_TargetCard[F_Target],Required_Zone) || F_Zones.equals("Any")) {                                       
                                        F_TargetCard[F_Target].addTempAttackBoost(F_Amount[0]);
                                        F_TargetCard[F_Target].addTempDefenseBoost(F_Amount[1]);
                                        AllZone.EndOfTurn.addUntil(untilEOT);
                    					}
                    				}
                			};
                		};
                        Command_Effects[F_Target] = Proper_resolve;      
                        if(Check_if_All_Targets(F_card, F_k).size() > 0) StackDescription = StackDescription + "all specified permanents get" + ((F_Amount[0] > -1)? "+" :"") + F_Amount[0] 
                        + "/" + ((F_Amount[1] > -1)? "+" :"") + F_Amount[1]  + " until End of Turn";
                        else StackDescription = StackDescription +  F_TargetCard[y] + " gets " + ((F_Amount[0] > -1)? "+" :"") + F_Amount[0] 
                                           + "/" + ((F_Amount[1] > -1)? "+" :"") + F_Amount[1]  + " until End of Turn";
                	}
                	
                		// KeywordPumpEOT/Keyword(s)
                		if(Effect[y].contains("KeywordPumpEOT")) {

                			Command Proper_resolve = new Command() {
                            	private static final long serialVersionUID = 151367344511590317L;

                    			public void execute() {
            						final Command untilEOT = new Command() {
                                        private static final long serialVersionUID = 1497565871061029469L;
                                        
                                        public void execute() {
                                            if(AllZone.GameAction.isCardInPlay(F_card)) {
                                                for(int i =0; i < F_S_Amount.length - 1; i++) {
                                                    F_card.removeIntrinsicKeyword(F_S_Amount[i + 1]);
                                                    }
                                            }
                                        }
                                    };//Command
                    				if(Whenever_Go(F_card,F_k) == true) {
                    					CardList All = Check_if_All_Targets(F_card, F_k);
                    					if(All.size() > 0) {
                    						for(int i = 0; i < All.size(); i++) { 
                                                for(int i2 =0; i2 < F_S_Amount.length - 1; i2++) {
                                                    F_card.addIntrinsicKeyword(F_S_Amount[i2 + 1]);
                                                    }
                                                    AllZone.EndOfTurn.addUntil(untilEOT);	
                    						}
                    					}
                    					else if(AllZone.GameAction.isCardInZone(F_TargetCard[F_Target],Required_Zone) || F_Zones.equals("Any")) {                                       
                                        for(int i =0; i < F_S_Amount.length - 1; i++) {
                                        F_card.addIntrinsicKeyword(F_S_Amount[i + 1]);
                                        }
                                        AllZone.EndOfTurn.addUntil(untilEOT);
                    				}
                    					}
                			};
                		};
                		String Desc = "";
                		for(int KW =0; KW < F_S_Amount.length - 1; KW++) {
                		Desc = Desc + F_S_Amount[KW + 1];
                		if(KW < F_S_Amount.length - 2) Desc = Desc + ", ";
                		}
                        Command_Effects[F_Target] = Proper_resolve;      
                        if(Check_if_All_Targets(F_card, F_k).size() > 0) StackDescription = StackDescription + "all specified permanents get" + Desc + " until End of Turn";
                        else StackDescription = StackDescription +  F_TargetCard[y] + " gets " + Desc + " until End of Turn";
                	}
                		
                		// ModifyLife/Amount
                		if(Effect[y].contains("ModifyLife")) {
                			Command Proper_resolve = new Command() {
                            	private static final long serialVersionUID = 151367344511590317L;

                    			public void execute() {
                    				if(Whenever_Go(F_card,F_k) == true) 
                    					if(AllZone.GameAction.isCardInZone(F_card,Required_Zone) || F_Zones.equals("Any")) {
    			          				//PlayerLife life = AllZone.GameAction.getPlayerLife(F_TargetPlayer[F_Target]);
    			        				if(F_Amount[0] > -1) 
    			        					F_TargetPlayer[F_Target].gainLife(F_Amount[0], F_card);
    			        				else 
    			        					F_TargetPlayer[F_Target].loseLife(F_Amount[0] * -1,F_card);
    			                      }

			                      }
                			};
                            Command_Effects[F_Target] = Proper_resolve;      
                            StackDescription = StackDescription +  F_TargetPlayer[F_Target] + ((F_Amount[0] > -1)? " gains " + F_Amount[0]:"") + ((F_Amount[0] <= -1)? " loses " + F_Amount[0] * -1:"") + " life";
                		}
                		
                		// Destroy
                		if(Effect[y].contains("Destroy")) {

                			Command Proper_resolve = new Command() {
                            	private static final long serialVersionUID = 151367344511590317L;

                    			public void execute() {
                        				if(Whenever_Go(F_card,F_k) == true) {
                        					CardList All = Check_if_All_Targets(F_card, F_k);
                        					if(All.size() > 0) {
                        						for(int i = 0; i < All.size(); i++) { 
                                						destroy(All.get(i));	
                        						}
                        					} else if(AllZone.GameAction.isCardInZone(F_card,Required_Zone) || F_Zones.equals("Any")) {
                        						for(int z = 0; z < Targets_Multi.length; z++) {
                        							if(AllZone.GameAction.isCardInPlay((Card) Targets_Multi[z])
    			                                      && CardFactoryUtil.canTarget(F_card, (Card) Targets_Multi[z])) {
                        								Card c = (Card) Targets_Multi[z];
                        								destroy(c);
    			                              }
    			                              }
    			                      }
                    			}
                			};
                		};
                        Command_Effects[F_Target] = Proper_resolve;      
                        if(Check_if_All_Targets(F_card, F_k).size() > 0) StackDescription = StackDescription + " destroys all specified permanents";
                        else StackDescription = StackDescription + " destroys " + (((Card) Targets_Multi[y] != null)? (Card) Targets_Multi[y]:"");
                	}
                		
                		// TapPermanent
                		if(Effect[y].contains("TapPermanent")) {

                			Command Proper_resolve = new Command() {
                            	private static final long serialVersionUID = 151367344511590317L;

                    			public void execute() {
                    				if(Whenever_Go(F_card,F_k) == true) {
                    					CardList All = Check_if_All_Targets(F_card, F_k);
                    					if(All.size() > 0) {
                    						for(int i = 0; i < All.size(); i++) { 
                    							All.get(i).tap();	
                    						}
                    					}
                    					else if(AllZone.GameAction.isCardInZone(F_card,Required_Zone) || F_Zones.equals("Any")) {
    			                    	  for(int z = 0; z < Targets_Multi.length; z++) {
    			                              if(AllZone.GameAction.isCardInPlay((Card) Targets_Multi[z])
    			                                      && CardFactoryUtil.canTarget(F_card, (Card) Targets_Multi[z])) {
    			                                  Card c = (Card) Targets_Multi[z];
    			                                  c.tap();
    			                              }
    			                      }
        	                    		}
                    			}
                			};
                		};
                        Command_Effects[F_Target] = Proper_resolve;      
                        if(Check_if_All_Targets(F_card, F_k).size() > 0) StackDescription = StackDescription + " taps all specified permanents";
                        else StackDescription = StackDescription + " taps " + (((Card) Targets_Multi[y] != null)? (Card) Targets_Multi[y]:"");
                	}
                	
                		// UntapPermanent
                		if(Effect[y].contains("UntapPermanent")) {

                			Command Proper_resolve = new Command() {
                            	private static final long serialVersionUID = 151367344511590317L;

                    			public void execute() {
                    				if(Whenever_Go(F_card,F_k) == true) {
                    					CardList All = Check_if_All_Targets(F_card, F_k);
                    					if(All.size() > 0) {
                    						for(int i = 0; i < All.size(); i++) { 
                    							All.get(i).untap();	
                    						}
                    					}
                    					else if(AllZone.GameAction.isCardInZone(F_card,Required_Zone) || F_Zones.equals("Any")) {
    			                    	  for(int z = 0; z < Targets_Multi.length; z++) {
    			                              if(AllZone.GameAction.isCardInPlay((Card) Targets_Multi[z])
    			                                      && CardFactoryUtil.canTarget(F_card, (Card) Targets_Multi[z])) {
    			                                  Card c = (Card) Targets_Multi[z];
    			                                  c.untap();
    			                              }
    			                    	  }
    			                      }
                    			}
                			};
                		};
                        Command_Effects[F_Target] = Proper_resolve;      
                        if(Check_if_All_Targets(F_card, F_k).size() > 0) StackDescription = StackDescription + " untaps all specified permanents";
                        else StackDescription = StackDescription + " untaps " + (((Card) Targets_Multi[y] != null)? (Card) Targets_Multi[y]:"");
                	}
                		
                		
                		// Draw Cards
                		if(Effect[y].contains("DrawCards")) {
                    			Command Proper_resolve = new Command() {
                                	private static final long serialVersionUID = 151367344511590317L;

                        			public void execute() {
                        				if(Whenever_Go(F_card,F_k) == true) 
                        					if(AllZone.GameAction.isCardInZone(F_card,Required_Zone) || F_Zones.equals("Any")) {
                        						F_TargetPlayer[F_Target].drawCard();
      			                      }

    			                      }
                    		};
                            Command_Effects[F_Target] = Proper_resolve;      
                            StackDescription = StackDescription +  F_TargetPlayer[F_Target] + " draws " + F_Amount[0] + " card(s)";                     
                		}

                		
                		// Discard Cards
                		if(Effect[y].contains("DiscardCards")) {
                    			Command Proper_resolve = new Command() {
                                	private static final long serialVersionUID = 151367344511590317L;

                        			public void execute() {
                        				if(Whenever_Go(F_card,F_k) == true) 
                        					if(AllZone.GameAction.isCardInZone(F_card,Required_Zone) || F_Zones.equals("Any")) {
                        						//this might not work:
                        						//AllZone.GameAction.discard(F_TargetPlayer[F_Target],F_Amount[0], Ability);
                        						F_TargetPlayer[F_Target].discard(F_Amount[0], Ability);
      			                      }

    			                      }
                    			};
                            Command_Effects[F_Target] = Proper_resolve;      
                            StackDescription = StackDescription + F_TargetPlayer[F_Target] + " discards " + F_Amount[0] + " card(s)";                        
                		}
                		
                		// Make Token-Type-color-Power-Toughness-Keywords---Amount
                		if(Effect[y].contains("MakeToken")) {
                            String[] TokenConditions = AmountParse.split("-");
                            
                            String[] KeyWordConditions = new String[TokenConditions.length - 6];
                            for(int z = 5; z < TokenConditions.length - 1; z++) 
                            	if(!TokenConditions[z - 5].equals("None")) KeyWordConditions[z - 5] = TokenConditions[z];
                            final String[] F_TokenConditions = TokenConditions;
                            final String[] F_KeyWordConditions = KeyWordConditions;
                            
        					String Color = F_TokenConditions[2];
        					if(F_TokenConditions[2].equals("c")) Color = "Colorless";
        					else if(F_TokenConditions[2].equals("W")) Color = "White";
        					else if(F_TokenConditions[2].equals("U")) Color = "Blue";
        					else if(F_TokenConditions[2].equals("G")) Color = "Green";
        					else if(F_TokenConditions[2].equals("R")) Color = "Red";
        					else if(F_TokenConditions[2].equals("B")) Color = "Black";
        					else Color = "Multicolored";
        					final String F_Color = Color;
                    			Command Proper_resolve = new Command() {
                                	private static final long serialVersionUID = 151367344511590317L;

                        			public void execute() {
                        				if(Whenever_Go(F_card,F_k) == true) 
                        					if(AllZone.GameAction.isCardInZone(F_card,Required_Zone) || F_Zones.equals("Any")) {
                        					String[] types = F_TokenConditions[1].split(" ");
                        					String[] creatTypes = new String[types.length+1];
                        					creatTypes[0] = "Creature";
                        					for (int i=0;i<types.length;i++)
                        						creatTypes[i+1]=types[i];

                        					String Color = F_TokenConditions[2];
                        					if(F_TokenConditions[2].equals("c")) Color = "1";
                        					for(int z = 0; z < F_Amount[0]; z++) 
                        						
                        					// CardFactoryUtil.makeToken( F_TokenConditions[1] + " Token", F_TokenConditions[2]+ " " + 
                        						
                                            CardFactoryUtil.makeToken( F_TokenConditions[1], F_TokenConditions[2]+ " " + 
                                            		Integer.valueOf(F_TokenConditions[3])+ " " + Integer.valueOf(F_TokenConditions[4])
                                             + " " + F_TokenConditions[1], F_card.getController(), Color, creatTypes, Integer.valueOf(F_TokenConditions[3]), 
                                             Integer.valueOf(F_TokenConditions[4]), F_KeyWordConditions);
      			                      }

    			                      }
                    			};
                            Command_Effects[F_Target] = Proper_resolve;      
                            StackDescription = StackDescription + F_TargetPlayer[F_Target] + " puts " + F_Amount[0] + " " +  F_TokenConditions[3] + 
                            "/" + F_TokenConditions[4] + " " + F_Color + " " + F_TokenConditions[1] + " creature token(s) onto the battlefield";                        
                		}
                		
                		// Copy Spell
                		if(Effect[y].contains("CopySpell")) {
                    			Command Proper_resolve = new Command() {
                                	private static final long serialVersionUID = 151367344511590317L;

                        			public void execute() {
                        				if(Whenever_Go(F_card,F_k) == true) 
                        					if(AllZone.GameAction.isCardInZone(F_card,Required_Zone) || F_Zones.equals("Any")) {
                        					AllZone.CardFactory.copySpellontoStack(F_card,F_TargetCard[F_Target], true);
                                    };

    			                      }
                    			};
                            Command_Effects[F_Target] = Proper_resolve;      
                            StackDescription = StackDescription + F_card.getController() + " copies " + F_TargetCard[y];                     
                		}
                		
                		// MoveFrom/From Zone1/to Zone2
                		if(Effect[y].contains("MoveFrom")) {
                            String[] ZoneConditions = AmountParse.split("-");
                            PlayerZone[] PZones = new PlayerZone[ZoneConditions.length];
                            for(int z = 0; z < ZoneConditions.length; z++) {
                             if(ZoneConditions[z].equals("Hand")) PZones[z] = AllZone.getZone(Constant.Zone.Hand, card.getController());
                             if(ZoneConditions[z].equals("Graveyard")) PZones[z] = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                             if(ZoneConditions[z].equals("Play")) PZones[z] = AllZone.getZone(Constant.Zone.Play, card.getController());
                             if(ZoneConditions[z].contains("Library")) PZones[z] = AllZone.getZone(Constant.Zone.Library, card.getController());
                             if(ZoneConditions[z].contains("Exiled")) PZones[z] = AllZone.getZone(Constant.Zone.Removed_From_Play, card.getController());
                          // if(ZoneConditions[z].contains("Sideboard")) PZones[z] = AllZone.getZone(Constant.Zone.Sideboard, card.getController());
                            }
                    			Command Proper_resolve = new Command() {
                                	private static final long serialVersionUID = 151367344511590317L;

                        			public void execute() {
                        				if(Whenever_Go(F_card,F_k) == true) {
                        					CardList All = Check_if_All_Targets(F_card, F_k);
                        					if(All.size() > 0) {
                        						for(int i = 0; i < All.size(); i++) { 
              			                    	  AllZone.GameAction.moveTo(Whenever_GetMoveToZone(All.get(i), F_k)[1], All.get(i));
              			                    	  checkStateEffects(); // For Legendaries	
                        						}
                        					}
                        					else {
                            				Card NewSearch[] = Search(F_card,F_TriggeringCard, F_k,Custom_Strings);
                            				if(NewSearch[0] != null) {
                            				for(int i = 0; i < NewSearch.length; i++) {
            			                    	  AllZone.GameAction.moveTo(Whenever_GetMoveToZone(NewSearch[i], F_k)[1], NewSearch[i]);
              			                    	  checkStateEffects(); // For Legendaries	
                            				}
                            				} else {
                                				if(F_TargetCard[F_Target] == null) {
                                					for(int z = 0; z < Targets_Multi.length; z++) { 
                                						F_TargetCard[F_Target] = (Card) Targets_Multi[z];
                                    					if(AllZone.GameAction.isCardInZone(F_TargetCard[F_Target],Whenever_GetMoveToZone(F_TargetCard[F_Target], F_k)[0])) {
                        			                    	  AllZone.GameAction.moveTo(Whenever_GetMoveToZone(F_TargetCard[F_Target], F_k)[1], F_TargetCard[F_Target]);
                        			                    	  checkStateEffects(); // For Legendaries
                                          					} 
                                				}
                                				} else if(AllZone.GameAction.isCardInZone(F_TargetCard[F_Target],Whenever_GetMoveToZone(F_TargetCard[F_Target], F_k)[0])) {
                                						AllZone.GameAction.moveTo(Whenever_GetMoveToZone(F_TargetCard[F_Target], F_k)[1], F_TargetCard[F_Target]);
                                						checkStateEffects(); // For Legendaries
                                					}
                        					} 
                        					}
                        				}
                        			}
                    			};
                            Command_Effects[F_Target] = Proper_resolve;      
                            if(Check_if_All_Targets(F_card, F_k).size() > 0) StackDescription = StackDescription  + " moves to all specified permanents from " + ZoneConditions[1] + " to " + ZoneConditions[2] + " zone";
                            else if(F_TargetCard[y] != null) StackDescription = StackDescription + F_TargetCard[y] + " moves from  " + ZoneConditions[1] + " to " + ZoneConditions[2] + " zone";                        
                            else {
                            	String[] SD = Search_Description(F_TriggeringCard ,k, Custom_Strings);
                            	StackDescription = StackDescription + F_card.getController() + " searches his/her " + SD[0] + " for a " + SD[1] + "card and moves it to the " + ZoneConditions[2] 
                            	                   + " zone. If that player searches a library this way, shuffle it";
                            }
                		}
                		
                		// Deal Damage
                		if(Effect[y].contains("Damage")) {
                    			Command Proper_resolve = new Command() {
                                	private static final long serialVersionUID = 151367344511590317L;

                        			public void execute() {
                        				if(Whenever_Go(F_card,F_k) == true) {
                        					CardList All = Check_if_All_Targets(F_card, F_k);
                        					if(All.size() > 0) {
                        						for(int i = 0; i < All.size(); i++) { 
                        							All.get(i).addDamage(F_Amount[0], F_card);	
                        						}
                        					}
                        					else if(AllZone.GameAction.isCardInZone(F_card,Required_Zone) || F_Zones.equals("Any")) {
      			                    	  if(F_card.getController().equals(AllZone.HumanPlayer)) {
      			                    	  for(int z = 0; z < Targets_Multi.length; z++) {
      			                    		  if(!(Targets_Multi[z].equals(AllZone.HumanPlayer) || Targets_Multi[z].equals(AllZone.ComputerPlayer))) {
      			                    			  if(AllZone.GameAction.isCardInPlay((Card) Targets_Multi[z])
      			                    					  && CardFactoryUtil.canTarget(F_card, (Card) Targets_Multi[z])) {
      			                    				  Card c = (Card) Targets_Multi[z];
      			                    				  c.addDamage(F_Amount[0], F_card);
      			                    			  }
      			                    		  } 
      			                    		  else {
      			                    			  ((Player) Targets_Multi[z]).addDamage(F_Amount[0], F_card);
      			                    		  }
      			                      }
      			                      }
      			                    	  if(F_card.getController().equals(AllZone.ComputerPlayer)) AllZone.HumanPlayer.addDamage(F_Amount[0]*F_Multiple_Targets, F_card);
                      			}
                        				}
                                    };

    			                      };
                            Command_Effects[F_Target] = Proper_resolve; 
                            if(Check_if_All_Targets(F_card, F_k).size() > 0) StackDescription = StackDescription  + "deals " + F_Amount[0]*F_Multiple_Targets + " damage" + " to all specified permanents/players";
                            else if(F_Multiple_Targets != 1) StackDescription = StackDescription + "deals " + F_Amount[0]*F_Multiple_Targets + " damage" + " divided among up to " +  Multiple_Targets + " target creatures and/or players";
                            else if(F_card.getController().equals(AllZone.ComputerPlayer)) StackDescription = StackDescription + "targeting Human ";
                            else StackDescription = StackDescription + "targeting " + ((F_TargetCard[y] != null)? F_TargetCard[y]:"") + 
                            ((F_TargetPlayer[y] != null)? F_TargetPlayer[y]:"");
                		}
                		
                		Effects_Count[0]++;
                		if(Effects_Count[0] != Effects) StackDescription = StackDescription + " and ";
                		else StackDescription = StackDescription + ".";
                		 }  // For     
                        F_SpellAbility.setStackDescription(StackDescription);
                        for(int Check = 0; Check < Command_Effects.length; Check++)
                        	if(!Command_Effects[Check].equals(Command.Blank)) {
                        		Whenever_Input(F_card,F_k,CommandExecute[0],F_SpellAbility);
                        		break;
                        	}
                		}
                	}
 		        }	
 			}
		}
    
	PlayerZone[] Whenever_GetMoveToZone (Card Target, String[] Keyword_Details) {
		String Zones = Keyword_Details[4];
		String[] Zone = Zones.split("-");
        PlayerZone[] Required_Zone = new PlayerZone[2];
    	if(Zone[1].contains("Hand")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Hand, Target.getController());
    	if(Zone[1].contains("Graveyard")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Graveyard, Target.getController());
    	if(Zone[1].contains("Play") || Zones.equals("Any")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Play, Target.getController());
    	if(Zone[1].contains("Library")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Library, Target.getController());
    	if(Zone[1].contains("Exiled")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Removed_From_Play, Target.getController());
    //	if(Zone[1].contains("Sideboard")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Sideboard, Target.getController());
    	
        	if(Zone[2].contains("Hand")) Required_Zone[1] = AllZone.getZone(Constant.Zone.Hand, Target.getController());
        	if(Zone[2].contains("Graveyard")) Required_Zone[1] = AllZone.getZone(Constant.Zone.Graveyard, Target.getController());
        	if(Zone[2].contains("Play") || Zones.equals("Any")) Required_Zone[1] = AllZone.getZone(Constant.Zone.Play, Target.getController());
        	if(Zone[2].contains("Library")) Required_Zone[1] = AllZone.getZone(Constant.Zone.Library, Target.getController());
        	if(Zone[2].contains("Exiled")) Required_Zone[1] = AllZone.getZone(Constant.Zone.Removed_From_Play, Target.getController());
        //	if(Zone[2].contains("Sideboard")) Required_Zone[1] = AllZone.getZone(Constant.Zone.Sideboard, Target.getController());

		return Required_Zone;
		}
    
    CardList Check_if_All_Targets (final Card Triggering_Card, String[] Keyword_Details) {
    	CardList Cards_inPlay = new CardList();
        if(Keyword_Details[5].contains("All") && Keyword_Details[4] != "Null") {
     	   final String[] AllTargets = Keyword_Details[5].split("/"); 
	      		
	      		Cards_inPlay.addAll(AllZone.Human_Play.getCards());
	      		Cards_inPlay.addAll(AllZone.Computer_Play.getCards());
	      		for(int i2 = 0; i2 < AllTargets.length - 1;i2++) {
	      		final int[] Restriction_Count = new int[1]; 
	      			if(AllTargets[i2+1].contains("Type.")) {
	      			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                         public boolean addCard(Card c) {
                             if(c.isType(AllTargets[Restriction_Count[0] + 1].replaceFirst("Type.", ""))) return true;
                             return false;
                         }
             		});
	      			}
	      			if(AllTargets[i2+1].contains("Color.")) {
		      			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                             public boolean addCard(Card c) {
                                 if(CardUtil.getColors(c).contains(AllTargets[Restriction_Count[0] + 1].replaceFirst("Color.", ""))) return true;
                                 return false;
                             }
                 		});                  		      			
		      			}
	      			if(AllTargets[i2+1].contains("AttachedCards")) {
		      			Cards_inPlay.clear();
		      			Cards_inPlay.addAll(Triggering_Card.getAttachedCards());
		      			
		      			}
	      		Restriction_Count[0]++;
	      		}
	      		
	      		// All Special Conditions
      			if(Keyword_Details[8].contains("AllTargets - Except Self")) {
	      			Cards_inPlay = Cards_inPlay.filter(new CardListFilter() {
                         public boolean addCard(Card c) {
                             if(c != Triggering_Card) return true;
                             return false;
                         }
	      			});
      			}
      			if(Cards_inPlay.size() == 0) Cards_inPlay.add(AllZone.CardFactory.HumanNullCard);
        }
    	return Cards_inPlay;
    }
    
    boolean Whenever_AI_GoodEffect(String[] Keyword_Details) {
    	boolean Good = true;
    	// List all the bad / possibly bad effects here
    	// Movefrom is bad because in order for it to have a target, the target must be in play.
    	if(Keyword_Details[4].contains("TapPermanent") || Keyword_Details[4].contains("DiscardCards") 
    			|| Keyword_Details[4].contains("MoveFrom") || Keyword_Details[4].contains("Damage"))
    			Good = false;
    	// Situational Effects
    	else {
    		String EffectInQuestion = Keyword_Details[4].split("/")[1]; 	
    	if(Keyword_Details[4].contains("ModifyLife") && EffectInQuestion.contains("-")) Good = false;
    	if(Keyword_Details[4].contains("StatsPumpEOT") && EffectInQuestion.contains("-")) Good = false;
    	}
    	return Good;
    }
    
    Card[] Search (Card Source, Card Initiator ,String[] Keyword_Details, final String[] Custom_Strings) {
    	String SearchDescription = " ";
    	boolean SearchLib = true;
    	if(Keyword_Details[7].contains("Choice_Instant-SearchLibrary")) {
    		if(Source.getController().equals(AllZone.HumanPlayer)) {
    			Object[] possibleValues = {"Yes", "No"};
    			Object q = JOptionPane.showOptionDialog(null, "Search Libraries?",Source.getName() + " F_SpellAbility", 
    					JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
    					null, possibleValues, possibleValues[0]);
    			if(q.equals(1)) {
    				SearchLib = false;
    			}
    		}	
    	}
    	int Target_Conditions = 1;
    	String TargetParse = Keyword_Details[5];                
    	String Targets[] = TargetParse.split("!");
    	Target_Conditions = Targets.length;
    	Player Zone_Owner = Source.getController();
    	Card SearchedCard[] = new Card[Target_Conditions];
    	for(int y = 0; y < Target_Conditions; y++) {       			
    		if(Targets[y].contains("SearchShuffle") /** && Keyword_Details[4] != "Null" **/) {
    			if(Targets[y].contains("OSearchShuffle")) {
    				Zone_Owner = Source.getController().getOpponent();
    				SearchDescription = SearchDescription + "Opponent's ";
    			}
    			else Zone_Owner = Source.getController();
    			String SearchParse = Targets[y];  
    			String Search[] = SearchParse.split("/");
    			String[] SearchZone = new String[Search.length - 1];
    			PlayerZone[] PZones = new PlayerZone[SearchZone.length];    
    			CardList SearchBase = new CardList();
    			for(int z = 0; z < PZones.length; z++) {
    				SearchZone[z] = Search[z+1];
    				if(SearchZone[z].equals("Hand")) PZones[z] = AllZone.getZone(Constant.Zone.Hand, Zone_Owner);
    				if(SearchZone[z].equals("Graveyard")) PZones[z] = AllZone.getZone(Constant.Zone.Graveyard, Zone_Owner);
    				if(SearchZone[z].equals("Play")) PZones[z] = AllZone.getZone(Constant.Zone.Play, Zone_Owner);
    				if(SearchZone[z].contains("Library") && SearchLib) PZones[z] = AllZone.getZone(Constant.Zone.Library, Zone_Owner);
    				if(SearchZone[z].contains("Exiled")) PZones[z] = AllZone.getZone(Constant.Zone.Removed_From_Play, Zone_Owner);
    				// if(ZoneConditions[z].contains("Sideboard")) PZones[z] = AllZone.getZone(Constant.Zone.Sideboard, Zone_Owner);
    				if(PZones[z] != null) {
    					SearchBase.addAll(PZones[z].getCards());
    					SearchDescription = SearchDescription + SearchZone[z] + " ";
    				}
    				if(z + 2 < PZones.length && PZones[z] != null) SearchDescription = SearchDescription + ", ";
    				else if(z + 2 == PZones.length) SearchDescription = SearchDescription + "and ";
    			}

    			@SuppressWarnings("unused")
    			Object check2 = AllZone.Display.getChoiceOptional("View" + SearchDescription,
    					SearchBase.toArray());
    			if(Search[0].contains("SearchShuffle_SameName")) SearchBase = SearchBase.getName(Initiator.getName());
    			if(Search[0].contains("SearchShuffle_Type")) {
    				for(int TypeRestrict = 0; TypeRestrict < Custom_Strings.length; TypeRestrict ++) {
    					if(Custom_Strings[TypeRestrict].startsWith("Type")) SearchBase = SearchBase.getType(Custom_Strings[TypeRestrict].replaceFirst("Type", ""));
    					if(Custom_Strings[TypeRestrict].startsWith("Color")) {
    						final int Number = TypeRestrict;
    						SearchBase = SearchBase.filter(new CardListFilter() {
    							public boolean addCard(Card c) {
    								if(CardUtil.getColors(c).contains(Custom_Strings[Number].replaceFirst("Color", ""))) return true;
    								return false;
    							}
    						});
    					}
    				}
    			}
    			if(SearchBase.size() != 0) {
    				Object check = AllZone.Display.getChoiceOptional("Select a Suitable Card",
    						SearchBase.toArray());
    				if(check != null) {
    					SearchedCard[y] = (Card) check;
    					if(SearchLib) ((Card) check).getController().shuffle();
    				}                      
    			} else {
    				JOptionPane.showMessageDialog(null, "No suitable cards in" + SearchDescription, "", JOptionPane.INFORMATION_MESSAGE);
    				if(SearchLib && Targets[y].contains("OSearchShuffle")) Source.getController().getOpponent().shuffle();
    				else if(SearchLib) Source.getController().shuffle();
    			}
    		}
    	}
    	return SearchedCard;
    }
    
    String[] Search_Description(Card Initiator ,String[] Keyword_Details, final String[] Custom_Strings) {
    	String[] SD = new String[2];
        String SearchDescription = "";
        String SearchType = "";
        int Target_Conditions = 1;
        String TargetParse = Keyword_Details[5];                
        String Targets[] = TargetParse.split("!");
        Target_Conditions = Targets.length;
            for(int y = 0; y < Target_Conditions; y++) {       			
		if(Targets[y].contains("SearchShuffle")) {
			if(Targets[y].contains("OSearchShuffle")) {
				SearchDescription = SearchDescription + "Opponent's";
			}
            String SearchParse = Targets[y];  
            String Search[] = SearchParse.split("/");   
            
            for(int z = 0; z < Search.length - 1; z++) {
             SearchDescription = SearchDescription + Search[z+1];
             if(z + 3 < Search.length) SearchDescription = SearchDescription + ", ";
             else if(z + 2 != Search.length) SearchDescription = SearchDescription + " and/or ";
            }       
            if(Search[0].contains("SearchShuffle_SameName")) SearchType = Initiator.getName() + " ";
            if(Search[0].contains("SearchShuffle_Type")) {
            	for(int TypeRestrict = 0; TypeRestrict < Custom_Strings.length; TypeRestrict ++) {
            	if(Custom_Strings[TypeRestrict].startsWith("Color")) {
            		SearchType = SearchType + Custom_Strings[TypeRestrict].replaceFirst("Color", "");
            		if(TypeRestrict + 1 != Custom_Strings.length) SearchType = SearchType + " ";
            	}
            }
            	for(int TypeRestrict = 0; TypeRestrict < Custom_Strings.length; TypeRestrict ++) {
                	if(Custom_Strings[TypeRestrict].startsWith("Type")) {
                		SearchType = SearchType + Custom_Strings[TypeRestrict].replaceFirst("Type", "");
                		if(TypeRestrict + 1 != Custom_Strings.length) SearchType = SearchType + " ";
                	}
            	}
            }
            }
            }
            SD[0] = SearchDescription;
            SD[1] = SearchType;
            
            return SD; 
    }
    
    void Whenever_ManaPaid (Card Source, String[] Keyword_Details, final Command Proper_Resolve, SpellAbility ability) {
		String S_Amount = "0";
		if(!Keyword_Details[7].contains("No_Condition") && !Keyword_Details[7].equals("Yes_No") 
				&& !Keyword_Details[7].equals("Opponent_Yes_No")&& !Keyword_Details[7].contains("Choice_Instant")) {
		if(Keyword_Details[7].contains("PayMana")) {
            String PayAmountParse = Keyword_Details[7];                
            S_Amount = PayAmountParse.split("/")[1];
		
        if(Source.getController().equals(AllZone.HumanPlayer)) {
        	final Command paid = new Command() {
            	private static final long serialVersionUID = 151367344511590317L;

    			public void execute() {
    				Proper_Resolve.execute();
                }
        	};
        	StringBuilder sb = new StringBuilder();
        	sb.append("Activate ").append(Source.getName()).append("'s ability: \n");

        	GameActionUtil.payManaDuringAbilityResolve(sb.toString(), S_Amount, paid, Command.Blank);
        } else {
            if(ComputerUtil.canPayCost(S_Amount)) {
            	ComputerUtil.payManaCost(ability);
            	Proper_Resolve.execute();
        }
        }
		}
		if(Keyword_Details[7].contains("SacrificeType")) {
            String PayAmountParse = Keyword_Details[7];                
            S_Amount = PayAmountParse.split("/")[1];
            PlayerZone play = AllZone.getZone(Constant.Zone.Play, Source.getController());
            CardList choice = new CardList(play.getCards());
            choice = choice.getType(S_Amount);
        if(Source.getController().equals(AllZone.HumanPlayer)) {
            AllZone.InputControl.setInput(CardFactoryUtil.Wheneverinput_sacrifice(ability, choice, "Select a " + S_Amount +" to sacrifice.",Proper_Resolve));
        } /**else {
            if(choice.size() > 5) {
            	sacrifice(choice.get(0));
            	Proper_Resolve.execute();
        }
        } **/ 
		}
    		} else Proper_Resolve.execute();

    }
    
    
	boolean Whenever_Go (Card Source, String[] Keyword_Details) {
		boolean Go = true;
		String Zones = Keyword_Details[3];
        PlayerZone[] Required_Zone = new PlayerZone[1];
        	if(Zones.equals("Hand")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Hand, Source.getController());
        	if(Zones.equals("Graveyard")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Graveyard, Source.getController());
        	if(Zones.equals("Play") || Zones.equals("Any")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Play, Source.getController());
        	if(Zones.contains("Library")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Library, Source.getController());
        	if(Zones.contains("Exiled")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Removed_From_Play, Source.getController());
        //	if(Zones.contains("Sideboard")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Sideboard, Source.getController());
        	
    		if(AllZone.GameAction.isCardInZone(Source,Required_Zone[0]) || Zones.equals("Any")) {
		if(Keyword_Details[7].equals("Yes_No")) {
    	if(Source.getController().equals(AllZone.HumanPlayer)) {
        	Object[] possibleValues = {"Yes", "No"};
        	Object q = JOptionPane.showOptionDialog(null, "Activate - " + Source.getName(),Source.getName() + " Ability", 
        			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
        			null, possibleValues, possibleValues[0]);
              if(q.equals(1)) {
            	  Go = false;
                }
    	}
		}
		if(Keyword_Details[7].equals("Opponent_Yes_No")) {
	    	if(!Source.getController().equals(AllZone.HumanPlayer)) {
	        	Object[] possibleValues = {"Yes", "No"};
	        	Object q = JOptionPane.showOptionDialog(null, "Activate - " + Source.getName(),Source.getName() + " Ability", 
	        			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
	        			null, possibleValues, possibleValues[0]);
	              if(q.equals(1)) {
	            	  Go = false;
	                }
	    	}
			}
    		}

		return Go;
		}
	
	public void Whenever_Input(Card Source, String[] Keyword_Details, Command paidCommand, final SpellAbility ability) {
		if(!Keyword_Details[8].contains("ActualEffect")) {
		String Zones = Keyword_Details[3];
        PlayerZone[] Required_Zone = new PlayerZone[1];
        	if(Zones.equals("Hand")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Hand, Source.getController());
        	if(Zones.equals("Graveyard")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Graveyard, Source.getController());
        	if(Zones.equals("Play") || Zones.equals("Any")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Play, Source.getController());
        	if(Zones.contains("Library")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Library, Source.getController());
        	if(Zones.contains("Exiled")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Removed_From_Play, Source.getController());
        //	if(Zones.contains("Sideboard")) Required_Zone[0] = AllZone.getZone(Constant.Zone.Sideboard, Source.getController());
        	
    		if(AllZone.GameAction.isCardInZone(Source,Required_Zone[0]) || Zones.equals("Any")) {	
    			if(Keyword_Details[6].equals("ASAP")) {
    				if(Keyword_Details[5].equals("InputType - CreatureORPlayer") && Source.getController().equals(AllZone.HumanPlayer)) {
    					paidCommand.execute();
    				}
    				else if(Keyword_Details[5].equals("InputType - CreatureORPlayer") && Source.getController().equals(AllZone.ComputerPlayer)) 
        	AllZone.Stack.add(ability);
    				else if(Keyword_Details[5].contains("NormalInput")) {
    					paidCommand.execute();
    				}                 				
    				else AllZone.Stack.add(ability);
    			}
		}
	} else paidCommand.execute();
	}
    // Whenever Keyword
	
   public void sacrificeDestroy(Card c) {
        if(!isCardInPlay(c)) return;
        
        boolean persist = false;
        PlayerZone play = AllZone.getZone(c);
        
        if(c.getOwner().equals(AllZone.HumanPlayer) || c.getOwner().equals(AllZone.ComputerPlayer)) ;
        else throw new RuntimeException("GameAction : destroy() invalid card.getOwner() - " + c + " "
                + c.getOwner());
        
        
        if(c.getKeyword().contains("Persist") && c.getCounters(Counters.M1M1) == 0) persist = true;
        
        if(c.isEquipped()){	// when equipped creature goes to the grave here.
        	// Deathrender & Oathkeeper, Takeno's Daisho have similar triggers to Skullclamp
        	skullClamp_destroy(c);
        	c.unEquipAllCards();
        }
        //tokens don't go into the graveyard
        //TODO: must change this if any cards have effects that trigger "when creatures go to the graveyard"
        if(!c.isToken())
        //resets the card, untaps the card, removes anything "extra", resets attack and defense
        moveToGraveyard(c);
        else play.remove(c);

        c.destroy();
        
        
        //destroy card effects:
        PlayerZone comp = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
        PlayerZone hum = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
        PlayerZone grv_comp = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
        PlayerZone grv_hum = AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer);
        CardList list = new CardList();
        list.addAll(comp.getCards());
        list.addAll(hum.getCards());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                ArrayList<String> keywords = c.getKeyword();
                for(String kw:keywords) {
                    if(kw.startsWith("Whenever ") && kw.contains(" put into")
                            && kw.contains("graveyard from the battlefield,")) return true;
                }
                return false;
            }
        });
        CardList grv = new CardList();
        grv.addAll(grv_comp.getCards());
        grv.addAll(grv_hum.getCards());
        grv = grv.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if(c.getName().contains("Bridge from Below")) return true;
                return false;
            }
        });
        
        checkWheneverKeyword(c, "PermanentIntoGraveyard",null);
        for(int i = 0; i < list.size(); i++)
            GameActionUtil.executeDestroyCardEffects(list.get(i), c);
        for(int i = 0; i < grv.size(); i++)
            GameActionUtil.executeGrvDestroyCardEffects(grv.get(i), c);
        
        if(persist) {
            /*
        	c.setDamage(0);
            c.untap();
            PlayerZone ownerPlay = AllZone.getZone(Constant.Zone.Play, c.getOwner());
            PlayerZone grave = AllZone.getZone(c);
            
            if(c.isEquipped()) c.unEquipAllCards();
            
            grave.remove(c);
            ownerPlay.add(c);
            
            c.setTempAttackBoost(0);
            c.setTempDefenseBoost(0);
            c.setExaltedBonus(false);
            //reset more stuff ?
            
            for(Counters counter:Counters.values())
                if(c.getCounters(counter) != 0) c.setCounter(counter, 0);
            
            c.addCounter(Counters.M1M1, 1);
            */
        	
        	PlayerZone ownerPlay = AllZone.getZone(Constant.Zone.Play, c.getOwner());
            PlayerZone grave = AllZone.getZone(c);
            grave.remove(c);
        	
        	Card crd = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
        	ownerPlay.add(crd);
        	
        	crd.addCounter(Counters.M1M1, 1);
        }
        
        //if (c.getName().equals("Rancor") || c.getName().equals("Brilliant Halo") || c.getName().equals("Undying Rage"))
        if(c.getKeyword().contains(
                "When CARDNAME is put into a graveyard from the battlefield, return CARDNAME to its owner's hand.")) {
            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
            moveTo(hand, c);
        }

        else if(c.getName().equals("Nissa's Chosen")) {
            PlayerZone library = AllZone.getZone(Constant.Zone.Library, c.getOwner());
            moveTo(library, c);
        }

        else if(c.getName().equals("Guan Yu, Sainted Warrior")) {
            PlayerZone library = AllZone.getZone(Constant.Zone.Library, c.getOwner());
            moveTo(library, c);
            c.getOwner().shuffle();
        }
    }//sacrificeDestroy()
    
    public boolean destroy(Card c) {
        if(!AllZone.GameAction.isCardInPlay(c)
                || (c.getKeyword().contains("Indestructible") && (!c.isCreature() || c.getNetDefense() > 0))) return false;        
        
        if(c.getShield() > 0) {
            c.subtractShield();
            c.setDamage(0);
            c.tap();
            return false;
        }
        
        if (c.isEnchanted())
        {
        	CardList list = new CardList(c.getEnchantedBy().toArray());
        	list = list.filter(new CardListFilter()
        	{
        		public boolean addCard(Card crd)
        		{
        			return crd.getKeyword().contains("Totem armor");
        		}
        	});
        	CardListUtil.sortCMC(list);
        	
        	
        	
        	if (list.size() != 0)
        	{
        		final Card crd;
	        	if (list.size() == 1)
	        	{
	        		crd = list.get(0);
	        	}
	        	else {
	        		if (c.getController().isHuman())
	        			crd = AllZone.Display.getChoiceOptional("Select totem armor to destroy", list.toArray());
	        		else 
	        			crd = list.get(0);
	        	}
	        	
	        	final Card card = c;
	        	Ability_Static ability = new Ability_Static(crd, "0")
	        	{
	        		public void resolve()
	        		{
	        			destroy(crd);
	    	        	card.setDamage(0);
	    	        	
	        		}
	        	};
	        	
	        	StringBuilder sb = new StringBuilder();
	        	sb.append(crd).append(" - Totem armor: destroy this aura.");
	        	ability.setStackDescription(sb.toString());
	        	
	        	AllZone.Stack.add(ability);
	        	return false;
        	}
        }//totem armor
        
        //System.out.println("Card " + c.getName() + " is getting sent to GY, and this turn it got damaged by: ");
        for(Card crd:c.getReceivedDamageFromThisTurn().keySet()) {
            if(c.getReceivedDamageFromThisTurn().get(crd) > 0) {
                //System.out.println(crd.getName() );
                GameActionUtil.executeVampiricEffects(crd);
            }
        }
        
        this.sacrificeDestroy(c);
        return true;
    }
    
    /**
     * exile a card
     * @param c the card to be exiled
     */
    public void exile(Card c) {
    	//removeFromGame(c);
    	if(AllZone.GameAction.isCardExiled(c)) return;

    	PlayerZone zone = AllZone.getZone(c); //could be hand, grave, play, ...
    	PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());

    	if (zone != null)	// for suspend
    		zone.remove(c);
    	if(!c.isToken()) removed.add(c);
    }
    
    public void removeUnearth(Card c)
    {
    	PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
    	removed.add(c);
    }
    
    //is this card a permanent that is in play?
    public boolean isCardInPlay(Card c) {
        return PlayerZoneUtil.isCardInZone(AllZone.Computer_Play, c)
                || PlayerZoneUtil.isCardInZone(AllZone.Human_Play, c);
    }
    
    public boolean isCardInGrave(Card c) {
        return PlayerZoneUtil.isCardInZone(AllZone.Computer_Graveyard, c)
                || PlayerZoneUtil.isCardInZone(AllZone.Human_Graveyard, c);
    }
    
    public boolean isCardExiled(Card c) {
        return PlayerZoneUtil.isCardInZone(AllZone.Computer_Removed, c)
                || PlayerZoneUtil.isCardInZone(AllZone.Human_Removed, c);
    }
    
    /**
     * prompts Human to see if a target player's library should be shuffled.  This should
     * only be called when the choice is made by the Human (target can be either), then
     * shuffles that player's library if appropriate
     * 
     * @param player the player's library we want to shuffle
     */
    public void promptForShuffle(final Player player) {
    	String[] choices = new String[] {"Yes", "No"};
		Object o = AllZone.Display.getChoice("Shuffle "+player+"'s library?", choices);
		String myChoice = (String) o;
		if(myChoice.equals("Yes")) {
			player.shuffle();
		}
    }
    
    public boolean isCardInZone(Card card, PlayerZone p) {
        ArrayList<Card> list = new ArrayList<Card>(Arrays.asList(p.getCards()));
        return list.contains(card);
    }
    
    //removes all damage from player's creatures
    public void removeDamage(Player player) {
        PlayerZone p = AllZone.getZone(Constant.Zone.Play, player);
        ArrayList<Card> a = PlayerZoneUtil.getCardType(p, "Creature");
        Card c[] = CardUtil.toCard(a);
        for(int i = 0; i < c.length; i++)
            c[i].setDamage(0);
    }
    
    //for Quest fantasy mode
    public void newGame(Deck humanDeck, Deck computerDeck, CardList human, CardList computer, int humanLife, int computerLife, Quest_Assignment qa)
    {
    	this.newGame(humanDeck, computerDeck);
    	
    	AllZone.ComputerPlayer.setLife(computerLife, null);
        AllZone.HumanPlayer.setLife(humanLife, null);
        
        if (qa != null){
        	computer.addAll(QuestUtil.getComputerCreatures(AllZone.QuestData, AllZone.QuestAssignment).toArray());
        }

        for (Card c : human)
        {
        	AllZone.Human_Play.add(c);
        	c.setSickness(true);
        }
         
        for (Card c: computer)
        {
        	AllZone.Computer_Play.add(c);
        	c.setSickness(true);
        }
        Constant.Quest.fantasyQuest[0] = true;
    }
    
    boolean Start_Cut = false;
    boolean StaticEffectKeywordReset = true;
    public void newGame(Deck humanDeck, Deck computerDeck) {
//    AllZone.Computer = new ComputerAI_Input(new ComputerAI_General());
        Constant.Quest.fantasyQuest[0] = false;
    	
        AllZone.GameInfo.setComputerMaxPlayNumberOfLands(1);
        AllZone.GameInfo.setHumanMaxPlayNumberOfLands(1);
        
        AllZone.GameInfo.setPreventCombatDamageThisTurn(false);
        AllZone.GameInfo.setHumanNumberOfTimesMulliganed(0);
        AllZone.GameInfo.setHumanMulliganedToZero(false);
        AllZone.GameInfo.setComputerStartedThisGame(false);
        
        AllZone.HumanPlayer.reset();
        AllZone.ComputerPlayer.reset();

        AllZone.Phase.reset();
        AllZone.Stack.reset();
        AllZone.Combat.reset();
        AllZone.Display.showCombat("");
        
        AllZone.Human_Graveyard.reset();
        AllZone.Human_Hand.reset();
        AllZone.Human_Library.reset();
        AllZone.Human_Play.reset();
        AllZone.Human_Removed.reset();
        
        AllZone.Computer_Graveyard.reset();
        AllZone.Computer_Hand.reset();
        AllZone.Computer_Library.reset();
        AllZone.Computer_Play.reset();
        AllZone.Computer_Removed.reset();
        
        AllZone.InputControl.resetInput();
        
        AllZone.StaticEffects.reset();
        StaticEffectKeywordReset = true;
        
        AllZone.HumanPlayer.clearHandSizeOperations();
        AllZone.ComputerPlayer.clearHandSizeOperations();
        

        {//re-number cards just so their unique numbers are low, just for user friendliness
            CardFactory c = AllZone.CardFactory;
            Card card;
            int nextUniqueNumber = 1;
            
            Random generator = new Random();
            
            for(int i = 0; i < humanDeck.countMain(); i++) {
                card = c.getCard(humanDeck.getMain(i), AllZone.HumanPlayer);
                card.setUniqueNumber(nextUniqueNumber++);
                
                //if(card.isBasicLand()) {
                String PC = card.getSVar("PicCount");
                int n = 0;
                if (PC.matches("[0-9][0-9]?"))
                	n = Integer.parseInt(PC);
                if (n > 1)
                    card.setRandomPicture(generator.nextInt(n));
                if (card.getSets().size() > 0)
                	card.setRandomSetCode();

                    //System.out.println("human random number:" + card.getRandomPicture());
                //}
                
                AllZone.Human_Library.add(card);
                
            }
            
            for(int i = 0; i < computerDeck.countMain(); i++) {
                card = c.getCard(computerDeck.getMain(i), AllZone.ComputerPlayer);
                card.setUniqueNumber(nextUniqueNumber++);
                
                //if(card.isBasicLand()) {
                String PC = card.getSVar("PicCount");
                int n = 0;
                if (PC.matches("[0-9][0-9]?"))
                	n = Integer.parseInt(PC);
                if (n > 1)
                    card.setRandomPicture(generator.nextInt(n));
                    //System.out.println("computer random number:" + card.getRandomPicture());
                //}
                
                AllZone.Computer_Library.add(card);
                
                //get card picture so that it is in the image cache
//        ImageCache.getImage(card);
            }
        }//end re-numbering
        
        for(int i = 0; i < 100; i++)
            AllZone.HumanPlayer.shuffle();
        
        //do this instead of shuffling Computer's deck
        boolean smoothLand = Constant.Runtime.Smooth[0];
        
        if(smoothLand) {
            Card[] c = smoothComputerManaCurve(AllZone.Computer_Library.getCards());
            AllZone.Computer_Library.setCards(c);
        } else {
            AllZone.Computer_Library.setCards(AllZone.Computer_Library.getCards());
            	AllZone.ComputerPlayer.shuffle();
        }
     
        // Only cut/coin toss if it's the first game of the match
        if (Constant.Runtime.WinLose.countWinLose() == 0)
        {
        	// New code to determine who goes first. Delete this if it doesn't work properly
	        if(Start_Cut) 
	        	seeWhoPlaysFirst();
	        else 
	        	seeWhoPlaysFirst_CoinToss();
        }
        else if (Constant.Runtime.WinLose.didWinRecently())	// if player won last, AI starts
        	computerStartsGame();
        
        for(int i = 0; i < 7; i++) {
            AllZone.HumanPlayer.drawCard();
            AllZone.ComputerPlayer.drawCard();
        }

        ManaPool mp = AllZone.ManaPool;
        AllZone.Human_Play.add(mp);
        //ButtonUtil.reset();

        AllZone.InputControl.setInput(new Input_Mulligan());
        Phase.GameBegins = 1;
    }//newGame()
    
    //this is where the computer cheats
    //changes AllZone.Computer_Library
    private Card[] smoothComputerManaCurve(Card[] in) {
        CardList library = new CardList(in);
        library.shuffle();
        
        //remove all land, keep non-basicland in there, shuffled
        CardList land = library.getType("Land");
        for(int i = 0; i < land.size(); i++)
            if(land.get(i).isLand()) library.remove(land.get(i));
        
        //non-basic lands are removed, because the computer doesn't seem to
        //effectively use them very well
        land = threadLand(land);
        
        try {
            //mana weave, total of 7 land
        	//  The Following have all been reduced by 1, to account for the computer starting first.
            library.add(6, land.get(0));
            library.add(7, land.get(1));
            library.add(8, land.get(2));
            library.add(9, land.get(3));
            library.add(10, land.get(4));
            
            library.add(12, land.get(5));
            library.add(15, land.get(6));
        } catch(IndexOutOfBoundsException e) {
            System.err.println("Error: cannot smooth mana curve, not enough land");
            return in;
        }
        
        //add the rest of land to the end of the deck
        for(int i = 0; i < land.size(); i++)
            if(!library.contains(land.get(i))) library.add(land.get(i));
        

        //check
        for(int i = 0; i < library.size(); i++)
            System.out.println(library.get(i));
        

        return library.toArray();
    }//smoothComputerManaCurve()
    
    /*
    @SuppressWarnings("unused")
    // getComputerLand
    private CardList getComputerLand(CardList in) {
        CardList land;
        land = in.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isLand();
            }
        });
        
        return land;
    }//getComputerLand()
    */
    
    //non-basic lands are removed, because the computer doesn't seem to
    //effectively used them very well
    public CardList threadLand(CardList in) {
        //String[] basicLand = {"Forest", "Swamp", "Mountain", "Island", "Plains"}; //unused
        
        //Thread stuff with as large a spread of colors as possible:
        String[] allLand = {
                "Bayou", "Volcanic Island", "Savannah", "Badlands", "Tundra", "Taiga", "Underground Sea",
                "Plateau", "Tropical Island", "Scrubland", "Overgrown Tomb", "Steam Vents", "Temple Garden",
                "Blood Crypt", "Hallowed Fountain", "Stomping Ground", "Watery Grave", "Sacred Foundry",
                "Breeding Pool", "Godless Shrine", "Pendelhaven", "Flagstones of Trokair", "Forest", "Swamp",
                "Mountain", "Island", "Plains", "Tree of Tales", "Vault of Whispers", "Great Furnace",
                "Seat of the Synod", "Ancient Den", "Treetop Village", "Ghitu Encampment", "Faerie Conclave",
                "Forbidding Watchtower", "Savage Lands", "Arcane Sanctum", "Jungle Shrine",
                "Crumbling Necropolis", "Seaside Citadel", "Elfhame Palace", "Coastal Tower", "Salt Marsh",
                "Kher Keep", "Library of Alexandria", "Dryad Arbor"};
        

        ArrayList<CardList> land = new ArrayList<CardList>();
        
        //get different CardList of all Forest, Swamps, etc...
        CardList check;
        for(int i = 0; i < allLand.length; i++) {
            check = in.getName(allLand[i]);
            
            if(!check.isEmpty()) land.add(check);
        }
        /*
            //get non-basic land CardList
            check = in.filter(new CardListFilter()
            {
              public boolean addCard(Card c)
              {
                return c.isLand() && !c.isBasicLand();
              }
            });
            if(! check.isEmpty())
              land.add(check);
        */

        //thread all separate CardList's of land together to get something like
        //Mountain, Plains, Island, Mountain, Plains, Island
        CardList out = new CardList();
        
        int i = 0;
        while(!land.isEmpty()) {
            i = (i + 1) % land.size();
            
            check = land.get(i);
            if(check.isEmpty()) {
                //System.out.println("removed");
                land.remove(i);
                i--;
                continue;
            }
            
            out.add(check.get(0));
            check.remove(0);
        }//while
        
        return out;
    }//threadLand()
    

    @SuppressWarnings("unused")
    // getDifferentLand
    private int getDifferentLand(CardList list, String land) {
        int out = 0;
        
        return out;
    }
    
    //decides who goes first when starting another game, used by newGame()
    public void seeWhoPlaysFirst_CoinToss() {
    	Object[] possibleValues = {ForgeProps.getLocalized(GAMEACTION_TEXT.HEADS), ForgeProps.getLocalized(GAMEACTION_TEXT.TAILS)};
    	Object q = JOptionPane.showOptionDialog(null, ForgeProps.getLocalized(GAMEACTION_TEXT.HEADS_OR_TAILS), ForgeProps.getLocalized(GAMEACTION_TEXT.COIN_TOSS), 
    			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
    			null, possibleValues, possibleValues[0]);
    	
    	int Flip = MyRandom.random.nextInt(2);
    	String Human_Flip = " ";
    	String Computer_Flip = " ";
  //  	JOptionPane.showMessageDialog(null, q, "", JOptionPane.INFORMATION_MESSAGE);
    	if(q.equals(0)) {
    		Human_Flip = ForgeProps.getLocalized(GAMEACTION_TEXT.HEADS);
    		Computer_Flip = ForgeProps.getLocalized(GAMEACTION_TEXT.TAILS);
    	}
    	else {
    		Human_Flip = ForgeProps.getLocalized(GAMEACTION_TEXT.TAILS);
    		Computer_Flip = ForgeProps.getLocalized(GAMEACTION_TEXT.HEADS);
    	}
    	
        if((Flip == 0 && q.equals(0)) || (Flip == 1 && q.equals(1))) 
        	JOptionPane.showMessageDialog(null, Human_Flip + "\r\n" + ForgeProps.getLocalized(GAMEACTION_TEXT.HUMAN_WIN), "", JOptionPane.INFORMATION_MESSAGE);
        else {
        	computerStartsGame();
    		JOptionPane.showMessageDialog(null, Computer_Flip + "\r\n" +  ForgeProps.getLocalized(GAMEACTION_TEXT.COMPUTER_WIN), "", JOptionPane.INFORMATION_MESSAGE);
        }
	}//seeWhoPlaysFirst_CoinToss()
    
    Card HumanCut = null;
    Card ComputerCut = null;   
    
    public void seeWhoPlaysFirst() {

    	CardList HLibrary = new CardList(AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer).getCards());
        HLibrary = HLibrary.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.isLand();
            }
        });
    	CardList CLibrary = new CardList(AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer).getCards());
        CLibrary = CLibrary.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.isLand();
            }
        });

        boolean Starter_Determined = false;
        int Cut_Count = 0;
        int Cut_CountMax = 20;
        for(int i = 0; i < Cut_CountMax; i++) {
	        if(Starter_Determined == true) break;
	        
	        if(HLibrary.size() > 0) 
	        	HumanCut = HLibrary.get(MyRandom.random.nextInt(HLibrary.size()));
	        else {
	        	computerStartsGame();
	        	JOptionPane.showMessageDialog(null, ForgeProps.getLocalized(GAMEACTION_TEXT.HUMAN_MANA_COST) + "\r\n" + ForgeProps.getLocalized(GAMEACTION_TEXT.COMPUTER_STARTS), "", JOptionPane.INFORMATION_MESSAGE);
	        	return;
	        }
	        
	        if(CLibrary.size() > 0) 
	        	ComputerCut = CLibrary.get(MyRandom.random.nextInt(CLibrary.size()));
	        else {
	        	JOptionPane.showMessageDialog(null, ForgeProps.getLocalized(GAMEACTION_TEXT.COMPUTER_MANA_COST) + "\r\n" + ForgeProps.getLocalized(GAMEACTION_TEXT.HUMAN_STARTS), "", JOptionPane.INFORMATION_MESSAGE);
	        	return;
	        }
	        
	        Cut_Count = Cut_Count + 1;	
	        AllZone.GameAction.moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer),AllZone.GameAction.HumanCut);
	        AllZone.GameAction.moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer),AllZone.GameAction.ComputerCut);
	        
	        StringBuilder sb = new StringBuilder();
	        sb.append(ForgeProps.getLocalized(GAMEACTION_TEXT.HUMAN_CUT) + HumanCut.getName() + " (" + HumanCut.getManaCost() + ")" + "\r\n");
	        sb.append(ForgeProps.getLocalized(GAMEACTION_TEXT.COMPUTER_CUT) + ComputerCut.getName()  + " (" + ComputerCut.getManaCost() + ")" + "\r\n");
	        sb.append("\r\n" + "Number of times the deck has been cut: " + Cut_Count + "\r\n");
	        if(CardUtil.getConvertedManaCost(ComputerCut.getManaCost()) > CardUtil.getConvertedManaCost(HumanCut.getManaCost())){
	        	computerStartsGame();
	        	JOptionPane.showMessageDialog(null, sb + ForgeProps.getLocalized(GAMEACTION_TEXT.COMPUTER_STARTS), "", JOptionPane.INFORMATION_MESSAGE);
	        	return;
	        } 
	        else if(CardUtil.getConvertedManaCost(ComputerCut.getManaCost()) < CardUtil.getConvertedManaCost(HumanCut.getManaCost())) {
	        	JOptionPane.showMessageDialog(null, sb + ForgeProps.getLocalized(GAMEACTION_TEXT.HUMAN_STARTS), "", JOptionPane.INFORMATION_MESSAGE);
	        	return;
	        } 
	        else{
	        	sb.append(ForgeProps.getLocalized(GAMEACTION_TEXT.EQUAL_CONVERTED_MANA) + "\r\n");
	        	if (i == Cut_CountMax-1)
	        	{
	        		sb.append(ForgeProps.getLocalized(GAMEACTION_TEXT.RESOLVE_STARTER));
	        	   	if(MyRandom.random.nextInt(2) == 1) 
	        	   		JOptionPane.showMessageDialog(null,sb + ForgeProps.getLocalized(GAMEACTION_TEXT.HUMAN_WIN), "", JOptionPane.INFORMATION_MESSAGE);
	        		else {
	        			computerStartsGame();
	        			JOptionPane.showMessageDialog(null,sb + ForgeProps.getLocalized(GAMEACTION_TEXT.COMPUTER_WIN), "", JOptionPane.INFORMATION_MESSAGE);
	        		}
	        	   	return;
	        	}
	        	else
	        	{
	        		sb.append(ForgeProps.getLocalized(GAMEACTION_TEXT.CUTTING_AGAIN));
	        	}
	        	JOptionPane.showMessageDialog(null,sb, "", JOptionPane.INFORMATION_MESSAGE);
	        }
        } // for-loop for multiple card cutting
        

    }//seeWhoPlaysFirst()
    
    public void computerStartsGame()
    {
    	AllZone.Phase.setPlayerTurn(AllZone.ComputerPlayer);
    	AllZone.GameInfo.setComputerStartedThisGame(true);
    }
    
    //if Card had the type "Aura" this method would always return true, since local enchantments are always attached to something
    //if Card is "Equipment", returns true if attached to something
    public boolean isAttachee(Card c) {
        CardList list = new CardList(AllZone.Computer_Play.getCards());
        list.addAll(AllZone.Human_Play.getCards());
        
        for(int i = 0; i < list.size(); i++) {
            CardList check = new CardList(list.getCard(i).getAttachedCards());
            if(check.contains(c)) return true;
        }
        
        return false;
    }//isAttached(Card c)
    
    public boolean playCard(Card c) {
        HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();
        SpellAbility[] abilities = canPlaySpellAbility(c.getSpellAbility());
        ArrayList<String> choices = new ArrayList<String>();
        
        if(c.isLand() && isCardInZone(c, AllZone.Human_Hand) && CardFactoryUtil.canHumanPlayLand()) 
        		choices.add("Play land");
        
        for(SpellAbility sa:abilities) {
        	// for uncastables like lotus bloom, check if manaCost is blank
            if(sa.canPlay() && (!sa.isSpell() || !sa.getManaCost().equals(""))) {
            	sa.setActivatingPlayer(c.getController());
                choices.add(sa.toString());
                map.put(sa.toString(), sa);
            }
        }
        
        String choice;
        if (choices.size() == 0) 
        	return false;
        else if (choices.size() == 1)
        	choice = choices.get(0);
        else
        	choice = (String) AllZone.Display.getChoiceOptional("Choose", choices.toArray());
        
        if (choice == null)
        	return false;
        
        if(choice.equals("Play land")){
        	playLand(c, AllZone.Human_Hand);
        	return true;
        }
        
        SpellAbility ability = map.get(choice);
        if(ability != null){
            playSpellAbility(ability);
            return true;
        }
		return false;
    }
    
    static public void playLand(Card land, PlayerZone zone)
    {
    	if (CardFactoryUtil.canHumanPlayLand()){
	    	zone.remove(land);
			AllZone.Human_Play.add(land);
			CardFactoryUtil.playLandEffects(land);
			AllZone.GameInfo.incrementHumanPlayedLands();
    	}
    }
    
    public void playCardNoCost(Card c) {
        //SpellAbility[] choices = (SpellAbility[]) c.getSpells().toArray();
        ArrayList<SpellAbility> choices = c.getBasicSpells();
        SpellAbility sa;
        
        //TODO: add Buyback, Kicker, ... , spells here
        /*
        ArrayList<SpellAbility> additional = c.getAdditionalCostSpells();
        for (SpellAbility s : additional)
        {
        	
        }
        */
        /*
         System.out.println(choices.length);
         for(int i = 0; i < choices.length; i++)
             System.out.println(choices[i]);
        */
        if(choices.size() == 0) return;
        else if(choices.size() == 1) sa = choices.get(0);
        else sa = (SpellAbility) AllZone.Display.getChoiceOptional("Choose", choices.toArray());
        
        if(sa == null) return;
        
        playSpellAbilityForFree(sa);
    }
    
    public void playSpellAbilityForFree(final SpellAbility sa) {

    	if(sa.getBeforePayMana() == null) {
    		boolean x = false;
        	if (sa.getSourceCard().getManaCost().contains("X"))
        		x = true;
    		
        	if (sa.isKickerAbility()) {
                Command paid1 = new Command() {
					private static final long serialVersionUID = -6531785460264284794L;

					public void execute() {
                        AllZone.Stack.add(sa);
                    }
                };
            	AllZone.InputControl.setInput(new Input_PayManaCost_Ability(sa.getAdditionalManaCost(),paid1));        		
        	}else {
        		AllZone.Stack.add(sa, x);
        	}
        } else {
        	sa.setManaCost("0"); // Beached As
        	if (sa.isKickerAbility()) {
        		sa.getBeforePayMana().setFree(false);
        		sa.setManaCost(sa.getAdditionalManaCost());
        	} else {
                sa.getBeforePayMana().setFree(true);        		
        	}
            AllZone.InputControl.setInput(sa.getBeforePayMana());
        }
    }
    
    int CostCutting_GetMultiMickerManaCostPaid = 0;
    String CostCutting_GetMultiMickerManaCostPaid_Colored = "";
    public ManaCost getSpellCostChange(SpellAbility sa, ManaCost originalCost) {
    	// Beached
        Card  originalCard = sa.getSourceCard();
        SpellAbility spell = sa;
        ManaCost manaCost = new ManaCost(originalCost.toString());
        
         if(spell.isSpell() == true) {
         if(originalCard.getName().equals("Avatar of Woe")){
 			Player player = AllZone.Phase.getPlayerTurn();
 			Player opponent = player.getOpponent();
 	        PlayerZone PlayerGraveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
 	        CardList PlayerCreatureList = new CardList(PlayerGraveyard.getCards());
 	        PlayerCreatureList = PlayerCreatureList.getType("Creature");
 			PlayerZone OpponentGraveyard = AllZone.getZone(Constant.Zone.Graveyard, opponent);
 	        CardList OpponentCreatureList = new CardList(OpponentGraveyard.getCards());
 	        OpponentCreatureList = OpponentCreatureList.getType("Creature");
 	        if((PlayerCreatureList.size() + OpponentCreatureList.size()) >= 10) {
             manaCost = new ManaCost("B B");           	
 	        } // Avatar of Woe
         } else if(originalCard.getName().equals("Avatar of Will")) {
 			Player player = AllZone.Phase.getPlayerTurn();
 			Player opponent = player.getOpponent();
 	        PlayerZone OpponentHand = AllZone.getZone(Constant.Zone.Hand, opponent); 
 	        CardList OpponentHandList = new CardList(OpponentHand.getCards());	        
 	        if(OpponentHandList.size() == 0) {
             manaCost = new ManaCost("U U");           	
 	        } // Avatar of Will
         } else if(originalCard.getName().equals("Avatar of Fury")) {
 			Player player = AllZone.Phase.getPlayerTurn();
 			Player opponent = player.getOpponent();
 	        PlayerZone OpponentPlay = AllZone.getZone(Constant.Zone.Play, opponent); 
 	        CardList OpponentLand = new CardList(OpponentPlay.getCards());	   
 	        OpponentLand = OpponentLand.getType("Land");
 	        if(OpponentLand.size() >= 7) {
             manaCost = new ManaCost("R R");           	
 	        } // Avatar of Fury
         } else if(originalCard.getName().equals("Avatar of Might")) {
 			Player player = AllZone.Phase.getPlayerTurn();
 			Player opponent = player.getOpponent();
 	        PlayerZone PlayerPlay = AllZone.getZone(Constant.Zone.Play, player); 
 	        CardList PlayerCreature = new CardList(PlayerPlay.getCards());	   
 	        PlayerCreature = PlayerCreature.getType("Creature");
 	        PlayerZone OpponentPlay = AllZone.getZone(Constant.Zone.Play, opponent); 
 	        CardList OpponentCreature = new CardList(OpponentPlay.getCards());	   
 	        OpponentCreature = OpponentCreature.getType("Creature");
 	        if(OpponentCreature.size() - PlayerCreature.size() >= 4) {
             manaCost = new ManaCost("G G");   	        	
 	        } // Avatar of Might
         }
     } // isSpell
         
         // Get Cost Reduction
         if(Phase.GameBegins == 1) { // Remove GameBegins from Phase and into The starting game code
 		CardList Cards_In_Play = new CardList();
 		Cards_In_Play.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards());
 		Cards_In_Play.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
 		Cards_In_Play = Cards_In_Play.filter(new CardListFilter() {
             public boolean addCard(Card c) {
                 if(c.getKeyword().toString().contains("CostChange")) return true;
                 return false;
             }
         });
 		Cards_In_Play.add(originalCard);
 		String Mana = manaCost.toString();
 		CardList Player_Play = new CardList(AllZone.getZone(Constant.Zone.Play, sa.getSourceCard().getController()).getCards());
 		int XBonus = 0;
 		int Max = 25;
 		if(sa.isXCost() && !sa.getSourceCard().isCopiedSpell()) sa.getSourceCard().setXManaCostPaid(0);
        if(sa.isMultiKicker()) CostCutting_GetMultiMickerManaCostPaid_Colored = "";
 		if(Mana.toString().length() == 0) Mana = "0";
 		for(int i = 0; i < Cards_In_Play.size() ; i++) {	
 			Card card = Cards_In_Play.get(i);
 		        ArrayList<String> a = card.getKeyword();
 		        int CostKeywords = 0;
 		        int CostKeyword_Number[] = new int[a.size()];
 		        for(int x = 0; x < a.size(); x++)
 		            if(a.get(x).toString().startsWith("CostChange")) {
 		            	CostKeyword_Number[CostKeywords] = x;
 		            	CostKeywords = CostKeywords + 1;
 		            }
 		        for(int CKeywords = 0; CKeywords < CostKeywords; CKeywords++) {
                 String parse = card.getKeyword().get(CostKeyword_Number[CKeywords]).toString();                
                 String k[] = parse.split(":");
                 if(card.equals(originalCard)) {
            		 if(!k[4].equals("Self")) k[2] = "Owned";
             }
                 if(k[2].equals("More")) { 
                   	if(k[7].equals("OnlyOneBonus")) {  // Only Works for Color and Type
                   		for(int string_no = 5; string_no < 7; string_no++) {
                            String spilt = k[string_no];                
                            String color_spilt[] = spilt.split("/");  
                   		
                            for(int cs_num = 0; cs_num < color_spilt.length; cs_num++) {
                            	k[string_no] = color_spilt[cs_num];
                            	if(string_no == 5 && CardUtil.getColors(sa.getSourceCard()).contains(k[5])) break; 	
                            	if(string_no == 6 && (sa.getSourceCard().isType(k[6]))) break;
                   		}
                   		}
                 	}
	                   	if(k[7].contains("All Conditions")) { // Only Works for Color and Type
  	                   		for(int string_no = 5; string_no < 7; string_no++) {
  	                            String spilt = k[string_no];                
  	                            String color_spilt[] = spilt.split("/");  
  	                            for(int cs_num = 0; cs_num < color_spilt.length; cs_num++) {
  	                            	k[string_no] = color_spilt[cs_num];
  	                            	if(string_no == 5) {
  	                            		if(CardUtil.getColors(sa.getSourceCard()).contains(k[5]) || k[5].equals("All"))  {  	                            			
  	                            	} else {
  	                            		k[5] = "Nullified";
  	                            		break;
  	                            	}
  	                            	}
  	                            	if(string_no == 6) {
  	                            		if(sa.getSourceCard().isType(k[6])  || k[6].equals("All"))  {  	                            			
  	                            	} else {
  	                            		k[6] = "Nullified";
  	                            		break;
  	                            	}
  	                            	}
  	                   		}
  	                   		}
  	                            if(!k[5].equals("Nullified")) k[5] = "All";
  	                            if(!k[6].equals("Nullified")) k[6] = "All";
  	                 	} 
                 if((k[1].equals("Player") && card.getController().equals(sa.getSourceCard().getController()) 
                 		|| (k[1].equals("Opponent") && card.getController().equals(sa.getSourceCard().getController().getOpponent())) || k[1].equals("All"))
                        && ((k[4].equals("Spell") && sa.isSpell() == true) || (k[4].equals("Ability") && sa.isAbility() == true) 
                        || (k[4].equals("Self") && originalCard.equals(card)) || k[4].equals("All"))
                 		&& ((CardUtil.getColors(sa.getSourceCard()).contains(k[5])) || k[5].equals("All")) 
                        && ((sa.getSourceCard().isType(k[6])) 
                        || (!(sa.getSourceCard().isType(k[6])) && k[7].contains("NonType")) || k[6].equals("All"))) {      
                  	if(k[7].contains("CardIsTapped")) {
                 		if(card.isTapped() == false) k[3] = "0";             		
                 	}
                 	if(k[7].contains("TargetInPlay")) {
                 		if(!Player_Play.contains(sa.getSourceCard())) k[3] = "0";             		
                 	}
                 	if(k[7].contains("NonType")) {
                 		if(originalCard.isType(k[6])) k[3] = "0";             		
                 	}
                 	if(k[7].contains("OpponentTurn")) {
                 		if(AllZone.Phase.isPlayerTurn(originalCard.getController())) k[3] = "0";             		
                 	}
                 	if(k[7].contains("Affinity")) {
                          String spilt = k[7];                
                          String color_spilt[] = spilt.split("/");  
                          	k[7] = color_spilt[1];	
                          	PlayerZone PlayerPlay = AllZone.getZone(Constant.Zone.Play, originalCard.getController()); 
                          	CardList PlayerList = new CardList(PlayerPlay.getCards());	   
                          	PlayerList = PlayerList.getType(k[7]);
                          	k[3] = String.valueOf(PlayerList.size());   		
                 	}
                 	String[] Numbers = new String[Max];
                 	if("X".equals(k[3])) {
						for(int no = 0; no < Max; no ++) Numbers[no] = String.valueOf(no);
						String Number_ManaCost = " ";
						if(Mana.toString().length() == 1) {
							Number_ManaCost = Mana.toString().substring(0, 1);
						}
						else if(Mana.toString().length() == 0) {
							Number_ManaCost = "0"; // Should Never Occur
						}
						else {
							Number_ManaCost = Mana.toString().substring(0, 2);
						}
						Number_ManaCost = Number_ManaCost.trim();
						for(int check = 0; check < Max; check ++) {
							if(Number_ManaCost.equals(Numbers[check])) {
								int xValue = CardFactoryUtil.xCount(originalCard, originalCard.getSVar("X"));
								//if((spell.isXCost()) || (spell.isMultiKicker()) && (check - Integer.valueOf(k[3])) < 0) XBonus = XBonus - check + Integer.valueOf(k[3]);
								Mana = Mana.replaceFirst(String.valueOf(check),String.valueOf(check + xValue));
							}
							if(Mana.equals("")) Mana = "0";
							manaCost = new ManaCost(Mana);	
						}
					}
                 	else if(!"WUGRB".contains(k[3])) {
                 	for(int no = 0; no < Max; no ++) Numbers[no] = String.valueOf(no);
                 	String Number_ManaCost = " ";
             		if(Mana.toString().length() == 1) Number_ManaCost = Mana.toString().substring(0, 1);
             		else if(Mana.toString().length() == 0) Number_ManaCost = "0"; // Should Never Occur
             		else Number_ManaCost = Mana.toString().substring(0, 2);
             		Number_ManaCost = Number_ManaCost.trim();
             		
                 	for(int check = 0; check < Max; check ++) {
                 		if(Number_ManaCost.equals(Numbers[check])) {
                 		Mana = Mana.replaceFirst(String.valueOf(check),String.valueOf(check + Integer.valueOf(k[3])));
                 		}
                 		if(Mana.equals("")) Mana = "0";
                 		manaCost = new ManaCost(Mana);	
                 	}
                 	if(!manaCost.toString().contains("0") && !manaCost.toString().contains("1") && !manaCost.toString().contains("2") 
                 			&& !manaCost.toString().contains("3") && !manaCost.toString().contains("4") && !manaCost.toString().contains("5")
                 			&& !manaCost.toString().contains("6") && !manaCost.toString().contains("7") && !manaCost.toString().contains("8")
                 			&& !manaCost.toString().contains("9")) {
                 		Mana = k[3] + " " + Mana;	
             			manaCost = new ManaCost(Mana);
                 	}
                     } else {
             			Mana = Mana + " " + k[3];	
             			manaCost = new ManaCost(Mana);
                     }              	
                 }
                 }
 		        }
 		}

                 if(Mana.equals("0") && spell.isAbility()) {
                 } else {
              		for(int i = 0; i < Cards_In_Play.size() ; i++) {	
             			Card card = Cards_In_Play.get(i);
             		        ArrayList<String> a = card.getKeyword();
             		        int CostKeywords = 0;
             		        int CostKeyword_Number[] = new int[a.size()];
             		        for(int x = 0; x < a.size(); x++)
             		            if(a.get(x).toString().startsWith("CostChange")) {
             		            	CostKeyword_Number[CostKeywords] = x;
             		            	CostKeywords = CostKeywords + 1;
             		            }
             		        for(int CKeywords = 0; CKeywords < CostKeywords; CKeywords++) {
                             String parse = card.getKeyword().get(CostKeyword_Number[CKeywords]).toString();                
                             String k[] = parse.split(":");
                             if(card.equals(originalCard)) {
                        		 if(!k[4].equals("Self")) k[2] = "Owned";
                         }
      	                 if(k[2].equals("Less")) {   
      	                   	if(k[7].equals("OnlyOneBonus")) { // Only Works for Color and Type
      	                   		for(int string_no = 5; string_no < 7; string_no++) {
      	                            String spilt = k[string_no];                
      	                            String color_spilt[] = spilt.split("/");  
      	                   		
      	                            for(int cs_num = 0; cs_num < color_spilt.length; cs_num++) {
      	                            	k[string_no] = color_spilt[cs_num];
      	                            	if(string_no == 5 && CardUtil.getColors(sa.getSourceCard()).contains(k[5])) break; 	
      	                            	if(string_no == 6 && (sa.getSourceCard().isType(k[6]))) break;
      	                   		}
      	                   		}
      	                 	}
      	                   	if(k[7].contains("All Conditions")) { // Only Works for Color and Type
      	                   		for(int string_no = 5; string_no < 7; string_no++) {
      	                            String spilt = k[string_no];                
      	                            String color_spilt[] = spilt.split("/");  
      	                            for(int cs_num = 0; cs_num < color_spilt.length; cs_num++) {
      	                            	k[string_no] = color_spilt[cs_num];
      	                            	if(string_no == 5) {
      	                            		if(CardUtil.getColors(sa.getSourceCard()).contains(k[5]) || k[5].equals("All"))  {  	                            			
      	                            	} else {
      	                            		k[5] = "Nullified";
      	                            		break;
      	                            	}
      	                            	}
      	                            	if(string_no == 6) {
      	                            		if(sa.getSourceCard().isType(k[6])  || k[6].equals("All"))  {  	                            			
      	                            	} else {
      	                            		k[6] = "Nullified";
      	                            		break;
      	                            	}
      	                            	}
      	                   		}
      	                   		}
      	                            if(!k[5].equals("Nullified")) k[5] = "All";
      	                            if(!k[6].equals("Nullified")) k[6] = "All";
      	                 	}                 
                         if((k[1].equals("Player") && card.getController().equals(sa.getSourceCard().getController()) 
                         		|| (k[1].equals("Opponent") && card.getController().equals(sa.getSourceCard().getController().getOpponent())) || k[1].equals("All"))
                         		&& ((k[4].equals("Spell") && sa.isSpell() == true) || (k[4].equals("Ability") && sa.isAbility() == true) 
                         		|| (k[4].equals("Self") && originalCard.equals(card)) || k[4].equals("All"))
                         		&& ((CardUtil.getColors(sa.getSourceCard()).contains(k[5])) || k[5].equals("All")) 
                         		/**
                                  *  Chris added a test for Changeling.
                                  *  This appears to reduce the cost correctly.
                                  *  Works for both the computer and the human.
                                  */
                                && ((sa.getSourceCard().isType(k[6]))
                         				
                         		|| (!(sa.getSourceCard().isType(k[6])) && k[7].contains("NonType")) || k[6].equals("All"))) { 
                         	if(k[7].contains("CardIsTapped")) {
                         		if(card.isTapped() == false) k[3] = "0";             		
                         	}
                         	if(k[7].contains("TargetInPlay")) {
                         		if(!Player_Play.contains(sa.getSourceCard())) k[3] = "0";             		
                         	}
                         	if(k[7].contains("NonType")) {
                         		if(originalCard.isType(k[6])) k[3] = "0";             		
                         	}
                         	if(k[7].contains("OpponentTurn")) {
                         		if(AllZone.Phase.isPlayerTurn(originalCard.getController())) k[3] = "0";             		
                         	}
                         	if(k[7].contains("Affinity")) {
      	                            String spilt = k[7];                
      	                            String color_spilt[] = spilt.split("/");  
      	                            	k[7] = color_spilt[1];	
      	                            	PlayerZone PlayerPlay = AllZone.getZone(Constant.Zone.Play, originalCard.getController()); 
      	                            	CardList PlayerList = new CardList(PlayerPlay.getCards());	   
      	                            	PlayerList = PlayerList.getType(k[7]);
      	                            	k[3] = String.valueOf(PlayerList.size());    		
                         	}

                     	String[] Numbers = new String[Max];
                     	if("X".equals(k[3])) {
							for(int no = 0; no < Max; no ++) Numbers[no] = String.valueOf(no);
							String Number_ManaCost = " ";
							if(Mana.toString().length() == 1) {
								Number_ManaCost = Mana.toString().substring(0, 1);
							}
							else if(Mana.toString().length() == 0) {
								Number_ManaCost = "0"; // Should Never Occur
							}
							else {
								Number_ManaCost = Mana.toString().substring(0, 2);
							}
							Number_ManaCost = Number_ManaCost.trim();
							for(int check = 0; check < Max; check ++) {
								if(Number_ManaCost.equals(Numbers[check])) {
									int xValue = CardFactoryUtil.xCount(originalCard, originalCard.getSVar("X"));
									//if((spell.isXCost()) || (spell.isMultiKicker()) && (check - Integer.valueOf(k[3])) < 0) XBonus = XBonus - check + Integer.valueOf(k[3]);
									Mana = Mana.replaceFirst(String.valueOf(check),String.valueOf(check - xValue));
								}
								if(Mana.equals("")) Mana = "0";
								manaCost = new ManaCost(Mana);	
							}
						}
                     	else if(!"WUGRB".contains(k[3])) {
                     	for(int no = 0; no < Max; no ++) Numbers[no] = String.valueOf(no);
                     	String Number_ManaCost = " ";
                 		if(Mana.toString().length() == 1) Number_ManaCost = Mana.toString().substring(0, 1);
                 		else if(Mana.toString().length() == 0) Number_ManaCost = "0";  // Should Never Occur
                 		else Number_ManaCost = Mana.toString().substring(0, 2);
                 		Number_ManaCost = Number_ManaCost.trim();
                 		
                     	for(int check = 0; check < Max; check ++) {
                     		if(Number_ManaCost.equals(Numbers[check])) {
                         		if((spell.isXCost()) || (spell.isMultiKicker()) && (check - Integer.valueOf(k[3])) < 0) XBonus = XBonus - check + Integer.valueOf(k[3]);
                     			if(check - Integer.valueOf(k[3]) < 0) k[3] = String.valueOf(check);
                     		Mana = Mana.replaceFirst(String.valueOf(check),String.valueOf(check - Integer.valueOf(k[3])));	                  		
                     		}
                     		if(Mana.equals("")) Mana = "0";
                     		manaCost = new ManaCost(Mana);	
                     	}		
                         } else {
                     //   	 JOptionPane.showMessageDialog(null, Mana + " " + Mana.replaceFirst(k[3],""), "", JOptionPane.INFORMATION_MESSAGE);
                        	 if(Mana.equals(Mana.replaceFirst(k[3], ""))) {                       		 
                        		// if(sa.isXCost()) sa.getSourceCard().addXManaCostPaid(1); Not Included as X Costs are not in Colored Mana
                        		 if(sa.isMultiKicker())	 CostCutting_GetMultiMickerManaCostPaid_Colored = CostCutting_GetMultiMickerManaCostPaid_Colored + k[3]; 
                        //		 JOptionPane.showMessageDialog(null, CostCutting_GetMultiMickerManaCostPaid_Colored, "", JOptionPane.INFORMATION_MESSAGE);
                        	 } else {	 
                         	Mana = Mana.replaceFirst(k[3], "");
                         	Mana = Mana.trim();
                         	if(Mana.equals("")) Mana = "0";                        	
                         	manaCost = new ManaCost(Mana);
                         }
                         }
                         }
                         Mana = Mana.trim();
                         if(Mana.length() == 0 || Mana.equals("0")) {
                         	if(sa.isSpell()) Mana = "0";
                         	else {
                         		Mana = "1";
                         	}
                         }
                         }   
         	            manaCost = new ManaCost(Mana);
                 	}	
              		}
      	                 }
                    if(sa.isXCost()) {
                    
                    for(int XPaid = 0; XPaid < XBonus; XPaid++) sa.getSourceCard().addXManaCostPaid(1);
                    }
                    if(sa.isMultiKicker()) {
                    	CostCutting_GetMultiMickerManaCostPaid = 0;
                               for(int XPaid = 0; XPaid < XBonus; XPaid++) CostCutting_GetMultiMickerManaCostPaid = CostCutting_GetMultiMickerManaCostPaid + 1;        
                    }
         }
         if(originalCard.getName().equals("Khalni Hydra") && spell.isSpell() == true) {
 			Player player = AllZone.Phase.getPlayerTurn();
 	        PlayerZone PlayerPlay = AllZone.getZone(Constant.Zone.Play, player); 
 	        CardList PlayerCreature = new CardList(PlayerPlay.getCards());	   
 	        PlayerCreature = PlayerCreature.getType("Creature");
 	        PlayerCreature = PlayerCreature.filter(new CardListFilter() {
 				public boolean addCard(Card c) {
 					return c.isCreature() && c.isGreen();
 				}
 			});       
 	        String Mana = manaCost + " ";
 	        if(PlayerCreature.size() > 0) {
                 for(int i = 0; i < PlayerCreature.size(); i++) {
                 	Mana = Mana.replaceFirst("G ", "");	
                 }
                 Mana = Mana.trim();
                 if(Mana.equals("")) Mana = "0";
 	            manaCost = new ManaCost(Mana);        	
 	        }
         } // Khalni Hydra      
         return manaCost;
    }//GetSpellCostChange
    
    public void playSpellAbility(SpellAbility sa) {
    	sa.setActivatingPlayer(AllZone.HumanPlayer);
    	if (sa.getPayCosts() != null){
    		Target_Selection ts = new Target_Selection(sa.getTarget(), sa);    		
    		Cost_Payment payment = new Cost_Payment(sa.getPayCosts(), sa);
    		
    		payment.changeCost();
    		
    		SpellAbility_Requirements req = new SpellAbility_Requirements(sa, ts, payment);
    		req.fillRequirements();
    	}
    	else{
	    	ManaCost manaCost = new ManaCost(sa.getManaCost());
	    	if(sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
	    		manaCost = new ManaCost("0"); 
	    	} else {
	    		
	    		manaCost = getSpellCostChange(sa, new ManaCost(sa.getManaCost()));    		
	    	}      
	        if(manaCost.isPaid() && sa.getBeforePayMana() == null) {
	        	if (sa.getAfterPayMana() == null){
		        	CardList HHandList = new CardList(AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer).getCards());
		        	if(HHandList.contains(sa.getSourceCard())) AllZone.Human_Hand.remove(sa.getSourceCard());
		            AllZone.Stack.add(sa);
		            if(sa.isTapAbility() && !sa.wasCancelled()) sa.getSourceCard().tap();
		            if(sa.isUntapAbility()) sa.getSourceCard().untap();
		            return;
	        	}
	        	else
	        		AllZone.InputControl.setInput(sa.getAfterPayMana());
	        }
	        else if(sa.getBeforePayMana() == null) 
	        	AllZone.InputControl.setInput(new Input_PayManaCost(sa));
	        else 
	        	AllZone.InputControl.setInput(sa.getBeforePayMana());
    	}
    }
    
    public SpellAbility[] canPlaySpellAbility(SpellAbility[] sa) {
        ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();
        
        for(int i = 0; i < sa.length; i++){
        	sa[i].setActivatingPlayer(AllZone.HumanPlayer);
            if(sa[i].canPlay()) 
            	list.add(sa[i]);
        }
        
        SpellAbility[] array = new SpellAbility[list.size()];
        list.toArray(array);
        return array;
    }//canPlaySpellAbility()
    
    public void skullClamp_destroy(Card c) {
        CardList equipment = new CardList();
        equipment.addAll(c.getEquippedBy().toArray());
        equipment = equipment.getName("Skullclamp");
        
        if(equipment.size() == 0) return;
        
        final Card crd = c;
        for(int i = 0; i < equipment.size(); i++) {
        	final Card skullclamp = equipment.get(i);
        	final Ability draw = new Ability(crd, "0") {
        		@Override
        		public void resolve() {
        			Player player = skullclamp.getController();
        			player.drawCards(2);
        		}
        	};//Ability
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append("Skullclamp - ").append(skullclamp.getController());
        	sb.append(" draws 2 cards (").append(c.getName()).append(").");
        	draw.setStackDescription(sb.toString());
        	
        	AllZone.Stack.add(draw);
        }
    }
    
    public void searchLibraryLand(String type, Player player, String Zone1, boolean tapLand) {
    	searchLibraryTwoLand(type, player, Zone1, tapLand, "", false);
    }
    
    public void searchLibraryBasicLand(Player player, String Zone1, boolean tapLand) {
    	searchLibraryTwoLand("Basic", player, Zone1, tapLand, "", false);
    }
    
    public void searchLibraryTwoLand(String type, Player player,
    		String Zone1, boolean tapFirstLand, 
    		String Zone2, boolean tapSecondLand) {
        if(player.equals(AllZone.HumanPlayer)) {
        	humanSearchTwoLand(type, Zone1, tapFirstLand, Zone2, tapSecondLand);
        } else {
        	aiSearchTwoLand(type, Zone1, tapFirstLand, Zone2, tapSecondLand);
        }
        
        player.shuffle();
		
	}
	public void searchLibraryTwoBasicLand(Player player,
			String Zone1, boolean tapFirstLand, 
			String Zone2, boolean tapSecondLand) {
        searchLibraryTwoLand("Basic", player, Zone1, tapFirstLand, Zone2, tapSecondLand);
    }
    	
    private void aiSearchTwoLand(String type, String Zone1, boolean tapFirstLand,
    		String Zone2, boolean tapSecondLand) {
        CardList land = new CardList(AllZone.Computer_Library.getCards());
        land = land.getType(type);
        PlayerZone firstZone = AllZone.getZone(Zone1, AllZone.ComputerPlayer);
        
        if (type.contains("Basic")) {
        	// No need for special sorting for basic land
        	// just shuffle to make the computer a little less predictable
        	land.shuffle();
        } else {
            Comparator<Card> aiLandComparator = new Comparator<Card>()
            {
            	private int scoreLand(Card a) {
            		String valakutName = "Valakut, the Molten Pinnacle";

            		int theScore = 0;
            		if (!a.isBasicLand()) {
            			// favor non-basic land
            			theScore++;
            			if (a.getName().contains(valakutName)) {
            				// TODO: Add names of other special lands
            				theScore++;
            			}
            		}
            		return theScore;
            	}
              public int compare(Card a, Card b)
              {
            	  int aScore = scoreLand(a);
            	  int bScore = scoreLand(b);
            	  return bScore - aScore;
              } // compare
            };//Comparator

        	// Prioritize the land somewhat
        	land.sort(aiLandComparator);
        }        
        //3 branches: 1-no land in deck, 2-one land in deck, 3-two or more land in deck
        if(land.size() != 0) {
            //branch 2 - at least 1 land in library
            Card firstLand = land.remove(0);
            if (tapFirstLand)
            	firstLand.tap();
            
            firstZone.add(firstLand);
            AllZone.Computer_Library.remove(firstLand);
            
            //branch 3
            if(Zone2.trim().length() != 0 && (land.size() != 0)) {
                PlayerZone secondZone = AllZone.getZone(Zone2, AllZone.ComputerPlayer);
                Card secondLand = land.remove(0);
                if (tapSecondLand)
                	secondLand.tap();
                secondZone.add(secondLand);
                AllZone.Computer_Library.remove(secondLand);
            }
        }
    }

    private void humanSearchTwoLand(String type, String Zone1, boolean tapFirstLand, String Zone2, boolean tapSecondLand) {
        PlayerZone firstZone = AllZone.getZone(Zone1, AllZone.HumanPlayer);
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer);
        
        CardList list = new CardList(library.getCards());
        list = list.getType(type);
        
        //3 branches: 1-no land in deck, 2-one land in deck, 3-two or more land in deck
        
        //branch 1
        if(list.size() == 0) return;
        
        // Check whether we were only asked for one land, and adjust the prompt accordingly
        boolean onlyOneLand = (Zone2.trim().length() == 0);
        String firstPrompt;
        if (onlyOneLand)
        	firstPrompt = "Choose a land";
        else
        	firstPrompt = "Choose first land";
        
        //branch 2
        Object o = AllZone.Display.getChoiceOptional(firstPrompt, list.toArray());
        if(o != null) {
            Card c = (Card) o;
            list.remove(c);
            if (tapFirstLand)
            	c.tap();

            library.remove(c);
            firstZone.add(c);
            
        }//if
        if ((list.size() == 0) || onlyOneLand) return;
        //branch 3
        o = AllZone.Display.getChoiceOptional(ForgeProps.getLocalized(GAMEACTION_TEXT.CHOOSE_2ND_LAND), list.toArray());
        if(o != null) {
            PlayerZone secondZone = AllZone.getZone(Zone2, AllZone.HumanPlayer);

            Card c = (Card) o;
            list.remove(c);
            if (tapSecondLand)
            	c.tap();
            
            library.remove(c);
            secondZone.add(c);
        }
    }
    
    public void proliferate(final Card c, String cost)
    {
    	Ability p = getProliferateAbility(c, cost);
    	AllZone.Stack.add(p);
    }
    
    public Ability getProliferateAbility(final Card c, final String cost)
    {
    	final Ability ability = new Ability(c, cost)
    	{
    		public void resolve()
    		{
    			CardList hperms = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
    			hperms = hperms.filter(new CardListFilter(){
    				public boolean addCard(Card crd)
    				{
    					return !crd.getName().equals("Mana Pool") /*&& crd.hasCounters()*/;
    				}
    			});
    			
    			CardList cperms = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
    			cperms = cperms.filter(new CardListFilter(){
    				public boolean addCard(Card crd)
    				{
    					return !crd.getName().equals("Mana Pool") /*&& crd.hasCounters()*/;
    				}
    			});
    			
    			if (c.getController().equals(AllZone.HumanPlayer))
    			{	
    				cperms.addAll(hperms.toArray());
    				final CardList unchosen = cperms;
    				AllZone.InputControl.setInput(new Input() {
    					private static final long serialVersionUID = -1779224307654698954L;

    					@Override
    					public void showMessage() {
    						AllZone.Display.showMessage("Choose permanents and/or players");
    						ButtonUtil.enableOnlyOK();
    					}

    					@Override
    					public void selectButtonOK() {
    						stop();
    					}

    					@Override
    					public void selectCard(Card card, PlayerZone zone)
    					{
    						if(!unchosen.contains(card)) return;
    						unchosen.remove(card);
    						ArrayList<String> choices = new ArrayList<String>();
    						for(Counters c_1:Counters.values())
    							if(card.getCounters(c_1) != 0) choices.add(c_1.getName());
    						if (choices.size() > 0)
    							card.addCounter(Counters.getType((choices.size() == 1 ? choices.get(0) : AllZone.Display.getChoice("Select counter type", choices.toArray()).toString())), 1);
    					}
    					boolean selComputer = false;
    					boolean selHuman = false;
    					@Override
    					public void selectPlayer(Player player){
    						if (player.equals(AllZone.HumanPlayer) && selHuman == false)
    						{
    							selHuman = true;
    							if (AllZone.HumanPlayer.getPoisonCounters() > 0)
    								AllZone.HumanPlayer.addPoisonCounters(1);
    						}
    						if (player.equals(AllZone.ComputerPlayer) && selComputer == false)
    						{
    							selComputer = true;
    							if (AllZone.ComputerPlayer.getPoisonCounters() > 0)
    								AllZone.ComputerPlayer.addPoisonCounters(1);
    						}
    					}
    				});
    			}
    			else //comp
    			{
    				cperms = cperms.filter(new CardListFilter(){
    					public boolean addCard(Card crd)
    					{
    						int pos = 0;
    						int neg = 0;
    						for(Counters c_1:Counters.values()) {
                                if(crd.getCounters(c_1) != 0)
                                {
                                	if (CardFactoryUtil.isNegativeCounter(c_1))
                                		neg++;
                                	else
                                		pos++;
                                }
    						}
    						return pos > neg;
    					}
    				});
    				
    				hperms = hperms.filter(new CardListFilter(){
    					public boolean addCard(Card crd)
    					{
    						int pos = 0;
    						int neg = 0;
    						for(Counters c_1:Counters.values()) {
                                if(crd.getCounters(c_1) != 0)
                                {
                                	if (CardFactoryUtil.isNegativeCounter(c_1))
                                		neg++;
                                	else
                                		pos++;
                                }
    						}
    						return pos < neg;
    					}
    				});
    				
    				StringBuilder sb = new StringBuilder();
    				sb.append("<html>Proliferate: <br>Computer selects ");
    				if (cperms.size() == 0 && hperms.size() == 0 && AllZone.HumanPlayer.getPoisonCounters() == 0)
    					sb.append("<b>nothing</b>.");
    				else
    				{
    					if (cperms.size()>0) {
	    					sb.append("<br>From Computer's permanents: <br><b>");
	    					for (Card c:cperms)
	    					{
	    						sb.append(c);
	    						sb.append(" ");
	    					}
	    					sb.append("</b><br>");
    					}
    					if (hperms.size()>0) {
	    					sb.append("<br>From Human's permanents: <br><b>");
	    					for (Card c:cperms)
	    					{
	    						sb.append(c);
	    						sb.append(" ");
	    					}
	    					sb.append("</b><br>");
    					}
    					if (AllZone.HumanPlayer.getPoisonCounters() > 0)
    						sb.append("<b>Human Player</b>.");
    				}//else
    				sb.append("</html>");
    				
    				
    				//add a counter for each counter type, if it would benefit the computer
    				for (Card c:cperms)
    				{
    					for(Counters c_1:Counters.values())
                            if(c.getCounters(c_1) != 0) c.addCounter(c_1, 1);
    				}
    				
    				//add a counter for each counter type, if it would screw over the player
    				for (Card c:hperms)
    				{
    					for(Counters c_1:Counters.values())
                            if(c.getCounters(c_1) != 0) c.addCounter(c_1, 1);
    				}
    				
    				//give human a poison counter, if he has one
    				if (AllZone.HumanPlayer.getPoisonCounters() > 0)
                		AllZone.HumanPlayer.addPoisonCounters(1);
    				
    			} //comp
    		}
    	};
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append(c).append(" - Proliferate (You choose any number of permanents and/or players with ");
    	sb.append("counters on them, then give each another counter of a kind already there.)");
    	ability.setStackDescription(sb.toString());
    	
    	return ability;
    }
    
    public static void main(String[] args) {
        GameAction gameAction = new GameAction();
        GenerateConstructedDeck gen = new GenerateConstructedDeck();
        
        for(int i = 0; i < 2000; i++) {
            CardList list = gen.generateDeck();
            
            Card[] card = gameAction.smoothComputerManaCurve(list.toArray());
            
            CardList check = new CardList();
            for(int a = 0; a < 30; a++)
                check.add(card[a]);
            
            if(check.getType("Land").size() != 7) {
                System.out.println("error - " + check);
                break;
            }
        }//for
    }
}

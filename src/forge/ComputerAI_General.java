package forge;
import static forge.error.ErrorViewer.*;

import java.util.ArrayList;
// import java.util.Collection;
// import java.util.TreeSet;

import com.esotericsoftware.minlog.Log;


public class ComputerAI_General implements Computer {
    //private boolean          playLand = true;
	//private int numberPlayLand = 1;
	//private int numberPlayLand = CardFactoryUtil.getCanPlayNumberOfLands(AllZone.ComputerPlayer);
    //private Collection<Card> playMain1Cards;
        
    // @SuppressWarnings("unchecked")
    // TreeSet type safety
    public ComputerAI_General() {
        //try to reduce the number of comparisons
        //playMain1Cards = new TreeSet(getMain1PlayHand());
    }
    
    public void main1() {
    	ComputerUtil.chooseLandsToPlay();
        playCards(Constant.Phase.Main1);
    }//main1()
    
    public void main2() {
    	ComputerUtil.chooseLandsToPlay();	// in case we can play more lands now, or drew cards since first main phase
        playCards(Constant.Phase.Main2);
   }
    
    private void playCards(final String phase) {
        SpellAbility[] sp = phase.equals(Constant.Phase.Main1)? getMain1():getMain2();
        
        boolean nextPhase = ComputerUtil.playCards(sp);
        
        if(nextPhase) {
            
            //AllZone.Phase.nextPhase();
            //for debugging: System.out.println("need to nextPhase(ComputerAI_General.playCards) = true");
            AllZone.Phase.setNeedToNextPhase(true);
        }
    }//playCards()
    

/*    private ArrayList<String> getMain1PlayHand() {
        ArrayList<String> play = new ArrayList<String>();
        play.add("Man-o'-War");
        play.add("Fire Imp");
        play.add("Flametongue Kavu");
        play.add("Briarhorn");
        play.add("Inner-Flame Acolyte");
        play.add("Affa Guard Hound");
        play.add("Keening Banshee");
        play.add("Aggressive Urge");
        play.add("Amnesia");
        play.add("Angelic Blessing");
        play.add("Ashes to Ashes");
        
        play.add("Beacon of Destruction");
        play.add("Blinding Light");
        play.add("Brute Force");
        play.add("Cackling Flames");
        
        play.add("Char");
        play.add("Control Magic");
        play.add("Crib Swap");
        play.add("Dark Banishing");
        play.add("Devour in Shadow");
        
        play.add("Do or Die");
        play.add("Douse in Gloom");
        play.add("Echoing Decay");
        play.add("Echoing Truth");
        play.add("Elvish Fury");
        play.add("Epic Proportions");
        
        play.add("Erratic Explosion");
        play.add("Explore");
        play.add("Expunge");
        play.add("Faceless Butcher");
        play.add("Feral Lightning");
        play.add("Finest Hour");
        play.add("Firebolt");
        play.add("Flamebreak");
        
        play.add("Gaea's Anthem");
        play.add("Giant Growth");
        play.add("Glorious Anthem");
        play.add("Hex");
        play.add("Hidetsugu's Second Rite");
        play.add("Hymn to Tourach");
        
        play.add("Ichor Slick");
        play.add("Infest");
        play.add("Inspirit");
        play.add("Kamahl, Pit Fighter");
        play.add("Kjeldoran War Cry");
        play.add("Lightning Bolt");
        
        play.add("Might of Oaks");
        play.add("Nameless Inversion");
        play.add("Needle Storm");
        play.add("Oblivion Ring");
        play.add("Oubliette");
        play.add("Path of Anger's Flame");
        play.add("Peel from Reality");
        
        play.add("Pestilence");
        play.add("Plague Wind");
        play.add("Pongify");
        play.add("Primal Boost");
        play.add("Psionic Blast");
        play.add("Pyrohemia");
        play.add("Repulse");
        play.add("Saltblast");
        
        play.add("Shock");
        play.add("Shriekmaw");
        play.add("Sower of Temptation");
        play.add("Strangling Soot");
        
        play.add("Sunlance");
        play.add("Swords to Plowshares");
        play.add("Take Possession");
        play.add("Tendrils of Corruption");
        play.add("Terror");
        play.add("Threaten");
        play.add("Tribal Flames");
        play.add("Tromp the Domains");
        
        play.add("Volcanic Hammer");
        play.add("Wildsize");
        play.add("Wings of Velis Vel");
        play.add("Wit's End");
        play.add("Wrap in Vigor");
        
        return play;
    }//getMain1PlayCards()
*/    
    private SpellAbility[] getMain1() {
        //Card list of all cards to consider
        CardList hand = new CardList(AllZone.Computer_Hand.getCards());
        
        hand = hand.filter(new CardListFilter() {
        	// Beached As Start
            public boolean addCard(Card c) {
                //Collection<Card> play = playMain1Cards;
            	if (c.getSVar("PlayMain1").equals("TRUE"))
            		return true;
            	
                if(c.isCreature() && (c.getKeyword().contains("Haste")) || c.getKeyword().contains("Exalted")) return true;

                CardList buffed = new CardList(AllZone.Computer_Play.getCards()); //get all cards the computer controls with BuffedBy
                for(int j = 0; j < buffed.size(); j++) {
                    Card buffedcard = buffed.get(j);
                    if (buffedcard.getSVar("BuffedBy").length() > 0) {
                            String buffedby = buffedcard.getSVar("BuffedBy");
                            String bffdby[] = buffedby.split(",");
                            if (c.isValidCard(bffdby)) return true;
                    }       
                }//BuffedBy

                CardList antibuffed = new CardList(AllZone.Human_Play.getCards()); //get all cards the human controls with AntiBuffedBy
                for(int k = 0; k < antibuffed.size(); k++) {
                    Card buffedcard = antibuffed.get(k);
                    if (buffedcard.getSVar("AntiBuffedBy").length() > 0) {
                            String buffedby = buffedcard.getSVar("AntiBuffedBy");
                            String bffdby[] = buffedby.split(",");
                            if (c.isValidCard(bffdby)) return true;
                    }       
                }//AntiBuffedBy

                if(c.isLand()) return false;

                CardList Vengevines = new CardList();
                Vengevines.addAll(AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer).getCards());       
                Vengevines = Vengevines.getName("Vengevine");
                if(Vengevines.size() > 0) {
                CardList Creatures = new CardList();  
                CardList Creatures2 = new CardList();       
                Creatures.addAll(AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer).getCards());
        		for(int i = 0; i < Creatures.size(); i++) {
        			if(Creatures.get(i).getType().contains("Creature") && CardUtil.getConvertedManaCost(Creatures.get(i).getManaCost()) <= 3) {
        				Creatures2.add(Creatures.get(i));
        			}
        		}
                if(Creatures2.size() + Phase.ComputerCreatureSpellCount > 1 && c.getType().contains("Creature") && CardUtil.getConvertedManaCost(c.getManaCost()) <= 3) return true;	
                } // AI Improvement for Vengevine
            	// Beached As End
                	return false;
            }
        });
        CardList all = new CardList();
        all.addAll(hand.toArray());
        all.addAll(AllZone.Computer_Play.getCards());
        
        CardList humanPlayable = new CardList();
        humanPlayable.addAll(AllZone.Human_Play.getCards());
        humanPlayable = humanPlayable.filter(new CardListFilter()
        {
          public boolean addCard(Card c)
          {
            return (c.canAnyPlayerActivate());
          }
        });
        
        all.addAll(humanPlayable.toArray());
        
        return getPlayable(all);
    }//getMain1()
    

    private SpellAbility[] getMain2() {
        //Card list of all cards to consider
        CardList all = new CardList();
        all.addAll(AllZone.Computer_Hand.getCards());
        all.addAll(AllZone.Computer_Play.getCards());
        all.addAll(CardFactoryUtil.getFlashbackCards(AllZone.ComputerPlayer).toArray());
        
        // Prevent the computer from summoning Ball Lightning type creatures during main phase 2
        all = all.getNotKeyword("At the beginning of the end step, sacrifice CARDNAME.");
        
        all = all.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if(c.isLand()) return false;
                return true;
            }
        });
        
        CardList humanPlayable = new CardList();
        humanPlayable.addAll(AllZone.Human_Play.getCards());
        humanPlayable = humanPlayable.filter(new CardListFilter()
        {
          public boolean addCard(Card c)
          {
            return (c.canAnyPlayerActivate());
          }
        });
        all.addAll(humanPlayable.toArray());
        
        return getPlayable(all);
    }//getMain2()
    
    /**
     * Returns the spellAbilities from the card list that the computer is able to play
     */
    private SpellAbility[] getPlayable(CardList l) {
        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for(Card c:l)
            for(SpellAbility sa:c.getSpellAbility())
                //This try/catch should fix the "computer is thinking" bug
                try {
                	sa.setActivatingPlayer(AllZone.ComputerPlayer);
                    if(ComputerUtil.canPayCost(sa) && sa.canPlayAI() && 
                    		(sa.canPlay()))
                    			spellAbility.add(sa);
                } catch(Exception ex) {
                    showError(ex, "There is an error in the card code for %s:%n", c.getName(), ex.getMessage());
                }
        return spellAbility.toArray(new SpellAbility[spellAbility.size()]);
    }
    
    public void declare_attackers_before()
    {
    	 AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_attackers() {
        final Combat c = ComputerUtil.getAttackers();
        c.setAttackingPlayer(AllZone.Combat.getAttackingPlayer());
        c.setDefendingPlayer(AllZone.Combat.getDefendingPlayer());
        
        //check for planeswalker
        Card walker = AllZone.HumanPlayer.getPlaneswalker();
        
        if(walker != null && MyRandom.random.nextBoolean()) {
            c.setPlaneswalker(walker);
            AllZone.pwCombat = c;
        } else AllZone.Combat = c;
        

        Card[] att = c.getAttackers();
        for(int i = 0; i < att.length; i++) {
            if(!att[i].getKeyword().contains("Vigilance")) att[i].tap();
            Log.debug("Computer just assigned " + att[i].getName() + " as an attacker.");
        }
        
        AllZone.Computer_Play.updateObservers();
        

        CombatUtil.showCombat();
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_General.declare_attackers) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_blockers() {
        CardList blockers = new CardList(AllZone.Computer_Play.getCards());
        Combat combat = getCombat(AllZone.pwCombat.getAttackers(), blockers);
        
        combat.setPlaneswalker(AllZone.pwCombat.getPlaneswalker());
        AllZone.pwCombat = combat;
        
        CardList remove = AllZone.pwCombat.getAllBlockers();
        for(int i = 0; i < remove.size(); i++)
            blockers.remove(remove.get(i));
        
        AllZone.Combat = getCombat(AllZone.Combat.getAttackers(), blockers);
        
        CombatUtil.showCombat();
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_General) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    private Combat getCombat(Card[] attackers, CardList availableBlockers) {
        ComputerUtil_Block2 com = new ComputerUtil_Block2(attackers, availableBlockers.toArray(),
                AllZone.ComputerPlayer.getLife());
        
        Combat c = com.getBlockers();
        c.setAttackingPlayer(AllZone.Combat.getAttackingPlayer());
        c.setDefendingPlayer(AllZone.Combat.getDefendingPlayer());
        
        return c;
    }
    
    
    public void declare_blockers_after() {
        
        //AllZone.Phase.nextPhase();
        
        //for debugging: System.out.println("need to nextPhase(ComputerAI_General) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void after_declare_blockers()    {
            /*
    		CardList list = new CardList();
            list.addAll(AllZone.Combat.getAllBlockers().toArray());
            list.addAll(AllZone.pwCombat.getAllBlockers().toArray());
            list = list.filter(new CardListFilter(){
            	public boolean addCard(Card c)
            	{
            		return !c.getCreatureBlockedThisCombat();
            	}
            });
            
            CardList attList = new CardList();
            attList.addAll(AllZone.Combat.getAttackers());
            
            CardList pwAttList = new CardList();
            pwAttList.addAll(AllZone.pwCombat.getAttackers());

            CombatUtil.checkDeclareBlockers(list);
            
            for (Card a:attList){
            	CardList blockList = AllZone.Combat.getBlockers(a);
            	for (Card b:blockList)
            		CombatUtil.checkBlockedAttackers(a, b);
            }
            
            for (Card a:pwAttList){
            	CardList blockList = AllZone.pwCombat.getBlockers(a);
            	for (Card b:blockList)
            		CombatUtil.checkBlockedAttackers(a, b);
            }
        	*/
            
    		if (!AllZone.GameInfo.getAssignedFirstStrikeDamageThisCombat()) {
    			AllZone.Combat.setAssignedFirstStrikeDamage();
    			AllZone.pwCombat.setAssignedFirstStrikeDamage();
    			
    			AllZone.GameInfo.setAssignedFirstStrikeDamageThisCombat(true);
    		}
            
    		AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void end_of_combat()
    {
    	AllZone.Phase.setNeedToNextPhase(true);
    }
    
    //end of Human's turn
    public void end_of_turn() {
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_General.end_of_turn) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    //must shuffle this
    public Card[] getLibrary() {
        //CardFactory cf = AllZone.CardFactory;
        CardList library = new CardList();
        /*
            for(int i = 0; i < 10; i++)
            {
              library.add(cf.getCard("Kodama's Reach", AllZone.ComputerPlayer));
              library.add(cf.getCard("Forest", AllZone.ComputerPlayer));
            }
        */
        return library.toArray();
    }
    
    public void stack_not_empty() {
        //same as Input.stop() method
        //ends the method
        //different than methods because this isn't a phase like Main1 or Declare Attackers
//    ComputerUtil.playCards(); this allows computer to play sorceries when it shouldn't
        
        AllZone.InputControl.resetInput();
        AllZone.InputControl.updateObservers();
    }
}

import static forge.error.ErrorViewer.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;


public class ComputerAI_General implements Computer {
    private boolean          playLand = true;
    private Collection<Card> playMain1Cards;
    
    @SuppressWarnings("unchecked")
    // TreeSet type safety
    public ComputerAI_General() {
        //try to reduce the number of comparisons
        playMain1Cards = new TreeSet(getMain1PlayHand());
    }
    
    public void main1() {
        if(playLand) {
            playLand = false;
            ComputerUtil.playLand();
            for(String effect:AllZone.StateBasedEffects.getStateBasedMap().keySet()) {
                Command com = GameActionUtil.commands.get(effect);
                com.execute();
            }
            GameActionUtil.executeCardStateEffects();
        }
        
//    AllZone.Phase.nextPhase();
        
        playCards(Constant.Phase.Main1);
    }//main1()
    
    public void main2() {
        playLand = true;
        
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
    

    private ArrayList<String> getMain1PlayHand() {
        ArrayList<String> play = new ArrayList<String>();
        play.add("Man-o'-War");
        play.add("Fire Imp");
        play.add("Flametongue Kavu");
        play.add("Briarhorn");
        play.add("Inner-Flame Acolyte");
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
        play.add("Expunge");
        play.add("Faceless Butcher");
        play.add("Feral Lightning");
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
    
    private SpellAbility[] getMain1() {
        //Card list of all cards to consider
        CardList hand = new CardList(AllZone.Computer_Hand.getCards());
        
        hand = hand.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                Collection<Card> play = playMain1Cards;
                if(c.isLand()) return false;
                if(play.contains(c.getName()) || (c.isCreature() && c.getKeyword().contains("Haste"))) return true;
                return false;
            }
        });
        CardList all = new CardList();
        all.addAll(hand.toArray());
        all.addAll(AllZone.Computer_Play.getCards());
        
        return getPlayable(all);
    }//getMain1()
    

    private SpellAbility[] getMain2() {
        //Card list of all cards to consider
        CardList all = new CardList();
        all.addAll(AllZone.Computer_Hand.getCards());
        all.addAll(AllZone.Computer_Play.getCards());
        all.addAll(CardFactoryUtil.getFlashbackCards(Constant.Player.Computer).toArray());
        all = all.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if(c.isLand()) return false;
                return true;
            }
        });
        
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
                    if(sa.canPlayAI() && ComputerUtil.canPayCost(sa)) spellAbility.add(sa);
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
        Card walker = AllZone.GameAction.getPlaneswalker(Constant.Player.Human);
        
        if(walker != null && MyRandom.random.nextBoolean()) {
            c.setPlaneswalker(walker);
            AllZone.pwCombat = c;
        } else AllZone.Combat = c;
        

        Card[] att = c.getAttackers();
        for(int i = 0; i < att.length; i++) {
            if(!att[i].getKeyword().contains("Vigilance")) att[i].tap();
            System.out.println("Computer just assigned " + att[i].getName() + " as an attacker.");
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
                AllZone.Computer_Life.getLife());
        
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
              library.add(cf.getCard("Kodama's Reach", Constant.Player.Computer));
              library.add(cf.getCard("Forest", Constant.Player.Computer));
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

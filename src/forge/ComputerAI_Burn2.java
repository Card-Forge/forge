package forge;
//import java.util.*;
import javax.swing.SwingUtilities;

import forge.error.ErrorViewer;


public class ComputerAI_Burn2 implements Computer {
    private volatile int numberPlayLand = CardFactoryUtil.getCanPlayNumberOfLands(Constant.Player.Computer);
    
    public void main1() {
        if(numberPlayLand > 0 ) {
        	numberPlayLand--;
            ComputerUtil.playLand();
            
            Card c[] = AllZone.Computer_Hand.getCards();
            if(c.length != 0) {
//        System.out.print("hand - ");
//        for(int i = 0; i < c.length; i++)
//          System.out.print(c[i] +" ");
//        System.out.println();
            }
        }
        Runnable run = new Runnable() {
            @SuppressWarnings("null")
            public void run() {
                synchronized(ComputerAI_Burn2.this) {
                    if(AllZone.Stack.size() == 0
                            && AllZone.Phase.is(Constant.Phase.Main1, Constant.Player.Computer)) {
                        SpellAbilityList list = null; //Move3.getBestMove();
                        if(list.toString().trim().length() != 0) {
//              System.out.println("Spell List - " +list);
                        }
                        if(list.size() != 0) {
                            SpellAbility sa = list.get(0);
                            if(ComputerUtil.canPlay(sa) && sa.canPlay()) {
                                if(sa.isSpell()) {
                                    AllZone.Computer_Hand.remove(sa.getSourceCard());
                                    sa.getSourceCard().comesIntoPlay();
                                }
                                
                                if(sa instanceof Ability_Tap) sa.getSourceCard().tap();
                                
                                ComputerUtil.payManaCost(sa);
                                AllZone.Stack.add(sa);
                            } else throw new RuntimeException("ComputerAI_Burn2 error, cannot pay for spell "
                                    + sa.getSourceCard() + " is spell? " + sa.isSpell() + "  sa.canPlayAI()-"
                                    + sa.canPlayAI() + " sa.canPlay()-" + sa.canPlay()
                                    + " CardUtil.getConvertedManaCost(sa)-" + CardUtil.getConvertedManaCost(sa)
                                    + " ComputerUtil.getAvailableMana().size()-"
                                    + ComputerUtil.getAvailableMana().size());
                        }//list.size != 0
                        if(AllZone.Stack.size() == 0
                                && AllZone.Phase.is(Constant.Phase.Main1, Constant.Player.Computer)) {
                            
                            //AllZone.Phase.nextPhase();
                            //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn2.running) = true; Note, this is untested, did it work?");
                            AllZone.Phase.setNeedToNextPhase(true);
                        }
                        
                    }//if
                }//synchronized
            }//run()
        };//Runnable
        try {
            SwingUtilities.invokeLater(run);
//      Thread thread = new Thread(run);
//      thread.start();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ComputerAI_Burn : main1() error, " + ex);
        }
        
    }//main1()
    
    public void main2() {
    	numberPlayLand = CardFactoryUtil.getCanPlayNumberOfLands(Constant.Player.Computer);
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn2.main2) = true; Note, this is not tested, did it work?");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_attackers_before()
    {
    	 AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_attackers() {
        final Combat c = ComputerUtil.getAttackers();
        c.setAttackingPlayer(AllZone.Combat.getAttackingPlayer());
        c.setDefendingPlayer(AllZone.Combat.getDefendingPlayer());
        AllZone.Combat = c;
        
        Card[] att = c.getAttackers();
        for(int i = 0; i < att.length; i++)
            att[i].tap();
        
        AllZone.Computer_Play.updateObservers();
        
        CombatUtil.showCombat();
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn2.declare_attackers) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_blockers() {
        Combat c = ComputerUtil.getBlockers();
        c.setAttackingPlayer(AllZone.Combat.getAttackingPlayer());
        c.setDefendingPlayer(AllZone.Combat.getDefendingPlayer());
        AllZone.Combat = c;
        
        CombatUtil.showCombat();
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn2.declare_blockers) = true");
        AllZone.Phase.setNeedToNextPhase(true);
        
        //System.out.println(Arrays.asList(c.getAttackers()));
        //System.out.println(Arrays.asList(c.getBlockers()));
    }
    
    public void declare_blockers_after() {
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn2.declre_blockers_after) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    //end of Human's turn
    public void end_of_turn() {
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn2.end_of_turn) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    //must shuffle this
    public Card[] getLibrary() {
        CardFactory cf = AllZone.CardFactory;
        CardList library = new CardList();
        for(int i = 0; i < 4; i++) {
            library.add(cf.getCard("Lightning Bolt", Constant.Player.Computer));
            library.add(cf.getCard("Shock", Constant.Player.Computer));
            library.add(cf.getCard("Steel Wall", Constant.Player.Computer));
        }
        for(int i = 0; i < 3; i++) {
            library.add(cf.getCard("Char", Constant.Player.Computer));
            library.add(cf.getCard("Shock", Constant.Player.Computer));
//      library.add(cf.getCard("Nevinyrral's Disk", Constant.Player.Computer));
        }
        for(int i = 0; i < 2; i++) {
            library.add(cf.getCard("Pyroclasm", Constant.Player.Computer));
//      library.add(cf.getCard("Hidetsugu's Second Rite", Constant.Player.Computer));
//      library.add(cf.getCard("Char", Constant.Player.Computer));
            library.add(cf.getCard("Flamebreak", Constant.Player.Computer));
        }
        
        library.add(cf.getCard("Lava Spike", Constant.Player.Computer));
        

        //	library.add(cf.getCard("Tanglebloom", Constant.Player.Computer));
        
        for(int i = 0; i < 17; i++)
            library.add(cf.getCard("Mountain", Constant.Player.Computer));
        
        if(library.size() != 40)
            throw new RuntimeException("ComputerAI_Burn : getLibrary() error, library size is " + library.size());
        
        return library.toArray();
    }
    
    public void addNumberPlayLands(int n)
    {
    	numberPlayLand += n;
    }
    
    public void setNumberPlayLands(int n)
    {
    	numberPlayLand = n;
    }
    
    public void stack_not_empty() {
        //same as Input.stop() method
        //ends the method
        //different than methods because this isn't a phase like Main1 or Declare Attackers
        AllZone.InputControl.resetInput();
        AllZone.InputControl.updateObservers();
    }
}
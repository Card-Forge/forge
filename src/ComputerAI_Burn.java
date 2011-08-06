//import java.util.*;
import javax.swing.SwingUtilities;

import forge.error.ErrorViewer;


public class ComputerAI_Burn implements Computer {
    private volatile boolean playLand = true;
    
    public void main1() {
        if(playLand) {
            playLand = false;
            ComputerUtil.playLand();
        }
        Runnable run = new Runnable() {
            public void run() {
                synchronized(ComputerAI_Burn.this) {
                    if(AllZone.Stack.size() == 0) {
                        SpellAbility[] all = ComputerUtil.getSpellAbility();
                        
                        for(int i = 0; i < all.length; i++) {
                            if(ComputerUtil.canPayCost(all[i]) && all[i].canPlay()) {
                                if(all[i].isSpell()) AllZone.Computer_Hand.remove(all[i].getSourceCard());
                                
                                if(all[i] instanceof Ability_Tap) {
                                    all[i].getSourceCard().tap();
                                }
                                
                                ComputerUtil.payManaCost(all[i]);
                                String name = all[i].getSourceCard().getName();
                                if(name.equals("Shock") || name.equals("Lightning Bolt") || name.equals("Char")) all[i].setTargetPlayer(Constant.Player.Human);
                                else all[i].chooseTargetAI();
                                AllZone.Stack.add(all[i]);
                                return;
                            }
                        }//for
                        if(AllZone.Phase.is(Constant.Phase.Main1, Constant.Player.Computer)) {
                            
                            //AllZone.Phase.nextPhase();
                            //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn.runnable) = true; Note: this is untested, did it work?");
                            AllZone.Phase.setNeedToNextPhase(true);
                        }
                    }//if
                }//synchronized
            }//run()
        };//Runnable
        try {
            SwingUtilities.invokeLater(run);
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ComputerAI_Burn : main1() error, " + ex);
        }
    }//main1()
    
    public void main2() {
        playLand = true;
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn.main2) = true; Note, this is untested, did it work?");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_attackers_before() {
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_attackers() {
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn.declare_attackers) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_blockers() {
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn.declare_blockers()) = true");
        AllZone.Phase.setNeedToNextPhase(true);
        
    }
    
    public void declare_blockers_after() {
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn.declare_blockers_after()) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    //end of Human's turn
    public void end_of_turn() {
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(ComputerAI_Burn.end_of_turn()) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    //must shuffle this
    public Card[] getLibrary() {
        CardFactory cf = AllZone.CardFactory;
        CardList library = new CardList();
        for(int i = 0; i < 4; i++) {
            library.add(cf.getCard("Lightning Bolt", Constant.Player.Computer));
            library.add(cf.getCard("Shock", Constant.Player.Computer));
            library.add(cf.getCard("Pyroclasm", Constant.Player.Computer));
        }
        for(int i = 0; i < 3; i++) {
            library.add(cf.getCard("Nevinyrral's Disk", Constant.Player.Computer));
            library.add(cf.getCard("Lava Spike", Constant.Player.Computer));
        }
        for(int i = 0; i < 2; i++) {
            library.add(cf.getCard("Hidetsugu's Second Rite", Constant.Player.Computer));
            library.add(cf.getCard("Char", Constant.Player.Computer));
            library.add(cf.getCard("Flamebreak", Constant.Player.Computer));
            library.add(cf.getCard("Mox Ruby", Constant.Player.Computer));
        }
        

        //	library.add(cf.getCard("Tanglebloom", Constant.Player.Computer));
        
        for(int i = 0; i < 14; i++)
            library.add(cf.getCard("Mountain", Constant.Player.Computer));
        
        if(library.size() != 40) throw new RuntimeException(
                "ComputerAI_Burn : getLibrary() error, library size is " + library.size());
        
        return library.toArray();
    }
    
    public void stack_not_empty() {
        //same as Input.stop() method
        //ends the method
        //different than methods because this isn't a phase like Main1 or Declare Attackers
        AllZone.InputControl.resetInput();
        AllZone.InputControl.updateObservers();
    }
}

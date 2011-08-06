
package forge;


import java.util.ArrayList;


public class Input_Draw extends Input {
    private static final long serialVersionUID = -2341125041806280507L;
    
    @Override
    public void showMessage() {
        if(AllZone.Phase.getActivePlayer().equals(Constant.Player.Computer)) {
            AllZone.GameAction.drawCard(Constant.Player.Computer);
            
            //AllZone.Phase.nextPhase();
            //for debugging: System.out.println("need to nextPhase(from Input_Draw on computer's draw) = true");
            AllZone.Phase.setNeedToNextPhase(true);
            return;
        }
        
        //check if human should skip their draw phase
        CardList humanCards = new CardList();
        humanCards.addAll(AllZone.Human_Play.getCards());
        boolean humanSkipsDrawPhase = humanCards.containsName("Necropotence")
                || humanCards.containsName("Yawgmoth's Bargain");
        
        if(AllZone.Phase.getPhase().equals(Constant.Phase.Draw) && humanSkipsDrawPhase) {
            //Input_Main.canPlayLand = true;
            AllZone.GameInfo.setHumanCanPlayNumberOfLands(CardFactoryUtil.getCanPlayNumberOfLands(Constant.Player.Human));
            AllZone.GameInfo.setHumanPlayedFirstLandThisTurn(false);
            AllZone.Phase.setNeedToNextPhase(true);
            
        } else { //continue with draw phase
            boolean drawCard = true;
            if(0 < getDredge().size()) {
                String choices[] = {"Yes", "No"};
                Object o = AllZone.Display.getChoice("Do you want to dredge?", choices);
                if(o.equals("Yes")) {
                    drawCard = false;
                    Card c = (Card) AllZone.Display.getChoice("Select card to dredge", getDredge().toArray());
                    
                    //might have to make this more sophisticated
                    //dredge library, put card in hand
                    AllZone.Human_Hand.add(c);
                    AllZone.Human_Graveyard.remove(c);
                    
                    for(int i = 0; i < getDredgeNumber(c); i++) {
                        Card c2 = AllZone.Human_Library.get(0);
                        AllZone.Human_Library.remove(0);
                        AllZone.Human_Graveyard.add(c2);
                    }
                }
            }//if(0 < getDredge().size())
            
            if(drawCard && AllZone.Phase.getTurn() > 1) AllZone.GameAction.drawCard(Constant.Player.Human);
            
            if(AllZone.Phase.getPhase().equals(Constant.Phase.Draw)) {
                //Input_Main.canPlayLand = true;
                AllZone.GameInfo.setHumanCanPlayNumberOfLands(CardFactoryUtil.getCanPlayNumberOfLands(Constant.Player.Human));
                AllZone.GameInfo.setHumanPlayedFirstLandThisTurn(false);
                
                //AllZone.Phase.nextPhase();
                //for debugging: System.out.println("need to nextPhase(from Input_Draw on human's draw) = true");
                AllZone.Phase.setNeedToNextPhase(true);
            } else stop();
        }
    } //end necro check
    
    public ArrayList<Card> getDredge() {
        ArrayList<Card> dredge = new ArrayList<Card>();
        Card c[] = AllZone.Human_Graveyard.getCards();
        
        for(int outer = 0; outer < c.length; outer++) {
            ArrayList<String> a = c[outer].getKeyword();
            for(int i = 0; i < a.size(); i++)
                if(a.get(i).toString().startsWith("Dredge")) {
                    if(AllZone.Human_Library.size() >= getDredgeNumber(c[outer])) dredge.add(c[outer]);
                }
        }
        return dredge;
    }//hasDredge()
    
    public int getDredgeNumber(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith("Dredge")) {
                String s = a.get(i).toString();
                return Integer.parseInt("" + s.charAt(s.length() - 1));
            }
        
        throw new RuntimeException("Input_Draw : getDredgeNumber() card doesn't have dredge - " + c.getName());
    }//getDredgeNumber()
}

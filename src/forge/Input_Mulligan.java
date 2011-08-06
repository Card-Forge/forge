
package forge;


//import javax.swing.*; //unused


public class Input_Mulligan extends Input {
    private static final long serialVersionUID = -8112954303001155622L;
    
    @Override
    public void showMessage() {
        ButtonUtil.enableAll();
        AllZone.Display.getButtonOK().setText("No");
        AllZone.Display.getButtonCancel().setText("Yes");
        AllZone.Display.showMessage("Do you want to Mulligan?");
    }
    
    @Override
    public void selectButtonOK() {
        end();
    }
    
    @Override
    public void selectButtonCancel() {
        Card[] hand = AllZone.Human_Hand.getCards();
        for(int i = 0; i < hand.length; i++) {
            AllZone.Human_Library.add(hand[i]);
            AllZone.Human_Hand.remove(hand[i]);
        }
        
        for(int i = 0; i < 100; i++)
            AllZone.GameAction.shuffle(Constant.Player.Human);
        
        int newHand = hand.length - 1;
        for(int i = 0; i < newHand; i++)
            AllZone.GameAction.drawCard(Constant.Player.Human);
        
        if(newHand == 1) end();
    }//selectButtonOK()
    
    void end() {
        ButtonUtil.reset();
        stop();
    }
}

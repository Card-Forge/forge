
package forge;

import java.util.LinkedList;

import com.esotericsoftware.minlog.Log;


public class Input_Cleanup extends Input {
    private static final long serialVersionUID = -4164275418971547948L;
    
    private static int NextHandSizeStamp = 0;
    
    public static int GetHandSizeStamp() {
       return NextHandSizeStamp++;
    }
    
    private static LinkedList<HandSizeOp> handSizeOperations = new LinkedList<HandSizeOp>();
    private static int MaxHandSize = 7;
    
    public static void clearHandSizeOperations() {
        handSizeOperations.clear();
        NextHandSizeStamp = 0;
    }

    public static void sortHandSizeOperations() {
       if(handSizeOperations.size() < 2) {
          return;
       }
       
       Object arr[] = handSizeOperations.toArray();
       int changes = 1;

       while(changes > 0) {
          changes = 0;
          for(int i=1;i<arr.length;i++) {
              if(((HandSizeOp)arr[i]).hsTimeStamp < ((HandSizeOp)arr[i-1]).hsTimeStamp) {
                 HandSizeOp tmp = (HandSizeOp)arr[i];
                 arr[i] = arr[i-1];
                 arr[i-1] = tmp;
                 changes++;
              }
           }
       }
        handSizeOperations.clear();
        for(int i=0;i<arr.length;i++) {
                handSizeOperations.add((HandSizeOp)arr[i]);
        }
    }

    public static void calcMaxHandSize() {
       
       int ret = 7;
       for(int i=0;i<handSizeOperations.size();i++)
       {
          if(handSizeOperations.get(i).Mode.equals("="))
          {
             ret = handSizeOperations.get(i).Amount;
          }
          else if(handSizeOperations.get(i).Mode.equals("+") && ret >= 0)
          {
             ret = ret + handSizeOperations.get(i).Amount;
          }
          else if(handSizeOperations.get(i).Mode.equals("-") && ret >= 0)
          {
             ret = ret - handSizeOperations.get(i).Amount;
             if(ret < 0) {
                ret = 0;
             }
          }
       }
       MaxHandSize = ret;
    }
    public static void addHandSizeOperation(HandSizeOp theNew)
    {
       handSizeOperations.add(theNew);
    }
    public static void removeHandSizeOperation(int timestamp)
    {
       for(int i=0;i<handSizeOperations.size();i++)
       {
          if(handSizeOperations.get(i).hsTimeStamp == timestamp)
          {
             handSizeOperations.remove(i);
             break;
          }
       }
    }
    
    @Override
    public void showMessage() {
        ButtonUtil.disableAll();
        int n = AllZone.Human_Hand.getCards().length;
        
        //MUST showMessage() before stop() or it will overwrite the next Input's message
        StringBuffer sb = new StringBuffer();
        sb.append("Cleanup Phase: You can only have a maximum of ").append(MaxHandSize);
        sb.append(" cards, you currently have ").append(n).append(" cards in your hand - select a card to discard");
        AllZone.Display.showMessage(sb.toString());
        
        // AllZone.Display.showMessage("Cleanup Phase: You can only have a maximum of 7 cards, you currently have "
        //         + n + " cards in your hand - select a card to discard");
        
        //goes to the next phase
        // if(n <= 7) {
        if(n <= MaxHandSize || MaxHandSize == -1) {
            CombatUtil.removeAllDamage();
            
            //AllZone.Phase.nextPhase();
            //for debugging: System.out.println("need to nextPhase(Input_Cleanup.showMessage(), n<=7) = true");
            AllZone.Phase.setNeedToNextPhase(true);
        }
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        if(zone.is(Constant.Zone.Hand, AllZone.HumanPlayer)) {
            card.getController().discard(card, null);
            showMessage();
            //We need a nextPhase here or else it will never get the needToNextPhase from showMessage() (because there isn't a nextPhsae() in the stack).
            Log.debug("There better not be a nextPhase() on the stack!");
            if(AllZone.Phase != null) {
                if(AllZone.Phase.isNeedToNextPhase()) {
                    AllZone.Phase.setNeedToNextPhase(false);
                    AllZone.Phase.nextPhase();
                }
            }
        }
    }//selectCard()
}

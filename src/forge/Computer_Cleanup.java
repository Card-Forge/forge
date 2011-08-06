
package forge;


import java.util.LinkedList;
import java.util.Random;


public class Computer_Cleanup extends Input {
	
    private static LinkedList<HandSizeOp> handSizeOperations = new LinkedList<HandSizeOp>();
    private static int MaxHandSize = 7;
    
    public static void clearHandSizeOperations() {
        handSizeOperations.clear();
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

	
    private static final long serialVersionUID = -145924458598185438L;
    
    @Override
    public void showMessage() {
        Random r = new Random();
        Card[] c = AllZone.Computer_Hand.getCards();
        // while(7 < c.length) {
        while(MaxHandSize < c.length && MaxHandSize != -1) {
        	c[r.nextInt(c.length)].getController().discard(c[r.nextInt(c.length)], null);
            c = AllZone.Computer_Hand.getCards();
        }
        
        CombatUtil.removeAllDamage();
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(Computer_Cleanup.showMessage()) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
}

package forge;
import java.util.*;

//when something is put on the stack, this objects observers the Stack
//and pushes an Input so the other player can play
//for example: computer plays a spell, StackObserver pushes and Input so the human can respond
public class StackObserver implements Observer
{
    public StackObserver (MagicStack stack) 
    {
	stack.addObserver(this);
    }    
    public void update(Observable a, Object b)
    {
//	if(AllZone.Stack.size() > 0)
//	    AllZone.InputControl.setInput(new Input_Instant(Input_Instant.NO_NEXT_PHASE, "Spell or Abilities are on the Stack"));
    }   
}

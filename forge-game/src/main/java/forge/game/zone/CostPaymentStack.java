package forge.game.zone;

import forge.game.cost.IndividualCostPaymentInstance;

import java.util.Iterator;
import java.util.Stack;

/*
 * simple stack wrapper class for tracking cost payments (mainly for triggers to use)
 */
public class CostPaymentStack implements Iterable<IndividualCostPaymentInstance>{

    private Stack<IndividualCostPaymentInstance> stack;

    public CostPaymentStack() {
        stack = new Stack<IndividualCostPaymentInstance>();
    }

    public IndividualCostPaymentInstance push(IndividualCostPaymentInstance costPaymentInstance) {
        return stack.push(costPaymentInstance);
    }

    public IndividualCostPaymentInstance pop(){
        return stack.pop();
    }

    public IndividualCostPaymentInstance peek() {
        if (stack.empty()) {
            return null;
        }

        return stack.peek();
    }

    public void clear() {
        stack.clear();
    }

	@Override
	public Iterator<IndividualCostPaymentInstance> iterator() {
		return stack.iterator();
	}
    
    
}

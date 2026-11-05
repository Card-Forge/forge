package forge.game.zone;

import java.util.Iterator;
import java.util.Stack;

import forge.game.cost.CostPart;
import forge.game.cost.CostPayment;

/*
 * simple stack wrapper class for tracking cost payments (mainly for triggers to use)
 */
public class CostPaymentStack implements Iterable<CostPaymentStack.Entry> {

    private Stack<Entry> stack;

    public CostPaymentStack() {
        stack = new Stack<>();
    }

    public Entry push(final CostPart cost, final CostPayment payment) {
        return stack.push(new Entry(cost, payment));
    }

    public Entry pop() {
        return stack.pop();
    }

    public Entry peek() {
        if (stack.empty()) {
            return null;
        }

        return stack.peek();
    }

    public void clear() {
        stack.clear();
    }

    @Override
    public Iterator<Entry> iterator() {
        return stack.iterator();
    }

    @Override
    public String toString() {
        return stack.toString();
    }

    public record Entry(CostPart cost, CostPayment payment) {

    }
}

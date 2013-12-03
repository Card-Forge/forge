package forge.util;

public abstract class Evaluator<T> implements Runnable {
    private T result;
    
    @Override
    public final void run() {
        result = evaluate();
    }
    
    public abstract T evaluate();
    
    public T getResult() {
        return result;
    }
}

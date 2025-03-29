package forge.gamesimulationtests.util;

/**
 * Base class for all constraint types in the system.
 * Provides common functionality and interface for constraints.
 */
public abstract class Constraint<T> {
    protected final T min;
    protected final T max;
    
    protected Constraint(T min, T max) {
        this.min = min;
        this.max = max;
    }
    
    /**
     * Gets the minimum allowed value
     * @return The minimum value
     */
    public T getMin() {
        return min;
    }
    
    /**
     * Gets the maximum allowed value
     * @return The maximum value
     */
    public T getMax() {
        return max;
    }
    
    /**
     * Checks if a value satisfies this constraint
     * @param value The value to check
     * @return true if the value is within the valid range
     */
    public abstract boolean satisfies(T value);
    
    @Override
    public abstract String toString();
    
    @Override
    public abstract boolean equals(Object o);
    
    @Override
    public abstract int hashCode();
} 
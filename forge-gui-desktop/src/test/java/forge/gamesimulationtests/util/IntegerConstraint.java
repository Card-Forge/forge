package forge.gamesimulationtests.util;

import java.util.Objects;

/**
 * Represents a constraint on an integer value, ensuring it falls within a specified range.
 * This class enforces proper encapsulation and validation of its internal state.
 */
public class IntegerConstraint extends Constraint<Integer> {
	private static final int MIN_DEFAULT = 0;
	private static final int MAX_DEFAULT = Integer.MAX_VALUE;
	
	// Factory methods for common constraint types
	public static final IntegerConstraint ONE = new IntegerConstraint(1);
	public static final IntegerConstraint ZERO_OR_MORE = new IntegerConstraint(0, Integer.MAX_VALUE);
	public static final IntegerConstraint ZERO_OR_ONE = new IntegerConstraint(0, 1);
	
	/**
	 * Creates a new IntegerConstraint with default range (0 to Integer.MAX_VALUE)
	 */
	public IntegerConstraint() {
		this(MIN_DEFAULT, MAX_DEFAULT);
	}
	
	/**
	 * Creates a new IntegerConstraint with specified minimum value
	 * @param min The minimum allowed value
	 * @throws IllegalArgumentException if min is negative
	 */
	public IntegerConstraint(int min) {
		this(min, MAX_DEFAULT);
	}
	
	/**
	 * Creates a new IntegerConstraint with specified range
	 * @param min The minimum allowed value
	 * @param max The maximum allowed value
	 * @throws IllegalArgumentException if min is negative or max is less than min
	 */
	public IntegerConstraint(int min, int max) {
		super(min, max);
		validateRange(min, max);
	}
	
	/**
	 * Validates the range parameters
	 * @param min The minimum value to validate
	 * @param max The maximum value to validate
	 * @throws IllegalArgumentException if the range is invalid
	 */
	private void validateRange(int min, int max) {
		if (min < MIN_DEFAULT) {
			throw new IllegalArgumentException("Minimum value cannot be negative");
		}
		if (max < min) {
			throw new IllegalArgumentException("Maximum value must be greater than or equal to minimum value");
		}
	}
	
	/**
	 * Checks if a value satisfies this constraint
	 * @param value The value to check
	 * @return true if the value is within the valid range
	 */
	@Override
	public boolean satisfies(Integer value) {
		return value >= min && value <= max;
	}
	
	/**
	 * Gets a string representation of the constraint
	 * @return A string in the format "[min,max]"
	 */
	@Override
	public String toString() {
		if (min.equals(max)) {
			return String.valueOf(min);
		}
		return "[" + min + "," + max + "]";
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		IntegerConstraint that = (IntegerConstraint) o;
		return min.equals(that.min) && max.equals(that.max);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(min, max);
	}
}

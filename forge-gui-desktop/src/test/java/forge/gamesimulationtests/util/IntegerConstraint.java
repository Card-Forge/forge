package forge.gamesimulationtests.util;

public class IntegerConstraint {
	public static final IntegerConstraint ONE = new IntegerConstraint( 1 );
	public static final IntegerConstraint ZERO_OR_MORE = new IntegerConstraint( 0, Integer.MAX_VALUE );
	public static final IntegerConstraint ZERO_OR_ONE = new IntegerConstraint( 0, 1 );
	
	private final int min;
	private final int max;
	
	public IntegerConstraint( int value ) {
		this( value, value );
	}
	
	public IntegerConstraint( int min, int max ) {
		this.min = min;
		this.max = max;
	}
	
	public boolean matches( int amount ) {
		return min <= amount && amount <= max;
	}
	
	@Override
	public String toString() {
		if (min == max) {
			return String.valueOf( min );
		}
		return "between " + min + " and " + max;
	}
}

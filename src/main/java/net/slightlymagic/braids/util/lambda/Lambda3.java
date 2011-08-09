package net.slightlymagic.braids.util.lambda;

public abstract class Lambda3<R,A1,A2,A3> implements Lambda<R> {

	public abstract R apply(A1 arg1, A2 arg2, A3 arg3);
	
	@SuppressWarnings("unchecked")
	//TODO @Override
	public R apply(Object[] args) {
		return apply((A1) args[0], (A2) args[1], (A3) args[2]);
	}

}

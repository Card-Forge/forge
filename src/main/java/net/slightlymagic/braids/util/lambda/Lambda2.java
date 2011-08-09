package net.slightlymagic.braids.util.lambda;

public abstract class Lambda2<R,A1,A2> implements Lambda<R> {

	public abstract R apply(A1 arg1, A2 arg2);
	
	@SuppressWarnings("unchecked")
	//TODO @Override
	public R apply(Object[] args) {
		return apply((A1) args[0], (A2) args[1]);
	}

}

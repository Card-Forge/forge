package net.slightlymagic.braids.util.lambda;

public abstract class Lambda1<R,A1> implements Lambda<R> {

	public abstract R apply(A1 arg1);

	@SuppressWarnings("unchecked")
	//TODO @Override
	public R apply(Object[] args) {
		return apply((A1) args[0]);
	}

}

package net.slightlymagic.braids.util.lambda;

public interface Thunk<T> {
	public abstract T apply();
}

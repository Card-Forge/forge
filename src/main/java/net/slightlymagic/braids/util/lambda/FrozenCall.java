package net.slightlymagic.braids.util.lambda;

import net.slightlymagic.braids.util.UtilFunctions;
import static net.slightlymagic.braids.util.UtilFunctions.checkNotNull;

/**
 * This embodies a promise to invoke a certain method at a later time; the 
 * FrozenCall remembers the arguments to use and the return type.
 *
 * @param <T>  the return type of apply
 * 
 * @see Thunk
 */
public class FrozenCall<T> implements Thunk<T> {
	private Lambda<T> proc;
	private Object[] args;

	public FrozenCall(Lambda<T> proc, Object[] args) {
		checkNotNull("proc", proc);
		checkNotNull("args", args);
		
		this.proc = proc;
		this.args = args;
	}

	public T apply() {
		return proc.apply(args);
	}


	@Override
	public boolean equals(Object obj) {
		FrozenCall<T> that = UtilFunctions.checkNullOrNotInstance(this, obj);
		if (that == null)  return false;
		else if (!this.proc.equals(that.proc))  return false;
		else if (this.args.length != that.args.length)  return false;

		for (int i = 0; i < args.length; i++) {
			if (this.args[i] == null && that.args[i] != null)  return false;
			else if (!this.args[i].equals(that.args[i]))  return false;
		}
		
		return true;
	}
}

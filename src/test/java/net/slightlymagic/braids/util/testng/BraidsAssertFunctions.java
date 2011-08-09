package net.slightlymagic.braids.util.testng;

import net.slightlymagic.braids.util.ClumsyRunnable;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A collection of assert functions that go with TestNG.
 */
@Test(groups = {"UnitTest"}) 
public class BraidsAssertFunctions {

	/** Do not instantiate.*/
	private BraidsAssertFunctions() {;}
	
	/** 
	 * Assert that a function (ClumsyRunnable) throws the given exception, or
	 * a subclass of it.
	 * 
	 * @param exnClass  the exception we expect
	 * @param withScissors  the code to run
	 */
	public static void assertThrowsException(
			@SuppressWarnings("rawtypes") Class exnClass, 
			ClumsyRunnable withScissors) 
	{
		try {
			withScissors.run();
		}
		catch (Exception exn) {
			if (!exnClass.isInstance(exn)) {
				Assert.fail("caught exception " + exn.getClass().getName() + 
						", but expected " + exnClass.getName());
			}
			
			return;  //success
		}
		
		Assert.fail("expected exception " + exnClass.getName() + ", but none was thrown");
	}
}

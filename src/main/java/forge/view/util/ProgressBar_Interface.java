package forge.view.util;

/**
 * Interface for a progress monitor that can have multiple phases
 * and periodically update its UI.
 * 
 * All times must be in seconds; absolute times are measured in seconds since
 * 01 Jan 1970 00:00:00 UTC (GMT) a la (new Date().getTime()/1000).
 */
public interface ProgressBar_Interface {

	/**
	 * Destroy this progress monitor, making it no longer usable and/or 
	 * visible.
	 */
	public void dispose();
	
	/** 
	 * @return the total number of phases monitored by this object.
	 */
	public abstract int getNumPhases();

	/**
	 * @return the approximate minimum interval in seconds at which the UI
	 * should be updated.
	 */
	public abstract float getMinUpdateIntervalSec();

	/**
	 * @return the current phase number; this is never less than 1 (one).
	 */
	public abstract int getCurrentPhase();

	/**
	 * @return the number of units (an intentionally vague amount) completed
	 * so far in the current phase.
	 */
	public abstract long getUnitsCompletedSoFarThisPhase();

	/**
	 * @return the total units we expect to process in this phase
	 */
	public abstract long getTotalUnitsThisPhase();

	/**
	 * @return the time in absolute seconds since the UI was last updated
	 */
	public abstract long getLastUIUpdateTime();

	/**
	 * @return the time in absolute seconds at which the first phase started
	 */
	public abstract long getPhaseOneStartTime();

	/**
	 * @return the time in absolute seconds at which the current phase started
	 */
	public abstract long getCurrentPhaseStartTime();

	/**
	 * @param value
	 *            the approximate time in relative seconds at which the UI
	 *            should be updated periodically
	 */
	public abstract void setMinUpdateIntervalSec(float value);

	/**
	 * @param value the total number of units expected to processed in this 
	 * phase
	 */
	public abstract void setTotalUnitsThisPhase(long value);

	/**
	 * Resulting string does not contain a percent sign.
	 * 
	 * @return the percentage completion of this phase as a String with no
	 * percent sign.
	 */
	public abstract String getPercentCompleteOfThisPhaseAsString();

	/**
	 * Resulting string does not contain a percent sign.
	 * 
	 * @return the percentage completion at this point, taking into account all
	 * phases and phase-weights, as a String with no percent sign.
	 */
	public abstract String getTotalPercentCompleteAsString();

	/**
	 * May return "unknown"
	 */
	public abstract String getRelativeETAAsString(boolean thisPhaseOnly);

	/**
	 * May return "unknown"
	 */
	public abstract String getAbsoluteETAAsLocalTimeString(boolean thisPhaseOnly);

	/**
	 * Note this will NOT advance the phase.  
	 * To do that, use markCurrentPhaseAsComplete().
	 */
	public abstract void incrementUnitsCompletedThisPhase(long numUnits);

	/**
	 * Returns a boolean, whether or not to display the updated information.
	 * This throttles the update so it doesn't refresh so fast that it is
	 * unreadable.  Implementers should call this method from their own
	 * incrementUnitsCompletedThisPhase method.
	 * 
	 * If we have just reached 100% for (the current phase, we return true, 
	 * even if it would otherwise be too soon to update the UI.
	 */
	public abstract boolean shouldUpdateUI();

	/**
	 * This is the only way to advance the phase number.
	 * It automatically "starts the clock" for the next phase.
	 * 
	 * @param totalUnitsNextPhase  if unknown, use zero (0), and be sure to call
	 * setTotalUnitsThisPhase() soon after.
	 */
	public abstract void markCurrentPhaseAsComplete(long totalUnitsNextPhase);

	/**
	 * Attempt to display a message to the user; not all implementations 
	 * support this.  
	 * 
	 * If they do not, they may silently ignore this call.
	 * 
	 * @param message  the message to display
	 */
	public abstract void sendMessage(String message);

	
	/**
	 * Mark the current phase as having an exponential rate; such phases
	 * reach their totalUnits slower and slower as they process more units.
	 * 
	 * By default, a phase is considered to be linear, meaning this value is
	 * 1.0f.
	 * 
	 * @param value  usually less than 1.0f; often determined empirically.
	 */
	public abstract void setCurrentPhaseAsExponential(float value);

	/**
	 * @return the exponent for this phase
	 */
	public abstract float getCurrentPhaseExponent();

}

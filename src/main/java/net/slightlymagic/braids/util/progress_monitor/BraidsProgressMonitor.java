/*
 * The files in the directory "net/slightlymagic/braids" and in all subdirectories of it (the "Files") are
 * Copyright 2011 Braids Cabal-Conjurer. They are available under either Forge's
 * main license (the GNU Public License; see LICENSE.txt in Forge's top directory)
 * or under the Apache License, as explained below.
 *
 * The Files are additionally licensed under the Apache License, Version 2.0 (the
 * "Apache License"); you may not use the files in this directory except in
 * compliance with one of its two licenses.  You may obtain a copy of the Apache
 * License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions and
 * limitations under the Apache License.
 *
 */
package net.slightlymagic.braids.util.progress_monitor;

/**
 * Interface for a progress monitor that can have multiple phases and
 * periodically update its UI.
 * 
 * All times must be in seconds; absolute times are measured in seconds since 01
 * Jan 1970 00:00:00 UTC (GMT) a la (new Date().getTime()/1000).
 */
public interface BraidsProgressMonitor {

    /**
     * Destroy this progress monitor, making it no longer usable and/or visible.
     */
    void dispose();

    /**
     * Gets the num phases.
     * 
     * @return the total number of phases monitored by this object.
     */
    int getNumPhases();

    /**
     * Gets the min update interval sec.
     * 
     * @return the approximate minimum interval in seconds at which the UI
     *         should be updated.
     */
    float getMinUpdateIntervalSec();

    /**
     * Gets the current phase.
     * 
     * @return the current phase number; this is never less than 1 (one).
     */
    int getCurrentPhase();

    /**
     * Gets the units completed so far this phase.
     * 
     * @return the number of units (an intentionally vague amount) completed so
     *         far in the current phase.
     */
    long getUnitsCompletedSoFarThisPhase();

    /**
     * Gets the total units this phase.
     * 
     * @return the total units we expect to process in this phase
     */
    long getTotalUnitsThisPhase();

    /**
     * Gets the last ui update time.
     * 
     * @return the time in absolute seconds since the UI was last updated
     */
    long getLastUIUpdateTime();

    /**
     * Gets the phase one start time.
     * 
     * @return the time in absolute seconds at which the first phase started
     */
    long getPhaseOneStartTime();

    /**
     * Gets the current phase start time.
     * 
     * @return the time in absolute seconds at which the current phase started
     */
    long getCurrentPhaseStartTime();

    /**
     * Sets the min update interval sec.
     * 
     * @param value
     *            the approximate time in relative seconds at which the UI
     *            should be updated periodically
     */
    void setMinUpdateIntervalSec(float value);

    /**
     * Sets the total units this phase.
     * 
     * @param value
     *            the total number of units expected to processed in this phase
     */
    void setTotalUnitsThisPhase(long value);

    /**
     * Resulting string does not contain a percent sign.
     * 
     * @return the percentage completion of this phase as a String with no
     *         percent sign.
     */
    String getPercentCompleteOfThisPhaseAsString();

    /**
     * Resulting string does not contain a percent sign.
     * 
     * @return the percentage completion at this point, taking into account all
     *         phases and phase-weights, as a String with no percent sign.
     */
    String getTotalPercentCompleteAsString();

    /**
     * May return "unknown".
     * 
     * @param thisPhaseOnly
     *            the this phase only
     * @return the relative eta as string
     */
    String getRelativeETAAsString(boolean thisPhaseOnly);

    /**
     * May return "unknown".
     * 
     * @param thisPhaseOnly
     *            the this phase only
     * @return the absolute eta as local time string
     */
    String getAbsoluteETAAsLocalTimeString(boolean thisPhaseOnly);

    /**
     * Note this will NOT advance the phase. To do that, use
     * markCurrentPhaseAsComplete().
     * 
     * @param numUnits
     *            the num units
     */
    void incrementUnitsCompletedThisPhase(long numUnits);

    /**
     * Returns a boolean, whether or not to display the updated information.
     * This throttles the update so it doesn't refresh so fast that it is
     * unreadable. Implementers should call this method from their own
     * incrementUnitsCompletedThisPhase method.
     * 
     * If we have just reached 100% for (the current phase, we return true, even
     * if it would otherwise be too soon to update the UI.
     * 
     * @return true, if successful
     */
    boolean shouldUpdateUI();

    /**
     * Subclasses must call this immediately after updating the UI, to preserve
     * the integrity of the shouldUpdateUI method.
     */
    void justUpdatedUI();

    /**
     * This is the only way to advance the phase number. It automatically
     * "starts the clock" for the next phase.
     * 
     * @param totalUnitsNextPhase
     *            if unknown, use zero (0), and be sure to call
     *            setTotalUnitsThisPhase() soon after.
     */
    void markCurrentPhaseAsComplete(long totalUnitsNextPhase);

    /**
     * Attempt to display a message to the user; not all implementations support
     * this.
     * 
     * If they do not, they may silently ignore this call.
     * 
     * @param message
     *            the message to display
     */
    void sendMessage(String message);

    /**
     * Mark the current phase as having an exponential rate; such phases reach
     * their totalUnits slower and slower as they process more units.
     * 
     * By default, a phase is considered to be linear, meaning this value is
     * 1.0f.
     * 
     * @param value
     *            usually less than 1.0f; often determined empirically.
     */
    void setCurrentPhaseAsExponential(float value);

    /**
     * Gets the current phase exponent.
     * 
     * @return the exponent for this phase
     */
    float getCurrentPhaseExponent();

}

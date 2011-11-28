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
 * The Class StderrProgressMonitor.
 */
public class StderrProgressMonitor extends BaseProgressMonitor {

    /**
     * Instantiates a new stderr progress monitor.
     * 
     * @param numPhases
     *            the num phases
     * @param totalUnitsFirstPhase
     *            the total units first phase
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#BaseProgressMonitor(int,
     *      long)
     */
    public StderrProgressMonitor(final int numPhases, final long totalUnitsFirstPhase) {
        this(numPhases, totalUnitsFirstPhase, 2.0f, null);
    }

    /**
     * Instantiates a new stderr progress monitor.
     * 
     * @param numPhases
     *            the num phases
     * @param totalUnitsFirstPhase
     *            the total units first phase
     * @param minUpdateIntervalSec
     *            the min update interval sec
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#BaseProgressMonitor(int,
     *      long, float)
     */
    public StderrProgressMonitor(final int numPhases,
            final long totalUnitsFirstPhase, final float minUpdateIntervalSec) {
        this(numPhases, totalUnitsFirstPhase, minUpdateIntervalSec, null);
    }

    /**
     * Instantiates a new stderr progress monitor.
     * 
     * @param numPhases
     *            the num phases
     * @param totalUnitsFirstPhase
     *            the total units first phase
     * @param minUpdateIntervalSec
     *            the min update interval sec
     * @param phaseWeights
     *            the phase weights
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#BaseProgressMonitor(int,
     *      long, float, float[])
     */
    public StderrProgressMonitor(final int numPhases, final long totalUnitsFirstPhase,
            final float minUpdateIntervalSec, final float[] phaseWeights) {
        super(numPhases, totalUnitsFirstPhase, minUpdateIntervalSec, phaseWeights);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#
     * incrementUnitsCompletedThisPhase(long)
     */

    /**
     * Increment units completed this phase.
     *
     * @param numUnits long
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#BaseProgressMonitor(int)
     */
    @Override
    public final void incrementUnitsCompletedThisPhase(final long numUnits) {
        super.incrementUnitsCompletedThisPhase(numUnits);

        if (shouldUpdateUI()) {

            if ((getNumPhases() > 1)) {
                printUpdate("Phase " + getCurrentPhase() + ": " + getUnitsCompletedSoFarThisPhase()
                        + " units processed. " + "Overall: " + getTotalPercentCompleteAsString() + "% complete, "
                        + "ETA in " + getRelativeETAAsString() + ".");
            } else {
                printUpdate("Overall: " + getUnitsCompletedSoFarThisPhase() + " units processed " + "("
                        + getTotalPercentCompleteAsString() + "%); " + "ETA in " + getRelativeETAAsString() + ".");
            }
        }
    }

    /**
     * Displays a message to stderr, overwriting the current estimate; calls
     * from outside this class should provide a newline character at the end of
     * the messsage.
     * 
     * @param message
     *            the message to display
     */
    public final void printUpdate(String message) {

        while (message.length() < 79) {
            message += ' ';
        }

        System.err.print("\r");
        System.err.print(message);

        if (message.length() > 79) {
            System.err.print("\n");
        }

        justUpdatedUI();

    }
}

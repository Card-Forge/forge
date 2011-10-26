package net.slightlymagic.braids.util;

/**
 * Like Runnable, but it can throw any Exception.
 */
public interface ClumsyRunnable {

    /**
     * Run.
     * 
     * @throws Exception
     *             the exception
     */
    void run() throws Exception;
}

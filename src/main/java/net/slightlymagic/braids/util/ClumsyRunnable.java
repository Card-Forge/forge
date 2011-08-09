package net.slightlymagic.braids.util;

/**
 * Like Runnable, but it can throw any Exception.
 */
public interface ClumsyRunnable {
	public void run() throws Exception;
}

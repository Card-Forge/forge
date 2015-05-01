/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.error;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;

import forge.FThreads;
import forge.GuiBase;
import forge.util.BuildInfo;
import forge.util.gui.SOptionPane;

/**
 * The class ErrorViewer. Enables showing and saving error messages that
 * occurred in forge.
 *
 * @author Clemens Koza
 * @version V1.0 02.08.2009
 */
public class BugReporter {
    private static final int STACK_OVERFLOW_MAX_MESSAGE_LEN = 16 * 1024;

    public static final String REPORT = "Report";
    public static final String SAVE = "Save";
    public static final String CONTINUE = "Continue";
    public static final String EXIT = "Exit";

    public static final String HELP_TEXT = String.format(
            "A template for a post in the bug reports forum topic is shown below.  Just select '%s' "
          + "and the template will be copied to your system clipboard and the forum page will open in your browser.  "
          + "Then all you have to do is paste the text into a forum post and edit the description line.", REPORT);
    public static final String HELP_URL_LABEL =
            "Reporting bugs in Forge is very important. We sincerely thank you for your time."
          + " For help writing a solid bug report, please see:";
    public static final String HELP_URL =
            "http://www.slightlymagic.net/forum/viewtopic.php?f=26&p=109925#p109925";
    private static final String FORUM_URL;

    static {
        final String forgeVersion = BuildInfo.getVersionString();
        if (StringUtils.containsIgnoreCase(forgeVersion, "svn") || StringUtils.containsIgnoreCase(forgeVersion, "snapshot")) {
            FORUM_URL = "http://www.slightlymagic.net/forum/viewtopic.php?f=52&t=6333&start=54564487645#bottom";
        } else {
            FORUM_URL = "http://www.slightlymagic.net/forum/viewforum.php?f=26";
        }
    }

    /**
     * Shows exception information in a format ready to post to the forum as a
     * crash report. Uses the exception's message as the reason if message is
     * null.
     */
    public static void reportException(final Throwable ex, final String message) {
        if (ex == null) {
            return;
        }
        if (message != null) {
            System.err.printf("%s > %s%n", FThreads.debugGetCurrThreadId(), message);
        }
        System.err.print(FThreads.debugGetCurrThreadId() + " > ");
        ex.printStackTrace();

        final StringBuilder sb = new StringBuilder();
        sb.append("Description: [describe what you were doing when the crash occurred]\n\n");
        buildSpoilerHeader(sb, ex.getClass().getSimpleName());
        sb.append("\n\n");
        if (null != message && !message.isEmpty()) {
            sb.append(FThreads.debugGetCurrThreadId()).append(" > ").append(message).append("\n");
        }

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);

        final String swStr = sw.toString();
        if (ex instanceof StackOverflowError && swStr.length() >= STACK_OVERFLOW_MAX_MESSAGE_LEN) {
            // most likely a cycle.  only take first portion so the message
            // doesn't grow too large to post
            sb.append(swStr, 0, STACK_OVERFLOW_MAX_MESSAGE_LEN);
            sb.append("\n... (truncated)");
        }
        else {
            sb.append(swStr);
        }

        buildSpoilerFooter(sb);

        GuiBase.getInterface().showBugReportDialog("Report a crash", sb.toString(), true);
    }

    /**
     * Alias for reportException(ex, null).
     */
    public static void reportException(final Throwable ex) {
        reportException(ex, null);
    }

    /**
     * Alias for reportException(ex, String.format(format, args)).
     */
    public static void reportException(final Throwable ex, final String format, final Object... args) {
        reportException(ex, String.format(format, args));
    }

    /**
     * Shows a forum post template for reporting a bug.
     */
    public static void reportBug(final String details) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Description: [describe the problem]\n\n");
        buildSpoilerHeader(sb, "General bug report");
        if (null != details && !details.isEmpty()) {
            sb.append("\n\n");
            sb.append(details);
        }
        buildSpoilerFooter(sb);

        GuiBase.getInterface().showBugReportDialog("Report a bug", sb.toString(), false);
    }

    private static StringBuilder buildSpoilerHeader(final StringBuilder sb, final String reportTitle) {
        sb.append("[spoiler=").append(reportTitle).append("][code]");
        sb.append("\nForge Version:    ").append(GuiBase.getInterface().getCurrentVersion());
        sb.append("\nOperating System: ").append(System.getProperty("os.name"))
                                         .append(" ").append(System.getProperty("os.version"))
                                         .append(" ").append(System.getProperty("os.arch"));
        sb.append("\nJava Version:     ").append(System.getProperty("java.version"))
                                         .append(" ").append(System.getProperty("java.vendor"));
        return sb;
    }

    private static StringBuilder buildSpoilerFooter(final StringBuilder sb) {
        sb.append("[/code][/spoiler]");
        return sb;
    }

    public static void copyAndGoToForums(final String text) {
        try {
            // copy text to clipboard
            GuiBase.getInterface().copyToClipboard(text);
            GuiBase.getInterface().browseToUrl(FORUM_URL);
        } catch (final Exception ex) {
            SOptionPane.showMessageDialog("Sorry, a problem occurred while opening the forum in your default browser.",
                    "A problem occurred", SOptionPane.ERROR_ICON);
        }
    }

    public static void saveToFile(final String text) {
        File f;
        final long curTime = System.currentTimeMillis();
        for (int i = 0;; i++) {
            final String name = String.format("%TF-%02d.txt", curTime, i);
            f = new File(name);
            if (!f.exists()) {
                break;
            }
        }

        f = GuiBase.getInterface().getSaveFile(f);

        try {
            final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(text);
            bw.close();
        }
        catch (final IOException ex) {
            SOptionPane.showMessageDialog("There was an error during saving. Sorry!\n" + ex,
                    "Error saving file", SOptionPane.ERROR_ICON);
        }
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private BugReporter() {
    }
}

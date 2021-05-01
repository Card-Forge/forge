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
package forge.gui.error;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Localizer;
import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

/**
 * The class ErrorViewer. Enables showing and saving error messages that
 * occurred in forge.
 *
 * @author Clemens Koza
 * @version V1.0 02.08.2009
 */
public class BugReporter {
    private static final int STACK_OVERFLOW_MAX_MESSAGE_LEN = 16 * 1024;

    public static final String REPORT = Localizer.getInstance().getMessage("lblReport");
    public static final String SAVE = Localizer.getInstance().getMessage("lblSave");
    public static final String DISCARD = Localizer.getInstance().getMessage("lblDiscardError");
    public static final String EXIT = Localizer.getInstance().getMessage("lblExit");
    public static final String SENTRY = Localizer.getInstance().getMessage("lblAutoSubmitBugReports");

    private static Throwable exception;
    private static String message;


    /**
     * Shows exception information in a format ready to post to the forum as a
     * crash report. Uses the exception's message as the reason if message is
     * null.
     */
    public static void reportException(final Throwable ex, final String message) {
        if (ex == null) {
            return;
        }
        exception = ex;
        if (message != null) {
            System.err.printf("%s > %s%n", FThreads.debugGetCurrThreadId(), message);
        }
        System.err.print(FThreads.debugGetCurrThreadId() + " > ");
        ex.printStackTrace();

        final StringBuilder sb = new StringBuilder();
        if (null != message && !message.isEmpty()) {
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(message).build()
            );
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
        if (isSentryEnabled()) {
            sendSentry();
        } else {
            GuiBase.getInterface().showBugReportDialog(Localizer.getInstance().getMessage("lblReportCrash"), sb.toString(), true);
        }
    }

    public static boolean isSentryEnabled() {
        return FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.USE_SENTRY);
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
        if (null != details && !details.isEmpty()) {
            sb.append("\n\n");
            sb.append(details);
        }
        message = sb.toString();

        if (isSentryEnabled()) {
            sendSentry();
        } else {
            GuiBase.getInterface().showBugReportDialog(Localizer.getInstance().getMessage("btnReportBug"), message, false);
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

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))){
            bw.write(text);
        } catch (final IOException ex) {
            SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblThereErrorWasDuringSaving", ex),
            Localizer.getInstance().getMessage("lblErrorSavingFile"), SOptionPane.ERROR_ICON);
        }
    }

    public static void sendSentry() {
        if (exception != null) {
            Sentry.capture(exception);
        } else if (message !=null) {
            Sentry.capture(message);
        }
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private BugReporter() {
    }
}

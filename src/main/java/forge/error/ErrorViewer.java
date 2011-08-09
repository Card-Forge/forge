package forge.error;


import static forge.properties.ForgeProps.getLocalized;
import static forge.properties.ForgeProps.getProperty;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_S;
import static java.awt.event.KeyEvent.VK_B;
import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import forge.properties.NewConstants;


/**
 * The class ErrorViewer. Enables showing and saving error messages that occurred in forge.
 *
 * @author Clemens Koza
 * @version V1.0 02.08.2009
 */
public class ErrorViewer implements NewConstants, NewConstants.LANG.ErrorViewer {
    /** Constant <code>NAME_OS="os.name"</code>. */
    private static final String NAME_OS = "os.name";
    /** Constant <code>VERSION_OS="os.version"</code>. */
    private static final String VERSION_OS = "os.version";
    /** Constant <code>ARCHITECTURE_OS="os.arch"</code>. */
    private static final String ARCHITECTURE_OS = "os.arch";
    /** Constant <code>VERSION_JAVA="java.version"</code>. */
    private static final String VERSION_JAVA = "java.version";
    /** Constant <code>VENDOR_JAVA="java.vendor"</code>. */
    private static final String VENDOR_JAVA = "java.vendor";

    /** Constant <code>ALL_THREADS_ACTION</code>. */
    public static final Action ALL_THREADS_ACTION = new ShowAllThreadsAction();

    private static JDialog dlg = null;

    /**
     * Shows an error dialog taking the exception's message as the error message.
     *
     * @param ex a {@link java.lang.Throwable} object.
     */
    public static void showError(final Throwable ex) {
        showError(ex, null);
    }

    /**
     * Shows an error dialog creating the error message by a formatting operation.
     *
     * @param ex a {@link java.lang.Throwable} object.
     * @param format a {@link java.lang.String} object.
     * @param args a {@link java.lang.Object} object.
     */
    public static void showError(final Throwable ex, final String format, final Object... args) {
        if (ex == null) {
            return;
        }
        showError(ex, String.format(format, args));
    }

    /**
     * Shows an error dialog with the specified error message.
     *
     * @param ex a {@link java.lang.Throwable} object.
     * @param message a {@link java.lang.String} object.
     */
    public static void showError(final Throwable ex, final String message) {
        if (ex == null) {
            return;
        }

        final StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        printError(pw, ex, message);

        showDialog(sw.toString());
    }

    /**
     * Shows an error without an exception that caused it.
     *
     * @param format a {@link java.lang.String} object.
     * @param args a {@link java.lang.Object} object.
     */
    public static void showError(final String format, final Object... args) {
        showError(String.format(format, args));
    }

    /**
     * Shows an error without an exception that caused it.
     *
     * @param message a {@link java.lang.String} object.
     */
    public static void showError(final String message) {
        showError(new Exception(), message);
    }

    /**
     * Shows an error message for all running threads.
     *
     * @param format a {@link java.lang.String} object.
     * @param args a {@link java.lang.Object} object.
     */
    public static void showErrorAllThreads(final String format, final Object... args) {
        showErrorAllThreads(String.format(format, args));
    }

    /**
     * Shows an error message for all running threads.
     *
     * @param message a {@link java.lang.String} object.
     */
    public static void showErrorAllThreads(final String message) {
        final StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        printError(pw, message);

        showDialog(sw.toString());
    }

    /**
     * <p>showDialog.</p>
     *
     * @param fullMessage a {@link java.lang.String} object.
     */
    private static void showDialog(final String fullMessage) {
        JTextArea area = new JTextArea(fullMessage, 40, 90);
        area.setFont(new Font("Monospaced", Font.PLAIN, 10));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        //Button is not modified, String gets the automatic listener to hide the dialog
        Object[] options = {
                new JButton(new BugzAction(area)), new JButton(new SaveAction(area)), getLocalized(BUTTON_CLOSE), new JButton(new ExitAction())};
        JOptionPane pane = new JOptionPane(new JScrollPane(area), ERROR_MESSAGE, DEFAULT_OPTION, null, options,
                options[1]);
        dlg = pane.createDialog(null, getLocalized(TITLE));
        dlg.setResizable(true);
        dlg.setVisible(true);
        dlg.dispose();
    }

    /**
     * Prints the error message for the specified exception to the print writer.
     *
     * @param pw a {@link java.io.PrintWriter} object.
     * @param ex a {@link java.lang.Throwable} object.
     * @param message a {@link java.lang.String} object.
     */
    private static void printError(final PrintWriter pw, final Throwable ex, final String message) {
        if (message != null) {
            System.err.println(message);
        }
        ex.printStackTrace();

        pw.printf(getLocalized(MESSAGE), getProperty(HOW_TO_REPORT_BUGS_URL),
                message != null ? message : ex.getMessage(), getProperty(VERSION),
                System.getProperty(NAME_OS), System.getProperty(VERSION_OS), System.getProperty(ARCHITECTURE_OS),
                System.getProperty(VERSION_JAVA), System.getProperty(VENDOR_JAVA));
        ex.printStackTrace(pw);
    }

    /**
     * Prints the error message to the print writer, showing all running threads' stack traces.
     *
     * @param pw a {@link java.io.PrintWriter} object.
     * @param message a {@link java.lang.String} object.
     */
    private static void printError(final PrintWriter pw, final String message) {
        System.err.println(message);

        pw.printf(getLocalized(MESSAGE), getProperty(HOW_TO_REPORT_BUGS_URL), message, getProperty(VERSION),
                System.getProperty(NAME_OS), System.getProperty(VERSION_OS), System.getProperty(ARCHITECTURE_OS),
                System.getProperty(VERSION_JAVA), System.getProperty(VENDOR_JAVA));
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (Entry<Thread, StackTraceElement[]> e : traces.entrySet()) {
            pw.println();
            pw.printf("%s (%s):%n", e.getKey().getName(), e.getKey().getId());
            for (StackTraceElement el : e.getValue()) {
                pw.println(el);
            }
        }
    }

    private static class SaveAction extends AbstractAction {

        private static final long serialVersionUID = 9146834661273525959L;

        private static JFileChooser c;

        private JTextArea area;

        public SaveAction(final JTextArea areaParam) {
            super(getLocalized(BUTTON_SAVE));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(VK_S, CTRL_DOWN_MASK));
            this.area = areaParam;
        }

        public void actionPerformed(final ActionEvent e) {
            if (c == null) {
                c = new JFileChooser();
            }

            File f;
            for (int i = 0;; i++) {
                String name = String.format("%TF-%02d.txt", System.currentTimeMillis(), i);
                f = new File(name);
                if (!f.exists()) {
                    break;
                }
            }
            c.setSelectedFile(f);
            c.showSaveDialog(null);
            f = c.getSelectedFile();

            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                bw.write(area.getText());
                bw.close();
            } catch (IOException ex) {
                showError(ex, getLocalized(ERRORS.SAVE_MESSAGE));
            }
        }
    }
    
    private static class BugzAction extends AbstractAction {

        private static final long serialVersionUID = 914634661273525959L;

        private JTextArea area;

        public BugzAction(JTextArea area) {
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(VK_B, CTRL_DOWN_MASK));
            putValue(NAME, "Report Bug");
            this.area = area;
        }

        public void actionPerformed(ActionEvent e) {
            BugzReporter br = new BugzReporter();
            br.setDumpText(area.getText());
            br.setVisible(true);
            dlg.dispose();
        }
    }

    private static class ExitAction extends AbstractAction {

        private static final long serialVersionUID = 276202595758381626L;

        public ExitAction() {
            super(getLocalized(BUTTON_EXIT));
        }


        public void actionPerformed(final ActionEvent e) {
            System.exit(0);
        }
    }

    private static class ShowAllThreadsAction extends AbstractAction {

        private static final long serialVersionUID = 5638147106706803363L;

        public ShowAllThreadsAction() {
            super(getLocalized(SHOW_ERROR));
        }

        public void actionPerformed(final ActionEvent e) {
            showErrorAllThreads(getLocalized(ERRORS.SHOW_MESSAGE));
        }
    }
}

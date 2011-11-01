package forge.error;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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

import forge.Singletons;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * The class ErrorViewer. Enables showing and saving error messages that
 * occurred in forge.
 * 
 * @author Clemens Koza
 * @version V1.0 02.08.2009
 */
public class ErrorViewer implements NewConstants, NewConstants.Lang.ErrorViewer {
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
     * Shows an error dialog taking the exception's message as the error
     * message.
     * 
     * @param ex
     *            a {@link java.lang.Throwable} object.
     */
    public static void showError(final Throwable ex) {
        ErrorViewer.showError(ex, null);
    }

    /**
     * Shows an error dialog creating the error message by a formatting
     * operation.
     * 
     * @param ex
     *            a {@link java.lang.Throwable} object.
     * @param format
     *            a {@link java.lang.String} object.
     * @param args
     *            a {@link java.lang.Object} object.
     */
    public static void showError(final Throwable ex, final String format, final Object... args) {
        if (ex == null) {
            return;
        }
        ErrorViewer.showError(ex, String.format(format, args));
    }

    /**
     * Shows an error dialog with the specified error message.
     * 
     * @param ex
     *            a {@link java.lang.Throwable} object.
     * @param message
     *            a {@link java.lang.String} object.
     */
    public static void showError(final Throwable ex, final String message) {
        if (ex == null) {
            return;
        }

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        ErrorViewer.printError(pw, ex, message);

        ErrorViewer.showDialog(sw.toString());
    }

    /**
     * Shows an error without an exception that caused it.
     * 
     * @param format
     *            a {@link java.lang.String} object.
     * @param args
     *            a {@link java.lang.Object} object.
     */
    public static void showError(final String format, final Object... args) {
        ErrorViewer.showError(String.format(format, args));
    }

    /**
     * Shows an error without an exception that caused it.
     * 
     * @param message
     *            a {@link java.lang.String} object.
     */
    public static void showError(final String message) {
        ErrorViewer.showError(new Exception(), message);
    }

    /**
     * Shows an error message for all running threads.
     * 
     * @param format
     *            a {@link java.lang.String} object.
     * @param args
     *            a {@link java.lang.Object} object.
     */
    public static void showErrorAllThreads(final String format, final Object... args) {
        ErrorViewer.showErrorAllThreads(String.format(format, args));
    }

    /**
     * Shows an error message for all running threads.
     * 
     * @param message
     *            a {@link java.lang.String} object.
     */
    public static void showErrorAllThreads(final String message) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        ErrorViewer.printError(pw, message);

        ErrorViewer.showDialog(sw.toString());
    }

    /**
     * <p>
     * showDialog.
     * </p>
     * 
     * @param fullMessage
     *            a {@link java.lang.String} object.
     */
    private static void showDialog(final String fullMessage) {
        final JTextArea area = new JTextArea(fullMessage, 40, 90);
        area.setFont(new Font("Monospaced", Font.PLAIN, 10));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        // Button is not modified, String gets the automatic listener to hide
        // the dialog
        final Object[] options = { new JButton(new BugzAction(area)), new JButton(new SaveAction(area)),
                ForgeProps.getLocalized(ErrorViewer.BUTTON_CLOSE), new JButton(new ExitAction()) };

        final JOptionPane pane = new JOptionPane(new JScrollPane(area), JOptionPane.ERROR_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, options[1]);
        ErrorViewer.dlg = pane.createDialog(null, ForgeProps.getLocalized(ErrorViewer.TITLE));
        ErrorViewer.dlg.setResizable(true);
        ErrorViewer.dlg.setVisible(true);
        ErrorViewer.dlg.dispose();
    }

    /**
     * Prints the error message for the specified exception to the print writer.
     * 
     * @param pw
     *            a {@link java.io.PrintWriter} object.
     * @param ex
     *            a {@link java.lang.Throwable} object.
     * @param message
     *            a {@link java.lang.String} object.
     */
    private static void printError(final PrintWriter pw, final Throwable ex, final String message) {
        if (message != null) {
            System.err.println(message);
        }
        ex.printStackTrace();

        pw.printf(ForgeProps.getLocalized(ErrorViewer.MESSAGE),
                ForgeProps.getProperty(NewConstants.HOW_TO_REPORT_BUGS_URL),
                message != null ? message : ex.getMessage(), Singletons.getModel().getBuildInfo().toPrettyString(),
                System.getProperty(ErrorViewer.NAME_OS), System.getProperty(ErrorViewer.VERSION_OS),
                System.getProperty(ErrorViewer.ARCHITECTURE_OS), System.getProperty(ErrorViewer.VERSION_JAVA),
                System.getProperty(ErrorViewer.VENDOR_JAVA));
        ex.printStackTrace(pw);
    }

    /**
     * Prints the error message to the print writer, showing all running
     * threads' stack traces.
     * 
     * @param pw
     *            a {@link java.io.PrintWriter} object.
     * @param message
     *            a {@link java.lang.String} object.
     */
    private static void printError(final PrintWriter pw, final String message) {
        System.err.println(message);

        pw.printf(ForgeProps.getLocalized(ErrorViewer.MESSAGE),
                ForgeProps.getProperty(NewConstants.HOW_TO_REPORT_BUGS_URL), message, Singletons.getModel()
                        .getBuildInfo().toPrettyString(), System.getProperty(ErrorViewer.NAME_OS),
                System.getProperty(ErrorViewer.VERSION_OS), System.getProperty(ErrorViewer.ARCHITECTURE_OS),
                System.getProperty(ErrorViewer.VERSION_JAVA), System.getProperty(ErrorViewer.VENDOR_JAVA));
        final Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (final Entry<Thread, StackTraceElement[]> e : traces.entrySet()) {
            pw.println();
            pw.printf("%s (%s):%n", e.getKey().getName(), e.getKey().getId());
            for (final StackTraceElement el : e.getValue()) {
                pw.println(el);
            }
        }
    }

    private static class SaveAction extends AbstractAction {

        private static final long serialVersionUID = 9146834661273525959L;

        private static JFileChooser c;

        private final JTextArea area;

        public SaveAction(final JTextArea areaParam) {
            super(ForgeProps.getLocalized(ErrorViewer.BUTTON_SAVE));
            this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
            this.area = areaParam;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (SaveAction.c == null) {
                SaveAction.c = new JFileChooser();
            }

            File f;
            for (int i = 0;; i++) {
                final String name = String.format("%TF-%02d.txt", System.currentTimeMillis(), i);
                f = new File(name);
                if (!f.exists()) {
                    break;
                }
            }
            SaveAction.c.setSelectedFile(f);
            SaveAction.c.showSaveDialog(null);
            f = SaveAction.c.getSelectedFile();

            try {
                final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                bw.write(this.area.getText());
                bw.close();
            } catch (final IOException ex) {
                ErrorViewer.showError(ex, ForgeProps.getLocalized(ERRORS.SAVE_MESSAGE));
            }
        }
    }

    private static class BugzAction extends AbstractAction {

        private static final long serialVersionUID = 914634661273525959L;

        private final JTextArea area;

        public BugzAction(final JTextArea neoArea) {
            this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
            this.putValue(Action.NAME, "Report Bug");
            this.area = neoArea;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final BugzReporter br = new BugzReporter();
            br.setDumpText(this.area.getText());
            br.setVisible(true);
            ErrorViewer.dlg.dispose();
        }
    }

    private static class ExitAction extends AbstractAction {

        private static final long serialVersionUID = 276202595758381626L;

        public ExitAction() {
            super(ForgeProps.getLocalized(ErrorViewer.BUTTON_EXIT));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            System.exit(0);
        }
    }

    private static class ShowAllThreadsAction extends AbstractAction {

        private static final long serialVersionUID = 5638147106706803363L;

        public ShowAllThreadsAction() {
            super(ForgeProps.getLocalized(ErrorViewer.SHOW_ERROR));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            ErrorViewer.showErrorAllThreads(ForgeProps.getLocalized(ERRORS.SHOW_MESSAGE));
        }
    }
}

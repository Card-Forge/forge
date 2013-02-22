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

import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.gui.WrapLayout;
import forge.gui.toolbox.FHyperlink;
import forge.gui.toolbox.FLabel;
import forge.model.BuildInfo;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * The class ErrorViewer. Enables showing and saving error messages that
 * occurred in forge.
 * 
 * @author Clemens Koza
 * @version V1.0 02.08.2009
 */
public class BugReporter {
    /**
     * Shows exception information in a format ready to post to the forum as a crash report.  Uses the exception's message
     * as the reason if message is null.
     */
    public static void reportException(final Throwable ex, final String message) {
        if (ex == null) {
            return;
        }
        if (message != null) {
            System.err.println(message);
        }
        ex.printStackTrace();
        
        StringBuilder sb = new StringBuilder();
        sb.append("Description: [describe what you were doing when the crash occurred]\n\n");
        _buildSpoilerHeader(sb, ex.getClass().getSimpleName());
        sb.append("\n\n");
        if (null != message && !message.isEmpty()) {
            sb.append(message);
            sb.append("\n");
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        sb.append(sw.toString());
        
        _buildSpoilerFooter(sb);

        _showDialog("Report a crash", sb.toString(), true);
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
    public static void reportBug(String details) {
        StringBuilder sb = new StringBuilder();
        sb.append("Description: [describe the problem]\n\n");
        _buildSpoilerHeader(sb, "General bug report");
        if (null != details && !details.isEmpty()) {
            sb.append("\n\n");
            sb.append(details);
        }
        _buildSpoilerFooter(sb);

        _showDialog("Report a bug", sb.toString(), false);
    }

    /**
     * Shows thread stack information in a format ready to post to the forum.
     */
    public static void reportThreadStacks(final String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("Description: [describe what you were doing at the time]\n\n");
        _buildSpoilerHeader(sb, "Thread stack dump");
        sb.append("\n\n");
        if (null != message && !message.isEmpty()) {
            sb.append(message);
            sb.append("\n");
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        final Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (final Entry<Thread, StackTraceElement[]> e : traces.entrySet()) {
            pw.println();
            pw.printf("%s (%s):%n", e.getKey().getName(), e.getKey().getId());
            for (final StackTraceElement el : e.getValue()) {
                pw.println(el);
            }
        }

        sb.append(sw.toString());
        _buildSpoilerFooter(sb);
        _showDialog("Thread stack dump", sb.toString(), false);
    }

    /**
     * Alias for reportThreadStacks(String.format(format, args))
     */
    public static void reportThreadStacks(final String format, final Object... args) {
        reportThreadStacks(String.format(format, args));
    }

    private static StringBuilder _buildSpoilerHeader(StringBuilder sb, String reportTitle) {
        sb.append("[spoiler=").append(reportTitle).append("][code]");
        sb.append("\nForge Version:    ").append(BuildInfo.getVersionString());
        sb.append("\nOperating System: ").append(System.getProperty("os.name"))
                                         .append(" ").append(System.getProperty("os.version"))
                                         .append(" ").append(System.getProperty("os.arch"));
        sb.append("\nJava Version:     ").append(System.getProperty("java.version"))
                                         .append(" ").append(System.getProperty("java.vendor"));
        return sb;
    }
    
    private static StringBuilder _buildSpoilerFooter(StringBuilder sb) {
        sb.append("[/code][/spoiler]");
        return sb;
    }
    
    private static void _showDialog(String title, String text, boolean showExitAppBtn) {
        JTextArea area = new JTextArea(text);
        area.setFont(new Font("Monospaced", Font.PLAIN, 10));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        String helpText = "<html>A template for a post in the bug reports forum topic is shown below.  Just select 'Copy and go to forum' "
                + "and the template will be copied to your system clipboard and the forum page will open in your browser.  "
                + "Then all you have to do is paste the text into a forum post and edit the description line.</html>";
        String helpUrlLabel = "Reporting bugs in Forge is very important. We sincerely thank you for your time."
                + " For help writing a solid bug report, please see:";
        String helpUrl = "http://www.slightlymagic.net/forum/viewtopic.php?f=26&p=109925#p109925";
        JPanel helpPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 2));
        for (String word : helpUrlLabel.split(" ")) {
            helpPanel.add(new FLabel.Builder().text("<html>" + word + "</html>").useSkinColors(false).build());
        }
        helpPanel.add(new FHyperlink.Builder().url(helpUrl).text("<html>this post</html>").useSkinColors(false).build());
        
        JPanel p = new JPanel(new MigLayout("wrap"));
        p.add(new FLabel.Builder().text(helpText).useSkinColors(false).build(), "gap 5");
        p.add(helpPanel, "w 600");
        p.add(new JScrollPane(area), "w 100%, h 100%, gaptop 5");
        
        // determine proper forum URL
        String forgeVersion = BuildInfo.getVersionString();
        final String url;
        if (StringUtils.containsIgnoreCase(forgeVersion, "svn")
         || StringUtils.containsIgnoreCase(forgeVersion, "snapshot")) {
            url = "http://www.slightlymagic.net/forum/viewtopic.php?f=52&t=6333&start=54564487645#bottom";
        } else {
            url = "http://www.slightlymagic.net/forum/viewforum.php?f=26";
        }

        // Button is not modified, String gets the automatic listener to hide
        // the dialog
        ArrayList<Object> options = new ArrayList<Object>();
        options.add(new JButton(new _CopyAndGo(url, area)));
        options.add(new JButton(new _SaveAction(area)));
        options.add("Close");
        if (showExitAppBtn) {
            options.add(new JButton(new _ExitAction()));
        }
        
        JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options.toArray(), options.get(0));
        JDialog dlg = pane.createDialog(null, title);
        dlg.setSize(showExitAppBtn ? 780 : 600, 400);
        dlg.setResizable(true);
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        dlg.dispose();
    }

    @SuppressWarnings("serial")
    private static class _CopyAndGo extends AbstractAction {
        private final String url;
        private final JTextArea text;
        
        public _CopyAndGo(String url, JTextArea text) {
            super("Copy and go to forum");
            this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            
            this.url = url;
            this.text = text;
        }
        
        @Override
        public void actionPerformed(final ActionEvent e) {
            try {
                // copy text to clipboard
                StringSelection ss = new StringSelection(text.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                
                // browse to url
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Sorry, a problem occurred while opening the forum in your default browser.",
                        "A problem occured", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    @SuppressWarnings("serial")
    private static class _SaveAction extends AbstractAction {
        private static JFileChooser c;
        private final JTextArea area;

        public _SaveAction(final JTextArea areaParam) {
            super("Save to file");
            this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            this.area = areaParam;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (c == null) {
                c = new JFileChooser();
            }

            File f;
            long curTime = System.currentTimeMillis();
            for (int i = 0;; i++) {
                final String name = String.format("%TF-%02d.txt", curTime, i);
                f = new File(name);
                if (!f.exists()) {
                    break;
                }
            }
            
            c.setSelectedFile(f);
            c.showSaveDialog(null);
            f = c.getSelectedFile();

            try {
                final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                bw.write(this.area.getText());
                bw.close();
            } catch (final IOException ex) {
                JOptionPane.showMessageDialog(area.getTopLevelAncestor(),
                        ForgeProps.getLocalized(NewConstants.Lang.ErrorViewer.ERRORS.SAVE_MESSAGE),
                        "Error saving file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @SuppressWarnings("serial")
    private static class _ExitAction extends AbstractAction {
        public _ExitAction() {
            super("Exit application");
            this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            System.exit(0);
        }
    }
    
    // disable instantiation
    private BugReporter() { }
}

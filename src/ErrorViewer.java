/**
 * ErrorViewer.java
 * 
 * Created on 02.08.2009
 */

import static javax.swing.JOptionPane.*;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/**
 * The class ErrorViewer. Enables showing and saving error messages that occured in forge.
 * 
 * @version V1.0 02.08.2009
 * @author Clemens Koza
 */
public class ErrorViewer {
    private static JFileChooser c;
    
    /**
     * Shows an error dialog taking the exception's message as the error message.
     */
    public static void showError(Throwable ex) {
        showError(ex, null);
    }
    
    /**
     * Shows an error dialog creating the error message by a formatting operation.
     */
    public static void showError(Throwable ex, String format, Object... args) {
        if(ex == null) return;
        showError(ex, String.format(format, args));
    }
    
    /**
     * Shows an error dialog with the specified error message.
     */
    public static void showError(final Throwable ex, String message) {
        if(ex == null) return;
        
        //As the first action, write to standard err for console debugging
        if(message != null) System.err.println(message);
        ex.printStackTrace();
        
        final StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("An error has occured. You can copy/paste this message or save it to a file.");
        pw.println("Please report this, plus what you tried to do, to:");
        pw.println("\thttp://www.slightlymagic.net/forum/viewforum.php?f=26");
        pw.println("If you don't want to register an account, you can mail it directly to");
        pw.println("\tmtgrares@yahoo.com");
        pw.println();
        pw.println(message != null? message:ex.getMessage());
        pw.println();
        pw.println("Detailed error trace:");
        ex.printStackTrace(pw);
        
        final JTextArea area = new JTextArea(sw.toString());
        area.setFont(new Font("Monospace", Font.PLAIN, 10));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        
        JScrollPane scroll = new JScrollPane(area);
        Dimension dim = scroll.getPreferredSize();
        dim.width = 500;
        scroll.setPreferredSize(dim);
        
        JButton save = new JButton("Save...");
        save.addActionListener(new ActionListener() {
            //@Override
            public void actionPerformed(ActionEvent e) {
                if(c == null) c = new JFileChooser();
                
                File f;
                for(int i = 0;; i++) {
                    String name = String.format("%TF-%02d-%s.txt", System.currentTimeMillis(), i,
                            ex.getClass().getSimpleName());
                    f = new File(name);
                    if(!f.exists()) break;
                }
                c.setSelectedFile(f);
                c.showSaveDialog(null);
                f = c.getSelectedFile();
                
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                    bw.write(area.getText());
                    bw.close();
                } catch(IOException ex) {
                    showError(ex, "There was an error during saving, sorry!");
                }
            }
        });
        //Button is not modified, String gets the automatic listener to hide the dialog 
        Object[] options = {save, "Close"};
        JOptionPane pane = new JOptionPane(scroll, ERROR_MESSAGE, DEFAULT_OPTION, null, options, options[1]);
        JDialog dlg = pane.createDialog("Error");
        dlg.setVisible(true);
        dlg.dispose();
    }
}

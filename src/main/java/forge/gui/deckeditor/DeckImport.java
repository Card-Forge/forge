package forge.gui.deckeditor;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

import forge.Singletons;
import forge.gui.GuiUtils;

/** 
 * Dialog for quick import of decks
 * 
 */
public class DeckImport extends JDialog {
    private static final long serialVersionUID = -5837776824284093004L;

    private JTextArea txtInput = new JTextArea();
    private JEditorPane htmlOutput = new JEditorPane();
    private JScrollPane scrollInput = new JScrollPane(txtInput);
    private JScrollPane scrollOutput = new JScrollPane(htmlOutput);
    private JButton cmdAccept = new JButton("Import Deck");
    private JButton cmdCancel = new JButton("Cancel");

    DeckEditorBase host;

    public DeckImport(DeckEditorBase g) {
        host = g;
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                host.setEnabled(true);
            }
        });
        
        int wWidth = 800;
        int wHeight = 600;
        
        setPreferredSize(new java.awt.Dimension(wWidth, wHeight));
        setSize(wWidth, wHeight);
        GuiUtils.centerFrame(this);
        
        setResizable(false);
        setTitle("Deck Import (wip)");
        

        if (!Singletons.getModel().getPreferences().lafFonts) {
            Font fButtons = new java.awt.Font("Dialog", 0, 13);
            cmdAccept.setFont(fButtons);
            cmdCancel.setFont(fButtons);
            
            txtInput.setFont(fButtons);
            htmlOutput.setFont(fButtons);
        }

        scrollInput.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), "Paste or type a decklist"));
        scrollOutput.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), "Expect the recognized lines to appear"));
        scrollInput.setViewportBorder(BorderFactory.createLoweredBevelBorder());
        scrollOutput.setViewportBorder(BorderFactory.createLoweredBevelBorder());
        
        getContentPane().setLayout(new MigLayout("fill"));
        getContentPane().add(scrollInput, "cell 0 0, w 50%, growy, pushy");
        getContentPane().add(scrollOutput, "cell 1 0, w 50%, growy, pushy");
        getContentPane().add(cmdAccept, "cell 0 1, w 100, align r");
        getContentPane().add(cmdCancel, "cell 1 1, w 100, align l");

        cmdCancel.addActionListener(new ActionListener() { 
            @Override public void actionPerformed(ActionEvent e) {
                processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING)); } });

        cmdAccept.addActionListener(new ActionListener() { 
            @Override public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(DeckImport.this, "This dialog still in development, don't expect any changes to deck yet.");
                processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING)); } });
        
    }


    
}

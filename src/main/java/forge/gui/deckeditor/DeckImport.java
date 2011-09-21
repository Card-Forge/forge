package forge.gui.deckeditor;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;

import net.miginfocom.swing.MigLayout;

import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckRecognizer;
import forge.gui.GuiUtils;

/** 
 * Dialog for quick import of decks
 * 
 */
public class DeckImport extends JDialog {
    private static final long serialVersionUID = -5837776824284093004L;

    private JTextArea txtInput = new JTextArea();
    private JEditorPane htmlOutput = new JEditorPane("text/html", "<html><style>.rose { color:#ebaeba; }</style><h3 class=\"rose\">Expect result here</h3></html>");
    private JScrollPane scrollInput = new JScrollPane(txtInput);
    private JScrollPane scrollOutput = new JScrollPane(htmlOutput);
    private JButton cmdAccept = new JButton("Import Deck");
    private JButton cmdCancel = new JButton("Cancel");
    
    List<DeckRecognizer.Token> tokens = new ArrayList<DeckRecognizer.Token>();

    DeckEditorBase host;

    public DeckImport(DeckEditorBase g) {
        host = g;

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
            //htmlOutput.setFont(fButtons);
        }
        
        htmlOutput.setEditable(false);

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
                buildDeck();
                JOptionPane.showMessageDialog(DeckImport.this, "This dialog still in development, don't expect any changes to deck yet.");
                processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING)); } });

        txtInput.getDocument().addDocumentListener(new OnChangeTextUpdate());
    }
    
    private void readInput()
    {
        tokens.clear();
        ElementIterator it = new ElementIterator(txtInput.getDocument().getDefaultRootElement());
        Element e;
        while ((e=it.next()) != null) {
            if (!e.isLeaf()) { continue; }
            int rangeStart = e.getStartOffset();
            int rangeEnd = e.getEndOffset();
            try {
                String line = txtInput.getText(rangeStart, rangeEnd-rangeStart);
                tokens.add(DeckRecognizer.recognizeLine(line));
            } catch (BadLocationException ex) {
            }
        }
    }
    
    private static String stylesheet = "<style>" +
    		"body, h1, h2, h3, h4, h5, h6, table, tr, td, p {margin: 0; padding: 0; font-weight: normal; font-style: normal; text-decoration: none; font-family: Arial; font-size: 10px;} " +
    		//"h1 {border-bottom: solid 1px black; color: blue; font-size: 12px; margin: 3px 0 9px 0; } " +
    		".unknown {color: #660000;} " +
    		".comment {color: #006666;} " +
    		".KnownCardWithNumber {color: #009900;} " +
    		".UnknownCardWithNumber {color: #000099;} " +
    		".SectionName {font-weight: bold;} " +
    		"</style>";
    
    private void displayTokens()
    {
        StringBuilder sbOut = new StringBuilder("<html>");
        sbOut.append(stylesheet);
        for (DeckRecognizer.Token t : tokens) {
            sbOut.append(makeHtmlViewOfToken(t));
        }
        sbOut.append("</html>");
        htmlOutput.setText(sbOut.toString());
    }
    
    private Deck buildDeck()
    {
        return new Deck();
    }

    protected class OnChangeTextUpdate implements DocumentListener {
        private void onChange() { readInput(); displayTokens(); }
        @Override public void insertUpdate(DocumentEvent e) { onChange(); }
        @Override public void removeUpdate(DocumentEvent e) { onChange(); }
        @Override public void changedUpdate(DocumentEvent e) { } // Happend only on ENTER pressed
    }    

    private String makeHtmlViewOfToken(DeckRecognizer.Token token) {
        switch(token.getType())
        {
        case Unknown:
            return String.format("<div class='unknown'>%s</div>", token.getText());
        default: 
            return String.format("<div class='%s'>%s</div>", token.getType(), token.getText());
        }
    }
    
}

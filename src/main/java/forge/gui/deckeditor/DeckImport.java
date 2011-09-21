package forge.gui.deckeditor;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
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
import forge.deck.DeckRecognizer.TokenType;
import forge.gui.GuiUtils;

/** 
 * Dialog for quick import of decks
 * 
 */
public class DeckImport extends JDialog {
    private static final long serialVersionUID = -5837776824284093004L;

    private JTextArea txtInput = new JTextArea();
    private static final String stylesheet = "<style>" +
            "body, h1, h2, h3, h4, h5, h6, table, tr, td, p {margin: 1px; padding: 0; font-weight: normal; font-style: normal; text-decoration: none; font-family: Arial; font-size: 10px;} " +
            //"h1 {border-bottom: solid 1px black; color: blue; font-size: 12px; margin: 3px 0 9px 0; } " +
            ".comment {color: #666666;} " +
            ".knowncard {color: #009900;} " +
            ".unknowncard {color: #990000;} " +
            ".section {padding: 3px 10px; margin: 3px 0; font-weight: 700; background-color: #DDDDDD; } " +
            "</style>";
    private static final String htmlWelcomeText = "<html>"+stylesheet+"<h3>You'll see recognized cards here</h3>" +
    		"<div class='section'>Legend</div>" +
    		"<ul>" +
    		"<li class='knowncard'>Recognized cards will be shown in green. These cards will be auto-imported into a new deck<BR></li>" +
    		"<li class='unknowncard'>Lines which seem to see cards but could not be recognized, are shown in red<BR></li>" +
    		"<li class='comment'>Lines that appear unsignificant will be shown in gray<BR><BR></li>" +
    		"</ul>" +
    		"<div class='comment'>Submit feedback to Max mtg on slightlymagic.net forum</div>" +
    		"<div class='comment'>Post bug-reports to http://cardforge.org/bugz/</div>" +
    		"</html>";
    
    private JEditorPane htmlOutput = new JEditorPane("text/html", htmlWelcomeText);
    private JScrollPane scrollInput = new JScrollPane(txtInput);
    private JScrollPane scrollOutput = new JScrollPane(htmlOutput);
    private JLabel summaryMain = new JLabel("Imported deck summary will appear here");
    private JLabel summarySide = new JLabel("This is second line");
    private JButton cmdAccept = new JButton("Import Deck");
    private JButton cmdCancel = new JButton("Cancel");
    
    List<DeckRecognizer.Token> tokens = new ArrayList<DeckRecognizer.Token>();

    DeckEditorBase host;

    public DeckImport(DeckEditorBase g) {
        host = g;

        int wWidth = 600;
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
        getContentPane().add(scrollInput, "cell 0 0, w 50%, sy 4, growy, pushy");
        getContentPane().add(scrollOutput, "cell 1 0, w 50%, growy, pushy");

        getContentPane().add(summaryMain, "cell 1 1, label");
        getContentPane().add(summarySide, "cell 1 2, label");

        getContentPane().add(cmdAccept, "cell 1 3, split 2, w 100, align c");
        getContentPane().add(cmdCancel, "w 100");

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
    

    
    private void displayTokens() {
        StringBuilder sbOut = new StringBuilder("<html>");
        sbOut.append(stylesheet);
        for (DeckRecognizer.Token t : tokens) {
            sbOut.append(makeHtmlViewOfToken(t));
        }
        sbOut.append("</html>");
        htmlOutput.setText(sbOut.toString());
    }
    
    private void updateSummaries() {
        int[] cardsOk = new int[2];
        int[] cardsUnknown = new int[2];
        int idx = 0;
        for (DeckRecognizer.Token t : tokens) {
            if (t.getType() == TokenType.KnownCardWithNumber) { cardsOk[idx] += t.getNumber(); }
            if (t.getType() == TokenType.UnknownCardWithNumber) { cardsUnknown[idx] += t.getNumber(); }
            if (t.getType() == TokenType.SectionName && t.getText().toLowerCase().contains("side") ) { idx = 1; }
        }
        summaryMain.setText(String.format("Main: %d cards recognized, %d unknown cards", cardsOk[0], cardsUnknown[0]));
        summarySide.setText(String.format("Sideboard: %d cards recognized, %d unknown cards", cardsOk[1], cardsUnknown[1]));
    }
    
    private Deck buildDeck(){
        return new Deck();
    }

    protected class OnChangeTextUpdate implements DocumentListener {
        private void onChange() { readInput(); displayTokens(); updateSummaries(); }
        @Override public void insertUpdate(DocumentEvent e) { onChange(); }
        @Override public void removeUpdate(DocumentEvent e) { onChange(); }
        @Override public void changedUpdate(DocumentEvent e) { } // Happend only on ENTER pressed
    }    

    private String makeHtmlViewOfToken(DeckRecognizer.Token token) {
        switch(token.getType())
        {
        case KnownCardWithNumber: 
            return String.format("<div class='knowncard'>%s * %s [%s]</div>", token.getNumber(), token.getCard().getName(), token.getCard().getSet());
        case UnknownCardWithNumber: 
            return String.format("<div class='unknowncard'>%s * %s</div>", token.getNumber(), token.getText());
        case SectionName:
            return String.format("<div class='section'>%s</div>", token.getText());
        case Unknown:
        case Comment:
            return String.format("<div class='comment'>%s</div>", token.getText());
        }
        return "";
    }
    
}

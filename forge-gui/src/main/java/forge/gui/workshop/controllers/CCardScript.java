package forge.gui.workshop.controllers;

import java.io.File;
import java.io.PrintWriter;

import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import forge.Command;
import forge.error.BugReporter;
import forge.gui.framework.ICDoc;
import forge.gui.workshop.views.VCardDesigner;
import forge.gui.workshop.views.VCardScript;
import forge.item.PaperCard;
import forge.util.FileUtil;


/** 
 * Controls the "card script" panel in the workshop UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCardScript implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    
    private PaperCard currentCard;
    private String baseText;
    private boolean isTextDirty;
    
    private CCardScript() {
    	VCardScript.SINGLETON_INSTANCE.getTarScript().getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				updateDirtyFlag();
			}
			
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				updateDirtyFlag();
			}
			
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				 //Plain text components do not fire these events
			}
		});
    }
    
    private void updateDirtyFlag() {
    	isTextDirty = !VCardScript.SINGLETON_INSTANCE.getTarScript().getText().equals(baseText);
    	VCardDesigner.SINGLETON_INSTANCE.getBtnSaveCard().setEnabled(isTextDirty);
    	VCardScript.SINGLETON_INSTANCE.getTabLabel().setText((isTextDirty ? "*" : "") + "Card Script");
    }

    public void showCard(PaperCard card) {
    	if (this.currentCard == card) { return; }
    	this.currentCard = card;

    	String text = "";
        boolean editable = false;
        File sourceFile = card.getRules().getSourceFile();
        if (sourceFile != null) {
        	try {
        		text = FileUtil.readFileToString(sourceFile);
        		editable = true;
        	}
        	catch (final Exception ex) {
        		text = "Couldn't read file - " + sourceFile + "\n\nException:\n" + ex.toString();
        	}
        }
        this.baseText = text;

        JTextArea tarScript = VCardScript.SINGLETON_INSTANCE.getTarScript();
        tarScript.setText(text);
        tarScript.setEditable(editable);
        tarScript.setCaretPosition(0); //keep scrolled to top
    }
    
    public void saveChanges() {
    	if (this.currentCard == null || !this.isTextDirty) { return; } //not need if text hasn't been changed

        File sourceFile = this.currentCard.getRules().getSourceFile();
        if (sourceFile == null) { return; }
    	
    	try {
    		String text = VCardScript.SINGLETON_INSTANCE.getTarScript().getText();

            PrintWriter p = new PrintWriter(sourceFile);
            p.print(text);
            p.close();
            
            this.baseText = text;
            updateDirtyFlag();
        } catch (final Exception ex) {
            BugReporter.reportException(ex);
            throw new RuntimeException("FileUtil : writeFile() error, problem writing file - " + sourceFile + " : " + ex);
        }
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }
}

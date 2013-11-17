package forge.gui.workshop.controllers;

import java.io.File;
import java.io.PrintWriter;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import forge.Command;
import forge.Singletons;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FTextEditor;
import forge.gui.workshop.menus.WorkshopFileMenu;
import forge.gui.workshop.views.VCardDesigner;
import forge.gui.workshop.views.VCardScript;
import forge.gui.workshop.views.VWorkshopCatalog;
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
    	VCardScript.SINGLETON_INSTANCE.getTxtScript().addDocumentListener(new DocumentListener() {
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
    	boolean isTextNowDirty = !VCardScript.SINGLETON_INSTANCE.getTxtScript().getText().equals(baseText);
    	if (this.isTextDirty == isTextNowDirty) { return; }
    	this.isTextDirty = isTextNowDirty;
    	VCardDesigner.SINGLETON_INSTANCE.getBtnSaveCard().setEnabled(isTextNowDirty);
    	VCardScript.SINGLETON_INSTANCE.getTabLabel().setText((isTextNowDirty ? "*" : "") + "Card Script");
    	WorkshopFileMenu.updateSaveEnabled();
    }

    public PaperCard getCurrentCard() {
    	return this.currentCard;
    }

    public void showCard(PaperCard card) {
    	if (this.currentCard == card) { return; }
    	if (!canSwitchAway(true)) { //ensure current card saved before changing to a different card
    		VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().setSelectedItem(this.currentCard); //return selection to current card //TODO: fix so clicking away again doesn't cause weird selection problems
    		return;
    	}

    	this.currentCard = card;
    	refresh();
    }
    
    public void refresh() {
    	String text = "";
        boolean editable = false;
        if (this.currentCard != null) {
	        File sourceFile = this.currentCard.getRules().getSourceFile();
	        if (sourceFile != null) {
	        	try {
	        		text = FileUtil.readFileToString(sourceFile);
	        		editable = true;
	        	}
	        	catch (final Exception ex) {
	        		text = "Couldn't read file - " + sourceFile + "\n\nException:\n" + ex.toString();
	        	}
	        }
        }
        this.baseText = text;

        FTextEditor txtScript = VCardScript.SINGLETON_INSTANCE.getTxtScript();
        txtScript.setText(text);
        txtScript.setEditable(editable);
        txtScript.setCaretPosition(0); //keep scrolled to top
    }
    
    public boolean hasChanges() {
    	return (this.currentCard != null && this.isTextDirty);
    }
    
    public boolean canSwitchAway(boolean isCardChanging) {
    	if (!hasChanges()) { return true; }

    	Singletons.getControl().ensureScreenActive(FScreen.WORKSHOP_SCREEN); //ensure Workshop is active before showing dialog
        final int choice = JOptionPane.showConfirmDialog(JOptionPane.getRootFrame(),
                "Save changes to " + this.currentCard + "?",
                "Save Changes?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.CANCEL_OPTION) { return false; }

        if (choice == JOptionPane.YES_OPTION && !saveChanges()) { return false; }

        if (!isCardChanging) {
        	refresh(); //refresh if current card isn't changing to restore script text from file
        }
        return true;
    }
    
    public boolean saveChanges() {
    	if (!hasChanges()) { return true; } //not need if text hasn't been changed

        File sourceFile = this.currentCard.getRules().getSourceFile();
        if (sourceFile == null) { return true; }
    	
    	try {
    		String text = VCardScript.SINGLETON_INSTANCE.getTxtScript().getText();

            PrintWriter p = new PrintWriter(sourceFile);
            p.print(text);
            p.close();
            
            this.baseText = text;
            updateDirtyFlag();
            return true;
        } catch (final Exception ex) {
        	JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "FileUtil : writeFile() error, problem writing file - " + sourceFile + " : " + ex);
        	return false;
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

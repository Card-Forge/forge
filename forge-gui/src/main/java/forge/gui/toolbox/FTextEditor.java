package forge.gui.toolbox;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

@SuppressWarnings("serial")
public class FTextEditor extends JScrollPane {
	private final JTextArea tarEditor;
	
	public FTextEditor() {
		tarEditor = new JTextArea();
        FSkin.JTextComponentSkin<JTextArea> skin = FSkin.get(tarEditor);
        skin.setFont(FSkin.getFixedFont(16));
        skin.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        skin.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        skin.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        tarEditor.setMargin(new Insets(3, 3, 3, 3));
        tarEditor.getDocument().addUndoableEditListener(new MyUndoableEditListener());
        tarEditor.addKeyListener(new KeyAdapter() {
        	@Override
            public void keyPressed(KeyEvent e) {
        		if (e.isControlDown() && !e.isMetaDown()) {
        			switch (e.getKeyCode()) {
        			case KeyEvent.VK_Z:
        				if (e.isShiftDown()) {
        					redoAction.actionPerformed(null);
        				}
        				else {
        					undoAction.actionPerformed(null);
        				}
        				break;
        			case KeyEvent.VK_Y:
        				if (!e.isShiftDown()) {
        					redoAction.actionPerformed(null);
        				}
        				break;
        			}
        		}
            }
        });

        this.setViewportView(tarEditor);
        this.setBorder(null);
        this.setOpaque(false);
	}
	
	//Mapped functions to JTextArea
	@Override
	public boolean isEnabled() {
		return tarEditor.isEnabled();
	}
	@Override
	public void setEnabled(boolean enabled) {
		tarEditor.setEnabled(enabled);
	}
	public String getText() {
		return tarEditor.getText();
	}
	public void setText(String t) {
		tarEditor.setText(t);
		undoManager.discardAllEdits();
	}
	public boolean isEditable() {
		return tarEditor.isEditable();
	}
	public void setEditable(boolean b) {
		tarEditor.setEditable(b);
	}
	public int getCaretPosition() {
		return tarEditor.getCaretPosition();
	}
	public void setCaretPosition(int position) {
		tarEditor.setCaretPosition(position);
	}
	public void addDocumentListener(DocumentListener listener) {
		tarEditor.getDocument().addDocumentListener(listener);
	}
	
	//Undo/Redo
	private UndoAction undoAction = new UndoAction();
	private RedoAction redoAction = new RedoAction();
	private UndoManager undoManager = new UndoManager();
	
	private class MyUndoableEditListener implements UndoableEditListener {
		public void undoableEditHappened(UndoableEditEvent e) {
			undoManager.addEdit(e.getEdit());
			undoAction.updateUndoState();
			redoAction.updateRedoState();
		}
	}
	
	private class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo");
            setEnabled(false);
        }
 
        public void actionPerformed(ActionEvent e) {
            try {
                undoManager.undo();
            } catch (CannotUndoException ex) {
                System.out.println("Unable to undo: " + ex);
                ex.printStackTrace();
            }
            updateUndoState();
            redoAction.updateRedoState();
        }
 
        protected void updateUndoState() {
            if (undoManager.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }
 
	private class RedoAction extends AbstractAction {
        public RedoAction() {
            super("Redo");
            setEnabled(false);
        }
 
        public void actionPerformed(ActionEvent e) {
            try {
                undoManager.redo();
            } catch (CannotRedoException ex) {
                System.out.println("Unable to redo: " + ex);
                ex.printStackTrace();
            }
            updateRedoState();
            undoAction.updateUndoState();
        }
 
        protected void updateRedoState() {
            if (undoManager.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }
}

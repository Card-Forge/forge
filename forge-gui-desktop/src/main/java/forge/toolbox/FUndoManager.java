package forge.toolbox;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/*
 **  This class will merge individual edits into a single larger edit.
 **  That is, characters entered sequentially will be grouped together and
 **  undone as a group. Any attribute changes will be considered as part
 **  of the group and will therefore be undone when the group is undone.
 */
@SuppressWarnings("serial")
public class FUndoManager extends UndoManager implements DocumentListener {
    private final UndoManager undoManager;
    private CompoundEdit compoundEdit;
    private final JTextComponent textComponent;
    private final UndoAction undoAction;
    private final RedoAction redoAction;

    //  These fields are used to help determine whether the edit is an
    //  incremental edit. The offset and length should increase by 1 for
    //  each character added or decrease by 1 for each character removed.

    private int lastOffset;
    private int lastLength;

    public FUndoManager(final JTextComponent textComponent) {
        this.textComponent = textComponent;
        undoManager = this;
        undoAction = new UndoAction();
        redoAction = new RedoAction();
        textComponent.getDocument().addUndoableEditListener(this);
    }

    /*
     **  Add a DocumentLister before the undo is done so we can position
     **  the Caret correctly as each edit is undone.
     */
    @Override
    public synchronized void undo() {
        if (canUndo()) {
            textComponent.getDocument().addDocumentListener(this);
            super.undo();
            textComponent.getDocument().removeDocumentListener(this);
        }
    }

    /*
     **  Add a DocumentLister before the redo is done so we can position
     **  the Caret correctly as each edit is redone.
     */
    @Override
    public synchronized void redo() {
        if (canRedo()) {
            textComponent.getDocument().addDocumentListener(this);
            super.redo();
            textComponent.getDocument().removeDocumentListener(this);
        }
    }

    /*
     **  Whenever an UndoableEdit happens the edit will either be absorbed
     **  by the current compound edit or a new compound edit will be started
     */
    @Override
    public void undoableEditHappened(final UndoableEditEvent e) {
        //  Start a new compound edit
        if (compoundEdit == null) {
            compoundEdit = startCompoundEdit(e.getEdit());
            return;
        }

        final int offsetChange = textComponent.getCaretPosition() - lastOffset;
        final int lengthChange = textComponent.getDocument().getLength() - lastLength;

        //  Check for an attribute change
        final AbstractDocument.DefaultDocumentEvent event = (AbstractDocument.DefaultDocumentEvent)e.getEdit();

        if  (event.getType().equals(DocumentEvent.EventType.CHANGE)) {
            if (offsetChange == 0) {
                compoundEdit.addEdit(e.getEdit());
                return;
            }
        }

        //  Check for an incremental edit or backspace.
        //  The Change in Caret position and Document length should both be
        //  either 1 or -1.
        if (offsetChange == lengthChange &&  Math.abs(offsetChange) == 1) {
            compoundEdit.addEdit(e.getEdit());
            lastOffset = textComponent.getCaretPosition();
            lastLength = textComponent.getDocument().getLength();
            return;
        }

        //  Not incremental edit, end previous edit and start a new one
        compoundEdit.end();
        compoundEdit = startCompoundEdit(e.getEdit());
    }

    /*
     **  Each CompoundEdit will store a group of related incremental edits
     **  (ie. each character typed or backspaced is an incremental edit)
     */
    private CompoundEdit startCompoundEdit(final UndoableEdit anEdit) {
        //  Track Caret and Document information of this compound edit
        lastOffset = textComponent.getCaretPosition();
        lastLength = textComponent.getDocument().getLength();

        //  The compound edit is used to store incremental edits
        compoundEdit = new MyCompoundEdit();
        compoundEdit.addEdit(anEdit);

        //  The compound edit is added to the UndoManager. All incremental
        //  edits stored in the compound edit will be undone/redone at once
        addEdit(compoundEdit);

        undoAction.updateUndoState();
        redoAction.updateRedoState();

        return compoundEdit;
    }

    /*
     *  The Action to Undo changes to the Document.
     *  The state of the Action is managed by the CompoundUndoManager
     */
    public Action getUndoAction() {
        return undoAction;
    }

    /*
     *  The Action to Redo changes to the Document.
     *  The state of the Action is managed by the CompoundUndoManager
     */
    public Action getRedoAction() {
        return redoAction;
    }

    /*
     *  Updates to the Document as a result of Undo/Redo will cause the
     *  Caret to be repositioned
     */
    @Override
    public void insertUpdate(final DocumentEvent e) {
        int offset = e.getOffset() + e.getLength();
        offset = Math.min(offset, textComponent.getDocument().getLength());
        textComponent.setCaretPosition(offset);
    }

    @Override
    public void removeUpdate(final DocumentEvent e) {
        textComponent.setCaretPosition(e.getOffset());
    }

    @Override
    public void changedUpdate(final DocumentEvent e) {}

    private class MyCompoundEdit extends CompoundEdit {
        @Override
        public boolean isInProgress() {
            //  in order for the canUndo() and canRedo() methods to work
            //  assume that the compound edit is never in progress
            return false;
        }

        @Override
        public void undo() throws CannotUndoException {
            //  End the edit so future edits don't get absorbed by this edit
            if (compoundEdit != null) {
                compoundEdit.end();
            }

            super.undo();

            //  Always start a new compound edit after an undo
            compoundEdit = null;
        }
    }

    /*
     *	Perform the Undo and update the state of the undo/redo Actions
     */
    private class UndoAction extends AbstractAction {
        public UndoAction() {
            putValue(Action.NAME, "Undo");
            putValue(Action.SHORT_DESCRIPTION, getValue(Action.NAME));
            putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_U));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            try {
                undoManager.undo();
                textComponent.requestFocusInWindow();
            }
            catch (final CannotUndoException ex) {}

            updateUndoState();
            redoAction.updateRedoState();
        }

        private void updateUndoState() {
            setEnabled(undoManager.canUndo());
        }
    }

    /*
     *	Perform the Redo and update the state of the undo/redo Actions
     */
    private class RedoAction extends AbstractAction {
        public RedoAction() {
            putValue(Action.NAME, "Redo");
            putValue(Action.SHORT_DESCRIPTION, getValue(Action.NAME));
            putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            try {
                undoManager.redo();
                textComponent.requestFocusInWindow();
            }
            catch (final CannotRedoException ex) {}

            updateRedoState();
            undoAction.updateUndoState();
        }

        protected void updateRedoState() {
            setEnabled(undoManager.canRedo());
        }
    }
}

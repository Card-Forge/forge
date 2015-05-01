package forge.toolbox;

import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.border.Border;
import javax.swing.event.DocumentListener;

import forge.gui.MouseUtil;
import forge.toolbox.FSkin.SkinnedScrollPane;
import forge.toolbox.FSkin.SkinnedTextArea;

@SuppressWarnings("serial")
public class FTextEditor extends SkinnedScrollPane {
    private final SkinnedTextArea tarEditor;
    private final FUndoManager undoManager;

    public FTextEditor() {
        tarEditor = new SkinnedTextArea();
        tarEditor.setFont(FSkin.getFixedFont(16));
        tarEditor.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tarEditor.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        tarEditor.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        undoManager = new FUndoManager(tarEditor);

        tarEditor.setMargin(new Insets(3, 3, 3, 3));
        tarEditor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.isControlDown() && !e.isMetaDown()) {
                    switch (e.getKeyCode()) {
                    case KeyEvent.VK_Z:
                        if (e.isShiftDown()) {
                            undoManager.redo();
                        }
                        else {
                            undoManager.undo();
                        }
                        break;
                    case KeyEvent.VK_Y:
                        if (!e.isShiftDown()) {
                            undoManager.redo();
                        }
                        break;
                    }
                }
            }
        });

        this.setViewportView(tarEditor);
        this.setBorder((Border)null);
        this.setOpaque(false);

        if (this.isEditable()) {
            MouseUtil.setComponentCursor(tarEditor, Cursor.TEXT_CURSOR);
        }
    }

    //Mapped functions to JTextArea
    @Override
    public boolean isEnabled() {
        return tarEditor.isEnabled();
    }
    @Override
    public void setEnabled(final boolean enabled) {
        tarEditor.setEnabled(enabled);
    }
    public String getText() {
        return tarEditor.getText();
    }
    public void setText(final String t) {
        tarEditor.setText(t);
        undoManager.discardAllEdits();
    }
    public boolean isEditable() {
        return tarEditor.isEditable();
    }
    public void setEditable(final boolean b) {
        tarEditor.setEditable(b);
    }
    public int getCaretPosition() {
        return tarEditor.getCaretPosition();
    }
    public void setCaretPosition(final int position) {
        tarEditor.setCaretPosition(position);
    }
    public void addDocumentListener(final DocumentListener listener) {
        tarEditor.getDocument().addDocumentListener(listener);
    }
}

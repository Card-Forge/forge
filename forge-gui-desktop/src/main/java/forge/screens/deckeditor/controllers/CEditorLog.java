package forge.screens.deckeditor.controllers;

import java.awt.Color;

import forge.gui.FThreads;
import forge.gui.framework.ICDoc;
import forge.screens.deckeditor.views.VEditorLog;

/**
 * Controls the "editor log" panel in the deck editor UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CEditorLog implements ICDoc {
    SINGLETON_INSTANCE;

    /** */
    CEditorDraftingProcess draftingProcess;

    private final VEditorLog view;

    CEditorLog() {
        this.view = VEditorLog.SINGLETON_INSTANCE;
    }

    //========== Overridden methods

    public final VEditorLog getView() {
        return view;
    }

    public final void addLogEntry(final String entry) {
        view.addLogEntry(entry);
    }

    public final void addLogEntry(final String message, final Color foreground) {
        view.addLogEntry(message, foreground);
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        FThreads.invokeInEdtNowOrLater(reset);
    }

    private final Runnable reset = new Runnable() {
        @Override
        public void run() {
            view.resetNewDraft();
        }
    };

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        FThreads.invokeInEdtNowOrLater(view::updateConsole);
    }
}

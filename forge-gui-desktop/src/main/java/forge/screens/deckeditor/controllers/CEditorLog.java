package forge.screens.deckeditor.controllers;

import forge.gui.FThreads;
import forge.gui.framework.ICDoc;
import forge.screens.deckeditor.views.VEditorLog;
import forge.screens.match.views.VLog;

/**
 * Controls the "editor log" panel in the deck editor UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public class CEditorLog implements ICDoc {
    /** */
    CEditorDraftingProcess draftingProcess;

    private final VEditorLog view;

    CEditorLog(final CEditorDraftingProcess draftingProcess) {
        this.draftingProcess = draftingProcess;
        this.view = new VEditorLog(this);
    }

    //========== Overridden methods

    public final VEditorLog getView() {
        return view;
    }

    public final void addLogEntry(final String entry) {
        view.addLogEntry(entry);
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    private final Runnable r = new Runnable() {
        @Override
        public void run() {
            view.updateConsole();
        }
    };

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        FThreads.invokeInEdtNowOrLater(r);
    }
}

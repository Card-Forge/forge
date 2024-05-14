package forge.screens.deckeditor.views;

import javax.swing.JPanel;

import com.google.common.collect.Lists;
import forge.gui.framework.*;
import forge.screens.deckeditor.controllers.CEditorDraftingProcess;
import forge.screens.deckeditor.controllers.CEditorLog;
import forge.screens.match.GameLogPanel;
import forge.screens.match.controllers.CLog;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import java.util.List;

/**
 * Assembles Swing components of card catalog in deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public class VEditorLog implements IVDoc<CEditorLog> {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblEditorLog"));

    private final GameLogPanel gameLog;

    private final JPanel pnlContent = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final FScrollPane scroller = new FScrollPane(pnlContent, false);


    private final List<String> editorLogEntries = Lists.newArrayList();

    private final CEditorLog controller;


    public VEditorLog(final CEditorLog controller) {
        this.controller = controller;
        pnlContent.setOpaque(false);
        scroller.getViewport().setBorder(null);

        this.gameLog = new GameLogPanel();
    }

    //========== Overridden from IVDoc

    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_LOG;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CEditorLog getLayoutControl() {
        return controller;
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    @Override
    public void populate() {
        final JPanel parentBody = parentCell.getBody();
        parentBody.setLayout(new MigLayout("insets 5, gap 0, wrap, hidemode 3"));
        // Add the panel that contains the log entries
        parentBody.add(gameLog, "w 10:100%, h 100%");
    }

    private void resetNewDraft() {
        // Should we store the draft?
        gameLog.reset();
        editorLogEntries.clear();
    }

    public void updateConsole() {
        resetNewDraft();
    }

    public void addLogEntry(String entry) {
        System.out.println(entry);
        gameLog.addLogEntry(entry);
        this.editorLogEntries.add(entry);
    }
}

package forge.screens.deckeditor.views;

import com.google.common.collect.Lists;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.item.PaperCard;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorLog;
import forge.screens.match.GameLogPanel;
import forge.toolbox.FScrollPane;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.Color;
import java.util.List;

/**
 * Assembles Swing components of card catalog in deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VEditorLog implements IVDoc<CEditorLog> {
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblEditorLog"));

    private final GameLogPanel gameLog;

    private final JPanel pnlContent = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final FScrollPane scroller = new FScrollPane(pnlContent, false);


    private record LogEntry(String text, Color color, PaperCard card) { }
    private final List<LogEntry> entries = Lists.newArrayList();
    private boolean draftLogVisible = true;

    VEditorLog() {
        pnlContent.setOpaque(false);
        scroller.getViewport().setBorder(null);

        this.gameLog = new GameLogPanel();
        gameLog.setOnItemHover(card -> CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture().showItem(card));
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

    public void showView() {
        tab.setVisible(true);
        pnlContent.setOpaque(true);
        pnlContent.setVisible(true);
    }

    /** Only a draft fills this panel, but its tab can be dragged into another editor's layout afterwards. */
    public void setDraftMode(boolean draft) {
        tab.setText(localizer.getMessage(draft ? "lblDraftLog" : "lblEditorLog"));
        tab.repaintSelf();
    }

    @Override
    public CEditorLog getLayoutControl() {
        return CEditorLog.SINGLETON_INSTANCE;
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

    public void resetNewDraft() {
        gameLog.reset();
        entries.clear();
    }

    public void updateConsole() {
        gameLog.updateUI();
    }

    public void addLogEntry(String entry) {
        addLogEntry(entry, null);
    }

    public void addLogEntry(String entry, Color foreground) {
        addLogEntry(entry, foreground, null);
    }

    public void addLogEntry(String entry, Color foreground, PaperCard card) {
        entries.add(new LogEntry(entry, foreground, card));
        if (draftLogVisible) {
            gameLog.addLogEntry(entry, foreground, card);
        }
    }

    public void setDraftLogVisible(boolean visible) {
        if (draftLogVisible == visible) return;
        draftLogVisible = visible;
        gameLog.reset();
        if (visible) {
            for (LogEntry e : entries) gameLog.addLogEntry(e.text, e.color, e.card);
        }
    }
}

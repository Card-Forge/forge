/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import com.google.common.collect.Lists;

import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameView;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.GameLogPanel;
import forge.screens.match.controllers.CLog;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinFont;

/**
 * Assembles Swing components of game log report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VLog implements IVDoc<CLog> {

    // Keeps a record of log entries currently displayed so we can
    // easily identify new entries to be added to the game log.
    private final List<GameLogEntry> displayedLogEntries = Lists.newArrayList();

    // Used to determine when a new game has started.
    private GameView gameLogModel = null;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Log");

    // Other fields
    private final GameLogPanel gameLog;
    private JPanel p = null;

    private final CLog controller;

    //========== Constructor
    public VLog(final CLog controller) {
        this.controller = controller;
        gameLog = new GameLogPanel();
    }

    //========== Overridden methods
    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        // (Panel uses observers to update, no permanent components here.)
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell()
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_LOG;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CLog getLayoutControl() {
        return controller;
    }

    /**
     * Called whenever there are new log entries to be displayed.
     * <p>
     * This is an Observer update method.
     * <p>
     * @param model contains list of log entries.
     */
    public void updateConsole() {
        final GameView model = controller.getMatchUI().getGameView();
        if (isGameLogConsoleVisible() && model != null) {
            resetDisplayIfNewGame(model);
            displayNewGameLogEntries(model);
            // Important : refreshLayout() needs to be called every update.
            refreshLayout();
        }
    }

    private boolean isGameLogConsoleVisible() {
        return parentCell.getSelected().equals(this);
    }

    private void resetDisplayIfNewGame(final GameView model) {
        if (this.gameLogModel != model) {
            gameLog.reset();
            this.displayedLogEntries.clear();
            this.gameLogModel = model;
        }
    }

    /**
     * Refreshes game log console container.
     * <p>
     * For some reason this needs to be called every time the console
     * is updated with a new event, otherwise if the console is dragged
     * to a new tab the log display disappears.
     * <p>
     * Since it is simply swapping in/out a reference to an existing object
     * I don't think it is a major concern but should probably try to
     * come up with more elegant solution some time.
     */
    private void refreshLayout() {
        //TODO: Find a way to avoid calling refreshLayout() on every update.
        p = parentCell.getBody();
        p.remove(gameLog);
        p.setLayout(new MigLayout("insets 1"));
        p.add(gameLog, "w 10:100%, h 100%");
    }

    private void displayNewGameLogEntries(final GameView model) {
        final List<GameLogEntry> newLogEntries = Lists.reverse(getNewGameLogEntries(model));
        if (newLogEntries.size() > 0) {
            addNewLogEntriesToJPanel(newLogEntries);
        }
    }

    private List<GameLogEntry> getNewGameLogEntries(final GameView model) {
        final String logEntryType = FModel.getPreferences().getPref(FPref.DEV_LOG_ENTRY_TYPE);
        final GameLogEntryType logVerbosityFilter = GameLogEntryType.valueOf(logEntryType);
        if (model != null && model.getGameLog() != null) {
            final List<GameLogEntry> logEntries = model.getGameLog().getLogEntries(logVerbosityFilter);
            // Set subtraction - remove all log entries from new list which are already displayed.
            logEntries.removeAll(this.displayedLogEntries);
            return logEntries;
        }
        return new ArrayList<GameLogEntry>();
    }

    private void addNewLogEntriesToJPanel(final List<GameLogEntry> newLogEntries) {
        for (final GameLogEntry logEntry : newLogEntries) {
            gameLog.setTextFont(getJTextAreaFont(logEntry.type));
            gameLog.addLogEntry(logEntry.message);
            this.displayedLogEntries.add(logEntry);
        }
    }

    private static SkinFont getJTextAreaFont(final GameLogEntryType logEntryType) {
        final boolean isNewTurn = (logEntryType == GameLogEntryType.TURN);
        return (isNewTurn ? FSkin.getBoldFont() : FSkin.getFont());
    }
}

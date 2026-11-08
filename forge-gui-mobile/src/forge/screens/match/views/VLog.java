package forge.screens.match.views;

import java.util.List;
import java.util.function.Supplier;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.game.GameLog;
import forge.game.GameLogEntry;
import forge.game.GameLogVerbosity;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FDropDown;
import forge.menu.FMenuTab;
import forge.model.FModel;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

public class VLog extends FDropDown {
    private static final float PADDING = Utils.scale(5);
    private static final FSkinFont FONT = FSkinFont.get(11);
    private static FSkinColor getAltRowColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_ZEBRA);
        return FSkinColor.get(Colors.CLR_ZEBRA);
    }
    private static FSkinColor getRowColor() {
        return getAltRowColor().darker();
    }
    private static FSkinColor getForeColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT);
        return FSkinColor.get(Colors.CLR_TEXT);
    }

    protected final Supplier<GameLog> logSupplier;
    private final boolean showAllEntries;

    protected FDisplayObject owner;

    public VLog(Supplier<GameLog> logSupplier) {
        this(logSupplier, false);
    }

    /** {@code showAllEntries} bypasses the match-log verbosity preference — the draft
     *  log holds only DRAFT entries, which the default verbosity would filter out. */
    public VLog(Supplier<GameLog> logSupplier, boolean showAllEntries) {
        this.logSupplier = logSupplier;
        this.showAllEntries = showAllEntries;
    }

    @Override
    protected boolean autoHide() {
        return !isArenaDock(); //the Arena board docks the log: it stays until it is put away
    }

    // ---- the Arena board: the log docked on the left, on dark glass ------------------------------------------------
    private final com.badlogic.gdx.math.Rectangle arenaDock = new com.badlogic.gdx.math.Rectangle();

    private static boolean isArenaDock() {
        return forge.screens.match.arena.ArenaTheme.isMatchBoard() && forge.screens.match.arena.ArenaMatchLayout.applies();
    }

    @Override
    protected void updateSizeAndPosition() {
        if (!isArenaDock()) {
            super.updateSizeAndPosition();
            return;
        }
        forge.screens.match.arena.ArenaMatchLayout.logDock(Forge.getScreenWidth(), Forge.getScreenHeight(), arenaDock);
        paneSize = updateAndGetPaneSize(arenaDock.width, arenaDock.height);
        setBounds(arenaDock.x, arenaDock.y, arenaDock.width, Math.min(paneSize.getHeight(), arenaDock.height));
    }

    @Override
    protected void drawOverlay(Graphics g) {
        if (isArenaDock()) {
            return; //the glass has its own rim
        }
        super.drawOverlay(g);
    }

    @Override
    public void setMenuTab(FMenuTab menuTab) {
        super.setMenuTab(menuTab);
        this.owner = menuTab;
    }

    public void setDropdownOwner(FDisplayObject owner) {
        this.owner = owner;
    }

    @Override
    protected FDisplayObject getDropDownOwner() {
        return owner;
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (isArenaDock()) {
            forge.screens.match.arena.ArenaArt.glassPanel(g, 0, 0, getWidth(), getHeight());
            return;
        }
        float w = getWidth();
        float h = getHeight();
        g.fillRect(getRowColor(), 0, 0, w, h); //can fill background with main row color since drop down will never be taller than number of rows
    }

    /** Re-lay out the open dropdown so newly added entries appear without reopening it. */
    public void refresh() {
        if (isVisible()) {
            updateSizeAndPosition();
        }
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        clear();

        List<GameLogEntry> logEntrys;
        if (showAllEntries) {
            logEntrys = logSupplier.get().getLogEntries(null);
        } else {
            GameLogVerbosity verbosity = GameLogVerbosity.fromString(FModel.getPreferences().getPref(FPref.DEV_LOG_ENTRY_TYPE));
            if (verbosity == GameLogVerbosity.CUSTOM) {
                logEntrys = logSupplier.get().getLogEntriesForTypes(
                        FModel.getPreferences().getCustomLogTypes());
            } else {
                logEntrys = logSupplier.get().getLogEntriesForVerbosity(verbosity);
            }
        }

        LogEntryDisplay logEntryDisplay;
        float width = isArenaDock() ? maxWidth : maxWidth - getDropDownOwner().screenPos.x; //stretch from tab to edge of screen
        float minWidth = 4 * Utils.AVG_FINGER_WIDTH;
        if (width < minWidth) {
            width = minWidth;
        }

        float y = 1;
        float height;
        if (logEntrys.isEmpty()) {
            logEntryDisplay = add(new LogEntryDisplay("[Empty]", false));
            height = logEntryDisplay.getMinHeight(width);
            logEntryDisplay.setBounds(0, y, width, height);
            y += height;
        }
        else {
            boolean isAltRow = false;
            for (int i = logEntrys.size() - 1; i >= 0; i--) { //show latest entry on bottom
                logEntryDisplay = add(new LogEntryDisplay(logEntrys.get(i).message(), isAltRow));
                height = logEntryDisplay.getMinHeight(width);
                logEntryDisplay.setBounds(0, y, width, height);
                isAltRow = !isAltRow;
                y += height;
            }
        }

        return new ScrollBounds(width, y + 1);
    }

    @Override
    protected void setScrollPositionsAfterLayout(float scrollLeft0, float scrollTop0) {
        super.setScrollPositionsAfterLayout(0, getMaxScrollTop()); //always scroll to bottom after layout
    }

    private static final com.badlogic.gdx.graphics.Color ARENA_ROW = new com.badlogic.gdx.graphics.Color();

    private class LogEntryDisplay extends FDisplayObject {
        private final String text;
        private final boolean isAltRow;
        private final TextRenderer renderer = new TextRenderer(true);

        private LogEntryDisplay(String text0, boolean isAltRow0) {
            text = text0;
            isAltRow = isAltRow0;
        }

        private float getMinHeight(float width) {
            width -= 2 * PADDING; //account for left and right insets
            float height = renderer.getWrappedBounds(text, FONT, width).height;
            height += 2 * PADDING;
            return Math.round(height);
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();

            if (isArenaDock()) { //light words on the glass, every other entry on a faint band
                if (isAltRow) {
                    g.fillRect(forge.screens.match.arena.ArenaTheme.alpha(forge.screens.match.arena.ArenaTheme.WHITE, 0.04f, ARENA_ROW), 0, 0, w, h);
                }
                renderer.drawText(g, text, FONT, forge.screens.match.arena.ArenaTheme.TEXT, PADDING, PADDING, w - 2 * PADDING, h, 0, h, true, Align.left, false);
                return;
            }
            if (isAltRow) {
                g.fillRect(getAltRowColor(), 0, 0, w, h);
            }

            //use full height without padding so text not scaled down
            renderer.drawText(g, text, FONT, getForeColor(), PADDING, PADDING, w - 2 * PADDING, h, 0, h, true, Align.left, false);
        }
    }
}

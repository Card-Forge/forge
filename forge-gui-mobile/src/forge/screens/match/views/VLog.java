package forge.screens.match.views;

import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.assets.FSkinColor.Colors;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FDropDown;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

public class VLog extends FDropDown {
    private static final float PADDING = Utils.scale(5);
    private static final FSkinFont FONT = FSkinFont.get(11);
    private static final FSkinColor ALT_ROW_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    private static final FSkinColor ROW_COLOR = ALT_ROW_COLOR.darker();
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    public VLog() {
    }

    @Override
    protected boolean autoHide() {
        return true;
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(ROW_COLOR, 0, 0, w, h); //can fill background with main row color since drop down will never be taller than number of rows
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        clear();

        GameLogEntryType logVerbosityFilter = GameLogEntryType.valueOf(FModel.getPreferences().getPref(FPref.DEV_LOG_ENTRY_TYPE));
        List<GameLogEntry> logEntrys = MatchController.instance.getGameView().getGameLog().getLogEntries(logVerbosityFilter);

        LogEntryDisplay logEntryDisplay;
        float width = maxWidth - getMenuTab().screenPos.x; //stretch from tab to edge of screen
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
                logEntryDisplay = add(new LogEntryDisplay(logEntrys.get(i).message, isAltRow));
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

            if (isAltRow) {
                g.fillRect(ALT_ROW_COLOR, 0, 0, w, h);
            }

            //use full height without padding so text not scaled down
            renderer.drawText(g, text, FONT, FORE_COLOR, PADDING, PADDING, w - 2 * PADDING, h, 0, h, true, Align.left, false);
        }
    }
}

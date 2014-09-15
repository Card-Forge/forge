package forge.screens.match.views;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.menu.FDropDown;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.FControl;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FList;
import forge.util.Utils;
import forge.view.CardView;
import forge.view.PlayerView;

public class VPlayers extends FDropDown {
    public VPlayers() {
        for (final PlayerView p : FControl.getGameView().getPlayers()) {
            add(new PlayerInfoPanel(p));
        }
    }

    @Override
    protected boolean autoHide() {
        return true;
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        float totalHeight = getChildCount() * PlayerInfoPanel.HEIGHT;
        float y = totalHeight - PlayerInfoPanel.HEIGHT;
        //display in reverse order so gui player on bottom
        for (FDisplayObject panel : getChildren()) {
            panel.setBounds(0, y, maxWidth, PlayerInfoPanel.HEIGHT);
            y -= PlayerInfoPanel.HEIGHT;
        }
        return new ScrollBounds(maxWidth, totalHeight);
    }

    private static class PlayerInfoPanel extends FContainer {
        private static final FSkinFont FONT = FSkinFont.get(12);
        private static final float PADDING = Utils.scaleMin(5);
        private static final float HEIGHT = Utils.AVG_FINGER_HEIGHT * 1.8f;
        private final PlayerView player;

        private PlayerInfoPanel(PlayerView player0) {
            player = player0;
        }

        @Override
        protected void doLayout(float width, float height) {
            //TODO: Add FLabels to click to select player in top or bottom panel of Match screen
        }

        @Override
        public void drawBackground(Graphics g) {
            float x = PADDING;
            float y = PADDING;
            float h = getHeight() - 2 * y;

            FImage avatarImage = FControl.getPlayerAvatar(player);
            g.drawImage(avatarImage, x, y, h, h);
            x += h + PADDING;

            StringBuilder builder = new StringBuilder();
            builder.append(player.getName());
            builder.append("\nLife: " + String.valueOf(player.getLife()));
            builder.append("  |  Poison counters: " + String.valueOf(player.getPoisonCounters()));
            builder.append("  |  Maximum hand size: " + String.valueOf(player.getMaxHandSize()));
            builder.append("  |  Cards drawn this turn: " + String.valueOf(player.getNumDrawnThisTurn()));
            builder.append("  |  Damage Prevention: " + String.valueOf(player.getPreventNextDamage()));
            if (!player.getKeywords().isEmpty()) {
                builder.append("  |  " + player.getKeywords().toString());
            }
            if (FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE)) {
                List<CardView> list = player.getAnteCards();
                builder.append("  |  Ante'd: ");
                for (int i = 0; i < list.size(); i++) {
                    builder.append(list.get(i));
                    if (i < (list.size() - 1)) {
                        builder.append(", ");
                    }
                }
            }
            if (FControl.getGameView().isCommander()) {
                builder.append("  |  " + player.getCommanderInfo());
            }

            g.drawText(builder.toString(), FONT, FList.FORE_COLOR, x, y, getWidth() - PADDING - x, h, true, HAlignment.LEFT, true);
        }

        @Override
        public void drawOverlay(Graphics g) {
            //draw bottom border
            float y = getHeight();
            g.drawLine(1, FList.LINE_COLOR, 0, y, getWidth(), y);
        }
    }
}

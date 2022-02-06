package forge.screens.match.views;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.deck.FDeckViewer;
import forge.game.player.PlayerView;
import forge.menu.FDropDown;
import forge.screens.match.MatchController;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.util.Utils;

public class VPlayers extends FDropDown {
    public VPlayers() {
        for (final PlayerView p : MatchController.instance.getGameView().getPlayers()) {
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
        private static final float PADDING = Utils.scale(5);
        private static final float HEIGHT = Utils.AVG_FINGER_HEIGHT * 1.8f;
        private final PlayerView player;
        private final Deck playerDeck;
        private final FLabel btnDeck;
        private boolean btnAdded = false;

        private PlayerInfoPanel(PlayerView player0) {
            player = player0;
            playerDeck = MatchController.getPlayerDeck(player0);
            btnDeck = new FLabel.ButtonBuilder().opaque(true).iconScaleFactor(0.99f).selectable().alphaComposite(1).iconInBackground(true).build();
            btnDeck.setEnabled(!Forge.isMobileAdventureMode);
            btnDeck.setCommand(new FEvent.FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (playerDeck != null) {
                        //pause game when spectating AI Match
                        if (!MatchController.instance.hasLocalPlayers()) {
                            if(!MatchController.instance.isGamePaused())
                                MatchController.instance.pauseMatch();
                        }
                        FDeckViewer.show(playerDeck);
                    }
                }
            });
        }

        @Override
        protected void doLayout(float width, float height) {
            if (!btnAdded) {
                btnDeck.setBounds(PADDING, PADDING, getHeight() - 2 * PADDING, getHeight() - 2 * PADDING);
                btnDeck.setIcon(MatchController.getPlayerAvatar(player));
                btnDeck.setOverlayIcon(Forge.hdbuttons ? FSkinImage.HDSEARCH : FSkinImage.SEARCH);
                add(btnDeck);
                btnAdded = true;
            }
        }

        @Override
        public void drawBackground(Graphics g) {
            float x = PADDING;
            float y = PADDING;
            float h = getHeight() - 2 * y;
            x += h + PADDING;
            //Draw Player Details
            g.drawText(player.getDetails() + playerDeck.getName(), FONT, FList.FORE_COLOR, x, y, getWidth() - PADDING - x, h, true, Align.left, true);
        }

        @Override
        public void drawOverlay(Graphics g) {
            //draw bottom border
            float y = getHeight();
            g.drawLine(1, FList.LINE_COLOR, 0, y, getWidth(), y);
        }
    }
}

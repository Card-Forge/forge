package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.card.MagicColor;
import forge.game.player.PlayerView;
import forge.match.MatchUtil;
import forge.player.GamePlayerUtil;
import forge.toolbox.FDisplayObject;

public class VManaPool extends VDisplayArea {
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinFont FONT = FSkinFont.get(16);

    private final PlayerView player;
    private final List<ManaLabel> manaLabels = new ArrayList<ManaLabel>();
    private int totalMana;

    public VManaPool(PlayerView player0) {
        player = player0;

        addManaLabel(FSkinImage.MANA_COLORLESS, MagicColor.COLORLESS);
        addManaLabel(FSkinImage.MANA_W, MagicColor.WHITE);
        addManaLabel(FSkinImage.MANA_U, MagicColor.BLUE);
        addManaLabel(FSkinImage.MANA_B, MagicColor.BLACK);
        addManaLabel(FSkinImage.MANA_R, MagicColor.RED);
        addManaLabel(FSkinImage.MANA_G, MagicColor.GREEN);
    }

    private void addManaLabel(FSkinImage image, byte colorCode) {
        manaLabels.add(add(new ManaLabel(image, colorCode)));
    }

    @Override
    public int getCount() {
        return totalMana;
    }

    @Override
    public void update() {
        totalMana = 0;
        for (ManaLabel label : manaLabels) {
            int colorCount = player.getMana(label.colorCode);
            totalMana += colorCount;
            label.text = Integer.toString(colorCount);
        }
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float x = 0;
        float y = 0;
        float labelWidth = visibleWidth / manaLabels.size();
        float labelHeight = visibleHeight;

        for (ManaLabel label : manaLabels) {
            label.setBounds(x, y, labelWidth, labelHeight);
            x += labelWidth;
        }

        return new ScrollBounds(visibleWidth, visibleHeight);
    }

    private class ManaLabel extends FDisplayObject {
        private final FSkinImage image;
        private final byte colorCode;
        private String text = "0";

        private ManaLabel(FSkinImage image0, byte colorCode0) {
            image = image0;
            colorCode = colorCode0;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (player.getLobbyPlayer() == GamePlayerUtil.getGuiPlayer()) {
                MatchUtil.getHumanController().useMana(colorCode);
            }
            return true;
        }

        @Override
        public boolean twoFingerTap(float x, float y, int count) {
            if (player.getLobbyPlayer() == GamePlayerUtil.getGuiPlayer()) {
                //on two finger tap, keep using mana until it runs out or no longer can be put towards the cost
                while (MatchUtil.getHumanController().useMana(colorCode)) {}
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float textHeight = FONT.getCapHeight();
            float gapY = textHeight / 4f;

            float maxImageHeight = getHeight() - textHeight - 3 * gapY;
            float h = image.getNearestHQHeight(maxImageHeight);
            if (h > maxImageHeight) {
                h /= 2;
            }
            float w = image.getWidth() * h / image.getHeight();
            while (w > getWidth()) {
                h /= 2;
                w = image.getWidth() * h / image.getHeight();
            }
            float x = (getWidth() - w) / 2;
            float y = gapY + (maxImageHeight - h) / 2;

            g.drawImage(image, x, y, w, h);

            x = 0;
            y += h + gapY;
            w = getWidth();
            h = getHeight() - y;

            g.drawText(text, FONT, FORE_COLOR, x, y, w, h, false, HAlignment.CENTER, false);
        }
    }
}

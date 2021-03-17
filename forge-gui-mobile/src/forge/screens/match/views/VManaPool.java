package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.player.PlayerView;
import forge.player.GamePlayerUtil;
import forge.screens.match.MatchController;
import forge.toolbox.FDisplayObject;

public class VManaPool extends VDisplayArea {
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinFont FONT = FSkinFont.get(16);

    private final PlayerView player;
    private final List<ManaLabel> manaLabels = new ArrayList<>();
    private int totalMana;

    public VManaPool(PlayerView player0) {
        player = player0;

        addManaLabel(FSkinImage.MANA_COLORLESS, (byte)ManaAtom.COLORLESS);
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

        if (Forge.isLandscapeMode()) {
            float labelWidth = visibleWidth / 2;
            float labelHeight = visibleHeight / 3;

            int count = 0;
            for (ManaLabel label : manaLabels) {
                label.setBounds(x, y, labelWidth, labelHeight);
                if (++count % 2 == 0) {
                    x = 0;
                    y += labelHeight;
                }
                else {
                    x += labelWidth;
                }
            }
        }
        else {
            float labelWidth = visibleWidth / manaLabels.size();
            float labelHeight = visibleHeight;

            for (ManaLabel label : manaLabels) {
                label.setBounds(x, y, labelWidth, labelHeight);
                x += labelWidth;
            }
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
            if (player.isLobbyPlayer(GamePlayerUtil.getGuiPlayer())) {
                MatchController.instance.getGameController().useMana(colorCode);
            }
            return true;
        }

        @Override
        public boolean flick(float x, float y) {
            if (player.isLobbyPlayer(GamePlayerUtil.getGuiPlayer())) {
                //on two finger tap, keep using mana until it runs out or no longer can be put towards the cost
                int oldMana, newMana = player.getMana(colorCode);
                do {
                    oldMana = newMana;
                    MatchController.instance.getGameController().useMana(colorCode);
                    newMana = player.getMana(colorCode);
                }
                while (oldMana != newMana);
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

            g.drawText(text, FONT, FORE_COLOR, x, y, w, h, false, Align.center, false);
        }
    }
}

package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.card.MagicColor;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.net.FServer;
import forge.screens.match.FControl;
import forge.screens.match.input.Input;
import forge.screens.match.input.InputPayMana;
import forge.toolbox.FDisplayObject;

public class VManaPool extends VDisplayArea {
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinFont FONT = FSkinFont.get(16);

    private final Player player;
    private final List<ManaLabel> manaLabels = new ArrayList<ManaLabel>();
    private int totalMana;

    public VManaPool(Player player0) {
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
        ManaPool m = player.getManaPool();
        for (ManaLabel label : manaLabels) {
            int colorCount = m.getAmountOfColor(label.colorCode);
            totalMana += colorCount;
            label.text = Integer.toString(colorCount);
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = 0;
        float y = 0;
        float labelWidth = width / manaLabels.size();
        float labelHeight = height;

        for (ManaLabel label : manaLabels) {
            label.setBounds(x, y, labelWidth, labelHeight);
            x += labelWidth;
        }
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
            if (player.getLobbyPlayer() == FServer.getLobby().getGuiPlayer()) {
                final Input input = FControl.getInputQueue().getInput();
                if (input instanceof InputPayMana) {
                    // Do something
                    ((InputPayMana) input).useManaFromPool(colorCode);
                }
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float textHeight = FONT.getFont().getCapHeight();
            float gapY = textHeight / 4f;
            float w = image.getWidth();
            float h = image.getHeight();
            float x = (getWidth() - w) / 2;
            float y = (getHeight() - h - textHeight - gapY) / 2;

            g.drawImage(image, x, y, w, h);

            x = 0;
            y += h + gapY;
            w = getWidth();
            h = getHeight() - y;

            g.drawText(text, FONT, FORE_COLOR, x, y, w, h, false, HAlignment.CENTER, false);
        }
    }
}

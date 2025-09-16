package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImageInterface;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.player.PlayerView;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputPayMana;
import forge.localinstance.skin.FSkinProp;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.screens.match.MatchController;
import forge.toolbox.FDisplayObject;

public class VManaPool extends VDisplayArea {
    private static FSkinColor getForeColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT);
        return FSkinColor.get(Colors.CLR_TEXT);
    }
    private static final FSkinFont FONT = FSkinFont.get(16);

    private final PlayerView player;
    private final List<ManaLabel> manaLabels = new ArrayList<>();
    private int totalMana;

    public VManaPool(PlayerView player0) {
        player = player0;

        addManaLabel(FSkinProp.IMG_MANA_COLORLESS, (byte)ManaAtom.COLORLESS);
        addManaLabel(FSkinProp.IMG_MANA_W, MagicColor.WHITE);
        addManaLabel(FSkinProp.IMG_MANA_U, MagicColor.BLUE);
        addManaLabel(FSkinProp.IMG_MANA_B, MagicColor.BLACK);
        addManaLabel(FSkinProp.IMG_MANA_R, MagicColor.RED);
        addManaLabel(FSkinProp.IMG_MANA_G, MagicColor.GREEN);
    }

    private void addManaLabel(FSkinProp prop, byte colorCode) {
        manaLabels.add(add(new ManaLabel(Forge.getAssets().images().get(prop), colorCode)));
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

        if (Forge.isLandscapeMode() && !Forge.altZoneTabs) {
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

    public class ManaLabel extends FDisplayObject {
        private final FSkinImageInterface image;
        private final byte colorCode;
        private String text = "0";

        private ManaLabel(FSkinImageInterface image0, byte colorCode0) {
            image = image0;
            colorCode = colorCode0;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            activate();
            return true;
        }
        public void activate() {
            if(!(MatchController.instance.getGameController() instanceof PlayerControllerHuman))
                return;
            PlayerControllerHuman controller = (PlayerControllerHuman) MatchController.instance.getGameController();
            final Input ipm = controller.getInputQueue().getInput();
            if(ipm instanceof InputPayMana && ipm.getOwner().equals(player))
                controller.useMana(colorCode);
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
            float w2 = Forge.altZoneTabs && "Horizontal".equalsIgnoreCase(Forge.altZoneTabMode)
                ? image.getWidth() * h * 0.7f / image.getHeight() : image.getWidth() * h / image.getHeight();
            float w =  w2;
            while (w > getWidth()) {
                h /= 2;
                w = w2;
            }
            float x = (getWidth() - w) / 2;
            float y = gapY + (maxImageHeight - h) / 2;

            if (isHovered())
                g.fillRect(FSkinColor.getStandardColor(50, 200, 150).alphaColor(0.3f), 0, 0, getWidth(), getHeight());
            g.drawImage(image, x, y, w, Forge.altZoneTabs && "Horizontal".equalsIgnoreCase(Forge.altZoneTabMode) ? w : h);

            x = 0;
            y += h + gapY;
            w = getWidth();
            h = getHeight() - y;

            g.drawText(text, FONT, getForeColor(), x, y, w, h, false, Align.center, false);
        }
    }
}

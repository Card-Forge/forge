package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
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
import forge.interfaces.IGameController;
import forge.localinstance.skin.FSkinProp;
import forge.screens.match.MatchController;
import forge.screens.match.arena.ArenaArt;
import forge.screens.match.arena.ArenaMatchLayout;
import forge.screens.match.arena.ArenaTheme;
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

        addManaLabel(FSkinProp.IMG_MANA_COLORLESS, (byte) ManaAtom.COLORLESS);
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

    /** @return how many mana icons the pool shows, so the alternate layout can size its row */
    public int getLabelCount() {
        return manaLabels.size();
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

    // ---- the Arena board: one tray of mana symbols beside the portrait, each with its amount ------------------------
    /** Shares of the tray's height: padding at its ends, a symbol, the gap before its amount, the space between colours. */
    private static final float ARENA_PAD = 0.26f, ARENA_SYMBOL = 0.62f, ARENA_TEXT_GAP = 0.1f, ARENA_SPACE = 0.2f, ARENA_TEXT = 0.5f;
    /** The tray's ground and rim, as the seats' floating mana chip. */
    private static final Color ARENA_TOP = ArenaTheme.rgba(27, 36, 54, 236), ARENA_BOTTOM = ArenaTheme.rgba(10, 14, 24, 236),
            ARENA_RIM = ArenaTheme.rgba(185, 196, 214, 150), ARENA_SHADOW = ArenaTheme.rgba(0, 0, 0, 120),
            ARENA_HOVER = ArenaTheme.rgba(246, 220, 154, 46), ARENA_HOVER_RIM = ArenaTheme.rgba(246, 220, 154, 170);

    private static FSkinFont arenaFont(float h) {
        return ArenaTheme.fontPx(Math.max(8, Math.round(h * ARENA_TEXT / 2f) * 2));
    }

    private static float arenaLabelWidth(ManaLabel label, float h) {
        return h * ARENA_SYMBOL + h * ARENA_TEXT_GAP + ArenaArt.textWidth(label.text, arenaFont(h));
    }

    /** How wide the Arena tray is at this height: its colours with mana side by side, 0 with none. */
    public float getArenaWidth(float h) {
        float w = 0;
        int shown = 0;
        for (ManaLabel label : manaLabels) {
            if (!"0".equals(label.text)) {
                w += arenaLabelWidth(label, h);
                shown++;
            }
        }
        if (shown == 0) {
            return 0;
        }
        return (float) Math.ceil(w + (shown - 1) * h * ARENA_SPACE + 2 * h * ARENA_PAD);
    }

    /** The tray's shadow and rim reach a little past its bounds: let them, not just what sits inside. */
    private static float arenaBleed() {
        return ArenaTheme.px(10);
    }

    @Override
    protected void startClip(Graphics g) {
        if (ArenaMatchLayout.applies()) {
            final float m = arenaBleed();
            g.startClip(-m, -m, getWidth() + 2 * m, getHeight() + 2 * m);
            return;
        }
        super.startClip(g);
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (!ArenaMatchLayout.applies()) {
            super.drawBackground(g);
            return;
        }
        if (totalMana <= 0) {
            return;
        }
        final float h = getHeight(), w = Math.min(getWidth(), getArenaWidth(h)), r = h / 2f;
        g.drawSoftShadow(ARENA_SHADOW, 0, ArenaTheme.px(2), w, h, r, ArenaTheme.px(6));
        g.fillSmoothRoundRectGradient(ARENA_TOP, ARENA_BOTTOM, true, 0, 0, w, h, r);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.5f)), ARENA_RIM, 0, 0, w, h, r);
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float x = 0;
        float y = 0;

        if (ArenaMatchLayout.applies()) { //the Arena board: the colours that have mana, side by side in one tray
            x = visibleHeight * ARENA_PAD;
            for (ManaLabel label : manaLabels) {
                final boolean has = !"0".equals(label.text);
                label.setVisible(has);
                if (has) {
                    final float w = arenaLabelWidth(label, visibleHeight);
                    label.setBounds(x, y, w, visibleHeight);
                    x += w + visibleHeight * ARENA_SPACE;
                }
            }
            return new ScrollBounds(visibleWidth, visibleHeight);
        }
        for (ManaLabel label : manaLabels) {
            label.setVisible(true);
        }
        if (Forge.isLandscapeMode() && VPlayerPanel.isAltZoneLayout()) {
            //alternate landscape layout: one horizontal row of icons, each with its count underneath
            float labelWidth = visibleWidth / manaLabels.size();
            if (VPlayerPanel.isCompactLayout()) { labelWidth = Math.min(labelWidth, visibleHeight); } //the compact box spans the panel, keep the icons together
            float mod = labelWidth * 0.2f;
            if (VPlayerPanel.isCompactLayout()) { x = Math.max(0f, (visibleWidth - labelWidth * manaLabels.size()) / 2); } //centered in the box
            for (ManaLabel label : manaLabels) {
                label.setBounds(x + mod, y + mod, labelWidth - mod, visibleHeight - mod);
                x += labelWidth;
            }
        } else if (Forge.isLandscapeMode()) {
            float labelWidth = visibleWidth / 2;
            float labelHeight = visibleHeight / 3;

            int count = 0;
            for (ManaLabel label : manaLabels) {
                label.setBounds(x, y, labelWidth, labelHeight);
                if (++count % 2 == 0) {
                    x = 0;
                    y += labelHeight;
                } else {
                    x += labelWidth;
                }
            }
        } else {
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
            IGameController controller = MatchController.instance.getGameController(player);
            // not a local human
            if (controller == null) {
                return;
            }
            controller.useMana(colorCode);
        }

        @Override
        public boolean flick(float x, float y) {
            IGameController controller = MatchController.instance.getGameController(player);
            if (controller == null) {
                return false;
            }
            //on two finger tap, keep using mana until it runs out or no longer can be put towards the cost
            int oldMana, newMana = player.getMana(colorCode);
            do {
                oldMana = newMana;
                controller.useMana(colorCode);
                newMana = player.getMana(colorCode);
            }
            while (oldMana != newMana);
            return true;
        }

        @Override
        public void draw(Graphics g) {
            if (ArenaMatchLayout.applies()) { //the colour's symbol and its amount on the tray; lit under the pointer (a click spends it)
                final float h = getHeight(), s = h * ARENA_SYMBOL;
                if (isHovered()) {
                    final float lh = h * 0.78f, ly = (h - lh) / 2f, lx = -h * 0.12f, lw = getWidth() + h * 0.24f;
                    g.fillSmoothRoundRect(ARENA_HOVER, lx, ly, lw, lh, lh / 2f);
                    g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.2f)), ARENA_HOVER_RIM, lx, ly, lw, lh, lh / 2f);
                }
                g.drawImage(image, 0, (h - s) / 2f, s, s);
                final float tx = s + h * ARENA_TEXT_GAP;
                ArenaArt.textBold(g, text, arenaFont(h), ArenaTheme.TEXT, tx, 0, getWidth() - tx + h * 0.1f, h, Align.left);
                return;
            }
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

            if (isHovered())
                g.fillRect(FSkinColor.getStandardColor(50, 200, 150).alphaColor(0.3f), 0, 0, getWidth(), getHeight());
            g.drawImage(image, x, y, w, h);

            x = 0;
            y += h + gapY;
            w = getWidth();
            h = getHeight() - y;

            g.drawText(text, FONT, getForeColor(), x, y, w, h, false, Align.center, false);
        }
    }
}

package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.RogueEffect.EffectType;
import forge.toolbox.FContainer;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A chest's loot as cards over the chest, under a title plaque, side by side sideways and stacked upright; a tap takes one. */
public class RogueChestScreen extends RogueScreen {
    private final String title;
    private final List<LootCard> cards = new ArrayList<>();
    private final List<RogueButton> buttons;

    RogueChestScreen(Supplier<RogueRun> run, String title, List<ChestEffect> loot, Consumer<ChestEffect> take, List<RogueButton> buttons) {
        super(run);
        this.title = title;
        for (ChestEffect effect : loot) {
            cards.add(add(new LootCard(effect, take)));
        }
        this.buttons = buttons;
        buttons.forEach(this::add);
    }

    @Override
    protected String background() {
        return "reward";
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(buttons);
    }

    private float titleTop() {
        return (Forge.isLandscapeMode() ? 3 : 64) * RogueLook.unit();
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        boolean wide = Forge.isLandscapeMode();
        float gap = 12 * u;
        int n = cards.size();
        float top = titleTop() + 44 * u;
        if (!wide) {
            float bw = Math.min(w - 30 * u, 330 * u);
            float bh = Math.min(96 * u, (h - top - 6 * u - (n - 1) * gap) / Math.max(1, n));
            for (int i = 0; i < n; i++) {
                cards.get(i).setBounds(x + (w - bw) / 2, top + i * (bh + gap), bw, bh);
            }
            return;
        }
        float cw = Math.min((wide ? 180 : 126) * u, (w - 6 * u - (n - 1) * gap) / Math.max(1, n));
        float ch = Math.min((wide ? 176 : 200) * u, h - titleTop() - 46 * u);
        float x0 = x + (w - n * cw - (n - 1) * gap) / 2;
        for (int i = 0; i < n; i++) {
            cards.get(i).setBounds(x0 + i * (cw + gap), top, cw, ch);
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float pw = 200 * u;
        float px = (getWidth() - pw) / 2;
        RogueLook.paper(g, px, titleTop(), pw, 36 * u);
        RogueLook.fit(g, title, px + 8 * u, titleTop() + 9 * u, 1.5f, RogueLook.INK, pw - 16 * u, Align.center);
    }

    private static final class LootCard extends FContainer {
        private final ChestEffect effect;
        private final Consumer<ChestEffect> take;
        private final RogueText text;
        private final String kind;

        LootCard(ChestEffect effect, Consumer<ChestEffect> take) {
            this.effect = effect;
            this.take = take;
            Localizer localizer = Localizer.getInstance();
            kind = localizer.getMessage(effect.getEffectType() == EffectType.ONESHOT ? "lblRogueLoot" : "lblRogueTrait");
            text = add(new RogueText(effect.getRawDescription(), effect.getEffectCardReference(), 1, RogueLook.INK));
        }

        /** Stacked upright, a box is wider than tall and reads left to right: its icon, then its name and text. */
        private boolean row() {
            return getWidth() > getHeight();
        }

        private float textLeft() {
            return (row() ? 66 : 10) * RogueLook.unit();
        }

        private float nameTop() {
            return (row() ? 10 : 62) * RogueLook.unit();
        }

        private float nameBottom() {
            float u = RogueLook.unit();
            return nameTop() + RogueLook.wrappedHeight(effect.getDisplayName(), getWidth() - textLeft() - 10 * u, 1.25f) + 2 * u;
        }

        @Override
        protected void doLayout(float width, float height) {
            float u = RogueLook.unit();
            float top = nameBottom() + 18 * u;
            float left = row() ? textLeft() : 12 * u;
            text.setBounds(left, top, width - left - 12 * u, height - top - 8 * u);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            take.accept(effect);
            return true;
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            RogueLook.paper(g, 0, 0, w, getHeight());
            float cx = row() ? 34 * u : w / 2;
            float cy = row() ? Math.min(34 * u, getHeight() / 2) : 34 * u;
            g.fillCircle(RogueMapView.NODE_FACE, cx, cy, 20 * u);
            RogueLook.ring(g, cx, cy, 20 * u, 1.2f, RogueLook.INK);
            if (effect == ChestEffect.TREASURE) {
                RogueLook.skin(g, FSkinImage.QUEST_COINSTACK, cx - 13 * u, cy - 13 * u, 26 * u);
            } else {
                RogueLook.building(g, 432, 272, 16, 16, cx - 13 * u, cy - 13 * u, 26 * u, 26 * u, 1);
            }
            int align = row() ? Align.left : Align.center;
            RogueLook.wrap(g, effect.getDisplayName(), textLeft(), nameTop(), w - textLeft() - 10 * u, 1.25f, RogueLook.DEEP, align);
            RogueLook.text(g, kind, row() ? textLeft() : 0, nameBottom(), 1, RogueLook.SOFT, row() ? w - textLeft() : w, align);
        }
    }
}

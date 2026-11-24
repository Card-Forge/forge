package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.toolbox.FContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/** Choices as tiles. One that cannot be taken is dashed, with a lock and its reason. A tap takes a choice. */
public class RogueChoiceList extends RogueScrollPane {
    /** reason is null when the choice can be taken. effectCard names the effect card its text mentions, or is null. */
    public record Choice(String title, String text, String reason, String effectCard) { }

    private final List<Tile> tiles = new ArrayList<>();
    private final float minHeight;
    private final IntPredicate chosen;

    /** minHeight is in units. */
    public RogueChoiceList(List<Choice> choices, float minHeight, IntConsumer onChoose) {
        this(choices, minHeight, onChoose, i -> false);
    }

    /** chosen marks the choices picked so far, for a question that takes several. */
    public RogueChoiceList(List<Choice> choices, float minHeight, IntConsumer onChoose, IntPredicate chosen) {
        this.minHeight = minHeight;
        this.chosen = chosen;
        for (int i = 0; i < choices.size(); i++) {
            tiles.add(add(new Tile(choices.get(i), i, onChoose)));
        }
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float u = RogueLook.unit();
        float y = 2 * u;
        float w = visibleWidth - 12 * u;
        for (Tile tile : tiles) {
            float h = tile.preferredHeight(w);
            tile.setBounds(6 * u, y, w, h);
            y += h + 6 * u;
        }
        return new ScrollBounds(visibleWidth, y);
    }

    private class Tile extends FContainer {
        private final Choice choice;
        private final int index;
        private final IntConsumer onChoose;
        private final RogueText text;

        Tile(Choice choice, int index, IntConsumer onChoose) {
            this.choice = choice;
            this.index = index;
            this.onChoose = onChoose;
            this.text = add(new RogueText(choice.text(), choice.effectCard(), 1, choice.reason() == null ? RogueLook.INK : RogueLook.SOFT));
        }

        float preferredHeight(float w) {
            float u = RogueLook.unit();
            float reason = choice.reason() == null ? 0 : RogueLook.wrappedHeight(choice.reason(), w - 32 * u, 1);
            return Math.max(minHeight * u, text.getPreferredHeight(w - 16 * u) + 27 * u + reason);
        }

        @Override
        protected void doLayout(float width, float height) {
            float u = RogueLook.unit();
            text.setBounds(8 * u, 19 * u, width - 16 * u, text.getPreferredHeight(width - 16 * u));
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (choice.reason() == null) {
                onChoose.accept(index);
            }
            return true;
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            boolean no = choice.reason() != null;
            if (no) {
                RogueLook.dashTile(g, 0, 0, w, getHeight());
            } else {
                RogueLook.tile(g, 0, 0, w, getHeight(), chosen.test(index));
            }
            RogueLook.fit(g, choice.title(), 8 * u, 3 * u, 1.15f, no ? RogueLook.SOFT : RogueLook.DEEP, w - 16 * u, Align.left);
            if (no) {
                float ry = text.getBottom() + u;
                RogueLook.skin(g, FSkinImage.LOCK, 8 * u, ry, 12 * u);
                RogueLook.wrap(g, choice.reason(), 24 * u, ry, w - 32 * u, 1, RogueLook.INK, Align.left);
            }
        }
    }
}

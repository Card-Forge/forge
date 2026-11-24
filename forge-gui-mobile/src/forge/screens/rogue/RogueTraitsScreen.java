package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.card.CardZoom;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.AetherEffect;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.EventEffect;
import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gamemodes.rogue.effect.WoundEffect;
import forge.item.PaperCard;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.List;

/** The run's traits and carry cards. Each trait is marked by its kind; a kind the page does not know gets its first letter. */
public class RogueTraitsScreen extends RogueScreen {
    private final List<RogueEffect> effects;
    private final RogueRun run;
    private final FScrollPane list;
    private final RogueButton mapTab;
    private final RogueButton traits;
    private final RogueButton deck;

    public RogueTraitsScreen() {
        super(() -> RogueController.get().flow().run());
        run = RogueController.get().flow().run();
        effects = new ArrayList<>(RogueEffectComposite.getAllEffects(run));
        list = add(new Content());
        Localizer localizer = Localizer.getInstance();
        mapTab = add(new RogueButton(localizer.getMessage("lblRogueMap"), null, RogueButton.State.NORMAL, Forge::back));
        traits = add(new RogueButton(localizer.getMessage("lblRogueEffects"), null, RogueButton.State.SELECTED, null));
        deck = add(new RogueButton(localizer.getMessage("lblDeck"), null, RogueButton.State.NORMAL, RogueDeckEditor::openForRun));
    }

    @Override
    protected String background() {
        return "tavern";
    }

    @Override
    protected float backgroundDim() {
        return 0.2f;
    }

    @Override
    protected int tab() {
        return 1;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(List.of(mapTab, traits, deck));
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        list.setBounds(x + 8 * u, y + 8 * u, w - 16 * u, h - 13 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        RogueLook.paper(g, 3 * u, 3 * u, getWidth() - 6 * u, list.getBottom() + 2 * u);
    }

    /**
     * Draws a trait as this page's rows do, from y, and returns the row's height: a tile, its mark, its name, then text.
     * Measures only when g is null.
     */
    static float traitRow(Graphics g, RogueEffect effect, String text, float x, float y, float w) {
        float u = RogueLook.unit();
        float h = Math.max(36 * u, RogueLook.wrappedHeight(text, w - 44 * u, 1) + 21 * u);
        if (g == null) {
            return h;
        }
        if (effect instanceof WoundEffect) {
            RogueLook.dashTile(g, x, y, w, h);
        } else {
            RogueLook.tile(g, x, y, w, h, false);
        }
        mark(g, effect, x + 19 * u, y + h / 2, 13 * u);
        RogueLook.fit(g, name(effect), x + 38 * u, y + 2 * u, 1.15f, RogueLook.DEEP, w - 42 * u, Align.left);
        RogueLook.wrap(g, text, x + 38 * u, y + 17 * u, w - 44 * u, 1, RogueLook.INK, Align.left);
        return h;
    }

    /** Draws the trait's mark centred on cx, cy. */
    static void mark(Graphics g, RogueEffect effect, float cx, float cy, float r) {
        float u = RogueLook.unit();
        g.fillCircle(RogueMapView.NODE_FACE, cx, cy, r);
        RogueLook.ring(g, cx, cy, r, 1, RogueLook.INK);
        float s = r * 1.25f;
        if (effect instanceof NPCEffect npc) {
            RogueLook.sprite(g, FSkin.getAvatars().get(npc.getOwnerNpc().avatarIndex), cx - s * 0.56f, cy - s * 0.56f, s * 1.12f, s * 1.12f, Color.WHITE);
        } else if (effect instanceof ChestEffect) {
            RogueLook.building(g, 432, 272, 16, 16, cx - s / 2, cy - s / 2, s, s, 1);
        } else if (effect instanceof EventEffect) {
            RogueLook.building(g, 240, 272, 16, 16, cx - s / 2, cy - s / 2, s, s, 1);
        } else if (effect instanceof WoundEffect) {
            RogueLook.skin(g, FSkinImage.QUEST_STAKES, cx - s / 2, cy - s / 2, s);
        } else if (effect instanceof AetherEffect) {
            RogueLook.skin(g, FSkinImage.QUEST_GOLD, cx - s / 2, cy - s / 2, s);
        } else {
            String name = effect.getDisplayName();
            RogueLook.text(g, name == null || name.isEmpty() ? "?" : name.substring(0, 1), cx - r, cy - 9 * u, 1.3f, RogueLook.INK, 2 * r, Align.center);
        }
    }

    static String name(RogueEffect effect) {
        String card = effect.getEffectCardDisplayName();
        return card == null || card.isEmpty() ? effect.getDisplayName() : card;
    }

    private class Content extends RogueScrollPane {
        private static final float NONE_HEIGHT = 18;
        private final List<TraitRow> rows = new ArrayList<>();
        private final List<CarryTile> carry = new ArrayList<>();
        private float carryTop;

        Content() {
            for (RogueEffect effect : effects) {
                rows.add(add(new TraitRow(effect)));
            }
            for (int i = 0; i < run.getCarryCards().size(); i++) {
                carry.add(add(new CarryTile(i)));
            }
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float u = RogueLook.unit();
            float y = 26 * u;
            float w = visibleWidth - 10 * u;
            for (TraitRow row : rows) {
                float h = row.preferredHeight(w);
                row.setBounds(5 * u, y, w, h);
                y += h + 5 * u;
            }
            if (rows.isEmpty()) {
                y += NONE_HEIGHT * u;
            }
            y += 24 * u;
            int cols = Forge.isLandscapeMode() ? 6 : 3;
            float cw = Math.min(55 * u, (w - (cols - 1) * 12 * u) / cols);
            for (CarryTile tile : carry) {
                int i = carry.indexOf(tile);
                tile.setBounds(14 * u + (i % cols) * (cw + 12 * u), y + (i / cols) * (cw * RogueLook.CARD_RATIO + 22 * u), cw,
                    cw * RogueLook.CARD_RATIO + 18 * u);
            }
            float bottom = carry.isEmpty() ? y + NONE_HEIGHT * u : carry.get(carry.size() - 1).getBottom();
            carryTop = y - 22 * u;
            return new ScrollBounds(visibleWidth, bottom + 8 * u);
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            Localizer localizer = Localizer.getInstance();
            float shift = -getScrollTop();
            RogueLook.text(g, localizer.getMessage("lblRogueYourEffects"), 7 * u, shift + 2 * u, 1.5f, RogueLook.INK);
            if (rows.isEmpty()) {
                RogueLook.text(g, localizer.getMessage("lblNone"), 9 * u, shift + 26 * u, 1, RogueLook.SOFT);
            }
            RogueLook.text(g, localizer.getMessage("lblRogueCarryCards"), 7 * u, shift + carryTop, 1.5f, RogueLook.INK);
            if (carry.isEmpty()) {
                RogueLook.text(g, localizer.getMessage("lblNone"), 9 * u, shift + carryTop + 24 * u, 1, RogueLook.SOFT);
            }
        }
    }

    private class TraitRow extends FContainer {
        private final RogueEffect effect;
        private final RogueText text;

        TraitRow(RogueEffect effect) {
            this.effect = effect;
            this.text = add(new RogueText(effect.getActiveDescription(run), effect.getEffectCardReference(), 1, RogueLook.INK));
        }

        float preferredHeight(float w) {
            float u = RogueLook.unit();
            return Math.max(36 * u, text.getPreferredHeight(w - 44 * u) + 21 * u);
        }

        @Override
        protected void doLayout(float width, float height) {
            float u = RogueLook.unit();
            text.setBounds(38 * u, 17 * u, width - 44 * u, height - 19 * u);
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float h = getHeight();
            if (effect instanceof WoundEffect) {
                RogueLook.dashTile(g, 0, 0, w, h);
            } else {
                RogueLook.tile(g, 0, 0, w, h, false);
            }
            mark(g, effect, 19 * u, h / 2, 13 * u);
            RogueLook.fit(g, name(effect), 38 * u, 2 * u, 1.15f, RogueLook.DEEP, w - 42 * u, Align.left);
        }
    }

    private class CarryTile extends FDisplayObject {
        private final int index;

        CarryTile(int index) {
            this.index = index;
        }

        @Override
        public void setHovered(boolean hovered) {
            super.setHovered(hovered);
            RogueScreen.hover(this, hovered, run.getCarryCards().get(index).toPaperCard());
        }

        @Override
        public boolean longPress(float x, float y) {
            CardZoom.show(run.getCarryCards().get(index).toPaperCard());
            return true;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            return longPress(x, y);
        }

        @Override
        public void draw(Graphics g) {
            float u = RogueLook.unit();
            RogueRun.CarryCard carry = run.getCarryCards().get(index);
            PaperCard card = carry.toPaperCard();
            float w = getWidth();
            RogueLook.card(g, card, 0, 0, w, 1);
            FSkinImage icon = switch (carry.type()) {
                case ITEM -> FSkinImage.ARTIFACT;
                case FELLOW -> FSkinImage.CREATURE;
                case SCROLL -> FSkinImage.SORCERY;
            };
            float y = w * RogueLook.CARD_RATIO + 3 * u;
            String label = carry.type().label();
            float lw = 13 * u + 3 * u + RogueLook.textWidth(label, 0.9f);
            RogueLook.skin(g, icon, (w - lw) / 2, y, 13 * u);
            RogueLook.text(g, label, (w - lw) / 2 + 16 * u, y, 0.9f, RogueLook.INK);
        }
    }
}

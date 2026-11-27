package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.card.CardZoom;
import forge.deck.FDeckViewer;
import forge.gamemodes.rogue.BazaarPricing;
import forge.gamemodes.rogue.effect.BazaarItem;
import forge.gamemodes.rogue.flow.RogueDecisions.ShopAnswer;
import forge.gamemodes.rogue.flow.RogueDecisions.ShopOffer;
import forge.item.PaperCard;
import forge.toolbox.FDisplayObject;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The Bazaar: cards, then traits and carry cards, each with its price. A pick the gold cannot pay for is refused. */
public class RogueShopScreen extends RogueScreen {
    private final ShopOffer offer;
    private final RogueQuestion<ShopAnswer> question;
    private final Set<BazaarItem> selected = new LinkedHashSet<>();
    private final List<BazaarItem> cards = new ArrayList<>();
    private final List<BazaarItem> specials = new ArrayList<>();
    private final Items items;
    private final RogueButton viewMap;
    private final RogueButton viewDeck;
    private final RogueButton reroll;
    private final RogueButton skip;
    private final RogueButton buy;

    public RogueShopScreen(ShopOffer offer, RogueQuestion<ShopAnswer> question) {
        super(offer::run);
        this.offer = offer;
        this.question = question;
        for (BazaarItem item : offer.items()) {
            (item.isSpecial() ? specials : cards).add(item);
        }
        items = add(new Items());
        Localizer localizer = Localizer.getInstance();
        String title = offer.title() == null ? localizer.getMessage("lblRogueBazaar") : offer.title();
        viewMap = add(new RogueButton(localizer.getMessage("lblRogueViewMap"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueMapScreen(title))));
        viewDeck = add(new RogueButton(localizer.getMessage("lblViewDeck"), null, RogueButton.State.NORMAL,
            () -> FDeckViewer.show(offer.run().getCurrentDeck())));
        reroll = offer.rerollLabel() == null ? null : add(new RogueButton(offer.rerollLabel(), "Refresh",
            offer.rerollEnabled() ? RogueButton.State.NORMAL : RogueButton.State.DISABLED, () -> answer(new ShopAnswer(true, Set.of(), offer.tab()))).after(FSkinImage.QUEST_COIN));
        skip = add(new RogueButton(localizer.getMessage("lblSkip"), null, RogueButton.State.NORMAL,
            () -> answer(new ShopAnswer(false, Set.of(), offer.tab()))));
        buy = add(new RogueButton(localizer.getMessage(specials.isEmpty() ? "lblRogueBuySelectedCards" : "lblRogueBuySelected"), null,
            RogueButton.State.DISABLED, () -> answer(new ShopAnswer(false, Set.copyOf(selected), offer.tab()))));
    }

    private void answer(ShopAnswer answer) {
        if (offer.accepts(answer)) {
            question.answer(answer);
        }
    }

    private int cost() {
        return BazaarPricing.calculateItemsTotalCost(selected);
    }

    private void toggle(BazaarItem item) {
        if (!selected.remove(item) && cost() + item.getPrice() <= offer.gold()) {
            selected.add(item);
        }
        buy.setState(selected.isEmpty() ? RogueButton.State.DISABLED : RogueButton.State.MAIN);
    }

    @Override
    protected String background() {
        return "market";
    }

    @Override
    protected float backgroundDim() {
        return 0.25f;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        List<RogueButton> top = new ArrayList<>(List.of(viewMap, viewDeck));
        if (reroll != null) {
            top.add(reroll);
        }
        return List.of(top, List.of(skip, buy));
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        items.setBounds(x + 8 * u, y + 46 * u, w - 16 * u, h - 52 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float w = getWidth();
        Localizer localizer = Localizer.getInstance();
        RogueLook.paper(g, 3 * u, 3 * u, w - 6 * u, items.getBottom() + 3 * u);
        String title = offer.title() == null ? localizer.getMessage("lblRogueBazaar") : offer.title();
        RogueLook.fit(g, title, 15 * u, 12 * u, 1.4f, RogueLook.INK, w * 0.45f, Align.left);
        String[] parts = {localizer.getMessage("lblRogueGoldTotal", offer.gold()), localizer.getMessage("lblRogueBazaarCost", -cost()),
            localizer.getMessage("lblRogueRemainingGold", offer.gold() - cost())};
        float x = w - 15 * u;
        for (int i = parts.length - 1; i >= 0; i--) {
            float tw = RogueLook.textWidth(parts[i], 1);
            x -= tw;
            RogueLook.text(g, parts[i], x, 15 * u, 1, i == 2 ? RogueLook.INK : RogueLook.SOFT);
            x -= 12 * u;
        }
        RogueLook.skin(g, FSkinImage.QUEST_COIN, x - 4 * u, 14 * u, 13 * u);
        RogueLook.line(g, 12 * u, 38 * u, w - 12 * u, 38 * u, 0.8f, RogueLook.SOFT);
    }

    private class Items extends RogueScrollPane {
        private final List<Tile> tiles = new ArrayList<>();
        private final RogueDeal deal = new RogueDeal(cards.size() + specials.size());
        private boolean dealt;
        private float specialsTop = -1;

        Items() {
            for (BazaarItem item : cards) {
                tiles.add(add(new Tile(item, tiles.size())));
            }
            for (BazaarItem item : specials) {
                tiles.add(add(new Tile(item, tiles.size())));
            }
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float u = RogueLook.unit();
            float gap = 7 * u;
            int cols = Forge.isLandscapeMode() ? twoRowColumns(visibleWidth, visibleHeight, gap) : 4;
            float cw = (visibleWidth - 8 * u - (cols - 1) * gap) / cols;
            float th = cw * RogueLook.CARD_RATIO + 16 * u;
            float y = 4 * u;
            int col = 0;
            BazaarItem firstSpecial = specials.isEmpty() ? null : specials.get(0);
            for (Tile tile : tiles) {
                if (tile.item == firstSpecial) {
                    if (col != 0) {
                        y += th + gap;
                        col = 0;
                    }
                    specialsTop = y;
                    y += 20 * u;
                }
                tile.setBounds(4 * u + col * (cw + gap), y, cw, th);
                if (++col == cols) {
                    col = 0;
                    y += th + gap;
                }
            }
            float bottom = col == 0 ? y : y + th + gap;
            if (!dealt && visibleHeight > 0) {
                dealt = true;
                deal.start();
            }
            return new ScrollBounds(visibleWidth, bottom);
        }

        /** Sideways, as many columns as it takes for two whole rows of cards to fit the height. */
        private int twoRowColumns(float visibleWidth, float visibleHeight, float gap) {
            float u = RogueLook.unit();
            float rowHeight = (visibleHeight - 4 * u - gap) / 2;
            float cw = (rowHeight - 16 * u) / RogueLook.CARD_RATIO;
            return Math.max(7, (int) ((visibleWidth - 8 * u + gap) / (cw + gap)));
        }

        @Override
        protected void drawBackground(Graphics g) {
            if (specialsTop >= 0) {
                float u = RogueLook.unit();
                RogueLook.text(g, Localizer.getInstance().getMessage("lblRogueTraitsAndCarryCards"), 4 * u, specialsTop - getScrollTop(),
                    1.1f, RogueLook.DEEP);
            }
        }
    }

    private class Tile extends FDisplayObject {
        private final BazaarItem item;
        private final int index;

        Tile(BazaarItem item, int index) {
            this.item = item;
            this.index = index;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (!items.deal.arrived(index)) {
                return true;
            }
            toggle(item);
            return true;
        }

        @Override
        public void setHovered(boolean hovered) {
            super.setHovered(hovered);
            RogueScreen.hover(this, hovered, item.card());
        }

        @Override
        public boolean longPress(float x, float y) {
            PaperCard card = item.card();
            if (card != null) {
                CardZoom.show(card);
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float ch = w * RogueLook.CARD_RATIO;
            boolean rare = item.isSpecial();
            if (rare) {
                items.deal.halo(g, index, w, ch);
            }
            if (!items.deal.arrived(index)) {
                items.deal.draw(g, index, item.card(), w, ch, items.getWidth() / 2 - getLeft(),
                    items.getScrollTop() + items.getHeight() - getTop(), rare);
                return;
            }
            boolean on = selected.contains(item);
            if (on) {
                g.fillRect(RogueLook.INK, -3.4f * u, -3.4f * u, w + 6.8f * u, ch + 6.8f * u);
                g.fillRect(RogueLook.GOLD, -2.2f * u, -2.2f * u, w + 4.4f * u, ch + 4.4f * u);
            }
            boolean affordable = on || cost() + item.getPrice() <= offer.gold();
            RogueLook.card(g, item.card(), 0, 0, w, affordable ? 1 : 0.55f);
            if (on) {
                g.fillCircle(RogueLook.PALE, w - 4 * u, 4 * u, 8 * u);
                RogueLook.item(g, "OK", w - 10.5f * u, -2.5f * u, 13 * u);
            }
            float py = ch + 3 * u;
            String price = String.valueOf(item.getPrice());
            float x = (w - 13 * u - RogueLook.textWidth(price, 1)) / 2;
            if (item.isDiscounted()) {
                String base = String.valueOf(item.getBasePrice());
                float bw = RogueLook.textWidth(base, 1);
                x -= (bw + 4 * u) / 2;
                RogueLook.skin(g, FSkinImage.QUEST_COIN, x, py, 12 * u);
                x += 14 * u;
                RogueLook.text(g, base, x, py, 1, RogueLook.SOFT);
                RogueLook.line(g, x, py + 6 * u, x + bw, py + 6 * u, 1, RogueLook.SOFT);
                x += bw + 4 * u;
                RogueLook.text(g, price, x, py, 1, RogueLook.INK);
            } else {
                RogueLook.skin(g, FSkinImage.QUEST_COIN, x, py, 12 * u);
                RogueLook.text(g, price, x + 14 * u, py, 1, RogueLook.INK);
            }
        }
    }
}

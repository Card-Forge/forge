package forge.itemmanager;

import java.util.Map.Entry;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardFaceSymbols;
import forge.card.CardRenderer;
import forge.card.ColorSet;
import forge.deck.DeckProxy;
import forge.deck.FDeckViewer;
import forge.deck.io.DeckPreferences;
import forge.game.GameType;
import forge.game.IHasGameType;
import forge.itemmanager.filters.AdvancedSearchFilter;
import forge.itemmanager.filters.DeckColorFilter;
import forge.itemmanager.filters.DeckFormatFilter;
import forge.itemmanager.filters.TextSearchFilter;
import forge.toolbox.FList;
import forge.toolbox.FList.CompactModeHandler;
import forge.util.Localizer;
import forge.util.Utils;

/** 
 * ItemManager for decks
 */
public final class DeckManager extends ItemManager<DeckProxy> implements IHasGameType {
    private final GameType gameType;

    /**
     * Creates deck list for selected decks for quick deleting, editing, and
     * basic info. "selectable" and "editable" assumed true.
     *
     * @param gt
     */
    public DeckManager(final GameType gt) {
        super(DeckProxy.class, true);
        gameType = gt;
        setCaption(Localizer.getInstance().getMessage("lblDecks"));
    }

    public GameType getGameType() {
        return gameType;
    }

    @Override
    public void setup(ItemManagerConfig config0) {
        boolean wasStringOnly = (getConfig() == ItemManagerConfig.STRING_ONLY);
        boolean isStringOnly = (config0 == ItemManagerConfig.STRING_ONLY);

        super.setup(config0, null);

        if (isStringOnly != wasStringOnly) {
            restoreDefaultFilters();
        }
    }

    @Override
    protected void addDefaultFilters() {
        if (getConfig() == ItemManagerConfig.STRING_ONLY) { return; }

        addFilter(new DeckColorFilter(this));
        addFilter(new DeckFormatFilter(this));
    }

    @Override
    protected TextSearchFilter<DeckProxy> createSearchFilter() {
        return new TextSearchFilter<>(this);
    }

    @Override
    protected AdvancedSearchFilter<DeckProxy> createAdvancedSearchFilter() {
        return new AdvancedSearchFilter<>(this);
    }

    @Override
    protected boolean allowSortChange() {
        return false;
    }

    private static final float IMAGE_SIZE = CardRenderer.MANA_SYMBOL_SIZE;

    @Override
    public ItemRenderer getListItemRenderer(final CompactModeHandler compactModeHandler) {
        return new ItemRenderer() {
            @Override
            public float getItemHeight() {
                if (DeckManager.this.getConfig().getCols().size() == 1) {
                    //if just string column, use normal list item height
                    return Utils.AVG_FINGER_HEIGHT;
                }
                return CardRenderer.getCardListItemHeight(compactModeHandler.isCompactMode()); //use same height for decks as for cards
            }

            @Override
            public boolean tap(Integer index, Entry<DeckProxy, Integer> value, float x, float y, int count) {
                float bottomRight = IMAGE_SIZE + 2 * FList.PADDING;
                if (x <= bottomRight && y <= bottomRight) {
                    DeckPreferences prefs = DeckPreferences.getPrefs(value.getKey());
                    prefs.setStarCount((prefs.getStarCount() + 1) % 2); //TODO: consider supporting more than 1 star
                    return true;
                }
                return false;
            }

            @Override
            public boolean longPress(Integer index, Entry<DeckProxy, Integer> value, float x, float y) {
                FDeckViewer.show(value.getKey().getDeck());
                return true;
            }

            @Override
            public void drawValue(Graphics g, Entry<DeckProxy, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                DeckProxy deck = value.getKey();

                if (DeckManager.this.getConfig().getCols().size() == 1) {
                    //if just string column, just draw deck string value
                    g.drawText(deck.toString(), font, foreColor, x, y, w, h, false, Align.left, true);
                    return;
                }

                //draw favorite, name, path and color on first line
                if (Forge.hdbuttons)
                    g.drawImage(DeckPreferences.getPrefs(deck).getStarCount() > 0 ? FSkinImage.HDSTAR_FILLED : FSkinImage.HDSTAR_OUTLINE, x, y, IMAGE_SIZE, IMAGE_SIZE);
                else
                    g.drawImage(DeckPreferences.getPrefs(deck).getStarCount() > 0 ? FSkinImage.STAR_FILLED : FSkinImage.STAR_OUTLINE, x, y, IMAGE_SIZE, IMAGE_SIZE);

                x += IMAGE_SIZE + FList.PADDING;
                //AI Icon
                g.drawImage(deck.getAI().inMainDeck == 0 ? FSkinImage.AI_ACTIVE : FSkinImage.AI_INACTIVE, x, y, IMAGE_SIZE, IMAGE_SIZE);
                x += IMAGE_SIZE + FList.PADDING;
                ColorSet deckColor = deck.getColor();
                float availableNameWidth = w - CardFaceSymbols.getWidth(deckColor, IMAGE_SIZE) - IMAGE_SIZE - 2 * FList.PADDING;
                availableNameWidth -= IMAGE_SIZE + FList.PADDING;
                String name = deck.getName();
                if (!deck.getPath().isEmpty()) { //render path after name if needed
                    name += " (" + deck.getPath().substring(1) + ")";
                }
                g.drawText(name, font, foreColor, x, y, availableNameWidth, IMAGE_SIZE, false, Align.left, true);
                x += availableNameWidth + FList.PADDING;
                CardFaceSymbols.drawColorSet(g, deckColor, x, y, IMAGE_SIZE);

                if (compactModeHandler.isCompactMode()) {
                    return; //skip second line if compact mode
                }

                //draw formats, main/side, and set/highest rarity on second line
                font = FSkinFont.get(12);
                float lineHeight = font.getLineHeight();

                x = FList.PADDING;
                y += IMAGE_SIZE + FList.PADDING + CardRenderer.SET_BOX_MARGIN;
                String set = deck.getEdition().getCode();
                float setWidth = CardRenderer.getSetWidth(font, set);
                float availableFormatWidth = w - setWidth + CardRenderer.SET_BOX_MARGIN;

                int mainSize = deck.getMainSize();
                if (mainSize < 0) {
                    mainSize = 0; //show main as 0 if empty
                }
                int sideSize = deck.getSideSize();
                if (sideSize < 0) {
                    sideSize = 0; //show sideboard as 0 if empty
                }

                g.drawText(deck.getFormatsString() + " (" + mainSize + " / " + sideSize + ")", font, foreColor, x, y, availableFormatWidth, lineHeight, false, Align.left, true);

                x += availableFormatWidth + CardRenderer.SET_BOX_MARGIN;
                y -= CardRenderer.SET_BOX_MARGIN;
                CardRenderer.drawSetLabel(g, font, set, deck.getHighestRarity(), x, y, setWidth, lineHeight + 2 * CardRenderer.SET_BOX_MARGIN);
            }

            @Override
            public boolean allowPressEffect(FList<Entry<DeckProxy, Integer>> list, float x, float y) {
                return true;
            }
        };
    }
}

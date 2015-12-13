package forge.screens.planarconquest;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.CardColorFilter;
import forge.itemmanager.filters.CardRarityFilter;
import forge.itemmanager.filters.CardTypeFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.model.FModel;
import forge.planarconquest.ConquestController;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.screens.FScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Utils;

public class ConquestAEtherScreen extends FScreen {
    private static final float PULL_BTN_HEIGHT = 1.2f * Utils.AVG_FINGER_HEIGHT;

    private final AEtherManager lstAEther = add(new AEtherManager());
    private final FLabel btnPull = add(new PullButton());

    private int shardCost;

    public ConquestAEtherScreen() {
        super("", ConquestMenu.getMenu());

        lstAEther.setup(ItemManagerConfig.CONQUEST_AETHER);
    }

    @Override
    public void onActivate() {
        ConquestController conquest = FModel.getConquest();
        ConquestData model = conquest.getModel();
        ConquestPlane plane = model.getCurrentPlane();

        setHeaderCaption(conquest.getName() + "\nPlane - " + plane.getName());

        CardPool pool = new CardPool();
        for (PaperCard card : plane.getCardPool().getAllCards()) {
            if (!model.hasUnlockedCard(card)) {
                pool.add(card);
            }
        }
        lstAEther.setPool(pool, true);
    }

    private void calculateShardCost() {
        shardCost = 100;
        btnPull.setText(String.valueOf(shardCost));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = ItemFilter.PADDING;
        float w = width - 2 * x;
        lstAEther.setBounds(x, startY, w, height - startY - PULL_BTN_HEIGHT - 2 * ItemFilter.PADDING);
        btnPull.setBounds(x, height - PULL_BTN_HEIGHT - ItemFilter.PADDING, w, PULL_BTN_HEIGHT);
    }

    private class AEtherManager extends CardManager {
        public AEtherManager() {
            super(false);
            setCaption("The AEther");
        }

        @Override
        protected void addDefaultFilters() {
            addFilter(new CardColorFilter(this));
            addFilter(new CardRarityFilter(this));
            addFilter(new CardTypeFilter(this));
        }

        @Override
        public void updateView(final boolean forceFilter, final Iterable<PaperCard> itemsToSelect) {
            super.updateView(forceFilter, itemsToSelect);
            calculateShardCost();
        }
    }

    private class PullButton extends FLabel {
        protected PullButton() {
            super(new FLabel.ButtonBuilder().font(FSkinFont.forHeight(PULL_BTN_HEIGHT * 0.45f))
                    .icon(FSkinImage.QUEST_COIN).iconScaleFactor(1f).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                }
            }));
        }

        @Override
        protected void drawContent(Graphics g, float x, float y, float w, float h) {
            FSkinFont font = getFont();
            float textHeight = font.getCapHeight() * 1.25f;
            y += h / 2 - textHeight;
            g.drawText("Pull from the AEther", font, getTextColor(), x, y, w, textHeight, false, HAlignment.CENTER, false);
            y += textHeight;

            //draw shard icon and cost
            super.drawContent(g, x, y, w, textHeight * 1.25f);
        }
    }
}

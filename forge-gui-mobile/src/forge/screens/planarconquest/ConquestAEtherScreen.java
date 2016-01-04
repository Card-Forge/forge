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
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.screens.FScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Aggregates;
import forge.util.Utils;

public class ConquestAEtherScreen extends FScreen {
    private static final float PULL_BTN_HEIGHT = 1.2f * Utils.AVG_FINGER_HEIGHT;

    private final AEtherManager lstAEther = add(new AEtherManager());
    private final FLabel lblTip = add(new FLabel.Builder()
        .text("Filter above list then press below button\nto unlock a random card from the filtered list.")
        .textColor(FLabel.INLINE_LABEL_COLOR)
        .align(HAlignment.CENTER).font(FSkinFont.get(12)).build());
    private final FLabel btnPull = add(new PullButton());

    private int shardCost;

    public ConquestAEtherScreen() {
        super("", ConquestMenu.getMenu());

        lstAEther.setup(ItemManagerConfig.CONQUEST_AETHER);
    }

    @Override
    public void onActivate() {
        ConquestData model = FModel.getConquest().getModel();
        ConquestPlane plane = model.getCurrentPlane();

        setHeaderCaption(model.getName() + "\nPlane - " + plane.getName());

        CardPool pool = new CardPool();
        for (PaperCard card : plane.getCardPool().getAllCards()) {
            if (!model.hasUnlockedCard(card)) {
                pool.add(card);
            }
        }
        lstAEther.setPool(pool, true);
    }

    private void calculateShardCost() {
        shardCost = FModel.getConquest().calculateShardCost(lstAEther.getFilteredItems(), lstAEther.getPool().countDistinct());
        updatePullButton();
    }

    private void updatePullButton() {
        int availableShards = FModel.getConquest().getModel().getAEtherShards();
        btnPull.setEnabled(shardCost > 0 && shardCost <= availableShards);
        btnPull.setText((shardCost > 0 ? String.valueOf(shardCost) : "---") + " / " + String.valueOf(availableShards));
    }

    private void pullFromTheAEther() {
        ConquestData model = FModel.getConquest().getModel();
        PaperCard card = Aggregates.random(lstAEther.getFilteredItems()).getKey();
        lstAEther.removeItem(card, 1);

        ConquestRewardDialog.show("Card pulled from the AEther", card);

        model.spendAEtherShards(shardCost);
        model.unlockCard(card);
        model.saveData();

        updatePullButton();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float padding = ItemFilter.PADDING;
        float x = padding;
        float w = width - 2 * padding;
        float tipLabelHeight = lblTip.getAutoSizeBounds().height;
        lstAEther.setBounds(x, startY, w, height - startY - PULL_BTN_HEIGHT - tipLabelHeight - 4 * padding);
        lblTip.setBounds(x, height - PULL_BTN_HEIGHT - tipLabelHeight - 2 * padding, width, tipLabelHeight);
        btnPull.setBounds(x, height - PULL_BTN_HEIGHT - padding, w, PULL_BTN_HEIGHT);
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
                    pullFromTheAEther();
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

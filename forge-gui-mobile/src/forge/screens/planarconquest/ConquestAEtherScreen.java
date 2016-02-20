package forge.screens.planarconquest;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Aggregates;
import forge.util.Utils;

public class ConquestAEtherScreen extends FScreen {
    private static final Color FILTER_BUTTON_COLOR = FSkinColor.alphaColor(Color.WHITE, 0.15f);
    private static final FSkinColor FILTER_BUTTON_PRESSED_COLOR = FSkinColor.getStandardColor(FSkinColor.alphaColor(Color.WHITE, 0.1f));
    private static final float PADDING = Utils.scale(5f);

    private final AEtherDisplay display = add(new AEtherDisplay());
    private final Set<PaperCard> pool = new HashSet<PaperCard>();
    private final Set<PaperCard> filteredPool = new HashSet<PaperCard>();

    private final FLabel btnColorFilter = add(new FilterButton(FSkinImage.MULTI, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            
        }
    }));
    private final FLabel btnTypeFilter = add(new FilterButton(FSkinImage.CREATURE, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            
        }
    }));
    private final FLabel btnRarityFilter = add(new FilterButton(FSkinImage.PW_BADGE_COMMON, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            
        }
    }));
    private final FLabel btnCMCFilter = add(new FilterButton(FSkinImage.MANA_X, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            
        }
    }));

    private int shardCost;

    public ConquestAEtherScreen() {
        super("", ConquestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        ConquestData model = FModel.getConquest().getModel();
        ConquestPlane plane = model.getCurrentPlane();

        setHeaderCaption(model.getName());

        pool.clear();
        for (PaperCard card : plane.getCardPool().getAllCards()) {
            if (!model.hasUnlockedCard(card)) {
                pool.add(card);
            }
        }
        updateFilteredPool();
    }

    private void updateFilteredPool() {
        filteredPool.clear();
        for (PaperCard card : pool) {
            filteredPool.add(card);
        }
        updateShardCost();
    }

    private void updateShardCost() {
        shardCost = FModel.getConquest().calculateShardCost(filteredPool, pool.size());
        int availableShards = FModel.getConquest().getModel().getAEtherShards();
    }

    private void pullFromTheAEther() {
        ConquestData model = FModel.getConquest().getModel();
        PaperCard card = Aggregates.random(filteredPool);
        pool.remove(card);
        filteredPool.remove(card);

        ConquestRewardDialog.show("Card pulled from the AEther", card, null);

        model.spendAEtherShards(shardCost);
        model.unlockCard(card);
        model.saveData();

        updateShardCost();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        display.setBounds(0, startY, width, height - startY);

        float buttonSize = width * 0.15f;
        btnColorFilter.setBounds(PADDING, startY + PADDING, buttonSize, buttonSize);
        btnTypeFilter.setBounds(width - buttonSize - PADDING, startY + PADDING, buttonSize, buttonSize);
        btnRarityFilter.setBounds(PADDING, height - buttonSize - PADDING, buttonSize, buttonSize);
        btnCMCFilter.setBounds(width - buttonSize - PADDING, height - buttonSize - PADDING, buttonSize, buttonSize);
    }

    private class AEtherDisplay extends FDisplayObject {
        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();

            FSkinTexture background = FSkinTexture.BG_SPACE;
            float backgroundHeight = w * background.getHeight() / background.getWidth();

            if (backgroundHeight < h / 2) {
                g.fillRect(Color.BLACK, 0, 0, w, h); //ensure no gap between top and bottom images
            }

            background.draw(g, 0, h - backgroundHeight, w, backgroundHeight);

            g.startClip(0, 0, w, h / 2); //prevent upper image extending beyond halfway point of screen
            background.drawFlipped(g, 0, 0, w, backgroundHeight);
            g.endClip();
        }
    }

    private class FilterButton extends FLabel {
        private FilterButton(FImage icon, FEventHandler command) {
            super(new FLabel.Builder().icon(icon).iconInBackground().pressedColor(FILTER_BUTTON_PRESSED_COLOR)
                    .command(command).alphaComposite(1f).align(HAlignment.CENTER));
        }

        @Override
        protected void drawContent(Graphics g, float w, float h, final boolean pressed) {
            if (!pressed) {
                g.fillRect(FILTER_BUTTON_COLOR, 0, 0, w, h);
            }
            super.drawContent(g, w, h, pressed);
        }
    }
}

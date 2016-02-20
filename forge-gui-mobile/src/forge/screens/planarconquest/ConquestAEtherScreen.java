package forge.screens.planarconquest;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;

import forge.Graphics;
import forge.assets.FSkinTexture;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.util.Aggregates;

public class ConquestAEtherScreen extends FScreen {
    private final AEtherDisplay display = add(new AEtherDisplay());
    private final Set<PaperCard> pool = new HashSet<PaperCard>();
    private final Set<PaperCard> filteredPool = new HashSet<PaperCard>();
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
}

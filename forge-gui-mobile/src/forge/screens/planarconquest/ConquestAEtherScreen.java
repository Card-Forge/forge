package forge.screens.planarconquest;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Rectangle;

import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.card.CardRenderer.CardStackPosition;
import forge.item.PaperCard;
import forge.itemmanager.filters.ItemFilter;
import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.screens.FScreen;
import forge.toolbox.FCardPanel;
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

    private PullAnimation activePullAnimation;
    private ItemFilter<PaperCard> editFilter;
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

        activePullAnimation = new PullAnimation(card);
        activePullAnimation.start();

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

            if (activePullAnimation != null) {
                activePullAnimation.drawCard(g);
            }
            else if (editFilter == null) {
            }
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (activePullAnimation != null) {
                if (activePullAnimation.finished) {
                    activePullAnimation = null;
                    return true;
                }
                return false;
            }
            if (editFilter == null) {
                pullFromTheAEther();
                return true;
            }
            return false;
        }

        @Override
        public boolean longPress(float x, float y) {
            if (activePullAnimation != null && activePullAnimation.finished) {
                CardZoom.show(activePullAnimation.card);
                return true;
            }
            return false;
        }
    }

    private class PullAnimation extends ForgeAnimation {
        private static final float DURATION = 0.6f;

        private final PaperCard card;
        private final Rectangle start, end;
        private float progress = 0;
        private boolean finished;

        private PullAnimation(PaperCard card0) {
            card = card0;

            float displayWidth = display.getWidth();
            float displayHeight = display.getHeight();
            float startHeight = displayHeight * 0.05f;
            float startWidth = startHeight / FCardPanel.ASPECT_RATIO;
            float endHeight = displayHeight - 2 * btnColorFilter.getHeight() - 4 * PADDING;
            float endWidth = endHeight / FCardPanel.ASPECT_RATIO;
            start = new Rectangle((displayWidth - startWidth) / 2, (displayHeight - startHeight) / 2, startWidth, startHeight);
            end = new Rectangle((displayWidth - endWidth) / 2, (displayHeight - endHeight) / 2, endWidth, endHeight);
        }

        private void drawCard(Graphics g) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            }
            else if (percentage > 1) {
                percentage = 1;
            }
            Rectangle pos = Utils.getTransitionPosition(start, end, percentage);
            CardRenderer.drawCard(g, card, pos.x, pos.y, pos.width, pos.height, CardStackPosition.Top);
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            finished = true;
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

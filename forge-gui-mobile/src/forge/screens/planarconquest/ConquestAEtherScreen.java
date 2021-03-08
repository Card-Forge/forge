package forge.screens.planarconquest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.assets.TextRenderer;
import forge.card.CardRarity;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.card.ColorSet;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.ColorSetImage;
import forge.item.PaperCard;
import forge.localinstance.assets.FSkinProp;
import forge.model.FModel;
import forge.planarconquest.ConquestCommander;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.planarconquest.ConquestUtil;
import forge.planarconquest.ConquestUtil.AEtherFilter;
import forge.screens.FScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.GuiChoose;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Aggregates;
import forge.util.Callback;
import forge.util.MyRandom;
import forge.util.Utils;
import forge.util.Localizer;

public class ConquestAEtherScreen extends FScreen {
    public static final Color FILTER_BUTTON_COLOR = ConquestMultiverseScreen.LOCATION_BAR_COLOR;
    public static final FSkinColor FILTER_BUTTON_TEXT_COLOR = FSkinColor.getStandardColor(ConquestMultiverseScreen.LOCATION_BAR_TEXT_COLOR);
    public static final FSkinColor FILTER_BUTTON_PRESSED_COLOR = FSkinColor.getStandardColor(FSkinColor.alphaColor(Color.WHITE, 0.1f));
    public static final FSkinFont LABEL_FONT = FSkinFont.get(16);
    private static final FSkinFont MESSAGE_FONT = FSkinFont.get(14);
    private static final float PADDING = Utils.scale(5f);

    private final AEtherDisplay display = add(new AEtherDisplay());
    private final Set<PaperCard> pool = new HashSet<>();
    private final Set<PaperCard> filteredPool = new HashSet<>();
    private final Set<PaperCard> strictPool = new HashSet<>();

    private final FilterButton btnColorFilter = add(new FilterButton(Localizer.getInstance().getMessage("lblColor"), ConquestUtil.COLOR_FILTERS));
    private final FilterButton btnTypeFilter = add(new FilterButton(Localizer.getInstance().getMessage("lblType"), ConquestUtil.TYPE_FILTERS));
    private final FilterButton btnRarityFilter = add(new FilterButton(Localizer.getInstance().getMessage("lblRarity"), ConquestUtil.RARITY_FILTERS));
    private final FilterButton btnCMCFilter = add(new FilterButton(Localizer.getInstance().getMessage("lblCMC"), ConquestUtil.CMC_FILTERS));

    private final FLabel lblShards = add(new FLabel.Builder().font(LABEL_FONT).align(Align.center).parseSymbols().build());

    private PullAnimation activePullAnimation;
    private int shardCost;
    private ConquestCommander commander;

    public ConquestAEtherScreen() {
        super("", ConquestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        ConquestData model = FModel.getConquest().getModel();
        ConquestPlane plane = model.getCurrentPlane();

        setHeaderCaption(model.getName());

        ConquestCommander commander0 = model.getSelectedCommander();
        if (commander != commander0) {
            commander = commander0;
            resetFilters(); //reset filters if commander changed since the last time this screen was opened
        }

        pool.clear();
        for (PaperCard card : plane.getCardPool().getAllCards()) {
            if (!model.hasUnlockedCard(card) && !card.getRules().getType().isBasicLand()) { //don't allow pulling basic lands
                pool.add(card);
            }
        }
        updateFilteredPool();
        updateAvailableShards();
    }

    private void resetFilters() {
        btnColorFilter.setSelectedOption(ConquestUtil.getColorFilter(commander.getCard().getRules().getColorIdentity()));
        btnTypeFilter.setSelectedOption(AEtherFilter.CREATURE);
        btnRarityFilter.setSelectedOption(AEtherFilter.COMMON);
        btnCMCFilter.setSelectedOption(AEtherFilter.CMC_LOW_MID);
    }

    private void updateFilteredPool() {
        Predicate<PaperCard> predicate = btnColorFilter.buildFilterPredicate(null);
        predicate = btnTypeFilter.buildFilterPredicate(predicate);
        predicate = btnRarityFilter.buildFilterPredicate(predicate);
        predicate = btnCMCFilter.buildFilterPredicate(predicate);

        final CardRarity selectedRarity = btnRarityFilter.selectedOption.getRarity();

        filteredPool.clear();
        strictPool.clear();
        for (PaperCard card : pool) {
            if (predicate == null || predicate.apply(card)) {
                filteredPool.add(card);
                if (selectedRarity == card.getRarity()) {
                    strictPool.add(card);
                }
            }
        }
        updateShardCost();
    }

    private void updateAvailableShards() {
        int availableShards = FModel.getConquest().getModel().getAEtherShards();
        lblShards.setText(Localizer.getInstance().getMessage("lblShardsAE", "{AE}") + availableShards);
    }

    private void updateShardCost() {
        if (filteredPool.isEmpty()) {
            shardCost = 0;
        }
        else {
            shardCost = ConquestUtil.getShardValue(btnRarityFilter.selectedOption.getRarity(), CQPref.AETHER_BASE_PULL_COST);
        }
        display.updateMessage();
    }

    private void pullFromTheAEther() {
        if (filteredPool.isEmpty() || strictPool.isEmpty()) { return; }

        ConquestData model = FModel.getConquest().getModel();
        if (model.getAEtherShards() < shardCost) { return; }

        //determine final pool to pull from based on rarity odds
        Iterable<PaperCard> rewardPool;
        CardRarity minRarity = btnRarityFilter.selectedOption.getRarity();
        CardRarity rarity = btnRarityFilter.selectedOption.getRarity(MyRandom.getRandom().nextDouble());
        while (true) {
            final CardRarity allowedRarity = rarity;
            rewardPool = Iterables.filter(filteredPool, new Predicate<PaperCard>() {
                @Override
                public boolean apply(PaperCard card) {
                    return allowedRarity == card.getRarity()
                    || allowedRarity == CardRarity.Rare && card.getRarity() == CardRarity.Special;
                }
            });
            if (Iterables.isEmpty(rewardPool)) { //if pool is empty, must reduce rarity and try again
                if (rarity == minRarity) {
                    return;
                }
                switch (rarity) {
                case MythicRare:
                    rarity = CardRarity.Rare;
                    continue;
                case Rare:
                    rarity = CardRarity.Uncommon;
                    continue;
                case Uncommon:
                    rarity = CardRarity.Common;
                    continue;
                default:
                    break;
                }
            }
            break;
        }

        PaperCard card = Aggregates.random(rewardPool);
        if (card == null) { return; } //shouldn't happen, but prevent crash if it does

        pool.remove(card);
        filteredPool.remove(card);
        strictPool.remove(card); // Card might not have been in strictPool in the first place; that's fine

        activePullAnimation = new PullAnimation(card);
        activePullAnimation.start();

        model.spendAEtherShards(shardCost);
        model.unlockCard(card);
        model.saveData();

        updateShardCost();
        updateAvailableShards();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        display.setBounds(0, startY, width, height - startY);

        float buttonSize = width * 0.15f;
        btnColorFilter.setBounds(PADDING, startY + PADDING, buttonSize, buttonSize);
        btnTypeFilter.setBounds(width - buttonSize - PADDING, startY + PADDING, buttonSize, buttonSize);
        btnRarityFilter.setBounds(PADDING, height - buttonSize - PADDING, buttonSize, buttonSize);
        btnCMCFilter.setBounds(width - buttonSize - PADDING, height - buttonSize - PADDING, buttonSize, buttonSize);

        float x = btnRarityFilter.getRight() + PADDING;
        float labelWidth = btnCMCFilter.getLeft() - PADDING - x;
        lblShards.setBounds(x, btnRarityFilter.getTop(), labelWidth, btnRarityFilter.getHeight());
    }

    private class AEtherDisplay extends FDisplayObject {
        private final TextRenderer textRenderer = new TextRenderer();
        private String message;

        private void updateMessage() {
            message = Localizer.getInstance().getMessage("lblTapToPullFromAE", "{AE}");

            if (shardCost == 0) {
                message += "--";
            }
            else if (FModel.getConquest().getModel().getAEtherShards() < shardCost) {
                message += TextRenderer.startColor(Color.RED) + shardCost + TextRenderer.endColor();
            }
            else {
                message += shardCost;
            }

            message += " (";

            if (strictPool.isEmpty()) {
                message += TextRenderer.startColor(Color.RED) + "0" + TextRenderer.endColor();
            }
            else {
                message += strictPool.size();
            }

            message += " [";

            if (filteredPool.isEmpty()) {
                message += TextRenderer.startColor(Color.RED) + "0" + TextRenderer.endColor();
            }
            else {
                message += filteredPool.size();
            }

            message += "] / " + pool.size() + ")";
        }

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
            else {
                textRenderer.drawText(g, message, MESSAGE_FONT, Color.WHITE, 0, 0, w, h, 0, h, false, Align.center, true);
            }
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (y < btnColorFilter.getBottom() + PADDING || y > btnCMCFilter.getTop() - PADDING) {
                return false; //ignore taps inline with buttons
            }

            if (activePullAnimation != null) {
                if (activePullAnimation.finished) {
                    activePullAnimation = null;
                    return true;
                }
                return false;
            }

            pullFromTheAEther();
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            if (y < btnColorFilter.getBottom() + PADDING || y > btnCMCFilter.getTop() - PADDING) {
                return false; //ignore long press inline with buttons
            }

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
        private final String caption;
        private final List<AEtherFilter> options;
        private AEtherFilter selectedOption;

        private FilterButton(String caption0, AEtherFilter[] options0) {
            super(new FLabel.Builder().iconInBackground().pressedColor(FILTER_BUTTON_PRESSED_COLOR)
                    .textColor(FILTER_BUTTON_TEXT_COLOR).alphaComposite(1f).align(Align.center));
            caption = caption0;
            options = ImmutableList.copyOf(options0);
            setSelectedOption(options.get(0));
            setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    GuiChoose.getChoices(Localizer.getInstance().getMessage("lblSelectCaptionFilter", caption), 0, 1, options, selectedOption, null, new Callback<List<AEtherFilter>>() {
                        @Override
                        public void run(List<AEtherFilter> result) {
                            if (!result.isEmpty()) {
                                setSelectedOption(result.get(0));
                                updateFilteredPool();
                            }
                        }
                    });
                }
            });
        }

        private void setSelectedOption(AEtherFilter selectedOption0) {
            if (selectedOption == selectedOption0) { return; }
            selectedOption = selectedOption0;

            FSkinProp skinProp = selectedOption.getSkinProp();
            if (skinProp != null) {
                setIcon(FSkin.getImages().get(skinProp));
            }
            else {
                ColorSet color = selectedOption.getColor();
                if (color != null) {
                    setIcon(new ColorSetImage(color));
                }
                else {
                    System.out.println("No icon for filter " + selectedOption.name());
                    setIcon(null);
                }
            }
        }

        private Predicate<PaperCard> buildFilterPredicate(Predicate<PaperCard> predicate) {
            if (predicate == null) {
                return selectedOption.getPredicate();
            }
            return Predicates.and(predicate, selectedOption.getPredicate());
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

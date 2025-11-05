package forge.screens.planarconquest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.google.common.collect.ImmutableList;

import com.google.common.collect.Iterables;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.assets.TextRenderer;
import forge.card.CardFaceSymbols;
import forge.card.CardRarity;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.IHasSkinProp;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.GuiChoose;
import forge.util.*;

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

    private final ColorButton btnColorFilter = add(new ColorButton(Forge.getLocalizer().getMessage("lblColor")));
    private final FilterButton<ConquestUtil.TypeFilter> btnTypeFilter = add(new FilterButton<ConquestUtil.TypeFilter>(Forge.getLocalizer().getMessage("lblType"), ConquestUtil.TypeFilter.values()));
    private final FilterButton<ConquestUtil.RarityFilter> btnRarityFilter = add(new FilterButton<ConquestUtil.RarityFilter>(Forge.getLocalizer().getMessage("lblRarity"), ConquestUtil.RarityFilter.values()));
    private final FilterButton<ConquestUtil.CMCFilter> btnCMCFilter = add(new FilterButton<ConquestUtil.CMCFilter>(Forge.getLocalizer().getMessage("lblCMC"), ConquestUtil.CMCFilter.values()));

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
        btnColorFilter.setSelectedOption(commander.getCard().getRules().getColorIdentity());
        btnTypeFilter.setSelectedOption(ConquestUtil.TypeFilter.CREATURE);
        btnRarityFilter.setSelectedOption(ConquestUtil.RarityFilter.COMMON);
        btnCMCFilter.setSelectedOption(ConquestUtil.CMCFilter.CMC_LOW_MID);
    }

    private void updateFilteredPool() {
        Predicate<PaperCard> predicate = btnColorFilter
                .and(btnTypeFilter)
                .and(btnRarityFilter)
                .and(btnCMCFilter);

        final CardRarity selectedRarity = btnRarityFilter.selectedOption.getRarity();

        filteredPool.clear();
        strictPool.clear();
        for (PaperCard card : pool) {
            if (predicate.test(card)) {
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
        lblShards.setText(Forge.getLocalizer().getMessage("lblShardsAE", "{AE}") + availableShards);
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
            rewardPool = IterableUtil.filter(filteredPool, card -> allowedRarity == card.getRarity()
                    || allowedRarity == CardRarity.Rare && card.getRarity() == CardRarity.Special);
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
            message = Forge.getLocalizer().getMessage("lblTapToPullFromAE", "{AE}");

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

    private abstract class AbstractFilterButton<T> extends FLabel implements Predicate<PaperCard> {
        private final String caption;
        private final List<T> options;
        protected T selectedOption;

        private AbstractFilterButton(String caption0, T[] options0, final Function<T, String> display) {
            super(new FLabel.Builder().iconInBackground().pressedColor(FILTER_BUTTON_PRESSED_COLOR)
                    .textColor(FILTER_BUTTON_TEXT_COLOR).alphaComposite(1f).align(Align.center));
            caption = caption0;
            options = ImmutableList.copyOf(options0);
            setSelectedOption(options.get(0));
            setCommand(e -> GuiChoose.getChoices(Forge.getLocalizer().getMessage("lblSelectCaptionFilter", caption), 0, 1, options, Set.of(selectedOption), display, result -> {
                if (!result.isEmpty()) {
                    setSelectedOption(result.get(0));
                    updateFilteredPool();
                }
            }));
        }

        public void setSelectedOption(T selectedOption0) {
            if (selectedOption == selectedOption0) {
                return;
            }
            selectedOption = selectedOption0;
        }


        @Override
        protected void drawContent(Graphics g, float w, float h, final boolean pressed) {
            if (!pressed) {
                g.fillRect(FILTER_BUTTON_COLOR, 0, 0, w, h);
            }
            super.drawContent(g, w, h, pressed);
        }
    }

    private class ColorButton extends AbstractFilterButton<ColorSet> {
        private ColorButton(String caption0) {
            super(caption0, Arrays.stream(ColorSet.values()).sorted(Comparator.comparing(ColorSet::getOrderWeight)).toArray(ColorSet[]::new),
                    c -> "Playable in " + c.stream().map(MagicColor.Color::getSymbol).collect(Collectors.joining()));
        }

        @Override
        public void setSelectedOption(ColorSet selectedOption0) {
            super.setSelectedOption(selectedOption0);

            setIcon(new ColorSetImage(selectedOption));
        }

        @Override
        public boolean test(PaperCard card) {
            return card.getRules().getColorIdentity().hasNoColorsExcept(selectedOption);
        }

        private record ColorSetImage(ColorSet colorSet, int shardCount) implements FImage {
            public ColorSetImage(ColorSet colorSet0) {
                this(colorSet0, colorSet0.getOrderedColors().size());
            }

            @Override
            public float getWidth() {
                return Forge.getAssets().images().get(FSkinProp.IMG_MANA_W).getWidth() * shardCount;
            }

            @Override
            public float getHeight() {
                return Forge.getAssets().images().get(FSkinProp.IMG_MANA_W).getHeight();
            }

            @Override
            public void draw(Graphics g, float x, float y, float w, float h) {
                float imageSize = w / shardCount;
                if (imageSize > h) {
                    imageSize = h;
                    float w0 = imageSize * shardCount;
                    x += (w - w0) / 2;
                    w = w0;
                }
                CardFaceSymbols.drawColorSet(g, colorSet, x, y, imageSize);
            }
        }
    }

    private class FilterButton<T extends Enum<T> & IHasSkinProp & Predicate<PaperCard>> extends AbstractFilterButton<T> {
        private FilterButton(String caption0, T[] options0) {
            super(caption0, options0, null);
        }

        @Override
        public void setSelectedOption(T selectedOption0) {
            super.setSelectedOption(selectedOption0);

            setIcon(FSkin.getImages().get(selectedOption.getSkinProp()));
        }

        @Override
        public boolean test(PaperCard card) {
            return selectedOption.test(card);
        }
    }
}

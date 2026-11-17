package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.character.ShopActor;
import forge.haptic.HapticEngine;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.adventure.data.RewardData;
import forge.adventure.data.ShopData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.assets.ImageCache;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.util.ItemPool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Displays the rewards of a fight or a treasure
 */
public class RewardScene extends UIScene {
    private TextraButton doneButton, detailButton, restockButton;
    private TextraLabel playerGold, playerShards;
    private TypingLabel headerLabel;
    private Vector2 headerLabelOrigPos;
    private boolean autoSell;

    private ShopActor shopActor;
    private static RewardScene object;

    private PointOfInterestChanges changes;

    public static RewardScene instance() {
        if (object == null)
            object = new RewardScene();
        return object;
    }

    public enum Type {
        Shop,
        Loot,
        QuestReward,
        RewardChoice,
        EventReward,
        Lost
    }

    Type type;
    Array<Actor> generated = new Array<>(32);
    private final List<RewardActor> rewardList = new ArrayList<>(32);
    static public final float CARD_WIDTH = 550f;
    static public final float CARD_HEIGHT = 400f;
    static public final float CARD_WIDTH_TO_HEIGHT = CARD_WIDTH / CARD_HEIGHT;
    ItemPool<PaperCard> collectionPool = null;
    private int remainingSelections = 0;
    public Image marketBackgroundImg = null;
    private Actor cachedCardsContainerActor = null;
    private Drawable origDrawable = null;
    // Below this card height the grid stops shrinking and scrolls instead, so screens of up to about 24 cards keep their layout.
    private static final float MIN_CARD_HEIGHT = 60f;
    // Room under a scrolling grid for the more-cards arrow, which is 6 units tall at the top of its bob, with 10 clear above and 6 below.
    private static final float CHEVRON_SPACE = 22f;
    private final Group cardGroup = new Group();
    private final ScrollPane cardScroller = new ScrollPane(cardGroup);
    private final MoreArrow cardArrow = new MoreArrow(cardScroller);
    private final TypingLabel deckSlotLabel;
    private final Array<Deck> pendingDecks = new Array<>();
    private ShopActor deckShop;
    private String deckSlotMessage;

    private RewardScene() {
        super(Forge.isLandscapeMode() ? "ui/items.json" : "ui/items_portrait.json");

        playerGold = Controls.newAccountingLabel(ui.findActor("playerGold"), false);
        playerShards = Controls.newAccountingLabel(ui.findActor("playerShards"), true);
        headerLabel = ui.findActor("shopName");
        headerLabelOrigPos = new Vector2(headerLabel.getX(), headerLabel.getY());
        if (ui.findActor("market_background") instanceof Image image)
            this.marketBackgroundImg = image;
        this.cachedCardsContainerActor = ui.findActor("cards");
        ui.onButtonPress("done", this::done);
        ui.onButtonPress("detail", this::toggleToolTip);
        ui.onButtonPress("restock", this::restockShop);
        detailButton = ui.findActor("detail");
        detailButton.setVisible(false);
        doneButton = ui.findActor("done");
        restockButton = ui.findActor("restock");
        origDrawable = getBGDrawable();
        cardGroup.setTouchable(Touchable.childrenOnly);
        cardScroller.setScrollingDisabled(true, false);
        cardScroller.setOverscroll(false, false);
        stage.addActor(cardScroller);
        stage.addActor(cardArrow);
        deckSlotLabel = Controls.newTypingLabel("");
        deckSlotLabel.setX(cachedCardsContainerActor.getX());
        deckSlotLabel.setWidth(cachedCardsContainerActor.getWidth());
        deckSlotLabel.setWrap(true);
        deckSlotLabel.setAlignment(Align.center);
        deckSlotLabel.setVisible(false);
        stage.addActor(deckSlotLabel);
    }

    @Override
    public void connected(Controller controller) {
        super.connected(controller);
        updateDetailButton();
    }

    @Override
    public void disconnected(Controller controller) {
        super.disconnected(controller);
        updateDetailButton();
    }

    private void updateDetailButton() {
        detailButton.setVisible(Controllers.getCurrent() != null);
        detailButton.layout();
    }

    private void toggleToolTip() {
        Selectable selectable = getSelected();
        if (selectable == null)
            return;
        RewardActor actor;
        if (selectable.actor instanceof BuyButton buyButton) {
            actor = buyButton.rewardActor;
        } else if (selectable.actor instanceof RewardActor) {
            actor = (RewardActor) selectable.actor;
        } else {
            return;
        }
        if (!actor.isFlipped())
            performTouch(actor);

    }

    boolean doneClicked = false, shown = false;
    float flipCountDown = 1.0f;
    float exitCountDown = 0.0f; //Serves as additional check for when scene is exiting, so you can't double tap too fast.

    public void quitScene() {
        for (int i = 0; i < generated.size; i++) {
            Actor actor = generated.get(i);
            if (actor instanceof RewardActor rewardActor) {
                rewardActor.removeTooltip();
                actor.remove();
            }
        }
        //save RAM
        ImageCache.getInstance().unloadCardTextures(true);
        Forge.advFreezePlayerControls = false;
        if (this.collectionPool != null) {
            this.collectionPool.clear();
            this.collectionPool = null;
        }
        Forge.switchToLast();
    }

    public void reactivateInputs() {
        Gdx.input.setInputProcessor(stage);
        doneButton.toFront();
    }

    public boolean done() {
        return done(false);
    }

    boolean done(boolean skipShowLoot) {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        if (!skipShowLoot) {
            showLootOrDone();
            return true;
        }
        if (type != null) {
            switch (type) {
                case Shop:
                case QuestReward:
                case Loot:
                    break;
            }
        }
        shown = false;
        clearGenerated();
        quitOrRevealDeck();
        return true;
    }

    void clearGenerated() {
        for (int i = 0; i < generated.size; i++) {
            Actor actor = generated.get(i);
            if (!(actor instanceof RewardActor rewardActor)) {
                continue;
            }
            if (type == Type.Loot || type == Type.EventReward)
                claimReward(rewardActor.getReward());
            if (type == Type.QuestReward)
                claimReward(rewardActor.getReward()); // TODO Want to customize this soon to have selectable rewards which will be handled different here
            rewardActor.clearLabel();
            rewardActor.remove();
        }
    }

    /** The bobbing down-arrow from the item preview, shown while its pane has more below. */
    private static class MoreArrow extends Actor {
        private final ScrollPane pane;
        private float elapsed;

        MoreArrow(ScrollPane pane) {
            this.pane = pane;
            setSize(7, 6);
        }

        @Override
        public void act(float delta) {
            elapsed += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (pane.getScrollY() < pane.getMaxY() - 1)
                RewardActor.drawMoreArrow(batch, getX() + getWidth() / 2, getY(), 1, elapsed, parentAlpha);
        }
    }

    private static boolean isDeckReward(Reward reward) {
        return reward.getDeck() != null && reward.getDeck().getTags().contains(Reward.DECK_TAG);
    }

    private void showDecklist(Deck deck) {
        List<DeckSection> sections = new ArrayList<>(List.of(DeckSection.values()));
        sections.remove(DeckSection.Commander);
        sections.add(0, DeckSection.Commander);
        StringBuilder list = new StringBuilder();
        for (DeckSection section : sections) {
            CardPool pool = deck.get(section);
            if (pool == null || pool.isEmpty())
                continue;
            list.append("[%?SHINY]").append(section.getLocalizedName()).append(" (").append(pool.countAll()).append(")[%]\n");
            List<Map.Entry<PaperCard, Integer>> entries = new ArrayList<>();
            pool.forEach(entries::add);
            entries.sort(Comparator.comparing(entry -> entry.getKey().getName()));
            for (Map.Entry<PaperCard, Integer> entry : entries)
                list.append(entry.getValue()).append(' ').append(entry.getKey().getName().replace("[", "[[")).append('\n');
            list.append('\n');
        }
        TextraLabel label = Controls.newTextraLabel(list.toString().stripTrailing());
        label.setWrap(true);
        TextureRegionDrawable white = new TextureRegionDrawable(new TextureRegion(Forge.getAssets().getWhiteTexture()));
        ScrollPane.ScrollPaneStyle barStyle = new ScrollPane.ScrollPaneStyle();
        barStyle.vScroll = white.tint(new Color(1, 1, 1, 0.15f));
        barStyle.vScroll.setMinWidth(2);
        barStyle.vScrollKnob = white.tint(new Color(1, 1, 1, 0.7f));
        barStyle.vScrollKnob.setMinWidth(2);
        barStyle.vScrollKnob.setMinHeight(8);
        ScrollPane pane = new ScrollPane(label, barStyle);
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);
        Dialog dialog = createGenericDialog("", null, Forge.getLocalizer().getMessage("lblClose"), null, this::removeDialog, null);
        Table content = dialog.getContentTable();
        content.add(Controls.newTextraLabel("[%?SHINY]" + deck.getName().replace("[", "[[") + "[%]\n"
                + Forge.getLocalizer().getMessage("lblNCards", deck.getAllCardsInASinglePool(true, true).countAll()))).left().padTop(4).padBottom(4);
        content.row();
        content.add(pane).width(stage.getWidth() * (Forge.isLandscapeMode() ? 0.6f : 0.8f)).height(stage.getHeight() * 0.55f).padRight(4);
        content.row();
        content.add(new MoreArrow(pane)).padTop(4);
        showDialog(dialog);
    }

    private void claimReward(Reward reward) {
        if (isDeckReward(reward))
            pendingDecks.add(reward.getDeck());
        else
            AdventurePlayer.current().addReward(reward);
    }

    /** Reveals any decks claimed on this screen before leaving, then returns to the shop they were bought in. */
    private void quitOrRevealDeck() {
        if (pendingDecks.notEmpty()) {
            revealDeck(pendingDecks.removeIndex(0));
        } else if (deckShop != null) {
            ShopActor shop = deckShop;
            deckShop = null;
            loadRewards(shop.getRewardData(), Type.Shop, shop);
        } else {
            quitScene();
        }
    }

    private void revealDeck(Deck reward) {
        // The shop keeps the reward, and an unlimited shop sells it again, so the slot gets its own copy
        Deck deck = (Deck) reward.copyTo(reward.getName());
        deck.getTags().remove(Reward.DECK_TAG);
        int slot = AdventurePlayer.current().addDeckToEmptySlot(deck);
        // Textra reads square brackets as markup, and commander precon names carry set and year in brackets
        String name = deck.getName().replace("[", "[[");
        deckSlotMessage = slot >= 0
                ? Forge.getLocalizer().getMessage("lblDeckAddedToSlot", name, slot + 1)
                : Forge.getLocalizer().getMessage("lblDeckSlotsFull", name);
        loadRewards(deck, Type.EventReward, null, deck.getTags().contains("noSell"));
    }

    public List<RewardActor> getGeneratedRewards() {
        rewardList.clear();
        for (int i = 0; i < generated.size; i++) {
            Actor actor = generated.get(i);
            if (!(actor instanceof RewardActor rewardActor)) {
                continue;
            }
            if (!rewardActor.frontSideUp()) {
                continue;
            }
            rewardList.add(rewardActor);
        }
        return rewardList;
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
        ImageCache.getInstance().allowSingleLoad();
        if (doneClicked) {
            if (type == Type.EventReward || type == Type.Loot || type == Type.QuestReward) {
                flipCountDown -= delta;
                exitCountDown += delta;
            }
            if (flipCountDown <= 0) {
                clearGenerated();
                quitScene();
            }
        }
    }

    @Override
    public void enter() {
        autoSell = false;
        updateDetailButton();
        if ((type == Type.Loot || type == Type.Lost) && Forge.lastPreview != null) {
            setUIBackground(getLastPreviewDrawable(new TextureRegion(Forge.lastPreview)));
        } else {
            setUIBackground(origDrawable);
        }
        super.enter();
    }

    private void showLootOrDone() {
        boolean exit = true;
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            if (!(actor instanceof RewardActor rewardActor)) {
                continue;
            }
            if (!rewardActor.isFlipped()) {
                exit = false;
                break;
            }
        }
        if (exit)
            done(true);
        else if ((type == Type.EventReward || type == Type.Loot || type == Type.QuestReward) && !shown) {
            shown = true;
            float delay = 0.09f;
            generated.shuffle();
            for (Actor actor : new Array.ArrayIterator<>(generated)) {
                if (!(actor instanceof RewardActor rewardActor)) {
                    continue;
                }
                if (!rewardActor.isFlipped()) {
                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            rewardActor.flip();
                        }
                    }, delay);
                    delay += 0.12f;
                }
            }
        } else {
            done(true);
        }
    }

    void updateRestockButton() {
        if (!shopActor.canRestock())
            return;
        int price = shopActor.getRestockPrice();
        restockButton.setText("[+Refresh][+shards]" + price);
        restockButton.setDisabled(WorldSave.getCurrentSave().getPlayer().getShards() < price);
    }

    void restockShop() {
        if (!shopActor.canRestock())
            return;
        int price = shopActor.getRestockPrice();
        if (changes != null)
            changes.generateNewShopSeed(shopActor.getObjectId());

        Current.player().takeShards(price);

        HapticEngine.vibrate(FPref.UI_VIBRATE_ON_SHOP_ACTION, 5);
        SoundSystem.instance.play(SoundEffectType.Shuffle, false);

        updateBuyButtons();
        if (changes == null)
            return;

        clearGenerated();

        ShopData data = shopActor.getShopData();
        Array<Reward> ret = new Array<>();

        long shopSeed = changes.getShopSeed(shopActor.getObjectId());
        WorldSave.getCurrentSave().getWorld().getRandom().setSeed(shopSeed);
        for (RewardData rdata : new Array.ArrayIterator<>(data.rewards)) {
            ret.addAll(rdata.generate(false, false));
        }
        shopActor.setRewardData(ret);
        loadRewards(ret, RewardScene.Type.Shop, shopActor);
    }

    public void loadRewards(Deck deck, Type type, ShopActor shopActor, boolean noSell) {
        Array<Reward> rewards = new Array<>();
        for (PaperCard card : deck.getAllCardsInASinglePool(true, true).toFlatList()) {
            rewards.add(new Reward(card, noSell));
        }
        loadRewards(rewards, type, shopActor);
    }

    public void loadSelectableRewards(Array<Reward> choices, Type type, int countToSelect) {
        if (type != Type.RewardChoice)
            return;
        this.remainingSelections = countToSelect;
        loadRewards(choices, type, null);
    }

    void updateCollectionPool() {
        if (Type.Shop != this.type)
            return;
        if (this.collectionPool == null)
            this.collectionPool = new ItemPool<>(PaperCard.class);
        else
            this.collectionPool.clear();

        this.collectionPool.addAllFlat(AdventurePlayer.current().getCollectionCards(true).toFlatList());
    }

    public void loadRewards(Array<Reward> newRewards, Type type, ShopActor shopActor) {
        // Merge Gold and Shards rewards into single entries
        int totalGold = 0;
        int totalShards = 0;
        Array<Reward> others = new Array<>();
        for (Reward r : new Array.ArrayIterator<>(newRewards)) {
            switch (r.getType()) {
                case Gold:
                    totalGold += r.getCount();
                    break;
                case Shards:
                    totalShards += r.getCount();
                    break;
                default:
                    others.add(r);
                    break;
            }
        }
        newRewards.clear();
        if (totalGold > 0) {
            newRewards.add(new Reward(Reward.Type.Gold, totalGold));
        }
        if (totalShards > 0) {
            newRewards.add(new Reward(Reward.Type.Shards, totalShards));
        }
        for (Reward r : others) {
            newRewards.add(r);
        }

        headerLabel.clearListeners();
        // Sort the rewards based on the rarity of the card inside the reward/ lets give items rarity
        newRewards.sort(Comparator.comparing(reward -> {
            if (reward.getCard() != null && reward.getCard().getRarity() != null) {
                return reward.getCard().getRarity().ordinal();
            }
            // Return a default value or handle the case where rarity is not present
            return Integer.MAX_VALUE; // Assuming higher values mean less priority in sorting
        }));
        clearSelectable();
        this.type = type;
        autoSell = false;
        doneClicked = false;
        playerGold.setVisible(type != Type.Lost);
        playerShards.setVisible(type != Type.Lost);
        updateCollectionPool();
        if (type == Type.Shop) {
            this.shopActor = shopActor;
            this.changes = shopActor.getMapStage().getChanges();
            addToSelectable(restockButton);
        } else {
            doneButton.setText("[+OK]");
        }
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            actor.remove();
            if (actor instanceof RewardActor rewardActor) {
                rewardActor.dispose();
            }
        }
        addToSelectable(doneButton);
        generated.clear();

        Actor card = this.cachedCardsContainerActor;
        //reset pos
        headerLabel.setPosition(headerLabelOrigPos.x, headerLabelOrigPos.y);
        headerLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (type == Type.EventReward || type == Type.Loot || type == Type.QuestReward) {
                    autoSell = !autoSell;
                    String cb = autoSell ? "\u2611 " : "\u2610 ";
                    headerLabel.setText("[%?SHINY][;]" + cb + Forge.getLocalizer().getMessage("lblAll"));
                    for (Actor actor : new Array.ArrayIterator<>(generated)) {
                        if (actor instanceof RewardActor rewardActor) {
                            rewardActor.setAutoSell(autoSell);
                        }
                    }
                }
            }
        });
        if (type == Type.Shop) {
            String shopName = shopActor.getDescription();
            if (shopName != null && !shopName.isEmpty()) {
                headerLabel.setVisible(true);
                headerLabel.setText("[%?SHINY]{GRADIENT}" + shopName + "{ENDGRADIENT}");
                headerLabel.skipToTheEnd();
            } else {
                headerLabel.setVisible(false);
            }

            if (this.marketBackgroundImg != null) {
                this.marketBackgroundImg.setVisible(true);
            }
        } else {
            headerLabel.setVisible(false);
            headerLabel.setText("");

            if (this.marketBackgroundImg != null) {
                this.marketBackgroundImg.setVisible(false);
            }
        }

        float targetWidth = card.getWidth();
        float targetHeight = card.getHeight();
        deckSlotLabel.setVisible(deckSlotMessage != null);
        if (deckSlotMessage != null) {
            deckSlotLabel.setText("[%?SHINY]" + deckSlotMessage);
            deckSlotLabel.layout();
            deckSlotLabel.setHeight(deckSlotLabel.getPrefHeight());
            deckSlotLabel.setY(headerLabelOrigPos.y + headerLabel.getHeight() - deckSlotLabel.getHeight());
            targetHeight = Math.min(targetHeight, deckSlotLabel.getY() - card.getY());
            deckSlotMessage = null;
        }
        float xOff;
        float yOff;

        int numberOfRows = 0;
        float cardWidth = 0;
        float cardHeight = 0;
        float bestCardHeight = 0;
        int numberOfColumns = 0;
        float targetArea = targetHeight * targetWidth;
        float oldCardArea = 0;
        float newArea = 0;

        switch (type) {
            case Shop:
                String shopName = shopActor.getDescription();
                if ((shopName != null && !shopName.isEmpty())) {
                    headerLabel.setVisible(true);
                    headerLabel.setText("[%?SHINY]{GRADIENT}" + shopName + "{ENDGRADIENT}");
                    headerLabel.skipToTheEnd();
                }

                if (shopActor.canRestock()) {
                    restockButton.setVisible(true);
                } else {
                    restockButton.setVisible(false);
                    restockButton.setDisabled(true);
                }
                break;
            case QuestReward:
            case EventReward:
            case Loot:
                headerLabel.setPosition(restockButton.getX(), restockButton.getY());
                headerLabel.setVisible(true);
                headerLabel.setText("[%?SHINY][;]\u2610 " + Forge.getLocalizer().getMessage("lblAll"));
                headerLabel.skipToTheEnd();
                restockButton.setVisible(false);
                break;
            case Lost:
                headerLabel.setPosition(restockButton.getX(), restockButton.getY());
                headerLabel.setVisible(true);
                headerLabel.setText("[%?SHINY]" + Forge.getLocalizer().getMessage("lblYouLost"));
                headerLabel.skipToTheEnd();
                restockButton.setVisible(false);
                break;
            case RewardChoice:
                restockButton.setVisible(false);
                headerLabel.setVisible(remainingSelections > 0);
                headerLabel.setText(Forge.getLocalizer().getMessage("lblSelectRewards", remainingSelections));
                doneButton.setDisabled(remainingSelections > 0);
        }
        // The stage is stretched over the window, so compensate to keep the card's real ratio at any screen size.
        // Needed before the grid search so rows and columns are counted with the final card width.
        float correction = ScreenAspectCorrection.widthCorrection(Scene.getIntendedWidth(), Scene.getIntendedHeight(),
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        boolean sellsDeck = false;
        if (type == Type.Shop) {
            for (Reward reward : new Array.ArrayIterator<>(newRewards))
                sellsDeck |= isDeckReward(reward);
        }
        // A deck for sale gets a decklist button under its price, so every cell in that shop reserves two button rows
        float buttonHeight = type == Type.Shop || type == Type.RewardChoice ? doneButton.getHeight() * (sellsDeck ? 2 : 1) : 0;
        for (int h = 1; h < targetHeight; h++) {
            cardHeight = h + buttonHeight;
            //cardHeight=targetHeight/i;
            cardWidth = h / CARD_WIDTH_TO_HEIGHT * correction;
            newArea = newRewards.size * cardWidth * cardHeight;

            int rows = (int) (targetHeight / cardHeight);
            int cols = (int) Math.ceil(newRewards.size / (double) rows);
            if (newArea > oldCardArea && newArea <= targetArea && rows * cardHeight < targetHeight && cols * cardWidth < targetWidth) {
                oldCardArea = newArea;
                numberOfRows = rows;
                numberOfColumns = cols;
                bestCardHeight = h;
            }
        }
        boolean scrolls = bestCardHeight < MIN_CARD_HEIGHT;
        if (scrolls)
            bestCardHeight = MIN_CARD_HEIGHT;
        cardHeight = bestCardHeight * 0.90f;
        cardWidth = (cardHeight / CARD_WIDTH_TO_HEIGHT) * correction;
        if (scrolls) {
            numberOfColumns = Math.max(1, (int) (targetWidth / cardWidth));
            numberOfRows = (int) Math.ceil(newRewards.size / (double) numberOfColumns);
            yOff = buttonHeight;
        } else {
            yOff = (targetHeight - (cardHeight * numberOfRows)) / 2f;
        }
        xOff = (targetWidth - (cardWidth * numberOfColumns)) / 2f;
        float viewBottom = card.getY();
        if (scrolls) {
            Actor gold = ui.findActor("playerGold");
            // The portrait layout puts the gold counter inside the card area, which a full scrolling grid would cover
            if (gold.getX() < card.getX() + targetWidth)
                viewBottom = Math.max(viewBottom, gold.getY() + gold.getHeight());
            viewBottom += CHEVRON_SPACE;
        }
        // The card area overlaps buttons in portrait, so only the cards themselves take touches unless the grid scrolls
        cardScroller.setTouchable(scrolls ? Touchable.enabled : Touchable.childrenOnly);
        cardScroller.setBounds(card.getX(), viewBottom, targetWidth, card.getY() + targetHeight - viewBottom);
        cardArrow.setPosition(card.getX() + (targetWidth - cardArrow.getWidth()) / 2, viewBottom - CHEVRON_SPACE + 6);
        cardGroup.setSize(targetWidth, scrolls ? numberOfRows * (cardHeight + buttonHeight) : targetHeight);

        float spacing = 2;
        int i = 0;
        for (Reward reward : new Array.ArrayIterator<>(newRewards)) {
            boolean skipCard = false;
            if (type == Type.Shop) {
                if (changes.wasCardBought(shopActor.getObjectId(), i)) {
                    skipCard = true;
                }
            }

            int currentRow = (i / numberOfColumns);
            float lastRowXAdjust = 0;
            if (currentRow == numberOfRows - 1) {
                int lastRowCount = newRewards.size % numberOfColumns;
                if (lastRowCount != 0)
                    lastRowXAdjust = ((numberOfColumns * cardWidth) - (lastRowCount * cardWidth)) / 2;
            }

            RewardActor actor = new RewardActor(reward, type == Type.EventReward || type == Type.Loot || type == Type.QuestReward, type, type == Type.Shop && (numberOfRows > 2 || numberOfColumns > 2));

            actor.setBounds(lastRowXAdjust + xOff + cardWidth * (i % numberOfColumns) + spacing, yOff + cardHeight * currentRow + spacing, cardWidth - spacing * 2, cardHeight - spacing * 2);

            if (type == Type.Shop) {
                if (currentRow != ((i + 1) / numberOfColumns))
                    yOff += buttonHeight;

                BuyButton buyCardButton = new BuyButton(shopActor.getObjectId(), i, actor, reward, doneButton, shopActor.getPriceModifier());
                generated.add(buyCardButton);
                if (!skipCard) {
                    cardGroup.addActor(buyCardButton);
                    addToSelectable(buyCardButton);
                }
                if (isDeckReward(reward)) {
                    TextraButton decklistButton = Controls.newTextButton(Forge.getLocalizer().getMessage("lblViewDecklist"),
                            () -> showDecklist(reward.getDeck()));
                    decklistButton.setBounds(buyCardButton.getX(), buyCardButton.getY() - doneButton.getHeight(),
                            buyCardButton.getWidth(), doneButton.getHeight());
                    generated.add(decklistButton);
                    if (!skipCard) {
                        cardGroup.addActor(decklistButton);
                        addToSelectable(decklistButton);
                    }
                }
            } else if (type == Type.RewardChoice) {
                if (currentRow != ((i + 1) / numberOfColumns))
                    yOff += doneButton.getHeight();
                ChooseRewardButton chooseRewardButton = new ChooseRewardButton(i, actor, reward, doneButton);
                generated.add(chooseRewardButton);
                cardGroup.addActor(chooseRewardButton);
                addToSelectable(chooseRewardButton);
            } else {
                if (type == Type.Lost)
                    actor.sold();
                addToSelectable(actor);
            }
            generated.add(actor);
            if (!skipCard) {
                cardGroup.addActor(actor);
            }
            i++;
        }
        cardScroller.layout();
        cardScroller.setScrollY(0);
        cardScroller.updateVisualScroll();
        stage.setScrollFocus(cardScroller);
        if (type == Type.Shop) {
            updateBuyButtons();
            updateRestockButton();
        }
    }

    private void updateBuyButtons() {
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            if (actor instanceof BuyButton buyButton) {
                buyButton.update();
            }
        }
    }

    private void updateChooseRewardButtons() {
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            if (actor instanceof ChooseRewardButton chooseRewardButton) {
                chooseRewardButton.update();
            }
        }
    }

    private class BuyButton extends TextraButton {
        private final int objectID;
        private final int index;
        public RewardActor rewardActor;
        private Reward reward;
        int price;
        boolean isSold;

        void update() {
            boolean uniqueOwned = isUniqueItemOwned();
            setDisabled(isSold || uniqueOwned || WorldSave.getCurrentSave().getPlayer().getGold() < price);
            if (isSold)
                setText("SOLD");
            else if (uniqueOwned)
                setText(Forge.getLocalizer().getMessage("lblOwned"));
            else
                updateOwned();
        }

        boolean isUniqueItemOwned() {
            return Reward.Type.Item.equals(reward.getType())
                && reward.getItem() != null
                && !AdventurePlayer.current().canObtainItem(reward.getItem());
        }

        void updateOwned() {
            if (Type.Shop != type)
                return;
            if (collectionPool != null && Reward.Type.Card.equals(reward.getType()))
                setText("[%75][+GoldCoin] " + price + "\n" + Forge.getLocalizer().getMessage("lblOwned") + ": " + collectionPool.count(reward.getCard()));
            else if (Reward.Type.Item.equals(reward.getType()))
                setText("[%75][+GoldCoin] " + price + "\n" + Forge.getLocalizer().getMessage("lblOwned") + ": " + AdventurePlayer.current().countItem(reward.getItem().name));
        }

        public BuyButton(int id, int i, RewardActor actor, Reward reward, TextraButton style, float shopModifier) {
            super("", style.getStyle(), Controls.getTextraFont());
            this.objectID = id;
            this.index = i;
            rewardActor = actor;
            this.reward = reward;
            setHeight(style.getHeight());
            setWidth(actor.getWidth());
            setX(actor.getX());
            setY(actor.getY() - getHeight());
            price = CardUtil.getRewardPrice(actor.getReward());
            price *= Current.player().goldModifier();
            price *= shopModifier;
            setText("[+GoldCoin] " + price);
            updateOwned();
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (isSold || isUniqueItemOwned())
                        return;
                    if (Current.player().getGold() >= price) {
                        if (!shopActor.isUnlimited())
                            changes.buyCard(objectID, index);

                        Current.player().takeGold(price);
                        claimReward(rewardActor.getReward());

                        HapticEngine.vibrate(FPref.UI_VIBRATE_ON_SHOP_ACTION, 5);
                        SoundSystem.instance.play(SoundEffectType.FlipCoin, false);

                        if (changes == null)
                            return;
                        isSold = true;
                        setDisabled(true);
                        rewardActor.sold();
                        getColor().a = 0.5f;
                        updateCollectionPool();
                        updateBuyButtons();
                        removeListener(this);
                        if (pendingDecks.notEmpty()) {
                            deckShop = shopActor;
                            revealDeck(pendingDecks.removeIndex(0));
                        }
                    }
                }
            });
        }
    }

    private class ChooseRewardButton extends TextraButton {
        private final int index;
        public RewardActor rewardActor;
        private Reward reward;
        boolean isSold;

        void update() {
            boolean uniqueOwned = isUniqueItemOwned();
            setDisabled(isSold || uniqueOwned || remainingSelections <= 0);
            if (isSold)
                setText("SELECTED");
            else if (uniqueOwned)
                setText(Forge.getLocalizer().getMessage("lblOwned"));
            else
                updateOwned();
        }

        boolean isUniqueItemOwned() {
            return Reward.Type.Item.equals(reward.getType())
                && reward.getItem() != null
                && !AdventurePlayer.current().canObtainItem(reward.getItem());
        }

        void updateOwned() {
            if (Type.Shop != type)
                return;
            if (collectionPool != null && Reward.Type.Card.equals(reward.getType()))
                setText("Pick Reward" + "\n" + Forge.getLocalizer().getMessage("lblOwned") + ": " + collectionPool.count(reward.getCard()));
            else if (Reward.Type.Item.equals(reward.getType()))
                setText("Pick Reward" + "\n" + Forge.getLocalizer().getMessage("lblOwned") + ": " + AdventurePlayer.current().countItem(reward.getItem().name));
        }

        public ChooseRewardButton(int i, RewardActor actor, Reward reward, TextraButton style) {
            super("", style.getStyle(), Controls.getTextraFont());
            this.index = i;
            rewardActor = actor;
            this.reward = reward;
            setHeight(style.getHeight());
            setWidth(actor.getWidth());
            setX(actor.getX());
            setY(actor.getY() - getHeight());

            setText("Pick Reward");
            updateOwned();
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (isSold || isUniqueItemOwned())
                        return;
                    if (remainingSelections >= 1) {

                        remainingSelections--;
                        claimReward(rewardActor.getReward());

                        headerLabel.setVisible(remainingSelections > 0);
                        headerLabel.setText("Select " + remainingSelections + " rewards");
                        doneButton.setDisabled(remainingSelections > 0);

                        HapticEngine.vibrate(FPref.UI_VIBRATE_ON_ADVENTURE_REWARD, 5);
                        //SoundSystem.instance.play(SoundEffectType.FlipCoin, false);

                        isSold = true;
                        setDisabled(true);
                        rewardActor.sold();
                        getColor().a = 0.5f;
                        updateCollectionPool();
                        updateChooseRewardButtons();
                        removeListener(this);
                    }
                }
            });
        }
    }
}

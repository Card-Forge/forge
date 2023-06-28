package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.character.ShopActor;
import forge.adventure.data.RewardData;
import forge.adventure.data.ShopData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.assets.ImageCache;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;

/**
 * Displays the rewards of a fight or a treasure
 */
public class RewardScene extends UIScene {
    private TextraButton doneButton, detailButton, restockButton;
    private TextraLabel shopNameLabel, playerGold, playerShards;

    private ShopActor shopActor;
    private static RewardScene object;

    private PointOfInterestChanges changes;

    public static RewardScene instance() {
        if(object==null)
            object=new RewardScene();
        return object;
    }

    private boolean showTooltips = false;
    public enum Type {
        Shop,
        Loot,
        QuestReward
    }

    Type type;
    Array<Actor> generated = new Array<>();
    static public final float CARD_WIDTH = 550f ;
    static public final float CARD_HEIGHT = 400f;
    static public final float CARD_WIDTH_TO_HEIGHT = CARD_WIDTH / CARD_HEIGHT;

    private RewardScene() {

        super(Forge.isLandscapeMode() ? "ui/items.json" : "ui/items_portrait.json");

        playerGold = Controls.newAccountingLabel(ui.findActor("playerGold"), false);
        playerShards = Controls.newAccountingLabel(ui.findActor("playerShards"),true);
        shopNameLabel = ui.findActor("shopName");
        ui.onButtonPress("done", this::done);
        ui.onButtonPress("detail", this::toggleToolTip);
        ui.onButtonPress("restock", this::restockShop);
        detailButton = ui.findActor("detail");
        detailButton.setVisible(false);
        doneButton = ui.findActor("done");
        restockButton = ui.findActor("restock");
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

        Selectable selectable=getSelected();
        if(selectable==null)
            return;
        RewardActor actor;
        if(selectable.actor instanceof BuyButton)
        {
            actor= ((BuyButton) selectable.actor).reward;
        }
        else if (selectable.actor instanceof RewardActor)
        {
            actor= (RewardActor) selectable.actor;
        }
        else
        {
            return;
        }
        if(actor.toolTipIsVisible())
        {
            actor.hideTooltip();
        }
        else
        {
            if(!actor.isFlipped())
                actor.showTooltip();
        }

    }

    boolean doneClicked = false, shown = false;
    float flipCountDown = 1.0f;
    float exitCountDown = 0.0f; //Serves as additional check for when scene is exiting, so you can't double tap too fast.

    public void quitScene() {
        //There were reports of memory leaks after using the shop many times, so remove() everything on exit to be sure.
        for(Actor A: new Array.ArrayIterator<>(generated)) {
            if(A instanceof RewardActor){
                ((RewardActor) A).removeTooltip();
                ((RewardActor) A).dispose();
                A.remove();
            }
        }
        //save RAM
        ImageCache.unloadCardTextures(true);
        Forge.restrictAdvMenus = false;
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
            doneButton.setText("[+OK]");
            showLootOrDone();
            return true;
        }
		if (type != null) {
			switch (type) {
				case Shop:
					doneButton.setText("[+OK]");
					break;
				case QuestReward:
				case Loot:
					doneButton.setText("[+OK]");
					break;
			}
        }
        shown = false;
        clearGenerated();
        quitScene();
        return true;
    }
    void clearGenerated() {
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            if (!(actor instanceof RewardActor)) {
                continue;
            }
            RewardActor reward = (RewardActor) actor;
            if (type == Type.Loot)
                AdventurePlayer.current().addReward(reward.getReward());
            if (type == Type.QuestReward)
                AdventurePlayer.current().addReward(reward.getReward()); // Want to customize this soon to have selectable rewards which will be handled different here
            reward.clearHoldToolTip();
            try {
                stage.getActors().removeValue(reward, true);
            } catch (Exception e) {}
        }
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
        ImageCache.allowSingleLoad();
        if (doneClicked) {
            if (type == Type.Loot || type == Type.QuestReward) {
                flipCountDown -= Gdx.graphics.getDeltaTime();
                exitCountDown += Gdx.graphics.getDeltaTime();
            }
            if (flipCountDown <= 0) {
                clearGenerated();
                quitScene();
            }
        }
    }

    @Override
    public void enter() {
        doneButton.setText("[+OK]");
        updateDetailButton();
        super.enter();
    }

    private void showLootOrDone() {
        boolean exit = true;
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            if (!(actor instanceof RewardActor)) {
                continue;
            }
            RewardActor reward = (RewardActor) actor;
            if (!reward.isFlipped()) {
                exit = false;
                break;
            }
        }
        if (exit)
            done(true);
        else if ((type == Type.Loot || type == Type.QuestReward) && !shown) {
            shown = true;
            float delay = 0.09f;
            generated.shuffle();
            for (Actor actor : new Array.ArrayIterator<>(generated)) {
                if (!(actor instanceof RewardActor)) {
                    continue;
                }
                RewardActor reward = (RewardActor) actor;
                if (!reward.isFlipped()) {
                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            reward.flip();
                        }
                    }, delay);
                    delay += 0.15f;
                }
            }
        } else {
            done(true);
        }
    }

    void updateRestockButton(){
        if (!shopActor.canRestock())
            return;
        int price = shopActor.getRestockPrice();
        restockButton.setText("[+Refresh][+shards]" + price);
        restockButton.setDisabled(WorldSave.getCurrentSave().getPlayer().getShards() < price);
    }

    void restockShop(){
        if (!shopActor.canRestock())
            return;
        int price = shopActor.getRestockPrice();
        if(changes!=null)
            changes.generateNewShopSeed(shopActor.getObjectId());

        Current.player().takeShards(price);

        Gdx.input.vibrate(5);
        SoundSystem.instance.play(SoundEffectType.Shuffle, false);

        updateBuyButtons();
        if(changes==null)
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
        loadRewards(ret, RewardScene.Type.Shop,shopActor);
    }

    public void loadRewards(Deck deck, Type type, ShopActor shopActor){
        Array<Reward> rewards = new Array<>();
        for (PaperCard card : deck.getAllCardsInASinglePool().toFlatList()){
            rewards.add(new Reward(card));
        }
        loadRewards(rewards, type, shopActor);
    }

    public void loadRewards(Array<Reward> newRewards, Type type, ShopActor shopActor) {
        clearSelectable();
        this.type   = type;
        doneClicked = false;
        if (type==Type.Shop) {
            this.shopActor = shopActor;
            this.changes = shopActor.getMapStage().getChanges();
        }
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            actor.remove();
            if (actor instanceof RewardActor) {
                ((RewardActor) actor).dispose();
            }
        }
        generated.clear();

        Actor card = ui.findActor("cards");
        if(type==Type.Shop) {
            String shopName = shopActor.getDescription();
            if (shopName != null && !shopName.isEmpty()) {
                shopNameLabel.setVisible(true);
                shopNameLabel.setText(shopName);
            }
            else
            {
                shopNameLabel.setVisible(false);
            }
            Actor background = ui.findActor("market_background");
            if(background!=null)
                background.setVisible(true);
        } else {
            shopNameLabel.setVisible(false);
            shopNameLabel.setText("");
            Actor background = ui.findActor("market_background");
            if(background!=null)
                background.setVisible(false);
        }

        float targetWidth = card.getWidth();
        float targetHeight = card.getHeight();
        float xOff = card.getX();
        float yOff = card.getY();

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
                doneButton.setText("[+OK]");
                String shopName = shopActor.getDescription();
                if ((shopName != null && !shopName.isEmpty())) {
                    shopNameLabel.setVisible(true);
                    shopNameLabel.setText(shopName);
                }

                if (shopActor.canRestock()) {
                    restockButton.setVisible(true);
                }
                else{
                    restockButton.setVisible(false);
                    restockButton.setDisabled(true);
                }
                break;
            case QuestReward:
            case Loot:
                shopNameLabel.setVisible(false);
                shopNameLabel.setText("");
                restockButton.setVisible(false);
                doneButton.setText("[+OK]");
                break;
        }
        for (int h = 1; h < targetHeight; h++) {
            cardHeight = h;
            if (type == Type.Shop) {
                cardHeight += doneButton.getHeight();
            }
            //cardHeight=targetHeight/i;
            cardWidth = h / CARD_WIDTH_TO_HEIGHT;
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
        float AR = 480f/270f;
        int x = Forge.getDeviceAdapter().getRealScreenSize(false).getLeft();
        int y = Forge.getDeviceAdapter().getRealScreenSize(false).getRight();
        int realX = Forge.getDeviceAdapter().getRealScreenSize(true).getLeft();
        int realY = Forge.getDeviceAdapter().getRealScreenSize(true).getRight();
        float fW = x > y ? x : y;
        float fH = x > y ? y : x;
        float mul = fW/fH < AR ? AR/(fW/fH) : (fW/fH)/AR;
        if (fW/fH >= 2f) {//tall display
            mul = (fW/fH) - ((fW/fH)/AR);
            if ((fW/fH) >= 2.1f && (fW/fH) < 2.2f)
                mul *= 0.9f;
            else if ((fW/fH) > 2.2f) //ultrawide 21:9 Galaxy Fold, Huawei X2, Xperia 1
                mul *= 0.8f;
        }
        cardHeight = bestCardHeight * 0.90f ;
        Float custom = Forge.isLandscapeMode() ? Config.instance().getSettingData().rewardCardAdjLandscape : Config.instance().getSettingData().rewardCardAdj;
        if (custom != null && custom != 1f) {
            mul *= custom;
        } else {
            if (realX > x || realY > y) {
                mul *= Forge.isLandscapeMode() ? 0.95f : 1.05f;
            } else {
                //immersive | no navigation and/or showing cutout cam
                if (fW/fH > 2.2f)
                    mul *= Forge.isLandscapeMode() ? 1.1f : 1.6f;
                else if (fW/fH >= 2.1f)
                    mul *= Forge.isLandscapeMode() ? 1.05f : 1.5f;
                else if (fW/fH >= 2f)
                    mul *= Forge.isLandscapeMode() ? 1f : 1.4f;

            }
        }
        cardWidth = (cardHeight / CARD_WIDTH_TO_HEIGHT)*mul;

        yOff += (targetHeight - (cardHeight * numberOfRows)) / 2f;
        xOff += (targetWidth - (cardWidth * numberOfColumns)) / 2f;

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

            RewardActor actor = new RewardActor(reward, type == Type.Loot || type == Type.QuestReward,type);

            actor.setBounds(lastRowXAdjust + xOff + cardWidth * (i % numberOfColumns) + spacing, yOff + cardHeight * currentRow + spacing, cardWidth - spacing * 2, cardHeight - spacing * 2);

            if (type == Type.Shop) {
                if (currentRow != ((i + 1) / numberOfColumns))
                    yOff += doneButton.getHeight();

                BuyButton buyCardButton = new BuyButton(shopActor.getObjectId(), i, actor, doneButton, shopActor.getPriceModifier());
                generated.add(buyCardButton);
                if (!skipCard) {
                    stage.addActor(buyCardButton);
                    addToSelectable(buyCardButton);
                }
            } else {
                addToSelectable(actor);
            }
            generated.add(actor);
            if (!skipCard) {
                stage.addActor(actor);
            }
            i++;
        }
        if (type == Type.Shop) {
            updateBuyButtons();
            updateRestockButton();
        }
    }


    private void updateBuyButtons() {
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            if (actor instanceof BuyButton) {
                ((BuyButton) actor).update();
            }
        }
    }
    private class BuyButton extends TextraButton {
        private final int objectID;
        private final int index;
        public RewardActor reward;
        int price;

        void update() {
            setDisabled(WorldSave.getCurrentSave().getPlayer().getGold() < price);
        }

        public BuyButton(int id, int i, RewardActor actor, TextraButton style, float shopModifier) {
            super("", style.getStyle(),Controls.getTextraFont());
            this.objectID = id;
            this.index = i;
            reward = actor;
            setHeight(style.getHeight());
            setWidth(actor.getWidth());
            setX(actor.getX());
            setY(actor.getY() - getHeight());
            price = CardUtil.getRewardPrice(actor.getReward());
            price *= Current.player().goldModifier();
            price *= shopModifier;
            setText(price+"[+Gold]");
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                if (Current.player().getGold() >= price) {
                    if(!shopActor.isUnlimited())
                        changes.buyCard(objectID, index);

                    Current.player().takeGold(price);
                    Current.player().addReward(reward.getReward());

                    Gdx.input.vibrate(5);
                    SoundSystem.instance.play(SoundEffectType.FlipCoin, false);

                    updateBuyButtons();
                    if(changes==null)
                        return;
                    setDisabled(true);
                    reward.sold();
                    getColor().a = 0.5f;
                    setText("SOLD");
                    removeListener(this);
                }
                }
            });
        }
    }
}

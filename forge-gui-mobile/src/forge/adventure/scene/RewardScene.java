package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.character.ShopActor;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.CardUtil;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.util.Reward;
import forge.adventure.util.RewardActor;
import forge.adventure.world.WorldSave;
import forge.assets.ImageCache;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;

/**
 * Displays the rewards of a fight or a treasure
 */
public class RewardScene extends UIScene {
    private TextButton doneButton;
    private Label goldLabel;
    public enum Type {
        Shop,
        Loot
    }

    Type type;
    Array<Actor> generated = new Array<>();
    static public final float CARD_WIDTH =550f ;
    static public final float CARD_HEIGHT = 400f;
    static public final float CARD_WIDTH_TO_HEIGHT = CARD_WIDTH / CARD_HEIGHT;

    public RewardScene() {
        super(Forge.isLandscapeMode() ? "ui/items.json" : "ui/items_portrait.json");
    }

    boolean doneClicked = false;
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
        Forge.switchToLast();
    }

    public boolean done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        if (doneClicked) {
            if(exitCountDown > 0.2f) {
                clearGenerated();
                quitScene();
            }
            return true;
        }

        if (type == Type.Loot) {
            boolean wait = false;
            for (Actor actor : new Array.ArrayIterator<>(generated)) {
                if (!(actor instanceof RewardActor)) {
                    continue;
                }
                RewardActor reward = (RewardActor) actor;
                AdventurePlayer.current().addReward(reward.getReward());
                if (!reward.isFlipped()) {
                    wait = true;
                    reward.flip();
                }
            }
            if (wait) {
                flipCountDown = Math.min(1.0f + (generated.size * 0.3f), 5.0f);
                exitCountDown = 0.0f;
                doneClicked = true;
            } else {
                clearGenerated();
                quitScene();
            }
        } else {
            clearGenerated();
            quitScene();
        }
        return true;
    }
    void clearGenerated() {
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            if (!(actor instanceof RewardActor)) {
                continue;
            }
            RewardActor reward = (RewardActor) actor;
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
            if (type == Type.Loot) {
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
    public void resLoaded() {
        super.resLoaded();
            goldLabel=ui.findActor("gold");
            ui.onButtonPress("done", () -> RewardScene.this.done());
            doneButton = ui.findActor("done");
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            done();
        }
        return true;
    }


    public void loadRewards(Array<Reward> newRewards, Type type, ShopActor shopActor) {
        this.type   = type;
        doneClicked = false;
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            actor.remove();
            if (actor instanceof RewardActor) {
                ((RewardActor) actor).dispose();
            }
        }
        generated.clear();


        Actor card = ui.findActor("cards");
        if(type==Type.Shop) {
            goldLabel.setText("Gold:"+Current.player().getGold());
            Actor background = ui.findActor("market_background");
            if(background!=null)
                background.setVisible(true);
        } else {
            goldLabel.setText("");
            Actor background = ui.findActor("market_background");
            if(background!=null)
                background.setVisible(false);
        }
        // card.setDrawable(new TextureRegionDrawable(new Texture(Res.CurrentRes.GetFile("ui/transition.png"))));

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
                doneButton.setText(Forge.getLocalizer().getMessage("lblLeave"));
                goldLabel.setText("Gold:"+Current.player().getGold());
                break;
            case Loot:
                goldLabel.setText("");
                doneButton.setText(Forge.getLocalizer().getMessage("lblDone"));
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
                if (shopActor.getMapStage().getChanges().wasCardBought(shopActor.getObjectId(), i)) {
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
            RewardActor actor = new RewardActor(reward, type == Type.Loot);
            actor.setBounds(lastRowXAdjust + xOff + cardWidth * (i % numberOfColumns) + spacing, yOff + cardHeight * currentRow + spacing, cardWidth - spacing * 2, cardHeight - spacing * 2);

            if (type == Type.Shop) {
                if (currentRow != ((i + 1) / numberOfColumns))
                    yOff += doneButton.getHeight();

                TextButton buyCardButton = new BuyButton(shopActor.getObjectId(), i, shopActor.isUnlimited()?null:shopActor.getMapStage().getChanges(), actor, doneButton);
                generated.add(buyCardButton);
                if (!skipCard) {
                    stage.addActor(buyCardButton);
                }
            }
            generated.add(actor);
            if (!skipCard) {
                stage.addActor(actor);
            }
            i++;
        }
        updateBuyButtons();
    }

    private void updateBuyButtons() {
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            if (actor instanceof BuyButton) {
                ((BuyButton) actor).update();
            }
        }
    }

    private class BuyButton extends TextButton {
        private final int objectID;
        private final int index;
        private final PointOfInterestChanges changes;
        RewardActor reward;
        int price;

        void update() {
            setDisabled(WorldSave.getCurrentSave().getPlayer().getGold() < price);
        }

        public BuyButton(int id, int i, PointOfInterestChanges ch, RewardActor actor, TextButton style) {
            super("", style.getStyle());
            this.objectID = id;
            this.index = i;
            this.changes = ch;
            reward = actor;
            setHeight(style.getHeight());
            setWidth(actor.getWidth());
            setX(actor.getX());
            setY(actor.getY() - getHeight());
            price = CardUtil.getRewardPrice(actor.getReward());
            price *= Current.player().goldModifier();
            setText("$ " + price);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                if (Current.player().getGold() >= price) {
                    if(changes!=null)
                        changes.buyCard(objectID, index);
                    Current.player().takeGold(price);
                    Current.player().addReward(reward.getReward());

                    Gdx.input.vibrate(5);
                    SoundSystem.instance.play(SoundEffectType.FlipCoin, false);

                    updateBuyButtons();
                    goldLabel.setText("Gold: " + String.valueOf(AdventurePlayer.current().getGold()));
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

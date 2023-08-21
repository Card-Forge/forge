package forge.adventure.scene;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.EnemyData;
import forge.adventure.data.WorldData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.util.Paths;
import forge.adventure.world.WorldSave;
import forge.assets.FBufferedImage;
import forge.card.ColorSet;
import forge.game.GameType;
import forge.localinstance.achievements.Achievement;
import forge.localinstance.achievements.AchievementCollection;
import forge.localinstance.achievements.CardActivationAchievements;
import forge.localinstance.achievements.PlaneswalkerAchievements;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.TextUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class PlayerStatisticScene extends UIScene {
    Image avatar, avatarBorder;
    Image colorFrame;
    TextraLabel money, life, shards;
    TextraLabel wins, totalWins, eventWins, eventMatchWins;
    TextraLabel loss, totalLoss, eventLosses, eventMatchLosses;
    TextraLabel winloss, lossWinRatio, eventLossWinRatio, eventMatchLossWinRatio;
    TextraLabel playerName, headerAchievements, headerAvatar, headerName, headerWinLoss;
    TextraButton back, toggleAward;
    private final Table scrollContainer, achievementContainer;
    TextraLabel blessingScroll;
    ScrollPane scroller;
    Table root;
    boolean toggle = false;
    AchievementCollection planeswalkers, achievements, cardActivation;
    Scene lastGameScene;

    private PlayerStatisticScene() {
        super(Forge.isLandscapeMode() ? "ui/statistic.json" : "ui/statistic_portrait.json");
        planeswalkers = PlaneswalkerAchievements.instance;
        achievements = FModel.getAchievements(GameType.Adventure);
        cardActivation = CardActivationAchievements.instance;
        scrollContainer = new Table(Controls.getSkin());
        scrollContainer.row();
        achievementContainer = new Table(Controls.getSkin());
        blessingScroll = Controls.newTextraLabel("");
        blessingScroll.setAlignment(Align.topLeft);
        blessingScroll.setWrap(true);
        ui.onButtonPress("return", PlayerStatisticScene.this::back);
        ui.onButtonPress("quests", PlayerStatisticScene.this::quests);
        avatar = ui.findActor("avatar");
        avatarBorder = ui.findActor("avatarBorder");
        playerName = ui.findActor("playerName");
        life = ui.findActor("lifePoints");
        money = ui.findActor("money");
        shards = ui.findActor("shards");
        wins = ui.findActor("wins");
        colorFrame = ui.findActor("colorFrame");
        totalWins = ui.findActor("totalWins");
        loss = ui.findActor("loss");
        totalLoss = ui.findActor("totalLoss");
        winloss = ui.findActor("winloss");
        lossWinRatio = ui.findActor("lossWinRatio");

        eventMatchWins = ui.findActor("eventMatchWins");
        eventMatchLosses = ui.findActor("eventMatchLosses");
        eventMatchLossWinRatio = ui.findActor("eventMatchLossWinRatio");

        eventWins = ui.findActor("eventWins");
        eventLosses = ui.findActor("eventLosses");
        eventLossWinRatio = ui.findActor("eventLossWinRatio");

        totalWins = ui.findActor("totalWins");
        loss = ui.findActor("loss");
        totalLoss = ui.findActor("totalLoss");
        winloss = ui.findActor("winloss");
        lossWinRatio = ui.findActor("lossWinRatio");


        back = ui.findActor("return");
        toggleAward = ui.findActor("toggleAward");
        headerAchievements = Controls.newTextraLabel("[%110]" + Forge.getLocalizer().getMessage("lblAchievements"));
        headerAvatar = Controls.newTextraLabel("[%110]" + Forge.getLocalizer().getMessage("lblAvatar"));
        headerName = Controls.newTextraLabel("[%110]" + Forge.getLocalizer().getMessage("lblName"));
        headerWinLoss = Controls.newTextraLabel("[%110]" + Forge.getLocalizer().getMessage("lblWinProper") + "/" + Forge.getLocalizer().getMessage("lblLossProper"));
        toggleAward.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleScroller();
            }
        });

        Window window = ui.findActor("scrollWindow");
        root = ui.findActor("enemies");
        root.add(headerAvatar).pad(5).center();
        root.add(headerName).fillX().pad(5).width(100).center();
        root.add(headerWinLoss).pad(5).center();
        root.row();
        scroller = new ScrollPane(scrollContainer);
        root.add(scroller).colspan(3);
        ScrollPane blessing = ui.findActor("blessingInfo");
        blessing.setActor(blessingScroll);
        window.add(root);
    }

    private static PlayerStatisticScene object;

    public static PlayerStatisticScene instance(Scene lastGameScene) {
        if (object == null)
            object = new PlayerStatisticScene();
        if (lastGameScene != null)
            object.lastGameScene=lastGameScene;
        return object;
    }

    @Override
    public void dispose() {
        if (achievements != null) {
            for (Achievement a : achievements)
                ((FBufferedImage) a.getImage()).dispose();
        }
        if (planeswalkers != null) {
            for (Achievement a : planeswalkers)
                ((FBufferedImage) a.getImage()).dispose();
        }
        if (cardActivation != null) {
            for (Achievement a : cardActivation)
                ((FBufferedImage) a.getImage()).dispose();
        }
    }

    private void toggleScroller() {
        toggle = !toggle;
        if (toggle) {
            root.clear();
            root.add(headerAchievements).pad(5).colspan(2).center().expand();
            root.row();
            root.add(scroller).expand();
            scroller.setActor(achievementContainer);
            toggleAward.setText("[%125][+VS]");
        } else {
            root.clear();
            root.add(headerAvatar).pad(5).center();
            root.add(headerName).fillX().pad(5).width(100).center();
            root.add(headerWinLoss).pad(5).center();
            root.row();
            root.add(scroller).colspan(3);
            scroller.setActor(scrollContainer);
            toggleAward.setText("[%125][+AWARD]");
        }
        performTouch(scroller);
    }

    private TextureRegion getColorFrame(ColorSet color) {
        String colorName = "color_";
        if (color.hasWhite())
            colorName += "w";
        if (color.hasBlue())
            colorName += "u";
        if (color.hasBlack())
            colorName += "b";
        if (color.hasRed())
            colorName += "r";
        if (color.hasGreen())
            colorName += "g";
        return Config.instance().getAtlas(Paths.COLOR_FRAME_ATLAS).findRegion(colorName);
    }

    @Override
    public void enter() {
        super.enter();
        GameHUD.getInstance().switchAudio();
        achievementContainer.clear();
        updateAchievements(cardActivation, true);
        updateAchievements(planeswalkers, true);
        updateAchievements(achievements, false);
        scrollContainer.clear();

        if (playerName != null) {
            playerName.setText(GamePlayerUtil.getGuiPlayer().getName());
        }
        if (avatar != null) {
            avatar.setDrawable(new TextureRegionDrawable(Current.player().avatar()));
        }
        if (life != null) {
            AdventurePlayer.current().onLifeChange(() -> life.setText("[+Life] [BLACK]" + AdventurePlayer.current().getLife() + "/" + AdventurePlayer.current().getMaxLife()));
        }
        if (money != null) {
            WorldSave.getCurrentSave().getPlayer().onGoldChange(() -> money.setText("[+Gold] [BLACK]" + AdventurePlayer.current().getGold()));
        }
        if (shards != null) {
            WorldSave.getCurrentSave().getPlayer().onShardsChange(() -> shards.setText("[+Shards] [BLACK]" + AdventurePlayer.current().getShards()));
        }
        if (totalWins != null) {
            totalWins.setText(String.valueOf(Current.player().getStatistic().totalWins()));
        }
        if (totalLoss != null) {
            totalLoss.setText(String.valueOf(Current.player().getStatistic().totalLoss()));
        }
        if (lossWinRatio != null) {
            lossWinRatio.setText(TextUtil.decimalFormat(Current.player().getStatistic().winLossRatio()));
        }
        if (eventMatchWins != null) {
            eventMatchWins.setText(String.valueOf(Current.player().getStatistic().eventWins()));
        }
        if (eventMatchLosses != null) {
            eventMatchLosses.setText(String.valueOf(Current.player().getStatistic().eventMatchLosses()));
        }
        if (eventMatchLossWinRatio != null) {
            eventMatchLossWinRatio.setText(Float.toString(Current.player().getStatistic().eventMatchWinLossRatio()));
        }
        if (eventWins != null) {
            eventWins.setText(String.valueOf(Current.player().getStatistic().eventWins()));
        }
        if (eventLosses != null) {
            eventLosses.setText(String.valueOf(Current.player().getStatistic().eventLosses()));
        }
        if (eventLossWinRatio != null) {
            eventLossWinRatio.setText(Float.toString(Current.player().getStatistic().eventWinLossRatio()));
        }

        if (colorFrame != null) {
            colorFrame.setDrawable(new TextureRegionDrawable(getColorFrame(Current.player().getColorIdentity())));
        }
        if (blessingScroll != null) {
            if (Current.player().getBlessing() != null) {
                blessingScroll.setText("[BLACK]" + Current.player().getBlessing().getDescription());
            } else {
                blessingScroll.setText("[BLACK]No blessing.");
            }
        }

        for (Map.Entry<String, Pair<Integer, Integer>> entry : Current.player().getStatistic().getWinLossRecord().entrySet()) {
            EnemyData data = WorldData.getEnemy(entry.getKey());
            if (data == null) continue;
            Image enemyImage = new Image(new EnemySprite(data).getAvatar());
            enemyImage.setScaling(Scaling.stretch);
            scrollContainer.add(enemyImage).pad(5).size(16).fillY();
            scrollContainer.add().width(16);
            scrollContainer.add(data.getName()).fillX().pad(5).width(120);
            scrollContainer.add(entry.getValue().getLeft().toString() + "/" + entry.getValue().getRight().toString()).pad(5);
            scrollContainer.row();
        }
        performTouch(scrollPaneOfActor(scrollContainer)); //can use mouse wheel if available to scroll
    }

    void updateAchievements(AchievementCollection achievementCollection, boolean isActive) {
        for (Achievement a : achievementCollection) {
            if (isActive) {
                if (!a.isActive()) //skip inactive
                    continue;
            } else {
                GameType g = GameType.smartValueOf(a.getKey());
                if (g != null) //skip variants
                    continue;
            }
            TextureRegion textureRegion = new TextureRegion(((FBufferedImage) a.getImage()).getTexture());
            textureRegion.flip(false, true);
            Image image = new Image(textureRegion);
            float alpha = a.isActive() ? 1f : 0.25f;
            image.getColor().a = alpha;
            achievementContainer.add(image).height(50).width(40).center().pad(5);
            String value = "[%105]" + a.getDisplayName() + "[%98]";
            String subTitle = a.getSubTitle(true);
            if (subTitle != null)
                value += "\n" + subTitle;
            TextraLabel label = Controls.newTextraLabel(value);
            label.getColor().a = alpha;
            achievementContainer.add(label).left().pad(5);
            achievementContainer.row();
        }
    }

    public boolean quests() {
        Forge.switchScene(QuestLogScene.instance(lastGameScene),true);
        return true;
    }

    @Override
    public boolean back(){
        Forge.switchScene(lastGameScene==null?GameScene.instance():lastGameScene);
        return true;
    }

}

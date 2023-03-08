package forge.adventure.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.EnemyData;
import forge.adventure.data.WorldData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.util.Paths;
import forge.adventure.world.WorldSave;
import forge.card.ColorSet;
import forge.player.GamePlayerUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class PlayerStatisticScene extends UIScene {
    Image avatar, avatarBorder, lifeIcon, goldIcon;
    Image colorFrame;
    TextraLabel money, life;
    TextraLabel wins, totalWins;
    TextraLabel loss, totalLoss;
    TextraLabel winloss, lossWinRatio;
    TextraLabel playerName;
    TextraButton back;
    private final Table scrollContainer;
    TextraLabel blessingScroll;

    private PlayerStatisticScene() {
        super(Forge.isLandscapeMode() ? "ui/statistic.json" : "ui/statistic_portrait.json");
        scrollContainer = new Table(Controls.getSkin());
        scrollContainer.row();
        blessingScroll = Controls.newTextraLabel("");
        blessingScroll.setColor(Color.BLACK);
        blessingScroll.setAlignment(Align.topLeft);
        blessingScroll.setWrap(true);
        ui.onButtonPress("return", PlayerStatisticScene.this::back);
        avatar = ui.findActor("avatar");
        avatarBorder = ui.findActor("avatarBorder");
        playerName = ui.findActor("playerName");
        life = ui.findActor("lifePoints");
        money = ui.findActor("money");
        lifeIcon = ui.findActor("lifeIcon");
        goldIcon = ui.findActor("goldIcon");
        wins = ui.findActor("wins");
        colorFrame = ui.findActor("colorFrame");
        totalWins = ui.findActor("totalWins");
        loss = ui.findActor("loss");
        totalLoss = ui.findActor("totalLoss");
        winloss = ui.findActor("winloss");
        lossWinRatio = ui.findActor("lossWinRatio");
        back = ui.findActor("return");
        Window window = ui.findActor("scrollWindow");
        Table root = ui.findActor("enemies");
        root.add(Forge.getLocalizer().getMessage("lblAvatar")).pad(3, 10, 3, 10).center();
        root.add(Forge.getLocalizer().getMessage("lblName")).fillX().pad(3, 10, 3, 60).center();
        root.add(Forge.getLocalizer().getMessage("lblWinProper") + "/" + Forge.getLocalizer().getMessage("lblLossProper")).pad(3, 5, 3, 10).center();
        root.row();
        ScrollPane scroller = new ScrollPane(scrollContainer);
        root.add(scroller).colspan(3);
        ScrollPane blessing = ui.findActor("blessingInfo");
        blessing.setActor(blessingScroll);
        window.add(root);
    }

    private static PlayerStatisticScene object;

    public static PlayerStatisticScene instance() {
        if (object == null)
            object = new PlayerStatisticScene();
        return object;
    }


    @Override
    public void dispose() {

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
        scrollContainer.clear();

        if (playerName != null) {
            playerName.setText(GamePlayerUtil.getGuiPlayer().getName());
        }
        if (avatar != null) {
            avatar.setDrawable(new TextureRegionDrawable(Current.player().avatar()));
        }
        if (life != null) {
            AdventurePlayer.current().onLifeChange(() -> life.setText(AdventurePlayer.current().getLife() + "/" + AdventurePlayer.current().getMaxLife()));
        }
        if (money != null) {
            WorldSave.getCurrentSave().getPlayer().onGoldChange(() -> money.setText(String.valueOf(AdventurePlayer.current().getGold())));
        }
        if (totalWins != null) {
            totalWins.setText(String.valueOf(Current.player().getStatistic().totalWins()));
        }
        if (totalLoss != null) {
            totalLoss.setText(String.valueOf(Current.player().getStatistic().totalLoss()));
        }
        if (lossWinRatio != null) {
            lossWinRatio.setText(Float.toString(Current.player().getStatistic().winLossRatio()));
        }
        if (colorFrame != null) {
            colorFrame.setDrawable(new TextureRegionDrawable(getColorFrame(Current.player().getColorIdentity())));
        }
        if (blessingScroll != null) {
            if (Current.player().getBlessing() != null) {
                blessingScroll.setText(Current.player().getBlessing().getDescription());
            } else {
                blessingScroll.setText("No blessing.");
            }
        }

        for (Map.Entry<String, Pair<Integer, Integer>> entry : Current.player().getStatistic().getWinLossRecord().entrySet()) {
            EnemyData data = WorldData.getEnemy(entry.getKey());
            if (data == null) continue;
            Image enemyImage = new Image();
            enemyImage.setDrawable(new TextureRegionDrawable(new EnemySprite(data).getAvatar()));
            enemyImage.setSize(8, 8);

            scrollContainer.add(enemyImage).pad(3, 10, 3, 10).center();
            scrollContainer.add((data.name)).fillX().pad(3, 10, 3, 40).center();
            scrollContainer.add(entry.getValue().getLeft().toString() + "/" + entry.getValue().getRight().toString()).pad(3, 5, 3, 10).center();
            scrollContainer.row();
        }
        performTouch(scrollPaneOfActor(scrollContainer)); //can use mouse wheel if available to scroll
    }
}

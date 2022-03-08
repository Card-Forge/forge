package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.EnemyData;
import forge.adventure.data.WorldData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;
import forge.player.GamePlayerUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class PlayerStatisticScene extends UIScene {
    Image avatar, avatarBorder, lifeIcon, goldIcon;
    Label money, life;
    Label wins, totalWins;
    Label loss, totalLoss;
    Label winloss, lossWinRatio;
    Label playerName;
    TextButton back;
    private Table enemiesGroup;
    boolean init;

    public PlayerStatisticScene() {
        super("ui/statistic.json");
    }


    @Override
    public void dispose() {
    }


    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            back();
        }
        return true;
    }

    public boolean back() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
        return true;
    }

    @Override
    public void enter() {
        super.enter();
        enemiesGroup.clear();

        enemiesGroup.add(Forge.getLocalizer().getMessage("lblAvatar")).align(Align.center).space(3, 10, 3, 10);
        enemiesGroup.add(Forge.getLocalizer().getMessage("lblName")).fillX().align(Align.center).fillX().space(3, 10, 3, 60);
        enemiesGroup.add(Forge.getLocalizer().getMessage("lblWinProper")).align(Align.center).space(3, 5, 3, 5);
        enemiesGroup.add("/").align(Align.center).space(3, 5, 3, 5);
        enemiesGroup.add(Forge.getLocalizer().getMessage("lblLossProper")).align(Align.center).space(3, 5, 3, 5);
        enemiesGroup.row().space(8);

        if (playerName != null) {
            playerName.setText(GamePlayerUtil.getGuiPlayer().getName());
        }
        if (avatar != null) {
            avatar.setDrawable(new TextureRegionDrawable(Current.player().avatar()));
        }
        if (life != null) {
            AdventurePlayer.current().onLifeChange(new Runnable() {
                @Override
                public void run() {
                    life.setText(AdventurePlayer.current().getLife() + "/" + AdventurePlayer.current().getMaxLife());
                }
            });
        }
        if (money != null) {
            WorldSave.getCurrentSave().getPlayer().onGoldChange(new Runnable() {
                @Override
                public void run() {
                    money.setText(String.valueOf(AdventurePlayer.current().getGold()));
                }
            });
        }
        if (totalWins != null) {
            totalWins.setText(Current.player().getStatistic().totalWins());
        }
        if (totalLoss != null) {
            totalLoss.setText(Current.player().getStatistic().totalLoss());
        }
        if (lossWinRatio != null) {
            lossWinRatio.setText(Float.toString(Current.player().getStatistic().winLossRatio()));
        }

        for (Map.Entry<String, Pair<Integer, Integer>> entry : Current.player().getStatistic().getWinLossRecord().entrySet()) {
            EnemyData data = WorldData.getEnemy(entry.getKey());
            if (data == null) continue;
            Image enemyImage = new Image();
            enemyImage.setDrawable(new TextureRegionDrawable(new EnemySprite(data).getAvatar()));
            enemyImage.setSize(8, 8);
            if (!Forge.isLandscapeMode())
                enemyImage.setScaleX(2);

            enemiesGroup.add(enemyImage).align(Align.center).space(3, 10, 3, 10);
            enemiesGroup.add((data.name)).fillX().align(Align.center).fillX().space(3, 10, 3, 10);
            enemiesGroup.add((entry.getValue().getLeft().toString())).space(3, 2, 3, 2);
            enemiesGroup.add(("/")).align(Align.center).space(3, 2, 3, 2);
            enemiesGroup.add((entry.getValue().getRight().toString())).align(Align.center).space(3, 2, 3, 2);
            enemiesGroup.row().space(8);
        }

        if (!Forge.isLandscapeMode()) {
            float w = Scene.GetIntendedWidth();
            back.setHeight(20);
            back.setX(w / 2 - back.getWidth() / 2);
            back.setY(0);
            ScrollPane enemies = ui.findActor("enemies");
            enemies.setWidth(w - 20);
            enemies.setX(w / 2 - enemies.getWidth() / 2);
            enemies.setHeight(150);
            enemies.setY(21);
            ScrollPane stats = ui.findActor("stats");
            stats.setWidth(w - 20);
            stats.setX(w / 2 - enemies.getWidth() / 2);
            stats.setHeight(90);
            stats.setY(enemies.getY() + 153);
            avatar.setScaleX(2);
            avatar.setX(40);
            avatar.setY(stats.getY() + 15);
            avatarBorder.setScaleX(2);
            avatarBorder.setX(40);
            avatarBorder.setY(stats.getY() + 15);
            playerName.setX(avatar.getRight() + 105);
            playerName.getStyle().font.getData().setScale(2, 1);
            playerName.setY(avatar.getY() + 45);
            wins.setY(avatar.getY() + 30);
            wins.setX(avatar.getRight() + 105);
            totalWins.setY(wins.getY());
            totalWins.setX(wins.getRight() + 85);
            loss.setY(avatar.getY() + 15);
            loss.setX(avatar.getRight() + 105);
            totalLoss.setX(loss.getRight() + 85);
            totalLoss.setY(loss.getY());
            winloss.setY(avatar.getY());
            winloss.setX(avatar.getRight() + 105);
            lossWinRatio.setY(winloss.getY());
            lossWinRatio.setX(winloss.getRight() + 85);
            lifeIcon.setScaleX(2);
            lifeIcon.setY(stats.getY() + 5);
            lifeIcon.setX(wins.getX()-35);
            life.setX(lifeIcon.getX() + 35);
            life.setY(lifeIcon.getY());
            goldIcon.setScaleX(2);
            goldIcon.setY(stats.getY() + 5);
            goldIcon.setX(totalWins.getX()-35);
            money.setY(goldIcon.getY());
            money.setX(goldIcon.getX() + 35);
        }

    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        if (!this.init) {
            enemiesGroup = new Table(Controls.GetSkin());
            enemiesGroup.row();
            ui.onButtonPress("return", new Runnable() {
                @Override
                public void run() {
                    PlayerStatisticScene.this.back();
                }
            });
            avatar = ui.findActor("avatar");
            avatarBorder = ui.findActor("avatarBorder");
            playerName = ui.findActor("playerName");
            life = ui.findActor("lifePoints");
            money = ui.findActor("money");
            lifeIcon = ui.findActor("lifeIcon");
            goldIcon = ui.findActor("goldIcon");
            wins = ui.findActor("wins");
            wins.setText(Forge.getLocalizer().getMessage("lblWinProper")+":");
            totalWins = ui.findActor("totalWins");
            loss = ui.findActor("loss");
            loss.setText(Forge.getLocalizer().getMessage("lblLossProper")+":");
            totalLoss = ui.findActor("totalLoss");
            winloss = ui.findActor("winloss");
            winloss.setText(Forge.getLocalizer().getMessage("lblWinProper")+"/"+Forge.getLocalizer().getMessage("lblLossProper"));
            lossWinRatio = ui.findActor("lossWinRatio");
            back = ui.findActor("return");
            back.getLabel().setText(Forge.getLocalizer().getMessage("lblBack"));
            ScrollPane scrollPane = ui.findActor("enemies");
            scrollPane.setActor(enemiesGroup);
            this.init = true;
        }
    }

    @Override
    public void create() {

    }
}

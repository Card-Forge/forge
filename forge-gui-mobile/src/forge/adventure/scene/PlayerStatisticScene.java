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

    public PlayerStatisticScene() {
        super(Forge.isLandscapeMode() ? "ui/statistic.json" : "ui/statistic_portrait.json");
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

            enemiesGroup.add(enemyImage).align(Align.center).space(3, 10, 3, 10);
            enemiesGroup.add((data.name)).fillX().align(Align.center).fillX().space(3, 10, 3, 10);
            enemiesGroup.add((entry.getValue().getLeft().toString())).space(3, 2, 3, 2);
            enemiesGroup.add(("/")).align(Align.center).space(3, 2, 3, 2);
            enemiesGroup.add((entry.getValue().getRight().toString())).align(Align.center).space(3, 2, 3, 2);
            enemiesGroup.row().space(8);
        }


    }

    @Override
    public void resLoaded() {
        super.resLoaded();
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
    }

    @Override
    public void create() {

    }
}

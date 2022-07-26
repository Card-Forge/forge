package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Current;

/**
 * Scene for the Inn in towns
 */
public class InnScene extends UIScene {
    TextButton tempHitPointCost, sell, leave;
    Label tempHitPoints;
    Image healIcon, sellIcon, leaveIcon;

    public InnScene() {
        super(Forge.isLandscapeMode() ? "ui/inn.json" : "ui/inn_portrait.json");
    }

    public void done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
    }

    public void potionOfFalseLife() {
        // TODO Pay a bit of money to gain a temporary +2 HP.
        Current.player().potionOfFalseLife();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
            ui.onButtonPress("done", new Runnable() {
                @Override
                public void run() {
                    InnScene.this.done();
                }
            });
            ui.onButtonPress("tempHitPointCost", new Runnable() {
                @Override
                public void run() {
                    // Pay 200 gp to gain temporary 2 hp.
                    InnScene.this.potionOfFalseLife();
                }
            });
            ui.onButtonPress("sell", new Runnable() {
                @Override
                public void run() {
                    InnScene.this.sell();
                }
            });
            leave = ui.findActor("done");
            leave.getLabel().setText(Forge.getLocalizer().getMessage("lblLeave"));
            sell = ui.findActor("sell");
            sell.getLabel().setText(Forge.getLocalizer().getMessage("lblSell"));

            tempHitPoints = ui.findActor("tempHitPoints");
            tempHitPoints.setText(Forge.getLocalizer().getMessage("lblTempHitPoints"));

            leaveIcon = ui.findActor("leaveIcon");
            healIcon = ui.findActor("healIcon");
            sellIcon = ui.findActor("sellIcon");
    }

    @Override
    public void render() {
        super.render();

        int tempHealthCost = Current.player().falseLifeCost();
        boolean purchaseable = Current.player().getMaxLife() == Current.player().getLife() &&
                tempHealthCost <= Current.player().getGold();

        tempHitPointCost = ui.findActor("tempHitPointCost");
        tempHitPointCost.setDisabled(!purchaseable);
        tempHitPointCost.getLabel().setText("$" + tempHealthCost);
    }

    private void sell() {
        Forge.switchScene(SceneType.ShopScene.instance);
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            done();
        }
        return true;
    }

}

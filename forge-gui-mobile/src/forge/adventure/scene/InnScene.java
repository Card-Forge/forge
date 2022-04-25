package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Current;

/**
 * Scene for the Inn in towns
 */
public class InnScene extends UIScene {
    TextButton heal, sell, leave;
    Image healIcon, sellIcon, leaveIcon;

    public InnScene() {
        super(Forge.isLandscapeMode() ? "ui/inn.json" : "ui/inn_portrait.json");
    }

    public void done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
    }

    public void heal() {
        Current.player().fullHeal();
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
            ui.onButtonPress("heal", new Runnable() {
                @Override
                public void run() {
                    InnScene.this.heal();
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
            heal = ui.findActor("heal");
            heal.getLabel().setText(Forge.getLocalizer().getMessage("lblHeal"));

            leaveIcon = ui.findActor("leaveIcon");
            healIcon = ui.findActor("healIcon");
            sellIcon = ui.findActor("sellIcon");

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

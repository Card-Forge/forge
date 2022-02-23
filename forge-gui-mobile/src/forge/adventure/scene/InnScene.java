package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Current;

/**
 * Scene for the Inn in towns
 *
 */
public class InnScene extends UIScene  {

    public InnScene()
    {
        super("ui/inn.json");
    }

    public void done()
    {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
    }
    public void heal()
    {
        Current.player().heal();
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
        TextButton doneButton = ui.findActor("done");
    }

    private void sell() {
        Forge.switchScene(SceneType.ShopScene.instance);
    }

    @Override
    public boolean keyPressed(int keycode)
    {
        if (keycode == Input.Keys.ESCAPE)
        {
            done();
        }
        return true;
    }

}

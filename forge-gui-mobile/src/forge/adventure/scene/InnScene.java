package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import forge.Forge;
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
        ui.onButtonPress("done",()->done());
        ui.onButtonPress("heal",()->heal());
        ui.onButtonPress("sell",()->sell());
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

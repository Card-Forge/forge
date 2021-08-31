package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.Current;

public class InnScene extends UIScene  {

    private TextButton doneButton;

    public InnScene()
    {
        super("ui/inn.json");
    }

    public void done()
    {
        AdventureApplicationAdapter.instance.switchToLast();
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
        doneButton=ui.findActor("done");
    }


}

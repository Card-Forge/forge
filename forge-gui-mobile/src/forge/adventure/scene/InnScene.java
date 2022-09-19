package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Current;

/**
 * Scene for the Inn in towns
 */
public class InnScene extends UIScene {
    private static InnScene object;

    public static InnScene instance() {
        if(object==null)
            object=new InnScene();
        return object;
    }

    TextraButton tempHitPointCost, sell, leave;
    Image healIcon, sellIcon, leaveIcon;

    private InnScene() {

        super(Forge.isLandscapeMode() ? "ui/inn.json" : "ui/inn_portrait.json");
        tempHitPointCost = ui.findActor("tempHitPointCost");
        ui.onButtonPress("done", InnScene.this::done);
        ui.onButtonPress("tempHitPointCost", InnScene.this::potionOfFalseLife);
        ui.onButtonPress("sell", InnScene.this::sell);
        leave = ui.findActor("done");
        sell = ui.findActor("sell");


        leaveIcon = ui.findActor("leaveIcon");
        healIcon = ui.findActor("healIcon");
        sellIcon = ui.findActor("sellIcon");
    }



    public void done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
    }

    public void potionOfFalseLife() {
        Current.player().potionOfFalseLife();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }


    @Override
    public void render() {
        super.render();
    }

    @Override
    public void enter() {
        super.enter();
        int tempHealthCost = Current.player().falseLifeCost();
        if (tempHealthCost < 0) // if computed negative set 250 as minimum
            tempHealthCost = 250;
        boolean purchaseable = Current.player().getMaxLife() == Current.player().getLife() &&
                tempHealthCost <= Current.player().getGold();

        tempHitPointCost.setDisabled(!purchaseable);
        tempHitPointCost.setText(  tempHealthCost+"[+Gold]");
        clearActorObjects();
        addActorObject(tempHitPointCost);
        addActorObject(sell);
        addActorObject(leave);
    }

    private void sell() {
        Forge.switchScene(ShopScene.instance());
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            done();
        }
        if (keycode == Input.Keys.DPAD_RIGHT) {
            if (selectedActor == null)
                selectActor(tempHitPointCost, false);
            else
                selectNextActor(false);
        } else if ( keycode == Input.Keys.DPAD_LEFT) {
            if (selectedActor == null)
                selectActor(leave, false);
            else
                selectPreviousActor(false);

        } else if (keycode == Input.Keys.BUTTON_B) {
            performTouch(leave);

        } else if (keycode == Input.Keys.BUTTON_A) {
            performTouch(selectedActor);

        }
        return true;
    }

}

package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Controls;
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
    private TextraLabel playerGold,playerShards;

    private InnScene() {

        super(Forge.isLandscapeMode() ? "ui/inn.json" : "ui/inn_portrait.json");
        tempHitPointCost = ui.findActor("tempHitPointCost");
        ui.onButtonPress("done", InnScene.this::done);
        ui.onButtonPress("tempHitPointCost", InnScene.this::potionOfFalseLife);
        ui.onButtonPress("sell", InnScene.this::sell);
        leave = ui.findActor("done");
        sell = ui.findActor("sell");
        playerGold = Controls.newAccountingLabel(ui.findActor("playerGold"), false);
        playerShards = Controls.newAccountingLabel(ui.findActor("playerShards"),true);

        leaveIcon = ui.findActor("leaveIcon");
        healIcon = ui.findActor("healIcon");
        sellIcon = ui.findActor("sellIcon");
    }



    public void done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
    }

    public void potionOfFalseLife() {
        if (Current.player().potionOfFalseLife()){
            refreshStatus();
        }
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }


    @Override
    public void render() {
        super.render();
    }

    int tempHealthCost = 0;

    @Override
    public void enter() {
        super.enter();
        refreshStatus();
    }

    private void refreshStatus(){
        tempHealthCost = Current.player().falseLifeCost();
        boolean purchaseable = Current.player().getMaxLife() == Current.player().getLife() &&
                tempHealthCost <= Current.player().getGold();

        tempHitPointCost.setDisabled(!purchaseable);
        tempHitPointCost.setText(  tempHealthCost+"[+Gold]");
    }

    private void sell() {
        Forge.switchScene(ShopScene.instance());
    }

}

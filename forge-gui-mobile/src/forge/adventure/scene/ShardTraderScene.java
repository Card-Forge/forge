package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;

/**
 * Scene for the Shard Trader in towns
 */
public class ShardTraderScene extends UIScene {
    private static ShardTraderScene object;

    public static final String spriteAtlas = "maps/tileset/buildings.atlas";
    public static final String sprite = "ShardTrader";

    public static ShardTraderScene instance() {
        if(object==null)
            object=new ShardTraderScene();
        return object;
    }

    TextraButton buyShardsCost, sellShardsQuantity, leave;
    Image leaveIcon;

    private TextraLabel playerGold, playerShards;

    int shardsToSell = 5;

    int shardsToBuy = 5;

    int shardPrice = Math.round(100 * Current.player().getDifficulty().shardSellRatio);

    int shardCost = 100;

    private ShardTraderScene() {
        super(Forge.isLandscapeMode() ? "ui/shardtrader.json" : "ui/shardtrader_portrait.json");
        buyShardsCost = ui.findActor("btnBuyShardsCost");
        sellShardsQuantity = ui.findActor("btnSellShardsQuantity");
        ui.onButtonPress("done", ShardTraderScene.this::done);
        ui.onButtonPress("btnBuyShardsCost", ShardTraderScene.this::buyShards);
        ui.onButtonPress("btnSellShardsQuantity", ShardTraderScene.this::sellShards);
        leave = ui.findActor("done");
        playerGold = Controls.newAccountingLabel(ui.findActor("playerGold"), false);
        playerShards = Controls.newAccountingLabel(ui.findActor("playerShards"),true);
        leaveIcon = ui.findActor("leaveIcon");
    }

    public void done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
    }

    public void buyShards() {
        Current.player().addShards(shardsToBuy);
        Current.player().takeGold(shardCost);
        refreshStatus(-shardCost,shardsToBuy);
    }

    public void sellShards() {
        Current.player().takeShards(shardsToSell);
        Current.player().giveGold(shardPrice);
        refreshStatus(shardPrice,-shardsToSell);
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
        refreshStatus(0,0);
    }

    private void refreshStatus(int goldAdded, int shardsAdded) {
        int currentGold = Current.player().getGold();
        int currentShards = Current.player().getShards();

        shardPrice = Math.round(100 * Current.player().getDifficulty().shardSellRatio);

        sellShardsQuantity.setDisabled(currentShards < shardsToSell);
        buyShardsCost.setDisabled(currentGold < shardCost);
        buyShardsCost.setText( "Buy [+Shards] " + shardsToBuy + " for [+GoldCoin] " + shardCost);
        sellShardsQuantity.setText("Sell [+Shards] " + shardsToSell + " for [+GoldCoin] " +shardPrice);
    }
}

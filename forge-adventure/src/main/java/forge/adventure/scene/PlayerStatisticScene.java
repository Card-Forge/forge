package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.EnemyData;
import forge.adventure.data.WorldData;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class PlayerStatisticScene  extends UIScene {


    Image avatar;
    Label totalWins;
    Label totalLoss;
    Label lossWinRatio;
    private Table enemiesGroup;

    public PlayerStatisticScene() {
        super("ui/statistic.json");
    }


    @Override
    public void dispose() {
    }


    @Override
    public boolean keyPressed(int keycode)
    {
        if (keycode == Input.Keys.ESCAPE)
        {
            back();
        }
        return true;
    }
    public boolean back() {
        AdventureApplicationAdapter.instance.switchToLast();
        return true;
    }
    @Override
    public void enter() {
        super.enter();
        enemiesGroup.clear();
        if(avatar!=null)
        {
            avatar.setDrawable(new TextureRegionDrawable(Current.player().avatar()));
        }
        if(totalWins!=null)
        {
            totalWins.setText(Current.player().getStatistic().totalWins());
        }
        if(totalLoss!=null)
        {
            totalLoss.setText(Current.player().getStatistic().totalLoss());
        }
        if(lossWinRatio!=null)
        {
            lossWinRatio.setText(Float.toString(Current.player().getStatistic().winLossRatio()));
        }

        for(Map.Entry<String, Pair<Integer,Integer>> entry : Current.player().getStatistic().getWinLossRecord().entrySet())
        {
            EnemyData data=WorldData.getEnemy(entry.getKey());
            if(data==null)continue;
            Image enemyImage=new Image();
            enemyImage.setDrawable(new TextureRegionDrawable(new EnemySprite(data).getAvatar()));
            enemyImage.setSize(8,8);
            Label name = Controls.newLabel(data.name);

            enemiesGroup.add(enemyImage).align(Align.left).space(2);
            enemiesGroup.add((data.name)).align(Align.left).space(2);
            enemiesGroup.add((entry.getValue().getLeft().toString())).align(Align.right).space(2);
            enemiesGroup.add(("/")).align(Align.right).space(2);
            enemiesGroup.add((entry.getValue().getRight().toString())).align(Align.right).space(2);
            enemiesGroup.row().space(5);
        }

    }
    @Override
    public void resLoaded() {
        super.resLoaded();
        enemiesGroup = new Table(Controls.GetSkin());


        enemiesGroup.row();
        ui.onButtonPress("return", () -> back());
        avatar=ui.findActor("avatar");

        totalWins=ui.findActor("totalWins");
        totalLoss=ui.findActor("totalLoss");
        lossWinRatio=ui.findActor("lossWinRatio");

        ScrollPane scrollPane = ui.findActor("enemies");
        scrollPane.setActor(enemiesGroup);
    }

    @Override
    public void create() {

    }
}

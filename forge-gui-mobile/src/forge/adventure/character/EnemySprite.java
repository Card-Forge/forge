package forge.adventure.character;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.EnemyData;
import forge.adventure.data.RewardData;
import forge.adventure.util.Current;
import forge.adventure.util.MapDialog;
import forge.adventure.util.Reward;

/**
 * EnemySprite
 * Character sprite that represents an Enemy
 */
public class EnemySprite extends CharacterSprite {
    EnemyData data;
    public MapDialog dialog;

    public EnemySprite(EnemyData enemyData) {
        this(0,enemyData);
    }

    public EnemySprite(int id, EnemyData enemyData) {
        super(id,enemyData.sprite);
        data = enemyData;
    }

    @Override
    void updateBoundingRect() { //We want enemies to take the full tile.
        boundingRect = new Rectangle(getX(), getY(), getWidth(), getHeight());
    }

    public void moveTo(Actor other, float delta) {
        Vector2 diff = new Vector2(other.getX(), other.getY()).sub(pos());

        diff.setLength(data.speed*delta);
        moveBy(diff.x, diff.y);
    }

    public EnemyData getData() {
        return data;
    }


    public Array<Reward> getRewards() {
        Array<Reward> ret=new Array<Reward>();
        if(data.rewards == null)
            return ret;
        for(RewardData rdata:data.rewards) {
            ret.addAll(rdata.generate(false,(Current.latestDeck()!=null? Current.latestDeck().getMain().toFlatList():null)));
        }
        return ret;
    }

}


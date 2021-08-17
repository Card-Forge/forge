package forge.adventure.character;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import forge.adventure.data.EnemyData;

public class MobSprite extends CharacterSprite {
    EnemyData data;

    public MobSprite(EnemyData enemyData) {
        super(enemyData.sprite);

        data = enemyData;
    }


    public void moveTo(Actor other) {
        Vector2 diff = new Vector2(other.getX(), other.getY()).sub(pos());

        diff.setLength(data.speed);
        moveBy(diff.x, diff.y);
    }

    public EnemyData getData() {
        return data;
    }


}


package forge.adventure.character;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.concurrent.Callable;

public class OnCollide extends MapActor {

    Callable onCollide;

    public OnCollide(Callable func) {
        onCollide = func;
    }

    @Override
    public boolean collideWith(Rectangle other) {

        boolean isColliding = super.collideWith(other);
        if (isColliding) {
            try {
                onCollide.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean collideWith(MapActor other) {
        boolean isColliding = super.collideWith(other);
        if (isColliding) {
            try {
                onCollide.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean collideWith(Actor other) {

        boolean isColliding = super.collideWith(other);
        if (isColliding) {
            try {
                onCollide.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}

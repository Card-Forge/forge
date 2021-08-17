package forge.adventure.character;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class MapActor extends Actor {
    Rectangle boundingRect;

    @Override
    protected void positionChanged() {

        updateBoundingRect();
        super.positionChanged();
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        updateBoundingRect();
    }

    void updateBoundingRect() {
        boundingRect = new Rectangle(getX(), getY(), getWidth(), getHeight());
    }

    public Rectangle boundingRect() {
        return boundingRect;
    }

    public boolean collideWith(Rectangle other) {
        return boundingRect().overlaps(other);
    }

    public boolean collideWith(MapActor other) {
        return boundingRect().overlaps(other.boundingRect());
    }

    public boolean collideWith(Actor other) {
        return boundingRect.x < other.getX() + other.getWidth() && boundingRect.x + boundingRect.width > other.getX() && boundingRect.y < other.getY() + other.getHeight() && boundingRect.y + boundingRect.height > other.getY();
    }
}

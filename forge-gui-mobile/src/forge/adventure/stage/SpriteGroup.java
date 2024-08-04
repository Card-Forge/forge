package forge.adventure.stage;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.SnapshotArray;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Sprite group to order actors based on the Y position on the map, the render sprites further up first.
 */
public class SpriteGroup extends Group {

    /**
     * Draws all children. {@link #applyTransform(Batch, Matrix4)} should be called before and {@link #resetTransform(Batch)}
     * after this method if {@link #setTransform(boolean) transform} is true. If {@link #setTransform(boolean) transform} is false
     * these methods don't need to be called, children positions are temporarily offset by the group position when drawn. This
     * method avoids drawing children completely outside the {@link #setCullingArea(Rectangle) culling area}, if set.
     */
    @Override
    protected void drawChildren(Batch batch, float parentAlpha) {

        Actor[] actors = getChildren().toArray();
        Arrays.sort(actors, Comparator.comparingInt(o -> (int) -o.getY()));

        for(int i=0;i<actors.length;i++)
        {
            if(i!=actors[i].getZIndex())
                actors[i].setZIndex(i);
        }
        super.drawChildren(batch, parentAlpha);

    }

    @Override
    public void addActor(Actor actor) {

        for (Actor child : getChildren()) {
            if (child.getY() < actor.getY()) {
                super.addActorBefore(child, actor);
                return;
            }
        }
        super.addActor(actor);
    }

    public void UpdateActorZ(Actor actor) {
        SnapshotArray<Actor> children = getChildren();
        for (int i = 0; i < children.size; i++) {
            if (children.get(i).getY() < actor.getY()) {
                actor.setZIndex(i);
                return;
            }
        }
        actor.setZIndex(children.size);
    }
}


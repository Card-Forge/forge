package forge.adventure.util.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector2;

public class NavigationEdge implements Connection<NavigationVertex> {
    NavigationVertex fromVertex;
    NavigationVertex toVertex;
    float cost;

    public NavigationEdge(NavigationVertex from, NavigationVertex to) {
        this.fromVertex = from;
        this.toVertex = to;
        cost = Vector2.dst(fromVertex.pos.x, fromVertex.pos.y, toVertex.pos.x, toVertex.pos.y);
    }

    @Override
    public float getCost() {
        return cost;
    }

    @Override
    public NavigationVertex getFromNode() {
        return fromVertex;
    }

    @Override
    public NavigationVertex getToNode() {
        return toVertex;
    }
}

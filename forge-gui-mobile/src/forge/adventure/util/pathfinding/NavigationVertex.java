package forge.adventure.util.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class NavigationVertex {
    public Vector2 pos = Vector2.Zero;
    public ObjectMap<NavigationVertex, NavigationEdge> incomingEdges = new ObjectMap<>();
    public ObjectMap<NavigationVertex, NavigationEdge> outgoingEdges = new ObjectMap<>();
    int index = -1;

    public NavigationVertex(Vector2 position) {
        pos = position;
    }

    public NavigationVertex(float x, float y) {
        pos = new Vector2(x, y);
    }

    public boolean hasEdgeTo(NavigationVertex otherNode) {
        return incomingEdges.containsKey(otherNode);
    }

    public Array<Connection<NavigationVertex>> getAllConnections() {

        Array<Connection<NavigationVertex>> ret = new Array<>();

        for (NavigationEdge e : incomingEdges.values()) {
            ret.add(e);
        }
        for (NavigationEdge e : outgoingEdges.values()) {
            ret.add(e);
        }
        return ret;
    }

    public void removeEdges(NavigationVertex node) {
        outgoingEdges.remove(node);
        incomingEdges.remove(node);
    }
}
package forge.adventure.util.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NavigationGraph implements IndexedGraph<NavigationVertex> {
    private int lastNodeIndex = 0;
    Map<Integer, NavigationVertex> nodes = new HashMap<>();

    EuclidianHeuristic navigationHeuristic = new EuclidianHeuristic();

    public NavigationVertex addVertex(NavigationVertex node){
        node.index = lastNodeIndex;
        lastNodeIndex++;
        nodes.put(node.index,node);
        return node;
    }

    public NavigationVertex addVertex(Vector2 position) {
        return addVertex(new NavigationVertex(position));
    }

    public NavigationVertex addVertex(float x, float y) {
        return addVertex(new NavigationVertex(x,y));
    }

    public void removeVertex(NavigationVertex node) {
        for (NavigationVertex v : node.incomingEdges.keys()) {
            v.removeEdges(node);
        }
        nodes.remove(node.index >=0? node.index: lookupIndex(node));
    }

    public void removeVertex(Vector2 position) {
        removeVertex(getVertexByPosition(position));
    }

    public void removeVertex(float x, float y) {
        removeVertex(new Vector2(x,y));
    }

    public void removeVertices(Collection<NavigationVertex> vertices) {
        for (NavigationVertex v : vertices) {
            removeVertex(v);
        }
    }

    public void removeVertexIf(Predicate<NavigationVertex> predicate) {
        removeVertices(nodes.values().stream().filter(predicate).collect(Collectors.toList()));
    }

    public int lookupIndex(NavigationVertex item) {
        return lookupIndex(item.pos);
    }

    public int lookupIndex(Vector2 pos) {
        for (int i : nodes.keySet())
            if (nodes.get(i).pos.equals(pos)) return i;

        return -1;
    }

    public void addEdge(NavigationVertex fromNode, NavigationVertex toNode) {
        if (fromNode.index < 0) {
            fromNode = getVertexByPosition(fromNode.pos);
        }
        if (toNode.index < 0) {
            toNode = getVertexByPosition(toNode.pos);
        }

        if (edgeExists(fromNode, toNode)) {
            System.out.println(fromNode.pos + " is already connected to " + toNode.pos);
            return;
        }

        if (!(fromNode.index < 0) || toNode.index < 0) {
            NavigationEdge fromAToB = new NavigationEdge(fromNode, toNode);
            NavigationEdge fromBToA = new NavigationEdge(toNode, fromNode);
            fromNode.outgoingEdges.put(toNode, fromAToB);
            fromNode.incomingEdges.put(toNode, fromBToA);
            toNode.outgoingEdges.put(fromNode, fromBToA);
            toNode.incomingEdges.put(fromNode, fromAToB);
        }
    }

    public void addEdge(Vector2 fromNode, NavigationVertex toNode) {
        addEdge(new NavigationVertex(fromNode), toNode);
    }

    public void addEdge(NavigationVertex fromNode, Vector2 toNode) {
        addEdge(fromNode, new NavigationVertex(toNode));
    }

    public void addEdgeUnchecked(NavigationVertex fromNode, NavigationVertex toNode) {
        //Assumes that nodes are in graph, are not connected already, and have correct index

        NavigationEdge fromAToB = new NavigationEdge(fromNode, toNode);
        NavigationEdge fromBToA = new NavigationEdge(toNode, fromNode);

        fromNode.outgoingEdges.put(toNode, fromAToB);
        fromNode.incomingEdges.put(toNode, fromBToA);
        toNode.outgoingEdges.put(fromNode, fromBToA);
        toNode.incomingEdges.put(fromNode, fromAToB);
    }

    public int getIndex(NavigationVertex node) {
        return node.index;
    }

    public int getNodeCount() {
        return lastNodeIndex;
    }

    @Override
    public Array<Connection<NavigationVertex>> getConnections(NavigationVertex fromNode) {
        return fromNode.getAllConnections();
    }

    public boolean edgeExists(NavigationVertex fromNode, NavigationVertex toNode) {
        if (fromNode.index < 0) {
            fromNode = getVertexByPosition(fromNode.pos);
        }
        if (toNode.index < 0) {
            toNode = getVertexByPosition(toNode.pos);
        }
        return fromNode.outgoingEdges.containsKey(toNode);
    }

    public Collection<NavigationVertex> getNodes() {
        return nodes.values();
    }

    public ProgressableGraphPath<NavigationVertex> findPath(Vector2 origin, Vector2 destination) {
        ProgressableGraphPath<NavigationVertex> navPath = new ProgressableGraphPath<>();

        NavigationVertex originVertex = getVertexByPosition(origin);
        NavigationVertex destinationVertex = getVertexByPosition(destination);

        if (originVertex.index > -1 && destinationVertex.index > -1) {

            new IndexedAStarPathFinder<>(this).searchNodePath(originVertex, destinationVertex, navigationHeuristic, navPath);
        }
        return navPath;
    }

    public NavigationVertex getVertexByPosition(Vector2 position) {
        return nodes.get(lookupIndex(position));
    }

    public boolean containsNode(Vector2 nodePosition) {
        return nodes.containsKey(lookupIndex(nodePosition));
    }
}


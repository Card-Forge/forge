package forge.adventure.util.pathfinding;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.utils.Array;

import java.util.Iterator;

public class ProgressableGraphPath<N> implements GraphPath<N> {
    public final Array<N> nodes;

    /** Creates a {@code DefaultGraphPath} with no nodes. */
    public ProgressableGraphPath () {
        this(new Array<N>());
    }

    /** Creates a {@code DefaultGraphPath} with the given capacity and no nodes. */
    public ProgressableGraphPath (int capacity) {
        this(new Array<N>(capacity));
    }

    /** Creates a {@code DefaultGraphPath} with the given nodes. */
    public ProgressableGraphPath (Array<N> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void clear () {
        nodes.clear();
    }

    @Override
    public int getCount () {
        return nodes.size;
    }

    @Override
    public void add (N node) {
        nodes.add(node);
    }

    @Override
    public N get (int index) {
        return nodes.get(index);
    }

    @Override
    public void reverse () {
        nodes.reverse();
    }

    @Override
    public Iterator<N> iterator () {
        return nodes.iterator();
    }

    public void remove (int index) { nodes.removeIndex(index);}
}

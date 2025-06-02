package forge.adventure.util.pathfinding;

import com.badlogic.gdx.ai.pfa.Heuristic;

public class EuclidianHeuristic  implements Heuristic<NavigationVertex> {

    @Override
    public float estimate(NavigationVertex start, NavigationVertex end) {
        return start.pos.dst(end.pos);
    }
}


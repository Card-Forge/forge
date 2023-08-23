package forge.adventure.util.pathfinding;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import forge.adventure.stage.MapStage;

import java.util.ArrayList;
import java.util.Comparator;

public class NavigationMap {
    float spriteSize = 16f;
    boolean rayCollided = false;

    public NavigationGraph navGraph = new NavigationGraph();

    Array<Rectangle> navBounds = new Array<>();
    float half = (spriteSize / 2);

    public NavigationMap(float spriteSize) {
        this.spriteSize = spriteSize;
        this.half = spriteSize / 2;
    }

    RayCastCallback callback = new RayCastCallback() {
        @Override
        public float reportRayFixture(Fixture fixture, Vector2 vector2, Vector2 vector21, float v) {
            if (v < 1.0)
                rayCollided = true;
            return 0;
        }
    };

    public void initializeGeometryGraph() {
        navGraph = new NavigationGraph();

        for (int i = 0; i < MapStage.getInstance().collisionRect.size; i++) {
            Rectangle r1 = MapStage.getInstance().collisionRect.get(i);

            if (r1.width < 3 && r1.height < 3)
                continue;
            int offsetX = -8;
            int offsetY = 0;

            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.StaticBody;
            bodyDef.position.set(r1.x + r1.getWidth() / 2 + offsetX, r1.y + r1.getHeight() / 2 + offsetY);
            Body body = MapStage.getInstance().gdxWorld.createBody(bodyDef);

            PolygonShape polygonShape = new PolygonShape();
            polygonShape.setAsBox(((r1.getWidth() + spriteSize) / 2), ((r1.getHeight() + spriteSize) / 2));
            FixtureDef fixture = new FixtureDef();
            fixture.shape = polygonShape;
            fixture.density = 1;

            body.createFixture(fixture);
            polygonShape.dispose();
        }

        float width = Float.parseFloat(MapStage.getInstance().tiledMap.getProperties().get("width").toString());
        float height = Float.parseFloat(MapStage.getInstance().tiledMap.getProperties().get("height").toString());
        float tileHeight = Float.parseFloat(MapStage.getInstance().tiledMap.getProperties().get("tileheight").toString());
        float tileWidth = Float.parseFloat(MapStage.getInstance().tiledMap.getProperties().get("tilewidth").toString());

        NavigationVertex[][] points = new NavigationVertex[(int)width][(int)height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                points[i][j] = navGraph.addVertex(i* tileWidth + (tileWidth/ 2), j*tileHeight + (tileHeight/ 2));
                if (i > 0) {
                    navGraph.addEdgeUnchecked(points[i][j],points[i-1][j]);
                }

                if (j > 0) {
                    navGraph.addEdgeUnchecked(points[i][j],points[i][j-1]);
                }

                if (i > 0 && j > 0) {
                    navGraph.addEdgeUnchecked(points[i][j],points[i-1][j-1]);
                }

                if (i > 0 && j + 1 < height) {
                    navGraph.addEdgeUnchecked(points[i][j],points[i-1][j+1]);
                }
                //remaining connections will be added by subsequent nodes
            }
        }

        Array<Fixture> fixtures = new Array<>();
        if (MapStage.getInstance().gdxWorld != null) {
            MapStage.getInstance().gdxWorld.getFixtures(fixtures);
            for (Fixture fix : fixtures) {
                navGraph.removeVertexIf(vertex -> fix.testPoint(vertex.pos));
            }
        }

        navGraph.removeVertexIf(v -> navGraph.getConnections(v).isEmpty());

        //Add additional vertices for map waypoints
        for (Vector2 waypointVector : MapStage.getInstance().waypoints.values()) {
            NavigationVertex waypointVertex = navGraph.addVertex(waypointVector);

            ArrayList<NavigationVertex> vertices = new ArrayList<>(navGraph.nodes.values());
            vertices.sort(Comparator.comparingInt(o -> Math.round((o.pos.x - waypointVector.x) * (o.pos.x - waypointVector.x) + (o.pos.y - waypointVector.y) * (o.pos.y - waypointVector.y))));

            for (int i = 0, j=0; i < vertices.size() && j < 4; i++) {
                if (waypointVector.epsilonEquals(vertices.get(i).pos))
                    continue; //rayCast() crashes if params are equal
                rayCollided = false;
                MapStage.getInstance().gdxWorld.rayCast(callback, waypointVector, vertices.get(i).pos);
                if (!rayCollided) {
                    navGraph.addEdgeUnchecked(waypointVertex, vertices.get(i));
                    j++;
                }
            }
        }
    }


    public ProgressableGraphPath<NavigationVertex> findShortestPath(Float spriteSize, Vector2 origin, Vector2 destination) {
        Array<Fixture> fixtures = new Array<>();
        MapStage.getInstance().gdxWorld.getFixtures(fixtures);

        boolean originPrecalculated = navGraph.containsNode(origin);
        boolean destinationPrecalculated = navGraph.containsNode(destination);

        try {
            if (!originPrecalculated)
                navGraph.addVertex(origin);

            if (!destinationPrecalculated)
                navGraph.addVertex(destination);

            ArrayList<NavigationVertex> vertices = new ArrayList<>();

            if (!(originPrecalculated && destinationPrecalculated)) {
                vertices.addAll(navGraph.nodes.values());
                vertices.sort(Comparator.comparingInt(o -> Math.round((o.pos.x - origin.x) * (o.pos.x - origin.x) + (o.pos.y - origin.y) * (o.pos.y - origin.y))));
            }

            if (!originPrecalculated) {
                for (int i = 0, j=0; i < vertices.size() && j < 10; i++) {
                    if (origin.epsilonEquals(vertices.get(i).pos))
                        continue; //rayCast() crashes if params are equal
                    rayCollided = false;
                    MapStage.getInstance().gdxWorld.rayCast(callback, origin, vertices.get(i).pos);
                    if (!rayCollided) {
                        navGraph.addEdge(origin, vertices.get(i));
                        j++;
                    }
                }
            }

            if (!destinationPrecalculated) {
                for (int i = 0, j=0; i < vertices.size() && j < 10; i++) {
                    if (destination.epsilonEquals(vertices.get(i).pos))
                        continue; //shouldn't happen, but would crash during rayCast if it did
                    rayCollided = false;
                    MapStage.getInstance().gdxWorld.rayCast(callback, vertices.get(i).pos, destination);
                    if (!rayCollided) {
                        navGraph.addEdge(destination, vertices.get(i));
                        j++;
                    }
                }
            }


            ProgressableGraphPath<NavigationVertex> shortestPath = navGraph.findPath(origin, destination);

            if (false) { //todo - re-evaluate. 8-way node links may be smooth enough to skip the extra raycast overhead
                //Trim path by cutting any unnecessary nodes
                for (int i = 0; i < shortestPath.getCount(); i++) {
                    for (int j = shortestPath.getCount() - 1; j > i + 1; j--) {
                        rayCollided = false;
                        MapStage.getInstance().gdxWorld.rayCast(callback, shortestPath.get(i).pos, shortestPath.get(j).pos);
                        if (!rayCollided) {
                            shortestPath.remove(j - 1);
                            i = 0;
                            j = shortestPath.getCount();
                        }
                    }
                }
            }

            if (!originPrecalculated)
                navGraph.removeVertex(origin);
            if (!destinationPrecalculated)
                navGraph.removeVertex(destination);
            return shortestPath;
        }
        catch(Exception e){
            if (!originPrecalculated && navGraph.lookupIndex(origin) > -1)
                navGraph.removeVertex(origin);
            if (!destinationPrecalculated && navGraph.lookupIndex(destination) > -1)
                navGraph.removeVertex(destination);
            throw(e);
        }
    }
}


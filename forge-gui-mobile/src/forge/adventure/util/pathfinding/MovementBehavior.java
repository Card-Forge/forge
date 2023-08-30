package forge.adventure.util.pathfinding;

import com.badlogic.gdx.math.Vector2;
import forge.adventure.stage.MapStage;
import forge.util.Aggregates;

public class MovementBehavior {
    public float duration = 0.0f;
    float x = 0.0f;
    float y = 0.0f;

    public String destination = "";

    public float getX(){
        return x;
    }
    public float getY(){
        return y;
    }
    public float getDuration(){
        return duration;
    }
    public Vector2 currentTargetVector;
    public Vector2 getNextTargetVector(int objectID, Vector2 currentPosition){
        if (currentTargetVector != null) {
            return currentTargetVector;
        }
        if (destination.isEmpty()) {
            currentTargetVector = new Vector2(currentPosition);
        } else {
            if (destination.startsWith("r")) {
                String[] randomWaypoints = destination.replaceAll("r", "").split("-");
                if (randomWaypoints.length > 0) {
                    int selectedWaypoint = Integer.parseInt(Aggregates.random(randomWaypoints));
                    if (MapStage.getInstance().waypoints.containsKey(selectedWaypoint)) {
                        currentTargetVector = new Vector2(MapStage.getInstance().waypoints.get(selectedWaypoint));
                    }
                }
                else {
                    currentTargetVector = new Vector2(currentPosition);
                }
            } else if (destination.startsWith("w")) {
                currentTargetVector = new Vector2(currentPosition);
                duration = Float.parseFloat(destination.replaceAll("w", ""));
            } else if (MapStage.getInstance().waypoints.containsKey(Integer.parseInt(destination))) {
                currentTargetVector = new Vector2(MapStage.getInstance().waypoints.get(Integer.parseInt(destination)));
            }
            else {
                System.err.println("Navigation error for object ID" + objectID + ", waypoint could not be parsed or does not exist: " + destination);
                destination = "";
            }
        }

        return currentTargetVector == null? currentPosition: currentTargetVector;
    }

    public void setX(float newVal){
        x = newVal;
    }
    public void setY(float newVal){
        y = newVal;
    }
}
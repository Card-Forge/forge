package forge.util;

import com.badlogic.gdx.math.Vector2;

public class PhysicsObject {
    private final Vector2 position, velocity, acceleration;
    private boolean allowVelocitySignChange;

    public PhysicsObject(Vector2 position0, Vector2 velocity0) {
        this(position0, velocity0, new Vector2(0, 0), false);
    }
    public PhysicsObject(Vector2 position0, Vector2 velocity0, Vector2 acceleration0, boolean allowVelocitySignChange0) {
        position = position0;
        velocity = velocity0;
        acceleration = acceleration0;
        allowVelocitySignChange = allowVelocitySignChange0;
    }

    //setup acceleration so sign of acceleration forces velocity to move towards 0
    //decelX and decelY should be positive values
    public void setDecel(float decelX, float decelY) {
        if (velocity.x > 0) {
            decelX = -decelX;
        }
        if (velocity.y > 0) {
            decelY = -decelY;
        }
        acceleration.set(decelX, decelY);
        allowVelocitySignChange = false; //assume we're not allowing sign of velocity to change
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public Vector2 getAcceleration() {
        return acceleration;
    }

    public void stop() {
        velocity.set(0, 0);
        acceleration.set(0, 0);
    }

    public boolean isMoving() {
        return velocity.x != 0 || velocity.y != 0 || acceleration.x != 0 || acceleration.y != 0;
    }

    public void advance(float dt) {
        float signum;
        if (acceleration.x == 0) {
            position.x += velocity.x * dt;
        }
        else {
            signum = Math.signum(velocity.x);
            position.x += (velocity.x + 0.5f * acceleration.x * dt) * dt;
            velocity.x += acceleration.x * dt;
            if (!allowVelocitySignChange && Math.signum(velocity.x) != signum) {
                velocity.x = 0; //stop in this direction if sign changed
                acceleration.x = 0;
            }
        }
        if (acceleration.y == 0) {
            position.y += velocity.y * dt;
        }
        else {
            signum = Math.signum(velocity.y);
            position.y += (velocity.y + 0.5f * acceleration.y * dt) * dt;
            velocity.y += acceleration.y * dt;
            if (!allowVelocitySignChange && Math.signum(velocity.y) != signum) {
                velocity.y = 0; //stop in this direction if sign changed
                acceleration.y = 0;
            }
        }
    }
}

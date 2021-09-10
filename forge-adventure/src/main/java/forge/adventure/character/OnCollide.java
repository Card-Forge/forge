package forge.adventure.character;

/**
 * Designed to add anonymous class for a single action on collision
 */
public class OnCollide extends MapActor {

    Runnable onCollide;
    public OnCollide(Runnable func) {
        onCollide = func;
    }

    @Override
    protected void onPlayerCollide() {
        try {
            onCollide.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

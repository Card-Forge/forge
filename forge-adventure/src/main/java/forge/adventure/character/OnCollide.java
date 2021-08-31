package forge.adventure.character;

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

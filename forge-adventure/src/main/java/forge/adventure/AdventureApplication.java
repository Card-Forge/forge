package forge.adventure;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;

public class AdventureApplication extends Lwjgl3Application {
    public AdventureApplication(AdventureApplicationConfiguration config) {
        super(new AdventureApplicationAdapter(config.Plane), config);

    }
}

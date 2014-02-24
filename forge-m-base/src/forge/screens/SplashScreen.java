package forge.screens;

import forge.screens.FScreen;
import forge.toolbox.FProgressBar;

public abstract class SplashScreen extends FScreen {
    private static SplashScreen splashScreen;

    public static FProgressBar getProgressBar(String desc) {
        return splashScreen.progressBar;
    }

    private FProgressBar progressBar = new FProgressBar();

    public SplashScreen() {
        super(false, null, false);
        if (splashScreen != null) {
            throw new RuntimeException("Cannot initialize SplashScreen more than once");
        }
        splashScreen = this;
        add(progressBar);
    }

    @Override
    protected final void doLayout(float startY, float width, float height) {
    }
}

package forge.adventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.graphics.GL20;
import forge.Forge;
import forge.adventure.util.Config;
import forge.assets.FSkin;
import forge.gui.GuiBase;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.util.BuildInfo;
import io.sentry.Sentry;
import io.sentry.SentryClient;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Wrapper to start forge first (splash screen and resources loading)
 *
 */
 class StartAdventure extends AdventureApplicationAdapter {


    public StartAdventure(  ) {

        super(new Lwjgl3Clipboard(), new DesktopAdapter(""), Files.exists(Paths.get("./res"))?"./":"../forge-gui/", true, false, 0, true, 0, "", "");
        Forge.isTabletDevice = true;
        Forge.isPortraitMode = false;
        Forge.hdbuttons = true;
        Forge.hdstart = true;

        isPortraitMode = true;
        totalDeviceRAM = 0;
        GuiBase.setDeviceInfo("", "", 0, 0);

    }

    @Override
    public void render() {
        if (splashScreen != null) {
            Gdx.gl.glClearColor(1, 0, 1, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.
            getGraphics().begin(getCurrentWidth(), getCurrentHeight());
            splashScreen.setSize(getCurrentWidth(), getCurrentHeight());
            splashScreen.screenPos.setSize(getCurrentWidth(), getCurrentHeight());
            if (splashScreen.getRotate180()) {
                getGraphics().startRotateTransform(getCurrentWidth() / 2f, getCurrentHeight() / 2f, 180);
            }
            splashScreen.draw(getGraphics());
            if (splashScreen.getRotate180()) {
                getGraphics().endTransform();
            }

            getGraphics().end();
        } else {
            super.render();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (splashScreen != null)
            splashScreen.setSize(width, height);
    }

    @Override
    public void create() {
        //install our error handler
        FSkin.loadLight("default", splashScreen,Config.instance().getFile("skin"));


        //load model on background thread (using progress bar to report progress)
        super.create();

    }
    @Override
    protected void afterDbLoaded()
    {
        FSkin.loadLight("default", splashScreen,Config.instance().getFile("skin"));
        FSkin.loadFull(splashScreen);
        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS); //start background music
        Gdx.input.setCatchKey(Input.Keys.MENU, true);
        //openHomeScreen(-1, null); //default for startup
        splashScreen = null;
        afterDBloaded = true;


        //adjust height modifier
        adjustHeightModifier(getScreenWidth(), getScreenHeight());

        resLoaded();
    }

}
/**
 * Main entry point
 */
public class Main {

    public static void main(String[] args) {

        Sentry.init();
        SentryClient sentryClient = Sentry.getStoredClient();
        sentryClient.setRelease(BuildInfo.getVersionString());
        sentryClient.setEnvironment(System.getProperty("os.name"));
        sentryClient.addTag("Java Version", System.getProperty("java.version"));

        // HACK - temporary solution to "Comparison method violates it's general contract!" crash
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        //Turn off the Java 2D system's use of Direct3D to improve rendering speed (particularly when Full Screen)
        System.setProperty("sun.java2d.d3d", "false");


        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setResizable(false);
        StartAdventure start=new StartAdventure();

        if (Config.instance().getSettingData().fullScreen)
        {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            config.setWindowedMode(Config.instance().getSettingData().width, Config.instance().getSettingData().height);
        }

        config.setWindowIcon(Config.instance().getFilePath("forge-adventure.png"));

        new Lwjgl3Application(start, config);

    }
}

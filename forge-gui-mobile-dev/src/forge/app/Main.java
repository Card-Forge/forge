package forge.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import forge.interfaces.IDeviceAdapter;
import forge.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jupnp.UpnpServiceConfiguration;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.SplashScreen;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.function.BiFunction;

public class Main {
    private static final String versionString = BuildInfo.getVersionString();
    public static void main(String[] args) {
        if (!OperatingSystem.isWindows()) {
            /* Prevents crash on non Windows OS before creating the LWJGL3 window.
               It seems it defeats the purpose of having a splash image since
               this is an indicator if the LWJGL3 has booted up succesfully. */
            closeSplash();
        }
        new GameLauncher(versionString, args);
    }
    public static void closeSplash() {
        try {
            Optional.ofNullable(SplashScreen.getSplashScreen()).ifPresent(SplashScreen::close);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static class DesktopAdapter implements IDeviceAdapter {
        private final String switchOrientationFile;

        DesktopAdapter(String switchOrientationFile0) {
            switchOrientationFile = switchOrientationFile0;
        }

        //just assume desktop always connected to wifi
        @Override
        public boolean isConnectedToInternet() {
            return true;
        }

        @Override
        public boolean isConnectedToWifi() {
            return true;
        }

        @Override
        public String getDownloadsDir() {
            return System.getProperty("user.home") + "/Downloads/";
        }

        @Override
        public String getVersionString() {
            return versionString;
        }

        @Override
        public String getLatestChanges(String commitsAtom, Date buildDateOriginal, Date max) {
            return RSSReader.getCommitLog(commitsAtom, buildDateOriginal, max);
        }

        @Override
        public String getReleaseTag(String releaseAtom) {
            return RSSReader.getLatestReleaseTag(releaseAtom);
        }

        @Override
        public boolean openFile(String filename) {
            try {
                File installer = new File(filename);
                if (installer.exists()) {
                    if (filename.endsWith(".jar")) {
                        installer.setExecutable(true, false);
                        Desktop.getDesktop().open(installer);
                    } else {
                        Desktop.getDesktop().open(installer.getParentFile());
                    }
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public void restart() {
            if (RestartUtil.prepareForRestart()) {
                // Calls dispose()
                Gdx.app.exit();
                // Commented out System.exit(0) since the thread are set to daemon so the game will gracefully exit
                // while disposing assets. Non daemon threads and System.exit(0) will cause the exit code -1073740791 (0xC0000409)
                //System.exit(0);
            }
        }

        @Override
        public void exit() {
            // Calls dispose()
            Gdx.app.exit();
            // Commented out System.exit(0) since the thread are set to daemon so the game will gracefully exit
            // while disposing assets. Non daemon threads and System.exit(0) will cause the exit code -1073740791 (0xC0000409)
            //System.exit(0);
        }

        @Override
        public void closeSplashScreen() {
            closeSplash();
        }

        @Override
        public boolean isTablet() {
            return true; //treat desktop the same as a tablet
        }

        @Override
        public void setLandscapeMode(boolean landscapeMode) {
            //create file to indicate that landscape mode should be used
            if (landscapeMode) {
                FileUtil.writeFile(switchOrientationFile, "1");
            } else {
                FileUtil.deleteFile(switchOrientationFile);
            }
        }

        @Override
        public void preventSystemSleep(boolean preventSleep) {
            OperatingSystem.preventSystemSleep(preventSleep);
        }

        @Override
        public void convertToJPEG(InputStream input, OutputStream output) throws IOException {
            BufferedImage image = ImageIO.read(input);
            ImageIO.write(image, "jpg", output);
        }

        @Override
        public void convertToPNG(InputStream input, OutputStream output) throws IOException {
            BufferedImage image = ImageIO.read(input);
            ImageIO.write(image, "png", output);
        }

        @Override
        public void decodeWebp(File file, BiFunction<Integer, Integer, ByteBuffer> rgba) throws IOException {
            BufferedImage image = ImageIO.read(file);
            if (image == null)
                throw new IOException("Not a WebP image");
            int w = image.getWidth(), h = image.getHeight();
            byte[] pixels = new byte[w * h * 4];
            if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                // getRGB converts each pixel through the colour model, which costs more than this copy
                byte[] bgr = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                for (int i = 0, j = 0; j < bgr.length; i += 4, j += 3) {
                    pixels[i] = bgr[j + 2];
                    pixels[i + 1] = bgr[j + 1];
                    pixels[i + 2] = bgr[j];
                    pixels[i + 3] = (byte) 0xFF;
                }
            } else {
                int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
                for (int i = 0; i < argb.length; i++)
                    argb[i] = argb[i] << 8 | argb[i] >>> 24;
                ByteBuffer.wrap(pixels).asIntBuffer().put(argb);
            }
            rgba.apply(w, h).put(pixels);
        }

        @Override
        public Pair<Integer, Integer> getRealScreenSize(boolean real) {
            return Pair.of(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }

        @Override
        public ArrayList<String> getGamepads() {
            return new ArrayList<>();
        }

        @Override
        public UpnpServiceConfiguration getUpnpPlatformService() {
            // shouldn't be reached
            return null;
        }

        @Override
        public boolean needFileAccess() {
            return false;
        }

        @Override
        public void requestFileAcces() {

        }

        @Override
        public void setMouseConfined(boolean confined) {
            if (!(Gdx.graphics instanceof Lwjgl3Graphics)) {
                return;
            }
            long handle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
            GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR,
                    confined ? GLFW.GLFW_CURSOR_CAPTURED : GLFW.GLFW_CURSOR_NORMAL);
        }
    }
}

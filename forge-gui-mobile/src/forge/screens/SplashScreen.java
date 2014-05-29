package forge.screens;

import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FContainer;
import forge.toolbox.FProgressBar;
import forge.util.FileUtil;
import forge.util.TextUtil;

public class SplashScreen extends FContainer {
    private TextureRegion background;
    private final FProgressBar progressBar;
    private FSkinFont disclaimerFont;

    public SplashScreen() {
        progressBar = new FProgressBar();
        progressBar.setDescription("Welcome to Forge");

        checkForAssets();

        final ForgePreferences prefs = new ForgePreferences();
        FSkin.loadLight(prefs.getPref(FPref.UI_SKIN), this);
    }

    public FProgressBar getProgressBar() {
        return progressBar;
    }

    public void setBackground(TextureRegion background0) {
        background = background0;
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (background == null) { return; }

        g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, getWidth(), getHeight());
 
        float x, y, w, h;
        float backgroundRatio = background.getRegionWidth() / background.getRegionHeight();
        float screenRatio = getWidth() / getHeight();
        if (backgroundRatio > screenRatio) {
            x = 0;
            w = getWidth();
            h = getWidth() * backgroundRatio;
            y = (getHeight() - h) / 2;
        }
        else {
            y = 0;
            h = getHeight();
            w = getHeight() / backgroundRatio;
            x = (getWidth() - w) / 2;
        }
        g.drawImage(background, x, y, w, h);

        y += h * 295f / 450f;
        if (disclaimerFont == null) {
            disclaimerFont = FSkinFont.get(9);
        }
        float disclaimerHeight = 30f / 450f * h;
        String disclaimer = "Forge is not affiliated in any way with Wizards of the Coast.\n"
                + "Forge is open source software, released under the GNU Public License.";
        g.drawText(disclaimer, disclaimerFont, FProgressBar.SEL_FORE_COLOR,
                x, y, w, disclaimerHeight, true, HAlignment.CENTER, true);

        float padding = 20f / 450f * w;
        float pbHeight = 57f / 450f * h;
        y += 78f / 450f * h;
        progressBar.setBounds(x + padding, y, w - 2 * padding, pbHeight);
        g.draw(progressBar);
    }

    //if not forge-gui-mobile-dev, check whether assets are up to date
    private void checkForAssets() {
        if (Gdx.app.getType() == ApplicationType.Desktop) { return; }

        File versionFile = new File(ForgeConstants.ASSETS_DIR + "version.txt");
        if (!versionFile.exists()) {
            try {
                versionFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
                Gdx.app.exit(); //can't continue if this fails
            }
        }
        else if (Forge.CURRENT_VERSION.equals(TextUtil.join(FileUtil.readFile(versionFile), "\n"))) {
            return; //if version matches what had been previously saved, no need to download assets
        }

        //save version string to file once assets finish downloading
        //so they don't need to be re-downloaded until you upgrade again
        FileUtil.writeFile(versionFile, Forge.CURRENT_VERSION);
    }
}

package forge.app;

import java.io.File;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import forge.Forge;

public class Main extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup portrait orientation
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (Build.VERSION.SDK_INT > 8) { //use dual-side portrait mode if supported
            this.setRequestedOrientation(7);
        }

        //establish assets directory
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Gdx.app.error("Forge", "Can't access external storage");
            Gdx.app.exit();
            return;
        }
        File assetsDir = new File(Environment.getExternalStorageDirectory() + "/Forge");
        if (!assetsDir.exists() && !assetsDir.mkdirs()) {
            Gdx.app.error("Forge", "Can't access external storage");
            Gdx.app.exit();
            return;
        }

        initialize(new Forge(getClipboard(), assetsDir.getAbsolutePath() + "/"), true);
    }
}

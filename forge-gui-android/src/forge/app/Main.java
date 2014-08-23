package forge.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;

import forge.Forge;
import forge.interfaces.INetworkConnection;
import forge.util.Callback;
import forge.util.FileUtil;

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
        String assetsDir = Environment.getExternalStorageDirectory() + "/Forge/";
        if (!FileUtil.ensureDirectoryExists(assetsDir)) {
            Gdx.app.error("Forge", "Can't access external storage");
            Gdx.app.exit();
            return;
        }

        initialize(Forge.getApp(new AndroidClipboard(), new AndroidNetworkConnection(),
                assetsDir, new Callback<String>() {
            @Override
            public void run(String runOnExit) {
                if (runOnExit != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(runOnExit));
                    startActivity(intent);  
                }

                //ensure process doesn't stick around after exiting
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }));
    }

    //special clipboard that words on Android
    private class AndroidClipboard implements com.badlogic.gdx.utils.Clipboard {
        private final ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        @Override
        public String getContents() {
            if (cm.getPrimaryClip().getItemCount() > 0) {
                try {
                    return cm.getPrimaryClip().getItemAt(0).coerceToText(getContext()).toString();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return "";
        }

        @Override
        public void setContents(String contents0) {
            cm.setPrimaryClip(ClipData.newPlainText("Forge", contents0));
        }
    }

    private class AndroidNetworkConnection implements INetworkConnection {
        private final ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        @Override
        public boolean isConnected() {
            NetworkInfo activeNetworkInfo = connManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

        @Override
        public boolean isConnectedToWifi() {
            NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifi.isConnected();
        }
    }
}

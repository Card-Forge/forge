package forge.app;

import java.io.File;
import java.util.concurrent.Callable;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;

import forge.FThreads;
import forge.Forge;
import forge.interfaces.IDeviceAdapter;
import forge.util.FileUtil;
import forge.util.ThreadUtil;

public class Main extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidAdapter adapter = new AndroidAdapter(this.getContext());

        //establish assets directory
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Gdx.app.error("Forge", "Can't access external storage");
            adapter.exit();
            return;
        }
        String assetsDir = Environment.getExternalStorageDirectory() + "/Forge/";
        if (!FileUtil.ensureDirectoryExists(assetsDir)) {
            Gdx.app.error("Forge", "Can't access external storage");
            adapter.exit();
            return;
        }

        //ensure .nomedia file exists in Forge directory so its images
        //and other media files don't appear in Gallery or other apps
        String noMediaFile = assetsDir + ".nomedia";
        if (!FileUtil.doesFileExist(noMediaFile)) {
            FileUtil.writeFile(noMediaFile, "");
        }

        //enforce orientation based on whether device is a tablet and user preference
        adapter.switchOrientationFile = assetsDir + "switch_orientation.ini";
        boolean landscapeMode = adapter.isTablet == !FileUtil.doesFileExist(adapter.switchOrientationFile);
        if (landscapeMode) {
            Main.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else {
            Main.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        initialize(Forge.getApp(new AndroidClipboard(), adapter, assetsDir));
    }

    /*@Override
    protected void onDestroy() {
        super.onDestroy();

        //ensure app doesn't stick around
        //ActivityManager am = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
        //am.killBackgroundProcesses(getApplicationContext().getPackageName());

        
    }*/

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

    private class AndroidAdapter implements IDeviceAdapter {
        private final boolean isTablet;
        private final ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        private String switchOrientationFile;

        private AndroidAdapter(Context context) {
            isTablet = (context.getResources().getConfiguration().screenLayout
                    & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        }

        @Override
        public boolean isConnectedToInternet() {
            return Boolean.TRUE.equals(ThreadUtil.executeWithTimeout(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    NetworkInfo activeNetworkInfo = connManager.getActiveNetworkInfo();
                    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
                }
            }, 2000)); //if can't determine Internet connection within two seconds, assume not connected
        }

        @Override
        public boolean isConnectedToWifi() {
            return Boolean.TRUE.equals(ThreadUtil.executeWithTimeout(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    return wifi.isConnected();
                }
            }, 2000)); //if can't determine Internet connection within two seconds, assume not connected
        }

        @Override
        public String getDownloadsDir() {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        }

        @Override
        public boolean openFile(String filename) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //ensure this task isn't linked to this application
                Uri uri = Uri.fromFile(new File(filename));
                String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
                intent.setDataAndType(uri, type);
                startActivity(intent);
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public void restart() {
            try { //solution from http://stackoverflow.com/questions/6609414/howto-programatically-restart-android-app
                Context c = getApplicationContext();
                PackageManager pm = c.getPackageManager();
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(c.getPackageName());
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called. 
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(c, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void exit() {
            // Replace the current task with one that is excluded from the recent
            // apps and that will finish itself immediately. It's critical that this
            // activity get launched in the task that you want to hide.
            final Intent relaunch = new Intent(getApplicationContext(), Exiter.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK // CLEAR_TASK requires this
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK // finish everything else in the task
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); // hide (remove, in this case) task from recents
            startActivity(relaunch);
        }

        @Override
        public boolean isTablet() {
            return isTablet;
        }

        @Override
        public void setLandscapeMode(boolean landscapeMode) {
            //create file to indicate that portrait mode should be used for tablet or landscape should be used for phone
            if (landscapeMode != isTablet) {
                FileUtil.writeFile(switchOrientationFile, "1");
            }
            else {
                FileUtil.deleteFile(switchOrientationFile);
            }
        }

        @Override
        public void preventSystemSleep(final boolean preventSleep) {
            FThreads.invokeInEdtNowOrLater(new Runnable() { //must set window flags from EDT thread
                @Override
                public void run() {
                    if (preventSleep) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    else {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
            });
        }
    }
}

package forge.app;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import com.badlogic.gdx.Version;
import com.badlogic.gdx.backends.android.AndroidApplication;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import forge.Forge;
import forge.interfaces.IDeviceAdapter;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.FileUtil;
import forge.util.ThreadUtil;
import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;
import io.sentry.event.BreadcrumbBuilder;

public class Main extends AndroidApplication {
    AndroidAdapter Gadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = this.getApplicationContext();
        String sentryDsn = "https://a0b8dbad9b8a49cfa51bf65d462e8dae:b3f27d7461224cb8836eb5c6050c666c@sentry.cardforge.org/2?buffer.enabled=false";
        //init Sentry
        Sentry.init(sentryDsn, new AndroidSentryClientFactory(ctx));
        //get total device RAM in mb
        ActivityManager actManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        int totalMemory = Math.round(memInfo.totalMem / 1024f / 1024f);

        boolean permissiongranted =  checkPermission();
        Gadapter = new AndroidAdapter(this.getContext());
        initForge(Gadapter, permissiongranted, totalMemory, isTabletDevice(this.getContext()), Build.VERSION.SDK_INT, Build.VERSION.RELEASE);
    }
    private static boolean isTabletDevice(Context activityContext) {
        Display display = ((Activity)   activityContext).getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        float widthInches = metrics.widthPixels / metrics.xdpi;
        float heightInches = metrics.heightPixels / metrics.ydpi;
        double diagonalInches = Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));
        if (diagonalInches >= 7.0) {
            return true;
        }
        return false;
    }
    private void displayMessage(AndroidAdapter adapter, boolean ex, String msg){
        TableLayout TL = new TableLayout(this);
        TableRow row = new TableRow(this);
        TableRow row2 = new TableRow(this);
        TextView text = new TextView(this);
        text.setGravity(Gravity.LEFT);
        text.setTypeface(Typeface.SERIF);

        String title="Forge needs Storage Permission to run properly...\n" +
                "Follow these simple steps:\n\n";
        String steps = " 1) Tap \"Open App Details\" Button.\n" +
                " 2) Tap Permissions\n"+
                " 3) Turn on the Storage Permission.\n\n"+
                "(You can tap anywhere to exit and restart the app)\n\n";
        if (ex) {
            title = "Forge didn't initialize!\n";
            steps = msg + "\n\n";
        }

        SpannableString ss1=  new SpannableString(title);
        ss1.setSpan(new StyleSpan(Typeface.BOLD), 0, ss1.length(), 0);
        text.append(ss1);
        text.append(steps);
        row.addView(text);
        row.setGravity(Gravity.CENTER);

        int[] colors = {Color.TRANSPARENT,Color.TRANSPARENT};
        int[] pressed = {Color.GREEN,Color.GREEN};
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, colors);
        gd.setStroke(3, Color.DKGRAY);
        gd.setCornerRadius(100);

        GradientDrawable gd2 = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, pressed);
        gd2.setStroke(3, Color.DKGRAY);
        gd2.setCornerRadius(100);

        Button button = new Button(this);
        button.setText("Open App Details");

        StateListDrawable states = new StateListDrawable();

        states.addState(new int[] {android.R.attr.state_pressed}, gd2);
        states.addState(new int[] { }, gd);

        button.setBackground(states);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });

        row2.addView(button);
        row2.setGravity(Gravity.CENTER);

        TL.addView(row, new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        TL.addView(row2, new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        TL.setGravity(Gravity.CENTER);
        TL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.exit();
            }
        });
        setContentView(TL);
    }
    @Override
    public void onBackPressed() {
        if (Gadapter!=null)
            Gadapter.exit();

        super.onBackPressed();
    }
    private boolean checkPermission() {
        int pid = android.os.Process.myPid();
        int uid = android.os.Process.myUid();
        try {
            int result = this.getBaseContext().checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, pid, uid);
            if (result == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                return false;
            }
        } catch (NullPointerException e) {
            return false;
        }
    }
    private void requestPermission() {
            //Show Information about why you need the permission
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Storage Permission Denied...");
            builder.setMessage("This app needs storage permission to run properly.\n\n\n\n");
            builder.setPositiveButton("Open App Details", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    //ActivityCompat crashes... maybe it needs the appcompat v7???
                    //ActivityCompat.requestPermissions(Main.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            });
            /*builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {

                }
            });*/
            builder.show();
    }

    private void initForge(AndroidAdapter adapter, boolean permissiongranted, int totalRAM, boolean isTabletDevice, int AndroidAPI, String AndroidRelease){
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //fake init for error message
            //set current orientation
            String message = getDeviceName()+"\n"+"Android "+AndroidRelease+"\n"+"RAM "+ totalRAM+"MB" +"\n"+"LibGDX "+ Version.VERSION+"\n"+"Can't access external storage";
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(message).build()
            );
            Main.this.setRequestedOrientation(Main.this.getResources().getConfiguration().orientation);
            initialize(Forge.getApp(new AndroidClipboard(), adapter, "", false, true, totalRAM, isTabletDevice, AndroidAPI, AndroidRelease, getDeviceName()));
            displayMessage(adapter, true, message);
            return;
        }
        String obbforge = Environment.getExternalStorageDirectory() + "/obbforge";
        //if obbforge file exists in Phone Storage, Forge uses app-specific Obb directory as path, Android 11+ is mandatory even without obbforge
        String assetsDir = (FileUtil.doesFileExist(obbforge) || Build.VERSION.SDK_INT > 29) ? getContext().getObbDir()+"/Forge/" : Environment.getExternalStorageDirectory()+"/Forge/";
        if (!FileUtil.ensureDirectoryExists(assetsDir)) {
            //fake init for error message
            //set current orientation
            String message = getDeviceName()+"\n"+"Android "+AndroidRelease+"\n"+"RAM "+ totalRAM+"MB" +"\n"+"LibGDX "+ Version.VERSION+"\n"+"Can't access external storage\nPath: " + assetsDir;
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(message).build()
            );
            Main.this.setRequestedOrientation(Main.this.getResources().getConfiguration().orientation);
            initialize(Forge.getApp(new AndroidClipboard(), adapter, "", false, true, totalRAM, isTabletDevice, AndroidAPI, AndroidRelease, getDeviceName()));
            displayMessage(adapter, true, message);
            return;
        }
        boolean isPortrait;
        if (permissiongranted) {
            //ensure .nomedia file exists in Forge directory so its images
            //and other media files don't appear in Gallery or other apps
            String noMediaFile = assetsDir + ".nomedia";
            if (!FileUtil.doesFileExist(noMediaFile)) {
                FileUtil.writeFile(noMediaFile, "");
            }
            //enforce orientation based on whether device is a tablet and user preference
            adapter.switchOrientationFile = assetsDir + "switch_orientation.ini";
            boolean landscapeMode = adapter.isTablet == !FileUtil.doesFileExist(adapter.switchOrientationFile);
            ForgePreferences prefs = FModel.getPreferences();
            boolean propertyConfig = prefs != null && prefs.getPrefBoolean(ForgePreferences.FPref.UI_NETPLAY_COMPAT);
            if (landscapeMode) {
                isPortrait = false;
                Main.this.setRequestedOrientation(Build.VERSION.SDK_INT >= 26 ?
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : //Oreo and above has virtual back/menu buttons
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                isPortrait = true;
                Main.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            initialize(Forge.getApp(new AndroidClipboard(), adapter, assetsDir, propertyConfig, isPortrait, totalRAM, isTabletDevice, AndroidAPI, AndroidRelease, getDeviceName()));
        } else {
            isPortrait = true;
            //fake init for permission instruction
            Main.this.setRequestedOrientation(Main.this.getResources().getConfiguration().orientation);
            initialize(Forge.getApp(new AndroidClipboard(), adapter, "", false, isPortrait, totalRAM, isTabletDevice, AndroidAPI, AndroidRelease, getDeviceName()));
            displayMessage(adapter, false, "");
        }
    }

    /*@Override
    protected void onDestroy() {
        super.onDestroy();

        //ensure app doesn't stick around
        //ActivityManager am = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
        //am.killBackgroundProcesses(getApplicationContext().getPackageName());

        
    }*/

    @Override
    protected void onPause()
    {
        super.onPause();

        ForgePreferences prefs = FModel.getPreferences();
        boolean minimizeonScreenLock = prefs != null && prefs.getPrefBoolean(ForgePreferences.FPref.UI_ANDROID_MINIMIZE_ON_SCRLOCK);

        if (minimizeonScreenLock) {
            // If the screen is off then the device has been locked
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            boolean isScreenOn = powerManager.isScreenOn();
            if (!isScreenOn) {
                this.moveTaskToBack(true);
                // Minimize the app to the background...
            }
        }
    }

    //special clipboard that words on Android
    private class AndroidClipboard implements com.badlogic.gdx.utils.Clipboard {
        private final ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        @Override
        public boolean hasContents() {
            if (cm.getPrimaryClip().getItemCount() > 0) {
                try {
                    return cm.getPrimaryClip().getItemAt(0).coerceToText(getContext()).length() > 0;
                }
                catch (Exception ex) {
                    return false;
                }
            }
            return false;
        }

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
            finish();

            //ensure process fully killed
            System.exit(0);
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
            // Setting getWindow() Flags needs to run on UI thread.
            // Should fix android.view.ViewRoot$CalledFromWrongThreadException:
            // Only the original thread that created a view hierarchy can touch its views.
            runOnUiThread(new Runnable() {
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

        @Override
        public void convertToJPEG(InputStream input, OutputStream output) {
            Bitmap bmp = BitmapFactory.decodeStream(input);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, output);
        }
    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}

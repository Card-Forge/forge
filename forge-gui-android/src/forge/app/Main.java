package forge.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Version;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.badlogic.gdx.backends.android.AsynchronousAndroidAudio;
import com.getkeepsafe.relinker.ReLinker;
import de.cketti.fileprovider.PublicFileProvider;
import forge.Forge;
import forge.interfaces.IDeviceAdapter;
import forge.util.FileUtil;
import forge.util.ThreadUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;

public class Main extends AndroidApplication {
    private AndroidAdapter Gadapter;
    private ArrayList<String> gamepads;
    private AndroidClipboard androidClipboard;
    private boolean isMIUI;
    private String ASSETS_DIR = "";
    private SharedPreferences sharedPreferences;
    private int mShortAnimationDuration;
    private View forgeLogo = null, forgeView = null, activeView = null;
    private ProgressBar progressBar;
    private TextView progressText;
    private String versionString;

    private AndroidClipboard getAndroidClipboard() {
        if (androidClipboard == null)
            androidClipboard = new AndroidClipboard();
        return androidClipboard;
    }

    public static boolean isMiUi() {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"));
    }

    public static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }

    @Override
    protected void onResume() {
        try {
            super.onResume();
        } catch (Exception e) {
        }
    }

    @Override
    public AndroidAudio createAudio(Context context, AndroidApplicationConfiguration config) {
        return new AsynchronousAndroidAudio(context, config);
        //return super.createAudio(context, config);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            versionString = pInfo.versionName;
        } catch (Exception e) {
            versionString = "0.0";
        }
        setContentView(getResources().getIdentifier("main", "layout", getPackageName()));
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        progressBar = findViewById(getResources().getIdentifier("pBar", "id", getPackageName()));
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        progressText = findViewById(getResources().getIdentifier("pText", "id", getPackageName()));
        progressText.setVisibility(View.GONE);

        isMIUI = isMiUi();
        if (isMIUI)
            preventSleep(true);

        gamepads = getGameControllers();

        //get total device RAM in mb
        ActivityManager actManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        int totalMemory = Math.round(memInfo.totalMem / 1024f / 1024f);

        boolean permissiongranted = checkPermission();
        Gadapter = new AndroidAdapter(getContext());
        initForge(Gadapter, permissiongranted, totalMemory, isTabletDevice(getContext()));
    }

    private void crossfade(View contentView, View previousView) {
         activeView = contentView;
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        contentView.setAlpha(0f);
        contentView.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        addContentView(contentView, params);

        Animator ac = ObjectAnimator.ofFloat(contentView, "alpha", 0f, 1f).setDuration(mShortAnimationDuration);
        Animator ap = ObjectAnimator.ofFloat(previousView, "alpha", 1f, 0f).setDuration(mShortAnimationDuration);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ac, ap);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                previousView.setVisibility(View.GONE);
            }
        });
        animatorSet.start();
    }

    private static boolean isTabletDevice(Context activityContext) {
        Display display = ((Activity) activityContext).getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        float widthInches = metrics.widthPixels / metrics.xdpi;
        float heightInches = metrics.heightPixels / metrics.ydpi;
        double diagonalInches = Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));
        return diagonalInches >= 7.0;
    }

    private void displayMessage(View previousView, AndroidAdapter adapter, boolean ex, String msg, boolean manageApp) {
        TableLayout TL = new TableLayout(this);
        TL.setBackgroundResource(android.R.color.black);
        TableRow row = new TableRow(this);
        TableRow row2 = new TableRow(this);
        TextView text = new TextView(this);
        text.setGravity(Gravity.LEFT);
        text.setTypeface(Typeface.SERIF);
        String SP = "Storage Permission";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SP = "Photos and Videos, Music and Audio Permissions";
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            SP = "Files & Media Permissions";
        }

        String title = "Forge needs " + SP + " to run properly...\n" +
                "Follow these simple steps:\n\n";
        String steps = " 1) Tap \"App Settings\" Button.\n" +
                " 2) Tap Permissions\n" +
                " 3) Enable the " + SP + ".\n\n" +
                "(You can tap anywhere to exit and restart the app)\n\n";
        if (ex) {
            title = manageApp ? "Forge AutoUpdater Permission...\n" : "Forge didn't initialize!\n";
            steps = manageApp ? " 1) Tap \"App Settings\" Button.\n" +
                    " 2) Enable \"Allow apps from this source\"\n" +
                    "(You can tap anywhere to exit and restart the app)\n\n" : msg + "\n\n";
        }

        SpannableString ss1 = new SpannableString(title);
        ss1.setSpan(new StyleSpan(Typeface.BOLD), 0, ss1.length(), 0);
        text.append(ss1);
        text.append(steps);
        row.addView(text);
        row.setGravity(Gravity.CENTER);

        int[] colors = {Color.TRANSPARENT, Color.TRANSPARENT};
        int[] pressed = {Color.GREEN, Color.GREEN};
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, colors);
        gd.setStroke(3, Color.DKGRAY);
        gd.setCornerRadius(100);

        GradientDrawable gd2 = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, pressed);
        gd2.setStroke(3, Color.DKGRAY);
        gd2.setCornerRadius(100);

        Button button = new Button(this);
        button.setText("App Settings");
        button.setTypeface(Typeface.DEFAULT_BOLD);

        StateListDrawable states = new StateListDrawable();

        states.addState(new int[]{android.R.attr.state_pressed}, gd2);
        states.addState(new int[]{}, gd);

        button.setBackground(states);

        button.setTextColor(Color.RED);
        button.setOnClickListener(v -> {
            if (manageApp) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse(String.format("package:%s", getPackageName())))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse(String.format("package:%s", getPackageName())))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        row2.addView(button);
        row2.setGravity(Gravity.CENTER);

        TL.addView(row, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        TL.addView(row2, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        TL.setGravity(Gravity.CENTER);
        TL.setOnClickListener(v -> adapter.restart());
        crossfade(TL, previousView);
    }

    private void loadGame(final String title, final String steps, boolean isLandscape, AndroidAdapter adapter, boolean permissiongranted, int totalRAM, boolean isTabletDevice, AndroidApplicationConfiguration config, boolean exception, String msg) {
        try {
            final Handler handler = new Handler();
            forgeLogo = findViewById(getResources().getIdentifier("logo_id", "id", getPackageName()));
            activeView = findViewById(getResources().getIdentifier("mainview", "id", getPackageName()));
            activeView.setBackgroundColor(Color.WHITE);
            forgeView = initializeForView(Forge.getApp(getAndroidClipboard(), adapter, ASSETS_DIR, false, !isLandscape, totalRAM, isTabletDevice, Build.VERSION.SDK_INT, Build.VERSION.RELEASE, getDeviceName()), config);

            getAnimator(ObjectAnimator.ofFloat(forgeLogo, "alpha", 1f, 1f).setDuration(800), ObjectAnimator.ofObject(activeView, "backgroundColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK).setDuration(1600), new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    handler.postDelayed(() -> {
                        if (!permissiongranted || exception) {
                            displayMessage(forgeLogo, adapter, exception, msg, false);
                        } else if (title.isEmpty() && steps.isEmpty()) {
                            if (isLandscape) {
                                Main.this.setRequestedOrientation(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : //Oreo and above has virtual back/menu buttons
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            } else {
                                Main.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                            }
                            crossfade(forgeView, forgeLogo);
                        } else {
                            if (sharedPreferences.getBoolean("run_anyway", false)) {
                                crossfade(forgeView, forgeLogo);
                                return;
                            }
                            TableLayout TL = new TableLayout(getContext());
                            TL.setBackgroundResource(android.R.color.black);
                            TableRow messageRow = new TableRow(getContext());
                            TableRow checkboxRow = new TableRow(getContext());
                            TableRow buttonRow = new TableRow(getContext());
                            TextView text = new TextView(getContext());
                            text.setGravity(Gravity.LEFT);
                            text.setTypeface(Typeface.SERIF);

                            SpannableString ss1 = new SpannableString(title);
                            ss1.setSpan(new StyleSpan(Typeface.BOLD), 0, ss1.length(), 0);
                            text.append(ss1);
                            text.append(steps + "\n");
                            messageRow.addView(text);
                            messageRow.setGravity(Gravity.CENTER);

                            CheckBox checkBox = new CheckBox(getContext());
                            checkBox.setTypeface(Typeface.SERIF);
                            checkBox.setGravity(Gravity.TOP);
                            checkBox.setChecked(false);
                            checkBox.setPadding(30, 30, 30, 30);
                            checkBox.setTypeface(Typeface.SERIF);
                            checkBox.setText(" Don't remind me next time. ");
                            checkBox.setScaleX(0.9f);
                            checkBox.setScaleY(0.9f);
                            checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                                    sharedPreferences.edit().putBoolean("run_anyway", isChecked).apply());
                            checkboxRow.addView(checkBox);
                            checkboxRow.setGravity(Gravity.CENTER);

                            int[] colors = {Color.TRANSPARENT, Color.TRANSPARENT};
                            int[] pressed = {Color.GREEN, Color.GREEN};
                            GradientDrawable gd = new GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM, colors);
                            gd.setStroke(3, Color.DKGRAY);
                            gd.setCornerRadius(100);

                            GradientDrawable gd2 = new GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM, pressed);
                            gd2.setStroke(3, Color.DKGRAY);
                            gd2.setCornerRadius(100);

                            Button button = new Button(getContext());
                            button.setText("Run Forge..");
                            button.setTypeface(Typeface.DEFAULT_BOLD);

                            StateListDrawable states = new StateListDrawable();

                            states.addState(new int[]{android.R.attr.state_pressed}, gd2);
                            states.addState(new int[]{}, gd);

                            button.setBackground(states);

                            button.setTextColor(Color.RED);
                            button.setOnClickListener(v -> {
                                button.setClickable(false);
                                crossfade(forgeView, TL);
                            });

                            buttonRow.addView(button);
                            buttonRow.setGravity(Gravity.CENTER);

                            TL.addView(messageRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                            TL.addView(checkboxRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                            TL.addView(buttonRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                            TL.setGravity(Gravity.CENTER);
                            crossfade(TL, forgeLogo);
                        }
                    }, 600);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AnimatorSet getAnimator(Animator play, Animator with, AnimatorListenerAdapter adapter) {
        AnimatorSet animatorSet = new AnimatorSet();
        if (with != null) {
            animatorSet.playSequentially(play, with);
        } else
            animatorSet.play(play);
        animatorSet.addListener(adapter);
        return animatorSet;
    }

    @Override
    public void onBackPressed() {
        if (Gadapter != null)
            Gadapter.exit();

        super.onBackPressed();
    }

    private boolean checkPermission() {
        int pid = android.os.Process.myPid();
        int uid = android.os.Process.myUid();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (getBaseContext().checkPermission(android.Manifest.permission.READ_MEDIA_IMAGES, pid, uid) == PackageManager.PERMISSION_GRANTED)
                    if (getBaseContext().checkPermission(android.Manifest.permission.READ_MEDIA_AUDIO, pid, uid) == PackageManager.PERMISSION_GRANTED)
                        return getBaseContext().checkPermission(android.Manifest.permission.READ_MEDIA_VIDEO, pid, uid) == PackageManager.PERMISSION_GRANTED;
                return false;
            } else {
                return getBaseContext().checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, pid, uid) == PackageManager.PERMISSION_GRANTED;
            }
        } catch (NullPointerException e) {
            return false;
        }
    }

    private void initForge(AndroidAdapter adapter, boolean permissiongranted, int totalRAM, boolean isTabletDevice) {
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        config.useRotationVectorSensor = false;
        config.useImmersiveMode = false;
        config.nativeLoader = () -> ReLinker.loadLibrary(getContext(), "gdx");

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String message = getDeviceName() + "\n" + "Android " + Build.VERSION.RELEASE + "\n" + "RAM " + totalRAM + "MB" + "\n" + "LibGDX " + Version.VERSION + "\n" + "Can't access external storage";
            Main.this.setRequestedOrientation(Main.this.getResources().getConfiguration().orientation);
            loadGame("", "", false, adapter, permissiongranted, totalRAM, isTabletDevice, config, true, message);
            return;
        }
        ASSETS_DIR = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q ? getContext().getObbDir() + "/Forge/" : Environment.getExternalStorageDirectory() + "/Forge/";
        if (!FileUtil.ensureDirectoryExists(ASSETS_DIR)) {
            String message = getDeviceName() + "\n" + "Android " + Build.VERSION.RELEASE + "\n" + "RAM " + totalRAM + "MB" + "\n" + "LibGDX " + Version.VERSION + "\n" + "Can't access external storage\nPath: " + ASSETS_DIR;
            Main.this.setRequestedOrientation(Main.this.getResources().getConfiguration().orientation);
            loadGame("", "", false, adapter, permissiongranted, totalRAM, isTabletDevice, config, true, message);
            return;
        }
        //ensure .nomedia file exists in Forge directory so its images
        //and other media files don't appear in Gallery or other apps
        String noMediaFile = ASSETS_DIR + ".nomedia";
        if (!FileUtil.doesFileExist(noMediaFile)) {
            try {
                FileUtil.writeFile(noMediaFile, "");
            } catch (Exception e) {
                String message = getDeviceName() + "\n" + "Android " + Build.VERSION.RELEASE + "\n" + "RAM " + totalRAM + "MB" + "\n" + "LibGDX " + Version.VERSION + "\n" + "Can't read/write to storage";
                Main.this.setRequestedOrientation(Main.this.getResources().getConfiguration().orientation);
                loadGame("", "", false, adapter, permissiongranted, totalRAM, isTabletDevice, config, true, message);
                return;
            }
        }
        //enforce orientation based on whether device is a tablet and user preference
        adapter.switchOrientationFile = ASSETS_DIR + "switch_orientation.ini";
        boolean landscapeMode = adapter.isTablet == !FileUtil.doesFileExist(adapter.switchOrientationFile);

        String info = totalRAM < 3500 || Build.VERSION.SDK_INT < Build.VERSION_CODES.R ? "Device Specification Check\n" + getDeviceName()
                + "\n" + "Android " + Build.VERSION.RELEASE + "\n" + "RAM " + totalRAM + "MB\n\nRecommended API:" : "";
        // Even though Forge runs on Android 8 as minimum, just show indicator that Android 11 is recommended
        String lowV = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ? "\nAPI: Android 11 or higher" : "";
        // also show minimum Device RAM
        String lowM = totalRAM < 3500 ? "\nRAM: 4GB RAM or higher" : "";
        if (landscapeMode && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) { //Android 11 onwards
            Main.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        loadGame(info, lowV + lowM, landscapeMode, adapter, permissiongranted, totalRAM, isTabletDevice, config, false, "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //ensure app doesn't stick around
        //ActivityManager am = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
        //am.killBackgroundProcesses(getApplicationContext().getPackageName());
    }

    @Override
    protected void onPause() {
        super.onPause();

        /*ForgePreferences prefs = FModel.getPreferences();
        boolean minimizeonScreenLock = prefs != null && prefs.getPrefBoolean(ForgePreferences.FPref.UI_ANDROID_MINIMIZE_ON_SCRLOCK);

        if (minimizeonScreenLock) {
            // If the screen is off then the device has been locked
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            boolean isScreenOn = powerManager.isScreenOn();
            if (!isScreenOn) {
                this.moveTaskToBack(true);
                // Minimize the app to the background...
            }
        }*/
    }

    //special clipboard that words on Android
    private class AndroidClipboard implements com.badlogic.gdx.utils.Clipboard {
        private final ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        @Override
        public boolean hasContents() {
            ClipData clipData = cm.getPrimaryClip();
            if (clipData == null)
                return false;
            if (clipData.getItemCount() > 0) {
                try {
                    return clipData.getItemAt(0).coerceToText(getContext()).length() > 0;
                } catch (Exception ex) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String getContents() {
            ClipData clipData = cm.getPrimaryClip();
            if (clipData == null)
                return "";
            if (clipData.getItemCount() > 0) {
                try {
                    String text = clipData.getItemAt(0).coerceToText(getContext()).toString();
                    return Normalizer.normalize(text, Normalizer.Form.NFD);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return "";
        }

        @Override
        public void setContents(String contents0) {
            try {
                cm.setPrimaryClip(ClipData.newPlainText("Forge", contents0));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class AndroidAdapter implements IDeviceAdapter {
        private final boolean isTablet;
        private final ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        private String switchOrientationFile;
        private final Context context;
        private boolean connected;

        private AndroidAdapter(Context context) {
            this.context = context;
            isTablet = (context.getResources().getConfiguration().screenLayout
                    & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE;
            try {
                if (connManager != null) {
                    connManager.registerDefaultNetworkCallback(
                            new ConnectivityManager.NetworkCallback() {
                                @Override
                                public void onAvailable(Network network) {
                                    connected = true;
                                }

                                @Override
                                public void onLost(Network network) {
                                    connected = false;
                                }
                            }
                    );
                }
            } catch (Exception e) {
                connected = false;
            }
        }

        private boolean hasInternet() {
            return isNetworkConnected(false);
        }

        private boolean hasWiFiInternet() {
            return isNetworkConnected(true);
        }

        private boolean isNetworkConnected(boolean wifiOnly) {
            boolean result = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (connManager != null) {
                    NetworkCapabilities capabilities = connManager.getNetworkCapabilities(connManager.getActiveNetwork());
                    if (capabilities != null) {
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            result = connected;
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            result = connected && !wifiOnly;
                        }
                    }
                }
            } else {
                if (connManager != null) {
                    NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
                    if (activeNetwork != null) {
                        // connected to the internet
                        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                            result = true;
                        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                            result = !wifiOnly;
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public boolean isConnectedToInternet() {
            //if it can't determine Internet connection within two seconds, assume not connected
            return Boolean.TRUE.equals(ThreadUtil.executeWithTimeout(this::hasInternet, 2000));
        }

        @Override
        public boolean isConnectedToWifi() {
            //if it can't determine Internet connection within two seconds, assume not connected
            return Boolean.TRUE.equals(ThreadUtil.executeWithTimeout(this::hasWiFiInternet, 2000));
        }

        @Override
        public String getDownloadsDir() {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        }

        @Override
        public String getVersionString() {
            return versionString;
        }

        @Override
        public String getLatestChanges(String commitsAtom, Date buildDateOriginal, Date maxDate) {
            return new GitLogs().getLatest(commitsAtom, buildDateOriginal, maxDate);
        }

        @Override
        public String getReleaseTag(String releaseAtom) {
            return new GitLogs().getLatestReleaseTag(releaseAtom);
        }

        @Override
        public boolean openFile(String filename) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //ensure this task isn't linked to this application
                    Uri uri = Uri.fromFile(new File(filename));
                    String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                            MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
                    intent.setDataAndType(uri, type);
                    startActivity(intent);
                    return true;
                } else {
                    Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    intent.setData(PublicFileProvider.getUriForFile(getContext(), "com.mydomain.publicfileprovider", new File(filename)));
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public void restart() {
            triggerRebirth();
        }

        @Override
        public void exit() {
            finish();

            //ensure process fully killed
            System.exit(0);
        }

        @Override
        public void closeSplashScreen() {
            //only for desktop mobile-dev
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
            } else {
                FileUtil.deleteFile(switchOrientationFile);
            }
        }

        @Override
        public void preventSystemSleep(final boolean preventSleep) {
            // Setting getWindow() Flags needs to run on UI thread.
            // Should fix android.view.ViewRoot$CalledFromWrongThreadException:
            // Only the original thread that created a view hierarchy can touch its views.
            preventSleep(preventSleep);
        }

        @Override
        public void convertToJPEG(InputStream input, OutputStream output) {
            Bitmap bmp = BitmapFactory.decodeStream(input);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, output);
        }

        @Override
        public Pair<Integer, Integer> getRealScreenSize(boolean real) {
            //app size
            WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point size = new Point();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // Seems it doesn't compile if using 4.1.1.4 since it's missing this method
                if (real)
                    display.getRealSize(size);
                else
                    display.getSize(size);
                //remove this line below and use the method above if using Android libs higher than 4.1.1.4
                //return Pair.of(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // this method don't take account the soft navigation bars taken in rendered screen
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                try {
                    size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                    size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
                } catch (Exception e) {
                    size.x = Gdx.graphics.getWidth();
                    size.y = Gdx.graphics.getHeight();
                }
            } else {
                size.x = Gdx.graphics.getWidth();
                size.y = Gdx.graphics.getHeight();
            }
            return Pair.of(size.x, size.y);
        }

        @Override
        public ArrayList<String> getGamepads() {
            return gamepads;
        }
    }

    private void preventSleep(boolean preventSleep) {
        runOnUiThread(() -> {
            if (preventSleep) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                if (!isMIUI)
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    private void triggerRebirth() {
        try {
            Context context = getApplicationContext();
            PackageManager packageManager = context.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            // Required for API 34 and later
            // Ref: https://developer.android.com/about/versions/14/behavior-changes-14#safer-intents
            mainIntent.setPackage(context.getPackageName());
            context.startActivity(mainIntent);
            Runtime.getRuntime().exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> getGameControllers() {
        ArrayList<String> gameControllerDeviceIds = new ArrayList<>();
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();
            String devNameId = dev.getName() + "[" + deviceId + "]";

            // Verify that the device has gamepad buttons, control sticks, or both.
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                    || ((sources & InputDevice.SOURCE_JOYSTICK)
                    == InputDevice.SOURCE_JOYSTICK)) {
                // This device is a game controller. Store its device ID.
                if (!gameControllerDeviceIds.contains(devNameId)) {
                    gameControllerDeviceIds.add(devNameId);
                }
            }
        }
        return gameControllerDeviceIds;
    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.BRAND + " - " + Build.MODEL;
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

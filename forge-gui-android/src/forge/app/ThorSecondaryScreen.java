package forge.app;

import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.badlogic.gdx.Gdx;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import forge.interfaces.ISecondaryScreen;

/**
 * Second physical display of a dual-screen handheld (e.g. the AYN Thor's bottom screen).
 * <p>
 * A {@link Presentation} on that display holds a {@link SurfaceView}. Forge draws into it from the libGDX GL thread
 * using the libGDX EGL context: each frame, after the primary display was drawn, {@link #beginFrame()} makes an EGL
 * window surface for the SurfaceView current, and {@link #endFrame()} presents it and switches back. Textures, fonts and
 * shaders are therefore shared with the primary display. Touches on the SurfaceView are forwarded to Forge.
 */
public class ThorSecondaryScreen implements ISecondaryScreen {
    private static final String TAG = "ForgeDualScreen";

    private final Activity activity;
    private final DisplayManager displayManager;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ReentrantLock surfaceLock = new ReentrantLock();

    private Display display;
    private BottomPresentation presentation;
    private boolean resumed;
    private volatile TouchListener touchListener;

    //written on the UI thread under surfaceLock, read on the GL thread under surfaceLock
    private Surface surface;
    private int width, height;
    private boolean surfaceChanged;

    //GL thread only
    private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
    private EGLSurface savedDraw, savedRead;
    private final int[] savedViewport = new int[4];

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            if (display == null) {
                display = findSecondaryDisplay();
                if (resumed) {
                    show();
                }
            }
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            if (display != null && display.getDisplayId() == displayId) {
                dismiss();
                display = null;
            }
        }

        @Override
        public void onDisplayChanged(int displayId) {
        }
    };

    public ThorSecondaryScreen(Activity activity0) {
        activity = activity0;
        displayManager = (DisplayManager) activity.getSystemService(Context.DISPLAY_SERVICE);
        display = findSecondaryDisplay();
        if (displayManager != null) {
            displayManager.registerDisplayListener(displayListener, uiHandler);
        }
        Log.i(TAG, display == null ? "no secondary display" : "secondary display: " + display);
    }

    /** True if the device has a second display (checked at startup and when displays are added/removed). */
    public boolean hasDisplay() {
        return display != null;
    }

    private Display findSecondaryDisplay() {
        if (displayManager == null) {
            return null;
        }
        Display[] presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (presentationDisplays.length > 0) {
            return presentationDisplays[0];
        }
        //some devices expose a built-in second screen without the presentation category
        for (Display d : displayManager.getDisplays()) {
            if (d.getDisplayId() != Display.DEFAULT_DISPLAY && d.isValid()) {
                return d;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- activity lifecycle (UI thread)

    public void onResume() {
        resumed = true;
        show();
    }

    public void onPause() {
        resumed = false;
        dismiss();
    }

    public void onDestroy() {
        resumed = false;
        dismiss();
        if (displayManager != null) {
            displayManager.unregisterDisplayListener(displayListener);
        }
    }

    private void show() {
        if (display == null || presentation != null || activity.isFinishing()) {
            return;
        }
        try {
            presentation = new BottomPresentation(activity, display);
            presentation.show();
        } catch (WindowManager.InvalidDisplayException | IllegalStateException e) {
            Log.w(TAG, "could not show on the secondary display", e);
            presentation = null;
        }
    }

    private void dismiss() {
        if (presentation != null) {
            presentation.dismiss(); //surfaceDestroyed releases the EGL surface
            presentation = null;
        }
    }

    // ---------------------------------------------------------------- ISecondaryScreen (GL thread)

    @Override
    public boolean isAvailable() {
        return surface != null && width > 0 && height > 0;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean beginFrame() {
        if (!surfaceLock.tryLock()) {
            return false; //surface being replaced right now: skip this frame
        }
        boolean ok = false;
        try {
            if (surface == null || width <= 0 || height <= 0) {
                return false;
            }
            EGLContext context = EGL14.eglGetCurrentContext();
            if (context == null || context.equals(EGL14.EGL_NO_CONTEXT)) {
                return false;
            }
            EGLDisplay currentDisplay = EGL14.eglGetCurrentDisplay();
            if (!context.equals(eglContext) || !currentDisplay.equals(eglDisplay)) {
                releaseEglSurface(); //libGDX recreated its context (e.g. after pause)
                eglContext = context;
                eglDisplay = currentDisplay;
            }
            if (eglSurface == EGL14.EGL_NO_SURFACE || surfaceChanged) {
                releaseEglSurface();
                surfaceChanged = false;
                if (!createEglSurface()) {
                    return false;
                }
            }
            savedDraw = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
            savedRead = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
            GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, savedViewport, 0);
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                Log.w(TAG, "eglMakeCurrent failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
                releaseEglSurface();
                EGL14.eglMakeCurrent(eglDisplay, savedDraw, savedRead, eglContext);
                return false;
            }
            GLES20.glViewport(0, 0, width, height);
            ok = true;
            return true;
        } finally {
            if (!ok) {
                surfaceLock.unlock();
            }
        }
    }

    @Override
    public void endFrame() {
        try {
            if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                Log.w(TAG, "eglSwapBuffers failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
            }
            EGL14.eglMakeCurrent(eglDisplay, savedDraw, savedRead, eglContext);
            GLES20.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
        } finally {
            if (surfaceLock.isHeldByCurrentThread()) {
                surfaceLock.unlock();
            }
        }
    }

    @Override
    public void setTouchListener(TouchListener listener) {
        touchListener = listener;
    }

    private boolean createEglSurface() {
        int[] configId = new int[1];
        if (!EGL14.eglQueryContext(eglDisplay, eglContext, EGL14.EGL_CONFIG_ID, configId, 0)) {
            Log.w(TAG, "eglQueryContext failed");
            return false;
        }
        int[] attribs = { EGL14.EGL_CONFIG_ID, configId[0], EGL14.EGL_NONE };
        EGLConfig[] configs = new EGLConfig[1];
        int[] count = new int[1];
        if (!EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, count, 0) || count[0] < 1) {
            Log.w(TAG, "eglChooseConfig failed for config " + configId[0]);
            return false;
        }
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, new int[] { EGL14.EGL_NONE }, 0);
        if (eglSurface == null || eglSurface.equals(EGL14.EGL_NO_SURFACE)) {
            Log.w(TAG, "eglCreateWindowSurface failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
            eglSurface = EGL14.EGL_NO_SURFACE;
            return false;
        }
        //don't let the bottom screen's vsync halve the frame rate; the main surface already waits for it
        EGLSurface draw = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        EGLSurface read = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
        if (EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            EGL14.eglSwapInterval(eglDisplay, 0);
            EGL14.eglMakeCurrent(eglDisplay, draw, read, eglContext);
        }
        Log.i(TAG, "bottom screen surface " + width + "x" + height);
        return true;
    }

    private void releaseEglSurface() {
        if (eglSurface != EGL14.EGL_NO_SURFACE && eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
        }
        eglSurface = EGL14.EGL_NO_SURFACE;
    }

    private static void requestRender() {
        if (Gdx.graphics != null) {
            Gdx.graphics.requestRendering();
        }
    }

    // ---------------------------------------------------------------- presentation (UI thread)

    private class BottomPresentation extends Presentation {
        BottomPresentation(Context context, Display display) {
            super(context, display);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //keep input focus (controller/keys) on the main display; touches still arrive here
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN);
            SurfaceView view = new SurfaceView(getContext());
            view.setBackgroundColor(Color.TRANSPARENT);
            view.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
                    surfaceLock.lock();
                    try {
                        surface = holder.getSurface();
                        width = w;
                        height = h;
                        surfaceChanged = true;
                    } finally {
                        surfaceLock.unlock();
                    }
                    requestRender();
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    //the surface must not be used once this returns: wait for a frame in progress to finish
                    boolean locked = false;
                    try {
                        locked = surfaceLock.tryLock(500, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignored) {
                    }
                    try {
                        surface = null;
                        width = 0;
                        height = 0;
                        surfaceChanged = true; //the GL thread drops its EGL surface on the next frame
                        if (eglSurface != EGL14.EGL_NO_SURFACE && eglDisplay != EGL14.EGL_NO_DISPLAY) {
                            //not current anywhere between frames, so it can be released from this thread
                            EGL14.eglDestroySurface(eglDisplay, eglSurface);
                            eglSurface = EGL14.EGL_NO_SURFACE;
                        }
                    } finally {
                        if (locked) {
                            surfaceLock.unlock();
                        }
                    }
                }
            });
            view.setOnTouchListener((v, event) -> {
                forwardTouch(event);
                return true;
            });
            setContentView(view);
            hideSystemBars();
        }

        private void hideSystemBars() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }

        //if this window gets key focus anyway (some devices focus the last touched display), hand keys to the game
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return activity.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            return activity.dispatchGenericMotionEvent(event);
        }
    }

    private void forwardTouch(MotionEvent event) {
        TouchListener listener = touchListener;
        if (listener == null) {
            return;
        }
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = event.getActionIndex();
                listener.touchDown((int) event.getX(i), (int) event.getY(i), event.getPointerId(i));
                break;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    listener.touchDragged((int) event.getX(i), (int) event.getY(i), event.getPointerId(i));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                int i = event.getActionIndex();
                listener.touchUp((int) event.getX(i), (int) event.getY(i), event.getPointerId(i));
                break;
            }
            default:
                break;
        }
    }
}

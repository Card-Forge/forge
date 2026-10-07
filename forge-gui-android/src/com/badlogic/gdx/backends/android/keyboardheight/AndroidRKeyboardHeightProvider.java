
package com.badlogic.gdx.backends.android.keyboardheight;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.WindowInsets;
import android.graphics.Insets;
import org.jetbrains.annotations.NotNull;

@TargetApi(VERSION_CODES.R)
public class AndroidRKeyboardHeightProvider implements KeyboardHeightProvider {

	private final Activity activity;
	private View view;
	private KeyboardHeightObserver observer;

	/** The cached landscape height of the keyboard */
	private static int keyboardLandscapeHeight;

	/** The cached portrait height of the keyboard */
	private static int keyboardPortraitHeight;

	/** The cached visible value of the keyboard */
	private static boolean cachedVisible;
	/** The cached inset to the left */
	private static int cachedInsetLeft;
	/** The cached inset to the right */
	private static int cachedInsetRight;
	/** The cached inset to the bottom */
	private static int cachedBottomInset;
	/** The cached orientation of the app */
	private static int cachedOrientation;

	public AndroidRKeyboardHeightProvider (final Activity activity) {
		this.activity = activity;
	}

	@Override
	public void start () {
		this.view = activity.findViewById(android.R.id.content);
		// We do this, to not dispatch changes that are not changes on first run
		cachedOrientation = activity.getResources().getConfiguration().orientation;

		view.setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
			@NotNull
			@Override
			public WindowInsets onApplyWindowInsets (@NotNull View v, @NotNull WindowInsets windowInsets) {
				if (observer == null) return windowInsets;
				int bottomInset = 0;
				int leftInset = 0;
				int rightInset = 0;

				int orientation = activity.getResources().getConfiguration().orientation;
				boolean isVisible = windowInsets.isVisible(WindowInsets.Type.ime());
				if (isVisible) {
					int inset = WindowInsets.Type.systemBars() | WindowInsets.Type.ime()
							| WindowInsets.Type.displayCutout() | WindowInsets.Type.mandatorySystemGestures();

					Insets insets = windowInsets.getInsets(inset);
					if (orientation == Configuration.ORIENTATION_PORTRAIT) {
						keyboardPortraitHeight = insets.bottom;
					} else {
						keyboardLandscapeHeight = insets.bottom;
					}

					bottomInset = insets.bottom;
					leftInset = insets.left;
					rightInset = insets.right;
				}

				if (isVisible == cachedVisible && bottomInset == cachedBottomInset && leftInset == cachedInsetLeft
						&& rightInset == cachedInsetRight && orientation == cachedOrientation) return windowInsets;

				cachedVisible = isVisible;
				cachedBottomInset = bottomInset;
				cachedInsetLeft = leftInset;
				cachedInsetRight = rightInset;
				cachedOrientation = orientation;

				observer.onKeyboardHeightChanged(isVisible, bottomInset, leftInset, rightInset, orientation);

				return windowInsets;
			}
		});
	}

	@Override
	public void close () {
		if (view != null) view.setOnApplyWindowInsetsListener(null);
		this.observer = null;
	}

	@Override
	public void setKeyboardHeightObserver (KeyboardHeightObserver observer) {
		this.observer = observer;
	}

	@Override
	public int getKeyboardLandscapeHeight () {
		return keyboardLandscapeHeight;
	}

	@Override
	public int getKeyboardPortraitHeight () {
		return keyboardPortraitHeight;
	}
}

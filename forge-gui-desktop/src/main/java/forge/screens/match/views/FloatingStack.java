/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match.views;

import java.awt.Dimension;
import java.awt.Rectangle;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.util.Localizer;

/**
 * The window the stack is shown in, floating over the board. Sized to whatever
 * is on the stack at the moment, and hidden entirely when the stack is empty.
 */
@SuppressWarnings("serial")
public class FloatingStack extends FloatingMatchWindow {
    /**
     * Fraction of the match window's height the cascade's bottom edge sits at
     * before the user moves the window. Chosen to sit clear of the centred
     * {@link FloatingPrompt}, since both are usually up at the same time.
     */
    private static final float DEFAULT_BOTTOM_FRACTION = 0.42f;

    public FloatingStack() {
        super(FPref.STACK_WINDOW_LOC, true);
        setTitle(Localizer.getInstance().getMessage("lblStack"));
        setMinimumSize(new Dimension(120, 160));
        // The stack is read-only, so it must never take focus away from the
        // prompt's buttons, which are driven by the keyboard.
        setFocusableWindowState(false);
    }

    /**
     * Fits the window to the current stack. Does nothing once the user has
     * sized it themselves — from then on the cascade fits the window instead.
     */
    public void sizeToContent() {
        if (isUserSized()) { return; }
        reposition(() -> {
            pack();
            placeByDefault();
        });
    }

    /** True once the window has a size the user chose. */
    public boolean isUserSized() {
        return isUserPlaced();
    }

    /** Widens the window to make room for content that just appeared. */
    public void grow(final int widthDelta) {
        reposition(() -> setSize(
                Math.max(getMinimumSize().width, getWidth() + widthDelta), getHeight()));
        validate();
    }

    @Override
    protected void placeByDefault() {
        final Rectangle r = ownerBounds();
        final int x = r.x + (r.width - getWidth()) / 2;
        final int y = Math.max(r.y + 8,
                r.y + Math.round(r.height * DEFAULT_BOTTOM_FRACTION) - getHeight());
        reposition(() -> setLocation(x, y));
    }
}

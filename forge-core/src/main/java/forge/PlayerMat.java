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
package forge;

/**
 * The play surface drawn under a player's battlefield.
 * <p>
 * A mat is identified by a string key so that the same field can later hold an
 * image reference (a file name under the mats resource folder) as well as one of
 * these built-in colours. {@link #fromKey} returns {@code null} for anything it
 * doesn't recognise, which callers should treat as "not a preset colour" rather
 * than as an error.
 * <p>
 * Colours are plain RGB ints, not {@code java.awt.Color}, because this class is
 * shared with the Android build.
 */
public enum PlayerMat {
    NONE   ("None",    0x000000),
    SLATE  ("Slate",   0x2E3440),
    ASH    ("Ash",     0x333333),
    OCEAN  ("Ocean",   0x1B3A4B),
    FOREST ("Forest",  0x1F3D2B),
    CRIMSON("Crimson", 0x4A1F24),
    AMBER  ("Amber",   0x4A3A1B),
    VIOLET ("Violet",  0x33254A),
    MOSS   ("Moss",    0x2F3A22),
    WINE   ("Wine",    0x3A1F33);

    /** Key stored when the player hasn't picked anything. */
    public static final String DEFAULT_KEY = SLATE.name();

    private final String label;
    private final int rgb;

    PlayerMat(final String label, final int rgb) {
        this.label = label;
        this.rgb = rgb;
    }

    /** Human-readable name for pickers. */
    public String getLabel() {
        return label;
    }

    /** 0xRRGGBB. Meaningless for {@link #NONE}, which draws nothing. */
    public int getRgb() {
        return rgb;
    }

    /** True if this mat paints no surface, leaving the theme background visible. */
    public boolean isTransparent() {
        return this == NONE;
    }

    /**
     * @param key a mat key, case-insensitive
     * @return the matching preset, or {@code null} if the key is empty or names
     *         something that isn't a built-in colour (e.g. a future image key)
     */
    public static PlayerMat fromKey(final String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        for (final PlayerMat mat : values()) {
            if (mat.name().equalsIgnoreCase(key)) {
                return mat;
            }
        }
        return null;
    }

    /** As {@link #fromKey}, falling back to the default rather than returning null. */
    public static PlayerMat fromKeyOrDefault(final String key) {
        final PlayerMat mat = fromKey(key);
        return mat == null ? SLATE : mat;
    }
}

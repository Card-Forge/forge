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
package forge.localinstance.properties;



public class ForgeNetPreferences extends PreferencesStore<ForgeNetPreferences.FNetPref> {

    /**
     * Preference identifiers and their default values.
     */
    public enum FNetPref implements PreferencesStore.IPref {
        NET_PORT("36743"),
        UPnP("ASK");

        private final String strDefaultVal;

        FNetPref(final String s0) {
            this.strDefaultVal = s0;
        }

        @Override
        public String getDefault() {
            return strDefaultVal;
        }


    }

    /** Instantiates a ForgePreferences object. */
    public ForgeNetPreferences() {
        super(ForgeConstants.SERVER_PREFS_FILE, FNetPref.class);
    }

    @Override
    protected FNetPref[] getEnumValues() {
        return FNetPref.values();
    }

    @Override
    protected FNetPref valueOf(final String name) {
        try {
            return FNetPref.valueOf(name);
        }
        catch (final Exception e) {
            return null;
        }
    }

    @Override
    protected String getPrefDefault(final FNetPref key) {
        return key.getDefault();
    }

}

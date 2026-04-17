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
package forge.gamemodes.match;

import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable snapshot of a player's yield-related preferences, used for bulk network sync. */
public final class YieldPrefs implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Boolean interrupt FPrefs captured in a bulk snapshot. */
    static final FPref[] INTERRUPT_PREFS = {
        FPref.YIELD_INTERRUPT_ON_ATTACKERS,
        FPref.YIELD_INTERRUPT_ON_TARGETING,
        FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL,
        FPref.YIELD_INTERRUPT_ON_TRIGGERS,
        FPref.YIELD_INTERRUPT_ON_REVEAL,
        FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL,
        FPref.YIELD_AUTO_PASS_NO_ACTIONS,
    };

    private final YieldMode mode;
    private final boolean onAttackers;
    private final boolean onTargeting;
    private final boolean onOpponentSpell;
    private final boolean onTriggers;
    private final boolean onReveal;
    private final boolean onMassRemoval;
    private final boolean autoPassNoActions;

    private YieldPrefs(YieldMode mode, boolean onAttackers, boolean onTargeting,
                       boolean onOpponentSpell, boolean onTriggers,
                       boolean onReveal, boolean onMassRemoval, boolean autoPassNoActions) {
        this.mode = mode == null ? YieldMode.NONE : mode;
        this.onAttackers = onAttackers;
        this.onTargeting = onTargeting;
        this.onOpponentSpell = onOpponentSpell;
        this.onTriggers = onTriggers;
        this.onReveal = onReveal;
        this.onMassRemoval = onMassRemoval;
        this.autoPassNoActions = autoPassNoActions;
    }

    /** Snapshot from an IGameController (controller-layer state). */
    public YieldPrefs(IGameController controller) {
        this(
            controller.getYieldMode(),
            controller.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_ATTACKERS),
            controller.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_TARGETING),
            controller.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL),
            controller.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_TRIGGERS),
            controller.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_REVEAL),
            controller.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL),
            controller.getYieldInterruptPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS)
        );
    }

    /** Snapshot from local ForgePreferences (used at game-start before a controller exists). */
    public static YieldPrefs fromCurrentPreferences() {
        ForgePreferences prefs = FModel.getPreferences();
        return new YieldPrefs(
            YieldMode.NONE,
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_ATTACKERS),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_TARGETING),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_TRIGGERS),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_REVEAL),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL),
            prefs.getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS)
        );
    }

    public YieldMode getMode() { return mode; }

    /** Returns false if {@code pref} is not a recognized yield interrupt key. */
    public boolean getInterrupt(FPref pref) {
        return switch (pref) {
            case YIELD_INTERRUPT_ON_ATTACKERS -> onAttackers;
            case YIELD_INTERRUPT_ON_TARGETING -> onTargeting;
            case YIELD_INTERRUPT_ON_OPPONENT_SPELL -> onOpponentSpell;
            case YIELD_INTERRUPT_ON_TRIGGERS -> onTriggers;
            case YIELD_INTERRUPT_ON_REVEAL -> onReveal;
            case YIELD_INTERRUPT_ON_MASS_REMOVAL -> onMassRemoval;
            case YIELD_AUTO_PASS_NO_ACTIONS -> autoPassNoActions;
            default -> false;
        };
    }

    /** Returns a read-only view of the boolean interrupt prefs keyed by FPref. */
    public Map<FPref, Boolean> getInterrupts() {
        EnumMap<FPref, Boolean> map = new EnumMap<>(FPref.class);
        for (FPref pref : INTERRUPT_PREFS) {
            map.put(pref, getInterrupt(pref));
        }
        return Collections.unmodifiableMap(map);
    }
}

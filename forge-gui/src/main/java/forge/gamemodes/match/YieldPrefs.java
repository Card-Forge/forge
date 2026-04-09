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

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

import java.io.Serializable;

/**
 * Immutable snapshot of a player's yield-related preferences.
 *
 * <p>In network play the host needs to know each remote client's interrupt
 * preferences (e.g. "interrupt my yield when an opponent casts a spell")
 * to evaluate yield decisions on the host side. The client serializes
 * its preferences into this value type and sends it via
 * {@code IGameController.notifyYieldStateChanged}; the host stores it on
 * the per-player {@code NetGuiGame} and reads it through
 * {@code IGuiGame.getRemoteYieldPrefs}.
 *
 * <p>The host's own preferences are read directly from
 * {@code FModel.getPreferences()} — this snapshot is only consulted when
 * {@code IGuiGame.isRemoteGuiProxy()} is true.
 */
public final class YieldPrefs implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean onAttackers;
    private final boolean onTargeting;
    private final boolean onOpponentSpell;
    private final boolean onTriggers;
    private final boolean onReveal;
    private final boolean onMassRemoval;
    private final boolean autoPassNoActions;
    private final String stackYieldScope;
    private final String noActionsScope;

    private YieldPrefs(boolean onAttackers, boolean onTargeting,
                       boolean onOpponentSpell, boolean onTriggers,
                       boolean onReveal, boolean onMassRemoval, boolean autoPassNoActions,
                       String stackYieldScope, String noActionsScope) {
        this.onAttackers = onAttackers;
        this.onTargeting = onTargeting;
        this.onOpponentSpell = onOpponentSpell;
        this.onTriggers = onTriggers;
        this.onReveal = onReveal;
        this.onMassRemoval = onMassRemoval;
        this.autoPassNoActions = autoPassNoActions;
        this.stackYieldScope = stackYieldScope;
        this.noActionsScope = noActionsScope;
    }

    /**
     * Snapshot the current yield preferences from {@code FModel.getPreferences()}.
     * Called by network clients when sending state to the host.
     */
    public static YieldPrefs fromCurrentPreferences() {
        ForgePreferences prefs = FModel.getPreferences();
        return new YieldPrefs(
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_ATTACKERS),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_TARGETING),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_TRIGGERS),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_REVEAL),
            prefs.getPrefBoolean(FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL),
            prefs.getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS),
            prefs.getPref(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD),
            prefs.getPref(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS)
        );
    }

    /**
     * Look up the value for one of the yield interrupt preferences.
     * Returns false if {@code pref} is not a yield interrupt or auto-pass key.
     */
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

    public String getStackYieldScope() { return stackYieldScope; }
    public String getNoActionsScope() { return noActionsScope; }
}

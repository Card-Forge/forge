package forge.web;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.primitives.Ints;
import forge.MulliganDefs;
import forge.StaticData;
import forge.ai.AiProfileUtil;
import forge.game.GameLogVerbosity;
import forge.game.phase.PhaseType;
import forge.gamemodes.match.DeclineScope;
import forge.gamemodes.match.YieldController;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.sound.SoundSystem;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.web.protocol.ToBrowser.ServerSettings;
import forge.web.protocol.ToBrowser.ShortcutPref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** What the options dialog reads and writes: a player's own settings (see {@link PlayerSettings}) and the phase stops. */
public final class Options implements IHasForgeLog {
    private static final Map<String, FPref> BOOLEAN_PREFS = Map.ofEntries(
            Map.entry("interruptAttackers", FPref.YIELD_INTERRUPT_ON_ATTACKERS),
            Map.entry("interruptOpponentSpell", FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL),
            Map.entry("interruptTargeting", FPref.YIELD_INTERRUPT_ON_TARGETING),
            Map.entry("interruptTriggers", FPref.YIELD_INTERRUPT_ON_TRIGGERS),
            Map.entry("interruptMassRemoval", FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL),
            Map.entry("interruptReveal", FPref.YIELD_INTERRUPT_ON_REVEAL),
            Map.entry("suppressAfterYield", FPref.YIELD_SUPPRESS_AFTER_END),
            Map.entry("suppressOwnTurn", FPref.YIELD_SUPPRESS_ON_OWN_TURN),
            Map.entry("autoPassNoActions", FPref.YIELD_AUTO_PASS_NO_ACTIONS));
    /** When each yield suggestion is offered again after it is declined, by its DeclineScope name. */
    private static final Map<String, FPref> SCOPE_PREFS = Map.of(
            "suggestStackYield", FPref.YIELD_DECLINE_SCOPE_STACK_YIELD,
            "suggestNoActions", FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS);
    /** Forge's keyboard shortcut preferences. The page offers each one where it has the matching action. */
    private static final List<FPref> SHORTCUTS = Arrays.stream(FPref.values()).filter(p -> p.name().startsWith("SHORTCUT_")).toList();
    /** The sound sets in Forge's sound folder. They are read only once, because every settings update sends them. */
    private static final Supplier<List<String>> SOUND_SETS = Suppliers.memoize(() -> List.of(SoundSystem.instance.getAvailableSoundSets()));
    private static final List<String> TABLE_SETTINGS = List.of("filteredHands", "mulliganRule", "aiProfile", "ante", "undoMode");
    private static final List<String> UNDO_MODES = List.of(ForgeConstants.UNDO_MODE_OFF, ForgeConstants.UNDO_MODE_SINGLE, ForgeConstants.UNDO_MODE_FREE);

    private Options() {
    }

    public static ServerSettings values(final PlayerSettings player) {
        return new ServerSettings(
                player.getBoolean(FPref.YIELD_INTERRUPT_ON_ATTACKERS),
                player.getBoolean(FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL),
                player.getBoolean(FPref.YIELD_INTERRUPT_ON_TARGETING),
                player.getBoolean(FPref.YIELD_INTERRUPT_ON_TRIGGERS),
                player.getBoolean(FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL),
                player.getBoolean(FPref.YIELD_INTERRUPT_ON_REVEAL),
                DeclineScope.fromPref(player.get(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD)).name(),
                DeclineScope.fromPref(player.get(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS)).name(),
                player.getBoolean(FPref.YIELD_SUPPRESS_AFTER_END),
                player.getBoolean(FPref.YIELD_SUPPRESS_ON_OWN_TURN),
                ForgeConstants.AUTO_DECISION_PER_CARD.equals(player.get(FPref.UI_AUTO_DECISION_MODE)) ? "card" : "ability",
                GameLogVerbosity.fromString(player.get(FPref.DEV_LOG_ENTRY_TYPE)),
                player.getBoolean(FPref.DEV_MODE_ENABLED),
                "#" + player.get(FPref.UI_ACTIONABLE_HIGHLIGHT_COLOR),
                // A volume of zero means off, so the on/off preference and the volume are reported as one number
                player.getBoolean(FPref.UI_ENABLE_SOUNDS) ? player.getInt(FPref.UI_VOL_SOUNDS) : 0,
                player.getBoolean(FPref.UI_ENABLE_MUSIC) ? player.getInt(FPref.UI_VOL_MUSIC) : 0,
                // The host serves every player's sounds, so a guest is sent the host's sound set, not its own
                FModel.getPreferences().getPref(FPref.UI_CURRENT_SOUND_SET),
                SOUND_SETS.get(),
                player.getBoolean(FPref.FILTERED_HANDS),
                MulliganDefs.GetRuleByName(player.get(FPref.MULLIGAN_RULE)).name(),
                Arrays.stream(MulliganDefs.MulliganRule.values()).map(Enum::name).toList(),
                player.get(FPref.UI_CURRENT_AI_PROFILE),
                AiProfileUtil.getProfilesDisplayList(),
                player.getBoolean(FPref.UI_ANTE),
                player.get(FPref.UI_UNDO_MODE),
                player.getBoolean(FPref.UI_OVERLAY_DRAFT_RANKING),
                SHORTCUTS.stream().map(p -> new ShortcutPref(p.name(), Localizer.getInstance().getMessage("lbl" + p.name()), player.get(p), p.getDefault())).toList());
    }

    /** Saves a setting. An illegal value is dropped, because it could reach the preferences file that the desktop client also uses. */
    public static void set(final PlayerSettings player, final IGameController controller, final String key, final String value) {
        final FPref pref = BOOLEAN_PREFS.get(key);
        if (pref != null) {
            // The host decides from its own copy of these, set when the game opened, so a change during a game must reach that copy too
            setEverywhere(player, controller, pref, String.valueOf(Boolean.parseBoolean(value)));
        } else if (SCOPE_PREFS.containsKey(key)) {
            setEverywhere(player, controller, SCOPE_PREFS.get(key), DeclineScope.fromPref(value).name());
        } else if ("devMode".equals(key)) {
            final boolean on = Boolean.parseBoolean(value);
            player.set(FPref.DEV_MODE_ENABLED, on);
            // Desktop also keeps this setting in a static field, which the draft code and debug output read
            if (player.shared()) {
                ForgePreferences.DEV_MODE = on;
            }
        } else if (TABLE_SETTINGS.contains(key)) {
            // These are the rules of the host's table, so a guest's browser cannot change them
            if (!player.shared() || !setTableRule(player, key, value)) {
                return;
            }
        } else if ("autoYieldMode".equals(key)) {
            setEverywhere(player, controller, FPref.UI_AUTO_DECISION_MODE,
                    "card".equals(value) ? ForgeConstants.AUTO_DECISION_PER_CARD : ForgeConstants.AUTO_DECISION_PER_ABILITY);
        } else if (SHORTCUTS.stream().anyMatch(s -> s.name().equals(key))) {
            // Use Forge's format of Java key codes separated by spaces, because desktop reads the same preference
            if (!value.matches("(\\d{1,5}( \\d{1,5})*)?")) {
                netLog.warn("Web client: bad shortcut {}", value);
                return;
            }
            player.set(FPref.valueOf(key), value);
        } else if ("draftRanking".equals(key)) {
            player.set(FPref.UI_OVERLAY_DRAFT_RANKING, Boolean.parseBoolean(value));
        } else if ("logDetail".equals(key)) {
            player.set(FPref.DEV_LOG_ENTRY_TYPE, GameLogVerbosity.fromString(value).toString());
        } else if ("soundSet".equals(key)) {
            // The host serves the sounds, so only its own choice counts
            if (!player.shared()) {
                return;
            }
            if (!SOUND_SETS.get().contains(value)) {
                netLog.warn("Web client: bad sound set {}", value);
                return;
            }
            player.set(FPref.UI_CURRENT_SOUND_SET, value);
            SoundSystem.instance.invalidateSoundCache();
        } else if ("soundVolume".equals(key) || "musicVolume".equals(key)) {
            final Integer volume = Ints.tryParse(value);
            if (volume == null || volume < 0 || volume > 100) {
                netLog.warn("Web client: bad volume {}", value);
                return;
            }
            final boolean effects = "soundVolume".equals(key);
            player.set(effects ? FPref.UI_VOL_SOUNDS : FPref.UI_VOL_MUSIC, value);
            // A volume of zero turns the sound off, because the desktop client reads the on/off preference, not the volume
            player.set(effects ? FPref.UI_ENABLE_SOUNDS : FPref.UI_ENABLE_MUSIC, volume > 0);
        } else {
            netLog.warn("Web client: unknown setting {}", key);
            return;
        }
        player.save();
    }

    /** Sets one of the host's table rules. Returns false for a value Forge does not know. */
    private static boolean setTableRule(final PlayerSettings player, final String key, final String value) {
        switch (key) {
            case "filteredHands" -> {
                final boolean on = Boolean.parseBoolean(value);
                player.set(FPref.FILTERED_HANDS, on);
                // Forge copies this into StaticData only when it starts, which is why desktop asks for a restart
                StaticData.instance().setFilteredHandsEnabled(on);
            }
            case "mulliganRule" -> {
                final MulliganDefs.MulliganRule rule = Arrays.stream(MulliganDefs.MulliganRule.values())
                        .filter(r -> r.name().equals(value)).findFirst().orElse(null);
                if (rule == null) {
                    netLog.warn("Web client: bad mulligan rule {}", value);
                    return false;
                }
                player.set(FPref.MULLIGAN_RULE, rule.name());
                StaticData.instance().setMulliganRule(rule);
            }
            case "aiProfile" -> {
                if (!AiProfileUtil.getProfilesDisplayList().contains(value)) {
                    netLog.warn("Web client: bad AI profile {}", value);
                    return false;
                }
                player.set(FPref.UI_CURRENT_AI_PROFILE, value);
            }
            case "undoMode" -> {
                if (!UNDO_MODES.contains(value)) {
                    netLog.warn("Web client: bad undo mode {}", value);
                    return false;
                }
                // The engine reads the host's preference at each priority prompt, so a change applies in a running game
                player.set(FPref.UI_UNDO_MODE, value);
            }
            default -> player.set(FPref.UI_ANTE, Boolean.parseBoolean(value));
        }
        return true;
    }

    /** Sets a setting that the game reads as well as the player. It sets the player's own copy, and the game's copy if there is a game. */
    private static void setEverywhere(final PlayerSettings player, final IGameController controller, final FPref pref,
            final String value) {
        player.set(pref, value);
        if (controller != null) {
            applyTo(controller, pref, value);
        }
    }

    /** Gives a game this player's settings, with two exceptions. Auto-pass always respects the interrupts. Playable cards and the lands that auto-tap would use are always highlighted. */
    public static void applyAll(final PlayerSettings player, final IGameController controller) {
        // Each setting is pushed after the engine sets its first values from the shared preferences, so a guest plays by its own settings
        for (final FPref pref : YieldController.SYNCED_PREFS) {
            final boolean always = pref == FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS || pref == FPref.UI_SHOW_ACTIONABLE_HIGHLIGHTS
                    || pref == FPref.UI_SHOW_AUTOTAP_PREVIEW;
            applyTo(controller, pref, always ? "true" : player.get(pref));
        }
        applyTo(controller, FPref.UI_AUTO_DECISION_MODE, player.get(FPref.UI_AUTO_DECISION_MODE));
    }

    private static void applyTo(final IGameController controller, final FPref pref, final String value) {
        controller.getYieldController().setPref(pref, value);
        // The host's engine keeps no copy of the auto-yield mode. The client decides how a choice is stored
        if (pref != FPref.UI_AUTO_DECISION_MODE) {
            controller.setYieldPref(pref, value);
        }
    }

    /** The phases that have a stop in one row of the stop grid. Untap never has a stop, as on desktop. */
    public static List<PhaseType> stops(final PlayerSettings player, final FPref[] keys) {
        final List<PhaseType> out = new ArrayList<>();
        final PhaseType[] phases = PhaseType.values();
        for (int i = 1; i < phases.length; i++) {
            if (player.getBoolean(keys[i - 1])) {
                out.add(phases[i]);
            }
        }
        return out;
    }

    /** Sets every stop in one row: the listed phases on and the rest off, or Forge's default stops when phases is null. Returns the phases that changed. */
    public static List<PhaseType> setStops(final PlayerSettings player, final boolean mine, final List<PhaseType> phases) {
        final List<PhaseType> changed = new ArrayList<>();
        for (final PhaseType phase : PhaseType.values()) {
            final boolean stop = phases == null ? phase.ordinal() > 0 && Boolean.parseBoolean(stopKey(phase, mine).getDefault()) : phases.contains(phase);
            if (phase.ordinal() > 0 && player.getBoolean(stopKey(phase, mine)) != stop) {
                player.set(stopKey(phase, mine), stop);
                changed.add(phase);
            }
        }
        if (!changed.isEmpty()) {
            player.save();
        }
        return changed;
    }

    public static FPref stopKey(final PhaseType phase, final boolean mine) {
        return (mine ? FPref.PHASES_HUMAN : FPref.PHASES_AI)[phase.ordinal() - 1];
    }
}

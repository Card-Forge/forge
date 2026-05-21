package forge.screens.home.welcome;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import forge.localinstance.properties.ForgePreferences.FPref;

public final class OnboardingPresets {
    private OnboardingPresets() {}

    public static final Map<FPref, String> AI_CASUAL = build(
            FPref.UI_OVERLAY_DRAFT_RANKING, "true",
            FPref.UI_ORDER_HAND, "true",
            FPref.YIELD_AUTO_PASS_NO_ACTIONS, "true",
            FPref.UI_REMIND_ON_PRIORITY, "true",
            FPref.UI_SHOW_STORM_COUNT_IN_PROMPT, "true",
            FPref.UI_DETAILED_SPELLDESC_IN_PROMPT, "true");
    public static final Map<FPref, String> AI_EXPERT = build(
            FPref.UI_OVERLAY_DRAFT_RANKING, "false",
            FPref.UI_ORDER_HAND, "false",
            FPref.YIELD_AUTO_PASS_NO_ACTIONS, "false",
            FPref.UI_REMIND_ON_PRIORITY, "false",
            FPref.UI_SHOW_STORM_COUNT_IN_PROMPT, "false",
            FPref.UI_DETAILED_SPELLDESC_IN_PROMPT, "false");

    public static final Map<FPref, String> LAYOUT_DEFAULT = build(
            FPref.UI_GROUP_PERMANENTS, "default");
    public static final Map<FPref, String> LAYOUT_COMPACT = build(
            FPref.UI_GROUP_PERMANENTS, "group_all");

    public static final Map<FPref, String> OVERLAYS_NONE = build(
            FPref.UI_SHOW_CARD_OVERLAYS, "false",
            FPref.UI_TARGETING_OVERLAY, "0");
    public static final Map<FPref, String> OVERLAYS_ON = build(
            FPref.UI_SHOW_CARD_OVERLAYS, "true",
            FPref.UI_TARGETING_OVERLAY, "2");

    private static Map<FPref, String> build(Object... kvs) {
        EnumMap<FPref, String> m = new EnumMap<>(FPref.class);
        for (int i = 0; i < kvs.length; i += 2) {
            m.put((FPref) kvs[i], (String) kvs[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }
}

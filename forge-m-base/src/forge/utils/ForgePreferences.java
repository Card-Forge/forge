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
package forge.utils;

import forge.ai.AiProfileUtil;
import forge.game.GameLogEntryType;
import forge.game.phase.PhaseType;
import forge.screens.match.FControl;
import forge.screens.match.views.VPhaseIndicator;

public class ForgePreferences extends PreferencesStore<ForgePreferences.FPref> {
    /**
     * Preference identifiers, and their default values.
     */
    public static enum FPref {
        PLAYER_NAME (""),
        CONSTRUCTED_P1_DECK_STATE(""),
        CONSTRUCTED_P2_DECK_STATE(""),
        CONSTRUCTED_P3_DECK_STATE(""),
        CONSTRUCTED_P4_DECK_STATE(""),
        CONSTRUCTED_P5_DECK_STATE(""),
        CONSTRUCTED_P6_DECK_STATE(""),
        CONSTRUCTED_P7_DECK_STATE(""),
        CONSTRUCTED_P8_DECK_STATE(""),
        UI_RANDOM_FOIL ("false"),
        UI_ENABLE_AI_CHEATS ("false"),
        UI_AVATARS ("0,1"),
        UI_SHOW_CARD_OVERLAYS ("true"),
        UI_OVERLAY_CARD_NAME ("true"),
        UI_OVERLAY_CARD_POWER ("true"),
        UI_OVERLAY_CARD_MANA_COST ("true"),
        UI_OVERLAY_CARD_ID ("true"),
        UI_OVERLAY_FOIL_EFFECT ("true"),
        UI_HIDE_REMINDER_TEXT ("false"),
        UI_UPLOAD_DRAFT ("false"),
        UI_RANDOM_ART_IN_POOLS ("true"),
        UI_BUGZ_NAME (""),
        UI_BUGZ_PWD (""),
        UI_ANTE ("false"),
        UI_MANABURN("false"),
        UI_SKIN ("Default"),
        UI_PREFERRED_AVATARS_ONLY ("false"),
        UI_TARGETING_OVERLAY ("false"),
        UI_ENABLE_SOUNDS ("true"),
        UI_ALT_SOUND_SYSTEM ("false"),
        UI_CURRENT_AI_PROFILE (AiProfileUtil.AI_PROFILE_RANDOM_MATCH),
        UI_CLONE_MODE_SOURCE ("false"),
        UI_MATCH_IMAGE_VISIBLE ("true"),

        UI_FOR_TOUCHSCREN("false"),

        MATCHPREF_PROMPT_FREE_BLOCKS("false"),

        ENFORCE_DECK_LEGALITY ("true"),

        DEV_MODE_ENABLED ("false"),
//        DEV_MILLING_LOSS ("true"),
        DEV_UNLIMITED_LAND ("false"),
        DEV_LOG_ENTRY_TYPE (GameLogEntryType.DAMAGE.toString()),

        DECK_DEFAULT_CARD_LIMIT ("4"),
        DECKGEN_SINGLETONS ("false"),
        DECKGEN_ARTIFACTS ("false"),
        DECKGEN_NOSMALL ("false"),

        PHASE_AI_UPKEEP ("true"),
        PHASE_AI_DRAW ("true"),
        PHASE_AI_MAIN1 ("true"),
        PHASE_AI_BEGINCOMBAT ("true"),
        PHASE_AI_DECLAREATTACKERS ("true"),
        PHASE_AI_DECLAREBLOCKERS ("true"),
        PHASE_AI_FIRSTSTRIKE ("true"),
        PHASE_AI_COMBATDAMAGE ("true"),
        PHASE_AI_ENDCOMBAT ("true"),
        PHASE_AI_MAIN2 ("true"),
        PHASE_AI_EOT ("true"),
        PHASE_AI_CLEANUP ("true"),

        PHASE_HUMAN_UPKEEP ("true"),
        PHASE_HUMAN_DRAW ("true"),
        PHASE_HUMAN_MAIN1 ("true"),
        PHASE_HUMAN_BEGINCOMBAT ("true"),
        PHASE_HUMAN_DECLAREATTACKERS ("true"),
        PHASE_HUMAN_DECLAREBLOCKERS ("true"),
        PHASE_HUMAN_FIRSTSTRIKE ("true"),
        PHASE_HUMAN_COMBATDAMAGE ("true"),
        PHASE_HUMAN_ENDCOMBAT ("true"),
        PHASE_HUMAN_MAIN2 ("true"),
        PHASE_HUMAN_EOT ("true"),
        PHASE_HUMAN_CLEANUP ("true");

        private final String strDefaultVal;

        /** @param s0 &emsp; {@link java.lang.String} */
        FPref(String s0) {
            this.strDefaultVal = s0;
        }

        /** @return {@link java.lang.String} */
        public String getDefault() {
            return strDefaultVal;
        }
    }

    /** Instantiates a ForgePreferences object. */
    public ForgePreferences() {
        super(Constants.MAIN_PREFS_FILE, FPref.class);
    }

    protected FPref[] getEnumValues() {
        return FPref.values();
    }

    protected FPref valueOf(String name) {
        try {
            return FPref.valueOf(name);
        }
        catch (Exception e) {
            return null;
        }
    }

    protected String getPrefDefault(FPref key) {
        return key.getDefault();
    }

    public void writeMatchPreferences() {
        VPhaseIndicator fvAi = FControl.getView().getTopPlayerPanel().getPhaseIndicator();
        this.setPref(FPref.PHASE_AI_UPKEEP, String.valueOf(fvAi.getLabel(PhaseType.UPKEEP).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_DRAW, String.valueOf(fvAi.getLabel(PhaseType.DRAW).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_MAIN1, String.valueOf(fvAi.getLabel(PhaseType.MAIN1).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_BEGINCOMBAT, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_BEGIN).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_DECLAREATTACKERS, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_DECLAREBLOCKERS, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_FIRSTSTRIKE, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_COMBATDAMAGE, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_DAMAGE).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_ENDCOMBAT, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_END).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_MAIN2, String.valueOf(fvAi.getLabel(PhaseType.MAIN2).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_EOT, String.valueOf(fvAi.getLabel(PhaseType.END_OF_TURN).getStopAtPhase()));
        this.setPref(FPref.PHASE_AI_CLEANUP, String.valueOf(fvAi.getLabel(PhaseType.CLEANUP).getStopAtPhase()));

        VPhaseIndicator fvHuman = FControl.getView().getBottomPlayerPanel().getPhaseIndicator();
        this.setPref(FPref.PHASE_HUMAN_UPKEEP, String.valueOf(fvHuman.getLabel(PhaseType.UPKEEP).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_DRAW, String.valueOf(fvHuman.getLabel(PhaseType.DRAW).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_MAIN1, String.valueOf(fvHuman.getLabel(PhaseType.MAIN1).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_BEGINCOMBAT, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_BEGIN).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_DECLAREATTACKERS, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_DECLAREBLOCKERS, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_FIRSTSTRIKE, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_COMBATDAMAGE, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_DAMAGE).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_ENDCOMBAT, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_END).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_MAIN2, String.valueOf(fvHuman.getLabel(PhaseType.MAIN2).getStopAtPhase()));
        this.setPref(FPref.PHASE_HUMAN_EOT, fvHuman.getLabel(PhaseType.END_OF_TURN).getStopAtPhase());
        this.setPref(FPref.PHASE_HUMAN_CLEANUP, fvHuman.getLabel(PhaseType.CLEANUP).getStopAtPhase());

        /*final VDev v = VDev.SINGLETON_INSTANCE;

        // this.setPref(FPref.DEV_MILLING_LOSS, v.getLblMilling().getEnabled());
        this.setPref(FPref.DEV_UNLIMITED_LAND, v.getLblUnlimitedLands().getEnabled());*/
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in
     * codebase.
     */
    public void actuateMatchPreferences() {
        VPhaseIndicator fvAi = FControl.getView().getTopPlayerPanel().getPhaseIndicator();
        fvAi.getLabel(PhaseType.UPKEEP).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_UPKEEP));
        fvAi.getLabel(PhaseType.DRAW).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_DRAW));
        fvAi.getLabel(PhaseType.MAIN1).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_MAIN1));
        fvAi.getLabel(PhaseType.COMBAT_BEGIN).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_BEGINCOMBAT));
        fvAi.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_DECLAREATTACKERS));
        fvAi.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_DECLAREBLOCKERS));
        fvAi.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_FIRSTSTRIKE));
        fvAi.getLabel(PhaseType.COMBAT_DAMAGE).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_COMBATDAMAGE));
        fvAi.getLabel(PhaseType.COMBAT_END).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_ENDCOMBAT));
        fvAi.getLabel(PhaseType.MAIN2).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_MAIN2));
        fvAi.getLabel(PhaseType.END_OF_TURN).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_EOT));
        fvAi.getLabel(PhaseType.CLEANUP).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_AI_CLEANUP));

        VPhaseIndicator fvHuman = FControl.getView().getBottomPlayerPanel().getPhaseIndicator();
        fvHuman.getLabel(PhaseType.UPKEEP).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_UPKEEP));
        fvHuman.getLabel(PhaseType.DRAW).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_DRAW));
        fvHuman.getLabel(PhaseType.MAIN1).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_MAIN1));
        fvHuman.getLabel(PhaseType.COMBAT_BEGIN).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_BEGINCOMBAT));
        fvHuman.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREATTACKERS));
        fvHuman.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREBLOCKERS));
        fvHuman.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_FIRSTSTRIKE));
        fvHuman.getLabel(PhaseType.COMBAT_DAMAGE).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_COMBATDAMAGE));
        fvHuman.getLabel(PhaseType.COMBAT_END).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_ENDCOMBAT));
        fvHuman.getLabel(PhaseType.MAIN2).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_MAIN2));
        fvHuman.getLabel(PhaseType.END_OF_TURN).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_EOT));
        fvHuman.getLabel(PhaseType.CLEANUP).setStopAtPhase(this.getPrefBoolean(FPref.PHASE_HUMAN_CLEANUP));
    }
}

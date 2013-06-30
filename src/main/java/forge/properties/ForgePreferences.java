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
package forge.properties;

import java.util.List;

import forge.Constant;
import forge.Constant.Preferences;
import forge.game.ai.AiProfileUtil;
import forge.gui.home.EMenuItem;
import forge.gui.match.VMatchUI;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.views.VDev;
import forge.gui.toolbox.special.PhaseIndicator;

public class ForgePreferences extends PreferencesStore<ForgePreferences.FPref> {
    /** 
     * Preference identifiers, and their default values.
     */
    public static enum FPref {
        UI_USE_OLD ("false"),
        UI_RANDOM_FOIL ("false"),
        UI_SMOOTH_LAND ("false"),
        UI_AVATARS ("0,1"),
        UI_CARD_OVERLAY ("true"),
        UI_UPLOAD_DRAFT ("false"),
        UI_SCALE_LARGER ("true"),
        UI_MAX_STACK ("3"),
        UI_STACK_OFFSET ("tiny"),
        UI_CARD_SIZE ("small"),
        UI_BUGZ_NAME (""),
        UI_BUGZ_PWD (""),
        UI_ANTE ("false"),
        UI_MANABURN("false"),
        UI_SKIN ("default"),
        UI_PREFERRED_AVATARS_ONLY ("false"),
        UI_TARGETING_OVERLAY ("false"),
        UI_ENABLE_SOUNDS ("true"),
        UI_ALT_SOUND_SYSTEM ("false"),
        UI_RANDOM_CARD_ART ("false"),
        UI_CURRENT_AI_PROFILE (AiProfileUtil.AI_PROFILE_RANDOM_MATCH),
        UI_CLONE_MODE_SOURCE ("false"), /** */
        
        UI_FOR_TOUCHSCREN("false"),

        SUBMENU_CURRENTMENU (EMenuItem.CONSTRUCTED.toString()),
        SUBMENU_SANCTIONED ("false"),
        SUBMENU_GAUNTLET ("false"),
        SUBMENU_VARIANT ("false"),
        SUBMENU_QUEST ("false"),
        SUBMENU_SETTINGS ("false"),
        SUBMENU_UTILITIES ("false"),

        ENFORCE_DECK_LEGALITY ("true"),

        DEV_MODE_ENABLED ("false"),
        DEV_MILLING_LOSS ("true"),
        DEV_UNLIMITED_LAND ("false"),

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
        PHASE_HUMAN_CLEANUP ("true"),

        SHORTCUT_SHOWSTACK ("83"),
        SHORTCUT_SHOWCOMBAT ("67"),
        SHORTCUT_SHOWCONSOLE ("76"),
        SHORTCUT_SHOWPLAYERS ("80"),
        SHORTCUT_SHOWDEV ("68"),
        SHORTCUT_CONCEDE ("17"),
        SHORTCUT_ENDTURN ("69"),
        SHORTCUT_ALPHASTRIKE ("65"),
        SHORTCUT_SHOWTARGETING ("84");

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

    public static enum CardSizeType {
        tiny, smaller, small, medium, large, huge
    }

   
    public static enum StackOffsetType {
        tiny, small, medium, large
    }

   
    public static enum HomeMenus {
        constructed, draft, sealed, quest, settings, utilities
    }

    /** Instantiates a ForgePreferences object. */
    public ForgePreferences() {
        super(NewConstants.MAIN_PREFS_FILE, FPref.class);
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in
     * codebase.
     */
    public void writeMatchPreferences() {
        final List<VField> fieldViews = VMatchUI.SINGLETON_INSTANCE.getFieldViews();

        // AI field is at index [1]
        PhaseIndicator fvAi = fieldViews.get(1).getPhaseInidicator();
        this.setPref(FPref.PHASE_AI_UPKEEP, String.valueOf(fvAi.getLblUpkeep().getEnabled()));
        this.setPref(FPref.PHASE_AI_DRAW, String.valueOf(fvAi.getLblDraw().getEnabled()));
        this.setPref(FPref.PHASE_AI_MAIN1, String.valueOf(fvAi.getLblMain1().getEnabled()));
        this.setPref(FPref.PHASE_AI_BEGINCOMBAT, String.valueOf(fvAi.getLblBeginCombat().getEnabled()));
        this.setPref(FPref.PHASE_AI_DECLAREATTACKERS, String.valueOf(fvAi.getLblDeclareAttackers().getEnabled()));
        this.setPref(FPref.PHASE_AI_DECLAREBLOCKERS, String.valueOf(fvAi.getLblDeclareBlockers().getEnabled()));
        this.setPref(FPref.PHASE_AI_FIRSTSTRIKE, String.valueOf(fvAi.getLblFirstStrike().getEnabled()));
        this.setPref(FPref.PHASE_AI_COMBATDAMAGE, String.valueOf(fvAi.getLblCombatDamage().getEnabled()));
        this.setPref(FPref.PHASE_AI_ENDCOMBAT, String.valueOf(fvAi.getLblEndCombat().getEnabled()));
        this.setPref(FPref.PHASE_AI_MAIN2, String.valueOf(fvAi.getLblMain2().getEnabled()));
        this.setPref(FPref.PHASE_AI_EOT, String.valueOf(fvAi.getLblEndTurn().getEnabled()));
        this.setPref(FPref.PHASE_AI_CLEANUP, String.valueOf(fvAi.getLblCleanup().getEnabled()));

        // Human field is at index [0]
        PhaseIndicator fvHuman = fieldViews.get(0).getPhaseInidicator();
        this.setPref(FPref.PHASE_HUMAN_UPKEEP, String.valueOf(fvHuman.getLblUpkeep().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_DRAW, String.valueOf(fvHuman.getLblDraw().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_MAIN1, String.valueOf(fvHuman.getLblMain1().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_BEGINCOMBAT, String.valueOf(fvHuman.getLblBeginCombat().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_DECLAREATTACKERS, String.valueOf(fvHuman.getLblDeclareAttackers().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_DECLAREBLOCKERS, String.valueOf(fvHuman.getLblDeclareBlockers().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_FIRSTSTRIKE, String.valueOf(fvHuman.getLblFirstStrike().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_COMBATDAMAGE, String.valueOf(fvHuman.getLblCombatDamage().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_ENDCOMBAT, String.valueOf(fvHuman.getLblEndCombat().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_MAIN2, String.valueOf(fvHuman.getLblMain2().getEnabled()));
        this.setPref(FPref.PHASE_HUMAN_EOT, fvHuman.getLblEndTurn().getEnabled());
        this.setPref(FPref.PHASE_HUMAN_CLEANUP, fvHuman.getLblCleanup().getEnabled());

        final VDev v = VDev.SINGLETON_INSTANCE;

        this.setPref(FPref.DEV_MILLING_LOSS, v.getLblMilling().getEnabled());
        this.setPref(FPref.DEV_UNLIMITED_LAND, v.getLblUnlimitedLands().getEnabled());
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in
     * codebase.
     */
    public void actuateMatchPreferences() {
        final List<VField> fieldViews = VMatchUI.SINGLETON_INSTANCE.getFieldViews();

        Preferences.DEV_MODE = this.getPrefBoolean(FPref.DEV_MODE_ENABLED);
        Preferences.UPLOAD_DRAFT = Constant.Runtime.NET_CONN && this.getPrefBoolean(FPref.UI_UPLOAD_DRAFT);

        // AI field is at index [0]
        PhaseIndicator fvAi = fieldViews.get(1).getPhaseInidicator();
        fvAi.getLblUpkeep().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_UPKEEP));
        fvAi.getLblDraw().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_DRAW));
        fvAi.getLblMain1().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_MAIN1));
        fvAi.getLblBeginCombat().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_BEGINCOMBAT));
        fvAi.getLblDeclareAttackers().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_DECLAREATTACKERS));
        fvAi.getLblDeclareBlockers().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_DECLAREBLOCKERS));
        fvAi.getLblFirstStrike().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_FIRSTSTRIKE));
        fvAi.getLblCombatDamage().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_COMBATDAMAGE));
        fvAi.getLblEndCombat().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_ENDCOMBAT));
        fvAi.getLblMain2().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_MAIN2));
        fvAi.getLblEndTurn().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_EOT));
        fvAi.getLblCleanup().setEnabled(this.getPrefBoolean(FPref.PHASE_AI_CLEANUP));

        // Human field is at index [1]
        PhaseIndicator fvHuman = fieldViews.get(0).getPhaseInidicator();
        fvHuman.getLblUpkeep().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_UPKEEP));
        fvHuman.getLblDraw().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_DRAW));
        fvHuman.getLblMain1().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_MAIN1));
        fvHuman.getLblBeginCombat().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_BEGINCOMBAT));
        fvHuman.getLblDeclareAttackers().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREATTACKERS));
        fvHuman.getLblDeclareBlockers().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREBLOCKERS));
        fvHuman.getLblFirstStrike().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_FIRSTSTRIKE));
        fvHuman.getLblCombatDamage().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_COMBATDAMAGE));
        fvHuman.getLblEndCombat().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_ENDCOMBAT));
        fvHuman.getLblMain2().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_MAIN2));
        fvHuman.getLblEndTurn().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_EOT));
        fvHuman.getLblCleanup().setEnabled(this.getPrefBoolean(FPref.PHASE_HUMAN_CLEANUP));

        //Singletons.getView().getViewMatch().setLayoutParams(this.getPref(FPref.UI_LAYOUT_PARAMS));
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
}

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
package forge.game;

import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.io.GameStateDeserializer;
import forge.game.io.GameStateSerializer;
import forge.game.io.IGameStateObject;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;

import java.util.*;

/**
 * <p>
 * StaticEffects class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class StaticEffects implements IGameStateObject {

    // **************** StaticAbility system **************************
    private final ArrayList<StaticEffect> staticEffects = new ArrayList<StaticEffect>();
    //Global rule changes
    private final EnumSet<GlobalRuleChange> ruleChanges = EnumSet.noneOf(GlobalRuleChange.class);
    
    public final Set<Card> clearStaticEffects() {
        ruleChanges.clear();
        Set<Card> clearedCards = new HashSet<Card>();

        // remove all static effects
        for (StaticEffect se : staticEffects) {
            clearedCards.addAll(this.removeStaticEffect(se));
        }
        this.staticEffects.clear();

        return clearedCards;
    }

    public void setGlobalRuleChange(GlobalRuleChange change) {
        this.ruleChanges.add(change);
    }

    public boolean getGlobalRuleChange(GlobalRuleChange change) {
        return this.ruleChanges.contains(change);
    }

    /**
     * addStaticEffect. TODO Write javadoc for this method.
     * 
     * @param staticEffect
     *            a StaticEffect
     */
    public final void addStaticEffect(final StaticEffect staticEffect) {
        this.staticEffects.add(staticEffect);
    }

    /**
     * removeStaticEffect TODO Write javadoc for this method.
     * 
     * @param se
     *            a StaticEffect
     */
    private final List<Card> removeStaticEffect(final StaticEffect se) {
        final List<Card> affectedCards = se.getAffectedCards();
        final ArrayList<Player> affectedPlayers = se.getAffectedPlayers();
        final Map<String, String> params = se.getParams();

        int powerBonus = 0;
        String addP = "";
        int toughnessBonus = 0;
        String addT = "";
        int keywordMultiplier = 1;
        boolean setPT = false;
        String[] addKeywords = null;
        String[] addHiddenKeywords = null;
        String addColors = null;

        if (params.containsKey("SetPower") || params.containsKey("SetToughness")) {
            setPT = true;
        }

        if (params.containsKey("AddPower")) {
            addP = params.get("AddPower");
            if (addP.matches("[0-9][0-9]?")) {
                powerBonus = Integer.valueOf(addP);
            } else if (addP.equals("AffectedX")) {
                // gets calculated at runtime
            } else {
                powerBonus = se.getXValue();
            }
        }

        if (params.containsKey("AddToughness")) {
            addT = params.get("AddToughness");
            if (addT.matches("[0-9][0-9]?")) {
                toughnessBonus = Integer.valueOf(addT);
            } else if (addT.equals("AffectedX")) {
                // gets calculated at runtime
            } else {
                toughnessBonus = se.getYValue();
            }
        }

        if (params.containsKey("KeywordMultiplier")) {
            String multiplier = params.get("KeywordMultiplier");
            if (multiplier.equals("X")) {
                keywordMultiplier = se.getXValue();
            } else {
                keywordMultiplier = Integer.valueOf(multiplier);
            }
        }

        if (params.containsKey("AddHiddenKeyword")) {
            addHiddenKeywords = params.get("AddHiddenKeyword").split(" & ");
        }

        if (params.containsKey("AddColor")) {
            final String colors = params.get("AddColor");
            if (colors.equals("ChosenColor")) {
                addColors = CardUtil.getShortColorsString(se.getSource().getChosenColor());
            } else {
                addColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(" & "))));
            }
        }

        if (params.containsKey("SetColor")) {
            final String colors = params.get("SetColor");
            if (colors.equals("ChosenColor")) {
                addColors = CardUtil.getShortColorsString(se.getSource().getChosenColor());
            } else {
                addColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(" & "))));
            }
        }

        if (params.containsKey("IgnoreEffectCost")) {
            for (final SpellAbility s : se.getSource().getSpellAbilities()) {
                if (s instanceof AbilityStatic && s.isTemporary()) {
                    se.getSource().removeSpellAbility(s);
                }
            }
        }

        // modify players
        for (final Player p : affectedPlayers) {
            p.setUnlimitedHandSize(false);
            p.setMaxHandSize(p.getStartingHandSize());

            if (params.containsKey("AddKeyword")) {
                addKeywords = params.get("AddKeyword").split(" & ");
            }

            // add keywords
            if (addKeywords != null) {
                for (final String keyword : addKeywords) {
                    for (int i = 0; i < keywordMultiplier; i++) {
                        p.removeKeyword(keyword);
                    }
                }
            }
        }

        // modify the affected card
        for (final Card affectedCard : affectedCards) {
            // Gain control
            if (params.containsKey("GainControl")) {
                affectedCard.removeTempController(se.getTimestamp());
            }

            // remove set P/T
            if (!params.containsKey("CharacteristicDefining") && setPT) {
                affectedCard.removeNewPT(se.getTimestamp());
            }

            // remove P/T bonus
            if (addP.startsWith("AffectedX")) {
                powerBonus = se.getXMapValue(affectedCard);
            }
            if (addT.startsWith("AffectedX")) {
                toughnessBonus = se.getXMapValue(affectedCard);
            }
            affectedCard.addSemiPermanentAttackBoost(powerBonus * -1);
            affectedCard.addSemiPermanentDefenseBoost(toughnessBonus * -1);

            // remove keywords
            // TODO regular keywords currently don't try to use keyword multiplier
            // (Although nothing uses it at this time)
            if (params.containsKey("AddKeyword") || params.containsKey("RemoveKeyword")
                    || params.containsKey("RemoveAllAbilities")) {
                affectedCard.removeChangedCardKeywords(se.getTimestamp());
            }

            // remove abilities
            if (params.containsKey("AddAbility") || params.containsKey("GainsAbilitiesOf")) {
                for (final SpellAbility s : affectedCard.getSpellAbilities()) {
                    if (s.isTemporary()) {
                        affectedCard.removeSpellAbility(s);
                    }
                }
            }

            if (addHiddenKeywords != null) {
                for (final String k : addHiddenKeywords) {
                    for (int j = 0; j < keywordMultiplier; j++) {
                        affectedCard.removeHiddenExtrinsicKeyword(k);
                    }
                }
            }

            // remove abilities
            if (params.containsKey("RemoveAllAbilities")) {
                for (final SpellAbility ab : affectedCard.getSpellAbilities()) {
                    ab.setTemporarilySuppressed(false);
                }
                for (final StaticAbility stA : affectedCard.getStaticAbilities()) {
                    stA.setTemporarilySuppressed(false);
                }
                for (final ReplacementEffect rE : affectedCard.getReplacementEffects()) {
                    rE.setTemporarilySuppressed(false);
                }
            }

            // remove Types
            if (params.containsKey("AddType") || params.containsKey("RemoveType")) {
                affectedCard.removeChangedCardTypes(se.getTimestamp());
            }

            // remove colors
            if (addColors != null) {
                affectedCard.removeColor(addColors, affectedCard, !se.isOverwriteColors(),
                        se.getTimestamp(affectedCard));
            }
        }
        se.clearTimestamps();
        return affectedCards;
    }

    @Override
    public void loadState(GameStateDeserializer gsd) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void saveState(GameStateSerializer gss) {
        // TODO Auto-generated method stub
        
    }

    // **************** End StaticAbility system **************************


} // end class StaticEffects

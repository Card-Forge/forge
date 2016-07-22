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
package forge.game.keyword;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

/**
 * <p>
 * Card_Keywords class.
 * </p>
 * 
 * @author Forge
 * @version $Id: KeywordsChange.java 27095 2014-08-17 07:32:24Z elcnesh $
 */
public class KeywordsChange {
    private final List<String> keywords;
    private final List<String> removeKeywords;
    private final boolean removeAllKeywords;

    private List<Trigger> triggers = Lists.<Trigger>newArrayList();
    private List<ReplacementEffect> replacements = Lists.<ReplacementEffect>newArrayList();
    private List<SpellAbility> abilities = Lists.<SpellAbility>newArrayList();
    /**
     * 
     * Construct a new {@link KeywordsChange}.
     * 
     * @param keywordList the list of keywords to add.
     * @param removeKeywordList the list of keywords to remove.
     * @param removeAll whether to remove all keywords.
     */
    public KeywordsChange(final List<String> keywordList, final List<String> removeKeywordList, final boolean removeAll) {
        this.keywords = keywordList == null ? Lists.<String>newArrayList() : Lists.newArrayList(keywordList);
        this.removeKeywords = removeKeywordList == null ? Lists.<String>newArrayList() : Lists.newArrayList(removeKeywordList);
        this.removeAllKeywords = removeAll;
    }

    /**
     * 
     * getKeywords.
     * 
     * @return ArrayList<String>
     */
    public final List<String> getKeywords() {
        return this.keywords;
    }

    /**
     * 
     * getRemoveKeywords.
     * 
     * @return ArrayList<String>
     */
    public final List<String> getRemoveKeywords() {
        return this.removeKeywords;
    }

    /**
     * 
     * isRemoveAllKeywords.
     * 
     * @return boolean
     */
    public final boolean isRemoveAllKeywords() {
        return this.removeAllKeywords;
    }

    /**
     * @return whether this KeywordsChange doesn't have any effect.
     */
    public final boolean isEmpty() {
        return !this.removeAllKeywords
                && this.keywords.isEmpty()
                && this.removeKeywords.isEmpty();
    }

    public final void addKeywordsToCard(final Card host) {
        for (String k : keywords) {
            CardFactoryUtil.addTriggerAbility(k, host, this);
            CardFactoryUtil.addReplacementEffect(k, host, this);
            CardFactoryUtil.addSpellAbility(k, host, this);
        }
    }

    public final void removeKeywords(final Card host) {
        for (Trigger t : triggers) {
            host.removeTrigger(t);
        }
        for (ReplacementEffect r : replacements) {
            host.removeReplacementEffect(r);
        }
        for (SpellAbility s : abilities) {
            host.removeSpellAbility(s);
        }
    }

    public final void addTrigger(final Trigger trg) {
        triggers.add(trg);
    }
    
    public final void addReplacement(final ReplacementEffect trg) {
        replacements.add(trg);
    }

    public final void addSpellAbility(final SpellAbility s) {
        abilities.add(s);
    }
}

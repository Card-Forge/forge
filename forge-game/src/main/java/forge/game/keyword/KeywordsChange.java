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

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.card.Card;

/**
 * <p>
 * Card_Keywords class.
 * </p>
 * 
 * @author Forge
 * @version $Id: KeywordsChange.java 27095 2014-08-17 07:32:24Z elcnesh $
 */
public class KeywordsChange {
    private final KeywordCollection keywords = new KeywordCollection();
    private final List<KeywordInterface> removeKeywordInterfaces = Lists.newArrayList(); 
    private final List<String> removeKeywords = Lists.newArrayList();
    private boolean removeAllKeywords;

    /**
     * 
     * Construct a new {@link KeywordsChange}.
     * 
     * @param keywordList the list of keywords to add.
     * @param removeKeywordList the list of keywords to remove.
     * @param removeAll whether to remove all keywords.
     */
    public KeywordsChange(
            final Iterable<String> keywordList,
            final Collection<String> removeKeywordList,
            final boolean removeAll) {
        if (keywordList != null) {
            this.keywords.addAll(keywordList);
        }

        if (removeKeywordList != null) {
            this.removeKeywords.addAll(removeKeywordList);
        }

        this.removeAllKeywords = removeAll;
    }
    
    public KeywordsChange(
            final Collection<KeywordInterface> keywordList,
            final Collection<KeywordInterface> removeKeywordInterfaces,
            final boolean removeAll) {
        if (keywordList != null) {
            this.keywords.insertAll(keywordList);
        }

        if (removeKeywordInterfaces != null) {
            this.removeKeywordInterfaces.addAll(removeKeywordInterfaces);
        }

        this.removeAllKeywords = removeAll;
    }

    /**
     * 
     * getKeywords.
     * 
     * @return ArrayList<String>
     */
    public final Collection<KeywordInterface> getKeywords() {
        return this.keywords.getValues();
    }

    public final Collection<KeywordInterface> getRemovedKeywordInstances() {
        return this.removeKeywordInterfaces;
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
        for (KeywordInterface inst : keywords.getValues()) {
            inst.createTraits(host, false, true);
        }
    }
    
    public final boolean removeKeywordfromAdd(final String keyword) {
        return keywords.remove(keyword);
    }
    
    public final void addKeyword(final String keyword) {
        keywords.add(keyword);
    }
    
    public final KeywordsChange merge(
            final Collection<KeywordInterface> keywordList,
            final Collection<KeywordInterface> removeKeywordList,
            final boolean removeAll) {
        KeywordsChange result = new KeywordsChange(keywordList, removeKeywordList, removeAll);
        result.__merge(this);
        return result;
    }
    
    public final KeywordsChange merge(
            final Iterable<String> keywordList,
            final Collection<String> removeKeywordList,
            final boolean removeAll) {
        KeywordsChange result = new KeywordsChange(keywordList, removeKeywordList, removeAll);
        result.__merge(this);
        return result;
    }
    
    private void __merge(KeywordsChange other) {
        keywords.insertAll(other.getKeywords());
        removeKeywords.addAll(other.removeKeywords);
        removeKeywordInterfaces.addAll(other.removeKeywordInterfaces);
        if (other.removeAllKeywords) {
            removeAllKeywords = true;
        }
    }
}

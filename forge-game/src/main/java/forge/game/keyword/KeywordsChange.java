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
import forge.game.card.ICardTraitChanges;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

/**
 * <p>
 * Card_Keywords class.
 * </p>
 *
 * @author Forge
 */
public class KeywordsChange implements ICardTraitChanges, IKeywordsChange, Cloneable {
    private KeywordCollection keywords = new KeywordCollection();
    private List<KeywordInterface> removeKeywordInterfaces = Lists.newArrayList();
    private List<String> removeKeywords = Lists.newArrayList();
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
            final Iterable<KeywordInterface> keywordList,
            final Collection<String> removeKeywordList,
            final boolean removeAll) {
        if (keywordList != null) {
            this.keywords.insertAll(keywordList);
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

    public void setHostCard(final Card host) {
        keywords.setHostCard(host);
        for (KeywordInterface k : removeKeywordInterfaces) {
            k.setHostCard(host);
        }
    }

    public KeywordsChange copy(final Card host, final boolean lki) {
        try {
            KeywordsChange result = (KeywordsChange)super.clone();

            result.keywords = new KeywordCollection();
            for (KeywordInterface ki : this.keywords.getValues()) {
                result.keywords.insert(ki.copy(host, lki));
            }

            result.removeKeywords = Lists.newArrayList(removeKeywords);

            result.removeKeywordInterfaces = Lists.newArrayList();
            for (KeywordInterface ki : this.removeKeywordInterfaces) {
                result.removeKeywordInterfaces.add(ki.copy(host, lki));
            }

            return result;
        }  catch (final Exception ex) {
            throw new RuntimeException("KeywordsChange : clone() error", ex);
        }
    }

    public List<SpellAbility> applySpellAbility(List<SpellAbility> list) {
        for (KeywordInterface k : this.keywords.getValues()) {
            k.applySpellAbility(list);
        }
        return list;
    }
    public List<Trigger> applyTrigger(List<Trigger> list) {
        for (KeywordInterface k : this.keywords.getValues()) {
            k.applyTrigger(list);
        }
        return list;
    }
    public List<ReplacementEffect> applyReplacementEffect(List<ReplacementEffect> list) {
        for (KeywordInterface k : this.keywords.getValues()) {
            k.applyReplacementEffect(list);
        }
        return list;
    }
    public List<StaticAbility> applyStaticAbility(List<StaticAbility> list) {
        for (KeywordInterface k : this.keywords.getValues()) {
            k.applyStaticAbility(list);
        }
        return list;
    }

    public void applyKeywords(KeywordCollection list) {
        if (isRemoveAllKeywords()) {
            list.clear();
        }
        else if (getRemoveKeywords() != null) {
            list.removeAll(getRemoveKeywords());
        }

        list.removeInstances(getRemovedKeywordInstances());

        if (getKeywords() != null) {
            list.insertAll(getKeywords());
        }
    }

    public boolean hasTraits() {
        for (KeywordInterface k : this.keywords.getValues()) {
            if (k.hasTraits()) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<+");
        sb.append(this.keywords);
        sb.append("|-");
        sb.append(this.removeKeywordInterfaces);
        sb.append("|-");
        sb.append(this.removeKeywords);
        sb.append(">");
        return sb.toString();
    }
}

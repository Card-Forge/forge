package forge.game.keyword;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import forge.game.card.Card;
import forge.game.card.ICardTraitChanges;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

public class KeywordCollection implements ICardTraitChanges, Iterable<KeywordInterface> {
    // don't use enumKeys it causes a slow down
    private final Multimap<Keyword, KeywordInterface> map = MultimapBuilder.hashKeys()
            .linkedHashSetValues().build();

    // Lazily-computed trait bitmask: whether any contained keyword contributes a
    // replacement effect / trigger / static ability. Invalidated on insert/remove/clear;
    // recomputed on first query. CAVEAT: if a caller mutates a contained
    // KeywordInstance's trait lists in place (e.g. a second createTraits call on
    // an already-inserted instance, as CardFactoryUtil.setupKeywordedAbilities does),
    // the flag is NOT invalidated — rebuild the collection via Card.updateKeywordsCache
    // in that case.
    private transient boolean traitFlagsDirty = true;
    private transient List<SpellAbility> cachedSpellAbilities;
    private transient List<Trigger> cachedTriggers;
    private transient List<ReplacementEffect> cachedReplacements;
    private transient List<StaticAbility> cachedStaticAbilities;

    public KeywordCollection() {
        super();
    }

    private void invalidateTraitFlags() { traitFlagsDirty = true; }

    private void computeTraitFlagsIfDirty() {
        if (!traitFlagsDirty) return;
        List<SpellAbility> sa = Lists.newArrayList();
        List<Trigger> tr = Lists.newArrayList();
        List<ReplacementEffect> re = Lists.newArrayList();
        List<StaticAbility> st = Lists.newArrayList();
        for (KeywordInterface kw : map.values()) {
            kw.applySpellAbility(sa);
            kw.applyTrigger(tr);
            kw.applyReplacementEffect(re);
            kw.applyStaticAbility(st);
        }
        cachedSpellAbilities = sa;
        cachedTriggers = tr;
        cachedReplacements = re;
        cachedStaticAbilities = st;
        traitFlagsDirty = false;
    }

    public boolean hasReplacementEffectKeyword() {
        computeTraitFlagsIfDirty();
        return !cachedReplacements.isEmpty();
    }

    public boolean hasTriggerKeyword() {
        computeTraitFlagsIfDirty();
        return !cachedTriggers.isEmpty();
    }

    public boolean hasStaticAbilityKeyword() {
        computeTraitFlagsIfDirty();
        return !cachedStaticAbilities.isEmpty();
    }

    public boolean contains(Keyword keyword) {
        return map.containsKey(keyword);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.values().size();
    }

    public int getAmount(Keyword keyword) {
        int amount = 0;
        for (KeywordInterface inst : map.get(keyword)) {
            amount += inst.getAmount();
        }
        return amount;
    }

    public KeywordInterface add(String k) {
        KeywordInterface inst = Keyword.getInstance(k);
        if (insert(inst)) {
            return inst;
        }
        return null;
    }
    public boolean insert(KeywordInterface inst) {
        Keyword keyword = inst.getKeyword();
        Collection<KeywordInterface> list = map.get(keyword);
        if (list.isEmpty() || !inst.redundant(list)) {
            list.add(inst);
            invalidateTraitFlags();
            return true;
        }
        return false;
    }

    public void addAll(Iterable<String> keywords) {
        for (String k : keywords) {
            add(k);
        }
    }

    public boolean insertAll(Iterable<KeywordInterface> inst) {
        boolean result = false;
        for (KeywordInterface k : inst) {
            if (insert(k)) {
                result = true;
            }
        }
        return result;
    }

    public boolean remove(String keyword) {
        Iterator<KeywordInterface> it = map.values().iterator();

        boolean result = false;
        while (it.hasNext()) {
            KeywordInterface k = it.next();
            if (k.getOriginal().startsWith(keyword)) {
                it.remove();
                result = true;
            }
        }

        if (result) invalidateTraitFlags();
        return result;
    }

    public boolean remove(KeywordInterface keyword) {
        boolean r = map.remove(keyword.getKeyword(), keyword);
        if (r) invalidateTraitFlags();
        return r;
    }

    public boolean removeAll(Keyword kenum) {
        boolean r = !map.removeAll(kenum).isEmpty();
        if (r) invalidateTraitFlags();
        return r;
    }

    public boolean removeAll(Iterable<String> keywords) {
        boolean result = false;
        for (String k : keywords) {
            if (remove(k)) {
                result = true;
            }
        }
        return result;
    }

    public boolean removeInstances(Iterable<KeywordInterface> keywords) {
        boolean result = false;
        for (KeywordInterface k : keywords) {
            if (map.remove(k.getKeyword(), k)) {
                result = true;
            }
        }
        if (result) invalidateTraitFlags();
        return result;
    }

    public void clear() {
        map.clear();
        invalidateTraitFlags();
    }

    public boolean contains(String keyword) {
        for (KeywordInterface inst : map.values()) {
            if (keyword.equals(inst.getOriginal())) {
                return true;
            }
        }
        return false;
    }

    public int getAmount(String k) {
        int amount = 0;
        for (KeywordInterface inst : map.values()) {
            if (k.equals(inst.getOriginal())) {
                amount++;
            }
        }
        return amount;
    }

    public Collection<KeywordInterface> getValues() {
        return map.values();
    }

    public Collection<KeywordInterface> getValues(final Keyword keyword) {
        return map.get(keyword);
    }

    public List<String> asStringList() {
        List<String> result = Lists.newArrayList();
        for (KeywordInterface kw : getValues()) {
            result.add(kw.getOriginal());
        }
        return result;
    }

    public KeywordCollectionView getView() {
        return new KeywordCollectionView(getValues().stream().map(KeywordInterface::getView).collect(Collectors.toList()));
    }

    public void setHostCard(final Card host) {
        for (KeywordInterface k : map.values()) {
            k.setHostCard(host);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb  = new StringBuilder();

        sb.append(map.values());
        return sb.toString();
    }

    @Override
    public List<SpellAbility> applySpellAbility(List<SpellAbility> list) {
        computeTraitFlagsIfDirty();
        list.addAll(cachedSpellAbilities);
        return list;
    }
    @Override
    public List<Trigger> applyTrigger(List<Trigger> list) {
        computeTraitFlagsIfDirty();
        list.addAll(cachedTriggers);
        return list;
    }
    @Override
    public List<ReplacementEffect> applyReplacementEffect(List<ReplacementEffect> list) {
        computeTraitFlagsIfDirty();
        list.addAll(cachedReplacements);
        return list;
    }
    @Override
    public List<StaticAbility> applyStaticAbility(List<StaticAbility> list) {
        computeTraitFlagsIfDirty();
        list.addAll(cachedStaticAbilities);
        return list;
    }
    @Override
    public KeywordCollection copy(Card host, boolean lki) {
        KeywordCollection result = new KeywordCollection();
        for (KeywordInterface ki : getValues()) {
            result.insert(ki.copy(host, lki));
        }
        return result;
    }

    public void applyChanges(Iterable<IKeywordsChange> changes) {
        for (final IKeywordsChange ck : changes) {
            ck.applyKeywords(this);
        }
    }

    @Override
    public Iterator<KeywordInterface> iterator() {
        return this.map.values().iterator();
    }
}

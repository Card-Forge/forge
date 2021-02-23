package forge.game.card;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;

import forge.game.CardTraitBase;
import forge.game.staticability.StaticAbility;
import forge.util.collect.FCollection;

public class LinkedAbilityTable<T> extends ForwardingTable<Card, Optional<StaticAbility>, FCollection<T>> {
    private Table<Card, Optional<StaticAbility>, FCollection<T>> dataTable = HashBasedTable.create();

    @Override
    protected Table<Card, Optional<StaticAbility>, FCollection<T>> delegate() {
        return dataTable;
    }

    protected FCollection<T> getSupplier() {
        return new FCollection<T>();
    }

    protected FCollection<T> putInternal(T object, Card host, StaticAbility stAb) {
        host = ObjectUtils.defaultIfNull(host.getEffectSource(), host);
        Optional<StaticAbility> st = Optional.fromNullable(stAb);
        FCollection<T> old;
        if (contains(host, st)) {
            old = get(host, st);
        } else {
            old = getSupplier();
            delegate().put(host, st, old);
        }
        old.add(object);
        return old;
    }

    public FCollection<T> put(T object, Card host) {
        return putInternal(object, host, null);
    }

    public FCollection<T> put(T object, CardTraitBase ctb) {
        return putInternal(object, ctb.getOriginalOrHost(), ctb.getGrantorStatic());
    }

    protected void setInternal(Iterable<T> list, Card host, StaticAbility stAb) {
        host = ObjectUtils.defaultIfNull(host.getEffectSource(), host);
        Optional<StaticAbility> st = Optional.fromNullable(stAb);
        if (list == null || Iterables.isEmpty(list)) {
            delegate().remove(host, st);
        } else {
            FCollection<T> old = getSupplier();
            old.addAll(list);
            delegate().put(host, st, old);
        }
    }

    public void set(Iterable<T> list, CardTraitBase ctb) {
        setInternal(list, ctb.getOriginalOrHost(), ctb.getGrantorStatic());
    }

    public FCollection<T> get(CardTraitBase ctb) {
        Card host = ctb.getOriginalOrHost();
        host = ObjectUtils.defaultIfNull(host.getEffectSource(), host);
        Optional<StaticAbility> st = Optional.fromNullable(ctb.getGrantorStatic());
        if (contains(host, st)) {
            return get(host, st);
        } else {
            return FCollection.<T>getEmpty();
        }
    }

    public boolean contains(T object, CardTraitBase ctb) {
        return get(ctb).contains(object);
    }

    public boolean remove(T value) {
        boolean changed = false;
        for (FCollection<T> col : delegate().values()) {
            if (col.remove(value)) {
                changed = true;
            }
        }
        return changed;
    }
}

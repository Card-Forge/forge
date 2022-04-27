package forge.game.card;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import forge.game.CardTraitBase;
import forge.game.StaticLayerInterface;
import forge.util.collect.FCollection;

public class LinkedAbilityTable<T> extends ForwardingTable<Card, List<StaticLayerInterface>, FCollection<T>> {
    private Table<Card, List<StaticLayerInterface>, FCollection<T>> dataTable = HashBasedTable.create();

    @Override
    protected Table<Card, List<StaticLayerInterface>, FCollection<T>> delegate() {
        return dataTable;
    }

    protected FCollection<T> getSupplier() {
        return new FCollection<T>();
    }

    protected FCollection<T> putInternal(T object, Card host, List<StaticLayerInterface> st) {
        host = ObjectUtils.defaultIfNull(host.getEffectSource(), host);
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
/*
    public FCollection<T> put(T object, Card host) {
        return putInternal(object, host, null);
    }
//*/
    public FCollection<T> put(T object, CardTraitBase ctb) {
        return putInternal(object, ctb.getOriginalOrHost(), ctb.getGrantedByStatic());
    }

    protected void setInternal(Iterable<T> list, Card host, List<StaticLayerInterface> st) {
        host = ObjectUtils.defaultIfNull(host.getEffectSource(), host);
        if (list == null || Iterables.isEmpty(list)) {
            delegate().remove(host, st);
        } else {
            FCollection<T> old = getSupplier();
            old.addAll(list);
            delegate().put(host, st, old);
        }
    }

    public void set(Iterable<T> list, CardTraitBase ctb) {
        setInternal(list, ctb.getOriginalOrHost(), ctb.getGrantedByStatic());
    }

    public FCollection<T> get(CardTraitBase ctb) {
        Card host = ctb.getOriginalOrHost();
        host = ObjectUtils.defaultIfNull(host.getEffectSource(), host);
        List<StaticLayerInterface> st = ctb.getGrantedByStatic();
        if (contains(host, st)) {
            return get(host, st);
        } else {
            return FCollection.<T>getEmpty();
        }
    }

    public FCollection<T> get(Card host) {
        if (contains(host, Lists.newArrayList())) {
            return get(host, Lists.newArrayList());
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

    public boolean hasOtherLinkedValues(Card host) {
        for (List<StaticLayerInterface> o : this.columnKeySet()) {
            if (!o.isEmpty()) {
                return true;
            }
        }
        for (Card ctbHost : this.rowKeySet()) {
            if (!host.equals(ctbHost)) {
                return true;
            }
        }
        return false;
    }
}

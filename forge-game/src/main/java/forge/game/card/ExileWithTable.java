package forge.game.card;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import forge.game.CardTraitBase;
import forge.game.staticability.StaticAbility;

public class ExileWithTable extends ForwardingTable<Card, Optional<StaticAbility>, CardCollection> {
    private Table<Card, Optional<StaticAbility>, CardCollection> dataTable = HashBasedTable.create();

    public ExileWithTable(Table<Card, Optional<StaticAbility>, CardCollection> map) {
        this.putAll(map);
    }


    public ExileWithTable() {
    }


    @Override
    protected Table<Card, Optional<StaticAbility>, CardCollection> delegate() {
        return dataTable;
    }


    public CardCollection put(Card object, CardTraitBase ctb) {
        Card host = ctb.getOriginalOrHost();
        host = ObjectUtils.defaultIfNull(host.getEffectSource(), host);
        Optional<StaticAbility> st = Optional.fromNullable(ctb.getGrantorStatic());
        CardCollection old;
        if (contains(host, st)) {
            old = get(host, st);
            old.add(object);
        } else {
            old = new CardCollection(object);
            delegate().put(host, st, old);
        }
        return old;
    }

    public CardCollectionView get(CardTraitBase ctb) {
        Card host = ctb.getOriginalOrHost();
        host = ObjectUtils.defaultIfNull(host.getEffectSource(), host);
        Optional<StaticAbility> st = Optional.fromNullable(ctb.getGrantorStatic());
        if (contains(host, st)) {
            return get(host, st);
        } else {
            return CardCollection.EMPTY;
        }
    }

    public boolean contains(Card object, CardTraitBase ctb) {
        return get(ctb).contains(object);
    }

    public boolean remove(Card value) {
        boolean changed = false;
        for (CardCollection col : delegate().values()) {
            if (col.remove(value)) {
                changed = true;
            }
        }
        return changed;
    }
}

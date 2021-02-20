package forge.game.card;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;

public class ChosenModesTable extends ForwardingTable<SpellAbility, Optional<StaticAbility>, List<String>> {
    Table<SpellAbility, Optional<StaticAbility>, List<String>> dataTable = HashBasedTable.create();

    @Override
    protected Table<SpellAbility, Optional<StaticAbility>, List<String>> delegate() {
        return dataTable;
    }

    protected SpellAbility getOriginal(SpellAbility sa) {
        SpellAbility original = null;
        SpellAbility root = sa.getRootAbility();

        // because trigger spell abilities are copied, try to get original one
        if (root.isTrigger()) {
            original = root.getTrigger().getOverridingAbility();
        } else {
            original = ObjectUtils.defaultIfNull(root.getOriginalAbility(), sa);
        }
        return original;
    }

    public List<String> put(SpellAbility sa, String mode) {
        SpellAbility root = sa.getRootAbility();
        SpellAbility original = getOriginal(sa);
        Optional<StaticAbility> st = Optional.fromNullable(root.getGrantorStatic());

        List<String> old;
        if (contains(original, st)) {
            old = get(original, st);
            old.add(mode);
        } else {
            old = Lists.newArrayList(mode);
            delegate().put(original, st, old);
        }
        return old;
    }

    public List<String> get(SpellAbility sa) {
        SpellAbility root = sa.getRootAbility();
        SpellAbility original = getOriginal(sa);
        Optional<StaticAbility> st = Optional.fromNullable(root.getGrantorStatic());
        if (contains(original, st)) {
            return get(original, st);
        } else {
            return ImmutableList.of();
        }
    }
}

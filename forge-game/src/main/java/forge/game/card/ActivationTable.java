package forge.game.card;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import forge.game.StaticLayerInterface;
import forge.game.spellability.SpellAbility;

public class ActivationTable extends ForwardingTable<SpellAbility, List<StaticLayerInterface>, Integer> {
    Table<SpellAbility, List<StaticLayerInterface>, Integer> dataTable = HashBasedTable.create();

    @Override
    protected Table<SpellAbility, List<StaticLayerInterface>, Integer> delegate() {
        return dataTable;
    }

    protected SpellAbility getOriginal(SpellAbility sa) {
        SpellAbility original = null;
        SpellAbility root = sa.getRootAbility();

        // because trigger spell abilities are copied, try to get original one
        if (root.isTrigger()) {
            original = root.getTrigger().getOverridingAbility();
        } else {
            original = ObjectUtils.defaultIfNull(root.getOriginalAbility(), root);
        }
        return original;
    }

    public void add(SpellAbility sa) {
        SpellAbility root = sa.getRootAbility();
        SpellAbility original = getOriginal(sa);

        if (original != null) {
            List<StaticLayerInterface> st = root.getGrantedByStatic();

            delegate().put(original, st, ObjectUtils.defaultIfNull(get(original, st), 0) + 1);
        }
    }

    public Integer get(SpellAbility sa) {
        SpellAbility root = sa.getRootAbility();
        SpellAbility original = getOriginal(sa);
        List<StaticLayerInterface> st = root.getGrantedByStatic();

        if (contains(original, st)) {
            return get(original, st);
        }
        return 0;
    }
}

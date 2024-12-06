package forge.game.card;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Optional;

public class ActivationTable extends ForwardingTable<SpellAbility, Optional<StaticAbility>, List<Player>> {
    Table<SpellAbility, Optional<StaticAbility>, List<Player>> dataTable = HashBasedTable.create();

    @Override
    protected Table<SpellAbility, Optional<StaticAbility>, List<Player>> delegate() {
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
            Optional<StaticAbility> st = Optional.ofNullable(root.getGrantorStatic());

            List<Player> activators = get(original, st);
            if (activators == null) {
                activators = Lists.newArrayList();
            }
            activators.add(sa.getActivatingPlayer());
            delegate().put(original, st, activators);
        }
    }

    public Integer get(SpellAbility sa) {
        return getActivators(sa).size();
    }

    public List<Player> getActivators(SpellAbility sa) {
        SpellAbility root = sa.getRootAbility();
        SpellAbility original = getOriginal(sa);
        Optional<StaticAbility> st = Optional.ofNullable(root.getGrantorStatic());

        if (contains(original, st)) {
            return get(original, st);
        }
        return Lists.newArrayList();
    }
}

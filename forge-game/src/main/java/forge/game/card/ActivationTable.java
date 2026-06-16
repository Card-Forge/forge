package forge.game.card;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;

import java.util.Objects;
import java.util.Optional;

public class ActivationTable extends ForwardingTable<SpellAbility, Optional<StaticAbility>, Multiset<Player>> {
    Table<SpellAbility, Optional<StaticAbility>, Multiset<Player>> dataTable = HashBasedTable.create();

    @Override
    protected Table<SpellAbility, Optional<StaticAbility>, Multiset<Player>> delegate() {
        return dataTable;
    }

    protected SpellAbility getOriginal(SpellAbility sa) {
        SpellAbility original = null;
        SpellAbility root = sa.getRootAbility();

        // because trigger spell abilities are copied, try to get original one
        if (root.isTrigger()) {
            original = root.getTrigger().getOverridingAbility();
        } else {
            original = Objects.requireNonNullElse(root.getOriginalAbility(), root);
        }
        return original;
    }

    public void add(SpellAbility sa) {
        SpellAbility root = sa.getRootAbility();
        SpellAbility original = getOriginal(sa);

        if (original != null) {
            Optional<StaticAbility> st = Optional.ofNullable(root.getGrantorStatic());

            Multiset<Player> activators = Objects.requireNonNullElse(get(original, st), HashMultiset.create());
            activators.add(sa.getActivatingPlayer());
            delegate().put(original, st, activators);
        }
    }

    public int get(SpellAbility sa) {
        return getActivators(sa).size();
    }

    public Multiset<Player> getActivators(SpellAbility sa) {
        SpellAbility root = sa.getRootAbility();
        SpellAbility original = getOriginal(sa);
        Optional<StaticAbility> st = Optional.ofNullable(root.getGrantorStatic());

        if (contains(original, st)) {
            return get(original, st);
        }
        return HashMultiset.create();
    }
}

package forge.game.card;

import com.google.common.base.Optional;
import com.google.common.collect.Table;

import forge.game.staticability.StaticAbility;
import forge.util.collect.FCollection;

public class ExileWithTable extends LinkedAbilityTable<Card> {

    public ExileWithTable(Table<Card, Optional<StaticAbility>, FCollection<Card>> map) {
        this.putAll(map);
    }


    public ExileWithTable() {
    }
}

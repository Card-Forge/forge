package forge.game.trigger;

import java.util.List;
import java.util.Map;

import forge.game.card.*;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class TriggerChangesZoneAll extends Trigger {

    public TriggerChangesZoneAll(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        final CardZoneTable table = (CardZoneTable) runParams2.get("Cards");

        if (filterCards(table).isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        final CardZoneTable table = (CardZoneTable) getRunParams().get("Cards");

        CardCollection allCards = this.filterCards(table);

        sa.setTriggeringObject("Cards", allCards);
        sa.setTriggeringObject("Amount", allCards.size());
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Amount: ").append(sa.getTriggeringObject("Amount"));
        return sb.toString();
    }

    private CardCollection filterCards(CardZoneTable table) {
        CardCollection allCards = new CardCollection();
        ZoneType destination = null;

        if (hasParam("Destination")) {
            if (!getParam("Destination").equals("Any")) {
                destination = ZoneType.valueOf(getParam("Destination"));
                if (!table.containsColumn(destination)) {
                    return allCards;
                }
            }
        }

        if (hasParam("Origin") && !getParam("Origin").equals("Any")) {
            if (getParam("Origin") == null) {
                return allCards;
            }
            final List<ZoneType> origin = ZoneType.listValueOf(getParam("Origin"));
            for (ZoneType z : origin) {
                if (table.containsRow(z)) {
                    if (destination != null) {
                        allCards.addAll(table.row(z).get(destination));
                    } else {
                        for (CardCollection c : table.row(z).values()) {
                            allCards.addAll(c);
                        }
                    }
                }
            }
        } else if (destination != null) {
            for (CardCollection c : table.column(destination).values()) {
                allCards.addAll(c);
            }
        } else {
            for (CardCollection c : table.values()) {
                allCards.addAll(c);
            }
        }

        if (hasParam("ValidCards")) {
            allCards = CardLists.getValidCards(allCards, getParam("ValidCards").split(","),
                    getHostCard().getController(), getHostCard(), null);
        }
        return allCards;
    }
}

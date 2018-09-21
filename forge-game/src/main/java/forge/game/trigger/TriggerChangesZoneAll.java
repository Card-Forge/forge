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
        @SuppressWarnings("unchecked")
        final Map<ZoneType, CardCollection> moved = (Map<ZoneType, CardCollection>) runParams2.get("Cards");

        if (hasParam("Destination")) {
            if (!getParam("Destination").equals("Any")) {
                if (!runParams2.get("Destination").equals(ZoneType.valueOf(getParam("Destination")))) {
                    return false;
                }
            }
        }

        final CardCollection allCards = new CardCollection();

        if (hasParam("Origin")) {
            if (!getParam("Origin").equals("Any")) {
                if (getParam("Origin") == null) {
                    return false;
                }
                final List<ZoneType> origin = ZoneType.listValueOf((String)getParam("Origin"));
                for (ZoneType z : origin) {
                    if (moved.containsKey(z)) {
                        allCards.addAll(moved.get(z));
                    }
                }
            }
        } else {
            for (CardCollection c : moved.values()) {
                allCards.addAll(c);
            }
        }

        if (hasParam("ValidCards")) {
            
            int count = CardLists.getValidCardCount(allCards, getParam("ValidCards").split(","),this.getHostCard().getController(),
                    this.getHostCard());
            if (count == 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        @SuppressWarnings("unchecked")
        final Map<ZoneType, CardCollection> moved = (Map<ZoneType, CardCollection>) getRunParams().get("Cards");

        CardCollection allCards = new CardCollection();

        if (hasParam("Origin")) {
            if (!getParam("Origin").equals("Any") && getParam("Origin") != null) {
                final List<ZoneType> origin = ZoneType.listValueOf((String)getParam("Origin"));
                for (ZoneType z : origin) {
                    if (moved.containsKey(z)) {
                        allCards.addAll(moved.get(z));
                    }
                }
            }
        } else {
            for (CardCollection c : moved.values()) {
                allCards.addAll(c);
            }
        }

        if (hasParam("ValidCards")) {
            allCards = CardLists.getValidCards(allCards, getParam("ValidCards").split(","),this.getHostCard().getController(),
                    this.getHostCard(), sa);
        }

        sa.setTriggeringObject("Cards", allCards);
        sa.setTriggeringObject("Amount", allCards.size());
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Amount: ").append(sa.getTriggeringObject("Amount"));
        return sb.toString();
    }
}

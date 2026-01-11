package forge.game.keyword;

import java.util.List;
import java.util.stream.Collectors;

import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.util.StreamUtil;

public class Compleated extends SimpleKeyword {

    @Override
    protected String formatReminderText(String reminderText) {
        Card card = this.getHostCard();
        if (card == null) {
            return reminderText;
        }
        ManaCost mc = card.getManaCost();
        if (!mc.hasPhyrexian()) {
            return reminderText;
        }
        List<ManaCostShard> shards = StreamUtil.stream(mc).filter(ManaCostShard::isPhyrexian).collect(Collectors.toList());
        if (shards.isEmpty()) {
            return reminderText;
        }
        ManaCostShard pip = shards.get(0);
        String[] parts = pip.toShortString().split("/");
        final StringBuilder rem = new StringBuilder();
        rem.append(pip).append(" can be paid with {").append(parts[0]).append("}");
        if (parts.length > 2) {
            rem.append(", {").append(parts[1]).append("},");
        }
        rem.append(" or 2 life. ");
        if (mc.getPhyrexianCount() > 1) {
            rem.append("For each ").append(pip).append(" paid with life,");
        } else {
            rem.append("If life was paid,");
        }
        rem.append(" this planeswalker enters with two fewer loyalty counters.");
        return rem.toString();
    }
}

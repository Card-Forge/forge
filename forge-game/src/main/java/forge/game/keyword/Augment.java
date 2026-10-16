package forge.game.keyword;

import forge.game.card.Card;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

public class Augment extends KeywordWithCost {
    protected String leftSideSVar = "";
    protected Trigger augmentTrigger;
    public String getLeftSideSVar() {
        return leftSideSVar;
    }

    public Augment() {
    }

    @Override
    protected void parse(String details) {
        String[] k = details.split(":");
        super.parse(k[0]);
        leftSideSVar = k[1];
    }

    public void createTraits(final Card host, final boolean intrinsic) {
        super.createTraits(host, intrinsic);
        String left = host.getSVar(leftSideSVar);
        int colonPos = left.indexOf(':');
        String key = colonPos > 0 ? left.substring(0, colonPos) : left;
        String value = colonPos > 0 ? left.substring(1+colonPos).trim() : null;
        switch (key.charAt(0)) {
            case 'T': addTrigger(augmentTrigger = TriggerHandler.parseTrigger(value, host, true, null));
        }
    }
}

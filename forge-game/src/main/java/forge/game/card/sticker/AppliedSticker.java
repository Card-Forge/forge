package forge.game.card.sticker;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.card.CardTraitChanges;
import forge.game.card.perpetual.PerpetualInterface;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.TriggerHandler;

/**
 * A sticker on an object. A name sticker also keeps its word position (CR 123.6b).
 */
public class AppliedSticker implements PerpetualInterface {
    private static final String HAS_ATTACK_EFFECT = "HasAttackEffect";

    private final Sticker sticker;
    private final long timestamp;
    private final int namePosition;

    public AppliedSticker(Sticker sticker, long timestamp) {
        this(sticker, timestamp, 0);
    }

    public AppliedSticker(Sticker sticker, long timestamp, int namePosition) {
        this.sticker = sticker;
        this.timestamp = timestamp;
        this.namePosition = namePosition;
    }

    public Sticker getSticker() {
        return sticker;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public int getNamePosition() {
        return namePosition;
    }

    public StickerKind getKind() {
        return sticker.getKind();
    }

    @Override
    public void applyEffect(Card c) {
        switch (sticker.getKind()) {
            case PT -> c.addNewPT(sticker.getPower(), sticker.getToughness(), timestamp, 0);
            case NAME -> c.recomputeStickerName();
            case ABILITY -> grantAbility(c);
            // CR 123.9
            case ART -> {
            }
        }
    }

    public void removeEffect(Card c) {
        switch (sticker.getKind()) {
            case PT -> c.removeNewPT(timestamp, 0);
            case NAME -> c.removeChangedName(timestamp, 0);
            case ABILITY -> {
                c.removeChangedCardKeywords(timestamp, 0);
                c.removeChangedCardTraits(timestamp, 0);
                c.removeChangedSVars(timestamp, 0);
            }
            case ART -> {
            }
        }
    }

    // CR 123.7
    private void grantAbility(Card c) {
        List<String> keywords = getGrantedKeywords();
        if (!keywords.isEmpty()) {
            c.addChangedCardKeywords(keywords, null, false, timestamp, null);
        }
        CardTraitChanges traits = getGrantedTraits(c);
        if (!traits.getAbilities().isEmpty() || !traits.getTriggers().isEmpty()
                || !traits.getStaticAbilities().isEmpty()) {
            c.addChangedCardTraits(traits, timestamp, 0, true);
        }
        if (grantsAttackTrigger(traits)) {
            c.addChangedSVars(Map.of(HAS_ATTACK_EFFECT, "TRUE"), timestamp, 0);
        }
    }

    public static boolean grantsAttackTrigger(CardTraitChanges traits) {
        return traits.getTriggers().stream().anyMatch(t -> t.getMode() == TriggerType.Attacks);
    }

    public List<String> getGrantedKeywords() {
        if (sticker.getKeywords() == null) {
            return List.of();
        }
        return Arrays.stream(sticker.getKeywords().split(",")).map(String::trim).toList();
    }

    public CardTraitChanges getGrantedTraits(Card c) {
        // the sheet's state, so SVars read at resolution are still found on the sheet
        CardState sheetState = sticker.getSheet().getCurrentState();
        List<SpellAbility> abilities = Lists.newArrayList();
        List<Trigger> triggers = Lists.newArrayList();
        List<StaticAbility> statics = Lists.newArrayList();
        if (sticker.getAbilitySVar() != null) {
            for (String svar : sticker.getAbilitySVar().split(",")) {
                abilities.add(AbilityFactory.getAbility(c, svar.trim(), sheetState));
            }
        }
        if (sticker.getTriggers() != null) {
            for (String svar : sticker.getTriggers().split(",")) {
                triggers.add(TriggerHandler.parseTrigger(sheetState.getSVar(svar.trim()), c, false, sheetState));
            }
        }
        if (sticker.getStatics() != null) {
            for (String svar : sticker.getStatics().split(",")) {
                statics.add(StaticAbility.create(sheetState.getSVar(svar.trim()), c, sheetState, false));
            }
        }
        return new CardTraitChanges(abilities, triggers, null, statics, null);
    }

    @Override
    public String toString() {
        return sticker.toString();
    }
}

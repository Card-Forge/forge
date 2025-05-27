package forge.game.ability.effects;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.keyword.KeywordInterface;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

import java.util.List;

/**
 * Exchanges text boxes between two creatures.
 */
public class TextBoxExchangeEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final List<Card> tgtCards = getTargetCards(sa);
        Card c1;
        Card c2;
        if (tgtCards.size() == 1) {
            c1 = sa.getHostCard();
            c2 = tgtCards.get(0);
        } else {
            c1 = tgtCards.get(0);
            c2 = tgtCards.get(1);
        }
        return c1 + " exchanges text box with " + c2 + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final List<Card> tgtCards = getTargetCards(sa);
        if (tgtCards.size() < 2) {
            return;
        }

        final Card c1 = tgtCards.get(0);
        final Card c2 = tgtCards.get(1);

        // snapshot the original text boxes before modifying
        final TextBoxData data1 = captureTextBoxData(c1);
        final TextBoxData data2 = captureTextBoxData(c2);

        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final long ts = game.getNextTimestamp();

        swapTextBox(c1, data2, ts);
        swapTextBox(c2, data1, ts);

        game.fireEvent(new GameEventCardStatsChanged(c1));
        game.fireEvent(new GameEventCardStatsChanged(c2));
    }

    private static void swapTextBox(final Card to, final TextBoxData from, final long ts) {
        List<SpellAbility> spellabilities = Lists.newArrayList();
        for (SpellAbility sa : from.spellabilities) {
            SpellAbility copy = sa.copy(to, false, true);
            // need to persist any previous word changes
            copy.changeTextIntrinsic(copy.getChangedTextColors(), copy.getChangedTextTypes());
            spellabilities.add(copy);
        }
        List<Trigger> triggers = Lists.newArrayList();
        for (Trigger tr : from.triggers) {
            Trigger copy = tr.copy(to, false, true);
            copy.changeTextIntrinsic(copy.getChangedTextColors(), copy.getChangedTextTypes());
            triggers.add(copy);
        }
        List<ReplacementEffect> reps = Lists.newArrayList();
        for (ReplacementEffect re : from.replacements) {
            ReplacementEffect copy = re.copy(to, false, true);
            copy.changeTextIntrinsic(copy.getChangedTextColors(), copy.getChangedTextTypes());
            reps.add(copy);
        }
        List<StaticAbility> statics = Lists.newArrayList();
        for (StaticAbility st : from.statics) {
            StaticAbility copy = st.copy(to, false, true);
            copy.changeTextIntrinsic(copy.getChangedTextColors(), copy.getChangedTextTypes());
            statics.add(copy);
        }
        to.addChangedCardTraitsByText(spellabilities, triggers, reps, statics, ts, 0);

        List<KeywordInterface> kws = Lists.newArrayList();
        for (KeywordInterface kw : from.keywords) {
            kws.add(kw.copy(to, false));
        }
        to.addChangedCardKeywordsByText(kws, ts, 0, false);

        to.updateChangedText();
        to.updateStateForView();
    }

    private static TextBoxData captureTextBoxData(final Card card) {
        TextBoxData data = new TextBoxData();
        CardState state = card.getCurrentState();

        data.spellabilities = Lists.newArrayList();
        for (SpellAbility sa : state.getSpellAbilities()) {
            if (sa.isIntrinsic() && sa.getKeyword() == null) {
                data.spellabilities.add(sa);
            }
        }
        data.triggers = Lists.newArrayList();
        for (Trigger tr : state.getTriggers()) {
            if (tr.isIntrinsic() && tr.getKeyword() == null) {
                data.triggers.add(tr);
            }
        }
        data.replacements = Lists.newArrayList();
        for (ReplacementEffect re : state.getReplacementEffects()) {
            if (re.isIntrinsic() && re.getKeyword() == null) {
                data.replacements.add(re);
            }
        }
        data.statics = Lists.newArrayList();
        for (StaticAbility st : state.getStaticAbilities()) {
            if (st.isIntrinsic() && st.getKeyword() == null) {
                data.statics.add(st);
            }
        }

        data.keywords = Lists.newArrayList();
        for (KeywordInterface ki : card.getKeywords()) {
            if (ki.isIntrinsic()) {
                data.keywords.add(ki);
            }
        }

        return data;
    }

    private static class TextBoxData {
        List<SpellAbility> spellabilities;
        List<Trigger> triggers;
        List<ReplacementEffect> replacements;
        List<StaticAbility> statics;
        List<KeywordInterface> keywords;
    }
}

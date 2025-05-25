package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordsChange;
import forge.game.card.CardTraitChanges;
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
        final Card host = sa.getHostCard();
        if (tgtCards.isEmpty()) {
            return;
        }

        final Card c1;
        final Card c2;
        if (tgtCards.size() == 1) {
            c1 = host;
            c2 = tgtCards.get(0);
        } else {
            c1 = tgtCards.get(0);
            c2 = tgtCards.get(1);
        }

        // snapshot the original text boxes before modifying
        final TextBoxData data1 = captureTextBoxData(c1);
        final TextBoxData data2 = captureTextBoxData(c2);

        final Game game = host.getGame();
        final long ts = game.getNextTimestamp();

        swapTextBox(c1, data2, ts);
        swapTextBox(c2, data1, ts);

        game.fireEvent(new GameEventCardStatsChanged(c1));
        game.fireEvent(new GameEventCardStatsChanged(c2));
    }

    private static void swapTextBox(final Card to, final TextBoxData from, final long ts) {
        to.getChangedCardTraitsByText().clear();
        to.clearChangedCardKeywords(false);
        to.copyChangedTextFrom(from.textHolder);
        to.copyChangedSVarsFrom(from.textHolder);
        to.setChangedCardTraitsByText(from.traits);
        to.getChangedCardKeywordsByText().clear();
        for (Cell<Long, Long, KeywordsChange> cell : from.keywordTable.cellSet()) {
            to.getChangedCardKeywordsByText().put(cell.getRowKey(), cell.getColumnKey(), cell.getValue().copy(to, true));
        }

        List<SpellAbility> spells = Lists.newArrayList();
        for (SpellAbility s : from.spells) {
            SpellAbility cp = s.copy(to, false);
            cp.setIntrinsic(true);
            spells.add(cp);
        }
        List<Trigger> triggers = Lists.newArrayList();
        for (Trigger tr : from.triggers) {
            triggers.add(tr.copy(to, false));
        }
        List<ReplacementEffect> reps = Lists.newArrayList();
        for (ReplacementEffect re : from.replacements) {
            reps.add(re.copy(to, false));
        }
        List<StaticAbility> statics = Lists.newArrayList();
        for (StaticAbility st : from.statics) {
            statics.add(st.copy(to, false));
        }
        to.addChangedCardTraitsByText(spells, triggers, reps, statics, ts, 0);

        List<KeywordInterface> kws = Lists.newArrayList();
        for (KeywordInterface kw : from.keywords) {
            kws.add(kw.copy(to, false));
        }
        if (!kws.isEmpty()) {
            to.addChangedCardKeywordsByText(kws, ts, 0, false);
        }

        to.updateChangedText();
        to.updateStateForView();
        to.updateKeywords();
    }

    private static TextBoxData captureTextBoxData(final Card card) {
        TextBoxData data = new TextBoxData();
        data.textHolder = new Card(0, card.getGame());
        data.textHolder.copyChangedTextFrom(card);
        data.textHolder.copyChangedSVarsFrom(card);

        data.traits = HashBasedTable.create();
        for (Cell<Long, Long, CardTraitChanges> cell : card.getChangedCardTraitsByText().cellSet()) {
            data.traits.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }

        data.keywordTable = HashBasedTable.create();
        for (Cell<Long, Long, KeywordsChange> cell : card.getChangedCardKeywordsByText().cellSet()) {
            data.keywordTable.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }

        CardState state = card.getCurrentState();
        data.spells = Lists.newArrayList();
        for (SpellAbility s : state.getSpellAbilities()) {
            if (s.isIntrinsic()) {
                data.spells.add(s);
            }
        }
        data.triggers = Lists.newArrayList();
        for (Trigger t : state.getTriggers()) {
            if (t.isIntrinsic()) {
                data.triggers.add(t);
            }
        }
        data.replacements = Lists.newArrayList();
        for (ReplacementEffect r : state.getReplacementEffects()) {
            if (r.isIntrinsic()) {
                data.replacements.add(r);
            }
        }
        data.statics = Lists.newArrayList();
        for (StaticAbility st : state.getStaticAbilities()) {
            if (st.isIntrinsic()) {
                data.statics.add(st);
            }
        }
        data.keywords = Lists.newArrayList(state.getIntrinsicKeywords());
        return data;
    }

    private static class TextBoxData {
        Card textHolder;
        Table<Long, Long, CardTraitChanges> traits;
        Table<Long, Long, KeywordsChange> keywordTable;
        List<SpellAbility> spells;
        List<Trigger> triggers;
        List<ReplacementEffect> replacements;
        List<StaticAbility> statics;
        List<KeywordInterface> keywords;
    }
}

package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Table.Cell;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.card.CardCopyService;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordsChange;
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
        final Card copy1 = CardCopyService.getLKICopy(c1);
        final Card copy2 = CardCopyService.getLKICopy(c2);

        final Game game = host.getGame();
        final long ts = game.getNextTimestamp();

        swapTextBox(c1, copy2, ts);
        swapTextBox(c2, copy1, ts);

        game.fireEvent(new GameEventCardStatsChanged(c1));
        game.fireEvent(new GameEventCardStatsChanged(c2));
    }

    private static void swapTextBox(final Card to, final Card from, final long ts) {
        to.getChangedCardTraitsByText().clear();
        to.clearChangedCardKeywords(false);
        to.copyChangedTextFrom(from);
        to.copyChangedSVarsFrom(from);
        to.setChangedCardTraitsByText(from.getChangedCardTraitsByText());
        to.getChangedCardKeywordsByText().clear();
        for (Cell<Long, Long, KeywordsChange> cell : from.getChangedCardKeywordsByText().cellSet()) {
            to.getChangedCardKeywordsByText().put(cell.getRowKey(), cell.getColumnKey(), cell.getValue().copy(to, true));
        }

        final CardState src = from.getCurrentState();
        List<SpellAbility> spells = Lists.newArrayList();
        for (SpellAbility s : src.getSpellAbilities()) {
            if (s.isIntrinsic()) {
                SpellAbility cp = s.copy(to, false);
                cp.setIntrinsic(true);
                spells.add(cp);
            }
        }
        List<Trigger> triggers = Lists.newArrayList();
        for (Trigger tr : src.getTriggers()) {
            if (tr.isIntrinsic()) {
                triggers.add(tr.copy(to, false));
            }
        }
        List<ReplacementEffect> reps = Lists.newArrayList();
        for (ReplacementEffect re : src.getReplacementEffects()) {
            if (re.isIntrinsic()) {
                reps.add(re.copy(to, false));
            }
        }
        List<StaticAbility> statics = Lists.newArrayList();
        for (StaticAbility st : src.getStaticAbilities()) {
            if (st.isIntrinsic()) {
                statics.add(st.copy(to, false));
            }
        }
        to.addChangedCardTraitsByText(spells, triggers, reps, statics, ts, 0);

        List<KeywordInterface> kws = Lists.newArrayList();
        for (KeywordInterface kw : src.getIntrinsicKeywords()) {
            kws.add(kw.copy(to, false));
        }
        if (!kws.isEmpty()) {
            to.addChangedCardKeywordsByText(kws, ts, 0, false);
        }

        to.updateChangedText();
        to.updateStateForView();
        to.updateKeywords();
    }
}

package forge.game.cost;

import java.util.List;

import forge.game.GameEntityCounterTable;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.TextUtil;

public class PaymentDecision {
    public int c = 0;
    public String type;
    public List<String> colors;

    public final CardCollection cards = new CardCollection();
    public final List<Mana> mana;
    public final List<Player> players;
    public final List<SpellAbility> sp;

    // used for CostRemoveAnyCounter
    public final GameEntityCounterTable counterTable;
    public ManaConversionMatrix matrix = null;

    public PaymentDecision(int cnt) {
        this(null, null, null, null, null);
        c = cnt;
    }

    private PaymentDecision(Iterable<Card> chosen, List<Mana> manaProduced, List<Player> players,
                List<SpellAbility> sp, GameEntityCounterTable counterTable) {
        if (chosen != null) {
            cards.addAll(chosen);
        }
        mana = manaProduced;
        this.players = players;
        this.sp = sp;
        this.counterTable = counterTable;
    }

    private PaymentDecision(Card chosen) {
        this(null, null, null, null, null);
        cards.add(chosen);
    }

    public PaymentDecision(String choice) {
        this(null, null, null, null, null);
        type = choice;
    }

    public PaymentDecision(List<String> choices) {
        this(null, null, null, null, null);
        colors = choices;
    }

    public static PaymentDecision card(Card chosen) {
        return new PaymentDecision(chosen);
    }

    public static PaymentDecision card(Card chosen, int n) {
        PaymentDecision res = new PaymentDecision(chosen);
        res.c = n;
        return res;
    }

    public static PaymentDecision number(int c) {
        return new PaymentDecision(c);
    }

    public static PaymentDecision card(Iterable<Card> chosen) {
        return new PaymentDecision(chosen, null, null, null, null);
    }

    public static PaymentDecision card(Iterable<Card> chosen, int n) {
        PaymentDecision res = new PaymentDecision(chosen, null, null, null, null);
        res.c = n;
        return res;
    }

    public static PaymentDecision mana(List<Mana> manas) {
        return new PaymentDecision(null, manas, null, null, null);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return TextUtil.concatWithSpace("Payment Decision:", TextUtil.addSuffix(String.valueOf(c),","), cards.toString());
    }

    public static PaymentDecision type(String choice) {
        return new PaymentDecision(choice);
    }

    public static PaymentDecision colors(List<String> choices) {
        return new PaymentDecision(choices);
    }

    public static PaymentDecision players(List<Player> players) {
        return new PaymentDecision(null, null, players, null, null);
    }

    public static PaymentDecision spellabilities(List<SpellAbility> sp) {
        return new PaymentDecision(null, null, null, sp, null);
    }

    public static PaymentDecision counters(GameEntityCounterTable counterTable) {
        return new PaymentDecision(null, null, null, null, counterTable);
    }
}

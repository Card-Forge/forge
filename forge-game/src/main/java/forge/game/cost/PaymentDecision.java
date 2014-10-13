package forge.game.cost;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterType;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;


public class PaymentDecision {
    public int c = 0;
    public String type;
    public CounterType ct;

    public final CardCollection cards = new CardCollection();
    public final List<Mana> mana;
    public final List<Player> players;
    public final List<SpellAbility> sp;

    public PaymentDecision(int cnt) {
        this(null, null, null, null);
        c = cnt;
    }

    private PaymentDecision(Iterable<Card> chosen, List<Mana> manaProduced, List<Player> players,
                List<SpellAbility> sp) {
        if (chosen != null) {
            cards.addAll(chosen);
        }
        mana = manaProduced;
        this.players = players;
        this.sp = sp;
    }
    
    private  PaymentDecision(Card chosen) {
        this(null, null, null, null);
        cards.add(chosen);
    }
    
    public PaymentDecision(String choice) {
        this(null, null, null, null);
        type = choice;
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
        return new PaymentDecision(chosen, null, null, null);
    }
    
    public static PaymentDecision card(Iterable<Card> chosen, int n) {
        PaymentDecision res = new PaymentDecision(chosen, null, null, null);
        res.c = n;
        return res;
    }
    
    public static PaymentDecision mana(List<Mana> manas) {
        return new PaymentDecision(null, manas, null, null);
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return  String.format("Payment Decision: %d, %s", c, cards);
    }

    public static PaymentDecision type(String choice) {
        return new PaymentDecision(choice);
    }

    public static PaymentDecision players(List<Player> players) {
        // TODO Auto-generated method stub
        return new PaymentDecision(null, null, players, null);
    }
    
    public static PaymentDecision spellabilities(List<SpellAbility> sp) {
        return new PaymentDecision(null, null, null, sp);
    }

    public static PaymentDecision card(Card selected, CounterType counterType) {
        PaymentDecision res = card(selected);
        res.ct = counterType;
        return res;
    }
}

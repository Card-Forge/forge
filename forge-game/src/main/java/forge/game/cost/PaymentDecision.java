package forge.game.cost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.mana.Mana;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PaymentDecision {
    public int c = 0;
    public String type;
    public CounterType ct;
    
    public final List<Card> cards = new ArrayList<Card>();
    public final List<Mana> mana;
    public final List<Player> players;
    
    public PaymentDecision(int cnt) {
        this(null, null, null);
        c = cnt;
    }

    private PaymentDecision(Collection<Card> chosen, List<Mana> manaProduced, List<Player> players) {
        if(chosen != null)
            cards.addAll(chosen);
        mana = manaProduced;
        this.players = players;
    }
    
    private  PaymentDecision(Card chosen) {
        this(null, null, null);
        cards.add(chosen);
    }
    
    public PaymentDecision(String choice) {
        this(null, null, null);
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

    public static PaymentDecision card(Collection<Card> chosen) {
        return new PaymentDecision(chosen, null, null);
    }
    
    public static PaymentDecision card(Collection<Card> chosen, int n) {
        PaymentDecision res = new PaymentDecision(chosen, null, null);
        res.c = n;
        return res;
    }
    
    public static PaymentDecision mana(List<Mana> manas) {
        return new PaymentDecision(null, manas, null);
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
        return new PaymentDecision(null, null, players);
    }

    public static PaymentDecision card(Card selected, CounterType counterType) {
        PaymentDecision res = card(selected);
        res.ct = counterType;
        return res;
    }
}

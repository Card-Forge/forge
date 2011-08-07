package forge.card.spellability;

import com.esotericsoftware.minlog.Log;

import forge.Card;

abstract public class Ability_Static extends Ability {
    public Ability_Static(Card sourceCard, String manaCost) {
        super(sourceCard, manaCost);
    }
}

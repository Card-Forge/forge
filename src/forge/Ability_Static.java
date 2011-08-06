package forge;

import com.esotericsoftware.minlog.Log;

abstract public class Ability_Static extends Ability {
    public Ability_Static(Card sourceCard, String manaCost) {
        super(sourceCard, manaCost);
    }
    
    public Ability_Static(Card sourceCard, String manaCost, String stackDescription) {
        this(sourceCard, manaCost);
        setStackDescription(stackDescription);
        Log.debug("an ability is being played from" + sourceCard.getName());
    }
}

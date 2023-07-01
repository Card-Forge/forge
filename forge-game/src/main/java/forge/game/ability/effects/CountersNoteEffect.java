package forge.game.ability.effects;

import java.util.Map.Entry;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class CountersNoteEffect extends SpellAbilityEffect {
    // Primarily used for "Noting" Counters on a card. i.e. Oubliette or Tawnos' Coffin
    // Counters get "noted" as SVars onto the Source
    // "Mode": "Store" or "Load"
    final static String MODE_STORE = "Store";
    final static String MODE_LOAD = "Load";
    final static String NOTE_COUNTERS = "NoteCounters";

    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        Player p = sa.getActivatingPlayer();
        String mode = sa.getParamOrDefault("Mode", "Load");

        for (Card c : getDefinedCardsOrTargeted(sa)) {
            if (mode.equals(MODE_STORE)) {
                noteCounters(c, source);
            } else if (mode.equals(MODE_LOAD)) {
                loadCounters(c, source, p, sa);
            }
        }
    }

    public static void noteCounters(Card notee, Card source) {
        for (Entry<CounterType, Integer> counter : notee.getCounters().entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(NOTE_COUNTERS).append(counter.getKey().getName());
            source.setSVar(sb.toString(), counter.getValue().toString());
        }
    }

    private void loadCounters(Card notee, Card source, final Player p, final SpellAbility sa) {
        for (Entry<String, String> svar : source.getSVars().entrySet()) {
            String key = svar.getKey();
            if (key.startsWith(NOTE_COUNTERS)) {
                notee.addEtbCounter(
                        CounterType.getType(key.substring(NOTE_COUNTERS.length())),
                        Integer.parseInt(svar.getValue()), p);
            }
            // TODO Probably should "remove" the svars that were temporarily used
        }
    }
}

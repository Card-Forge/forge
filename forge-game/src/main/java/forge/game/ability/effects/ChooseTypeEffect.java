package forge.game.ability.effects;

import forge.card.CardType;
import forge.deck.CardPool;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.item.PaperCard;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ChooseTypeEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a type.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final String type = sa.getParam("Type");
        final List<String> invalidTypes = sa.hasParam("InvalidTypes") ? Arrays.asList(sa.getParam("InvalidTypes").split(",")) : new ArrayList<String>();

        final List<String> validTypes = new ArrayList<String>();
        if (sa.hasParam("ValidTypes")) {
            validTypes.addAll(Arrays.asList(sa.getParam("ValidTypes").split(",")));
        }

        if (validTypes.isEmpty()) {
            switch (type) {
            case "Card":
                validTypes.addAll(CardType.getAllCardTypes());
                break;
            case "Creature":
                validTypes.addAll(CardType.getAllCreatureTypes());
                sortCreatureTypes(validTypes, sa);
                break;
            case "Basic Land":
                validTypes.addAll(CardType.getBasicTypes());
                break;
            case "Land":
                validTypes.addAll(CardType.getAllLandTypes());
                break;
            }
        }

        for (final String s : invalidTypes) {
            validTypes.remove(s);
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (!validTypes.isEmpty()) {
            for (final Player p : tgtPlayers) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    String choice = p.getController().chooseSomeType(type, sa, validTypes, invalidTypes);
                    card.setChosenType(choice);
                }
            }
        }
        else {
            throw new InvalidParameterException(sa.getHostCard() + "'s ability resulted in no types to choose from");
        }
    }

    //sort creature types such that the types most prevalent
    //in the activating player's deck are sorted to the top
    private void sortCreatureTypes(List<String> validTypes, SpellAbility sa) {
        //build map of creature types in player's main deck against the occurrences of each
        CardPool pool = sa.getActivatingPlayer().getRegisteredPlayer().getDeck().getMain();
        HashMap<String, Integer> typesInDeck = new HashMap<String, Integer>();
        for (Entry<PaperCard, Integer> entry : pool) {
            Set<String> cardCreatureTypes = entry.getKey().getRules().getType().getCreatureTypes();
            for (String type : cardCreatureTypes) {
                Integer count = typesInDeck.get(type);
                if (count == null) { count = 0; }
                typesInDeck.put(type, count + entry.getValue());
            }
        }

        //create sorted list from map from least to most frequent 
        List<Entry<String, Integer>> sortedList = new LinkedList<Entry<String, Integer>>(typesInDeck.entrySet());
        Collections.sort(sortedList, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        //loop through sorted list and move each type to the front of the validTypes collection
        for (Entry<String, Integer> entry : sortedList) {
            String type = entry.getKey();
            if (validTypes.remove(type)) { //ensure an invalid type isn't introduced
                validTypes.add(0, type);
            }
        }
    }
}

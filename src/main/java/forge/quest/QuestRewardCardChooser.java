package forge.quest;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import forge.Singletons;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.card.CardRules;
import forge.item.InventoryItem;
import forge.item.ItemPool;

/** 
 * Resolves a card chooser InventoryItem into a CardPrinted.
 * The initial version includes "duplicate", other type may be added later.
 *
 */
public class QuestRewardCardChooser implements InventoryItem  {

    /**
     * Possible types for this object.
     */
    public enum poolType {
        /** The player's own cardpool (duplicate card). */
        playerCards,
        /** Filtered by a predicate that will be parsed. */
        predicateFilter
    }

    private poolType type;
    private final String description;
    private final Predicate<CardPrinted> predicates;

    /**
     * The constructor.
     * The parameter indicates the more specific type.
     * @param setType String, the type of the choosable card.
     * @param creationParameters String, used to build the predicates and description for the predicateFilter type
     */
    public QuestRewardCardChooser(final poolType setType, final String[] creationParameters) {
        type = setType;
        if (type == poolType.playerCards) {
            description = "a duplicate card";
            predicates = null;
        } else {
            description = buildDescription(creationParameters);
            predicates = buildPredicates(creationParameters);
        }
    }

    private String buildDescription(final String [] input) {
        final String defaultDescription = "a card";
        if (input == null || input.length < 1) {
            return defaultDescription;
        }

        String buildDesc = null;

        for (String s : input) {
            if (s.startsWith("desc:") || s.startsWith("Desc:")) {
                String[] tmp = s.split(":");
                if (tmp.length > 1) {
                    buildDesc = new String(tmp[1]);
                } else {
                    buildDesc = new String();
                }
            } else if (buildDesc != null) {
                if (s.contains(":")) {
                    return buildDesc;
                } else {
                    buildDesc = buildDesc + " " + s;
                }
            }
        }

        if (buildDesc != null) {
            return buildDesc;
        }
        return defaultDescription;
    }


    private Predicate<CardPrinted> buildPredicates(final String [] input) {
        if (input == null || input.length < 1) {
            return null;
        }

        Predicate<CardPrinted> filters = Singletons.getModel().getQuest().getFormat().getFilterPrinted();
        Predicate<CardRules> filterRules = null;
        Predicate<CardPrinted> filterRarity = null;

        for (String s : input) {
            if (s.startsWith("sets:") || s.startsWith("Sets:")) {
                final String[] tmp = s.split(":");
                if (tmp.length > 1) {
                    String [] setcodes = tmp[1].split(",");
                    if (setcodes.length > 0) {
                        List<String> sets = new ArrayList<String>();
                        for (String code : setcodes) {
                            if (Singletons.getModel().getEditions().contains(code)) {
                                // System.out.println("Set " + code + " was found!");
                                sets.add(code);
                            }
                            // else { System.out.println("Unknown set code " + code); }
                        }
                        if (sets.size() > 0) {
                            filters = CardPrinted.Predicates.printedInSets(sets, true);
                        }
                    }
                }
            } else if (s.startsWith("rules:") || s.startsWith("Rules:")) {
                final String[] tmp = s.split(":");
                if (tmp.length > 1) {
                    String [] ruleCodes = tmp[1].split(",");
                    if (ruleCodes.length > 0) {
                        for (String rule : ruleCodes) {
                            final Predicate<CardRules> newRule = BoosterUtils.parseRulesLimitation(rule);
                            if (newRule != null) {
                                filterRules = (filterRules == null ? newRule : Predicates.and(filterRules, newRule));
                            }
                        }
                    }
                }
            } else if (s.startsWith("rarity:") || s.startsWith("Rarity:")) {
                final String[] tmp = s.split(":");
                if (tmp.length > 1) {
                    String [] rarityCodes = tmp[1].split(",");
                    if (rarityCodes.length > 0) {
                        for (String rarity : rarityCodes) {
                            if (rarity.startsWith("C") || rarity.startsWith("c")) {
                                filterRarity = (filterRarity == null ? CardPrinted.Predicates.Presets.IS_COMMON : Predicates.or(filterRarity, CardPrinted.Predicates.Presets.IS_COMMON));
                            } else if (rarity.startsWith("U") || rarity.startsWith("u")) {
                                filterRarity = (filterRarity == null ? CardPrinted.Predicates.Presets.IS_UNCOMMON : Predicates.or(filterRarity, CardPrinted.Predicates.Presets.IS_UNCOMMON));
                            } else if (rarity.startsWith("R") || rarity.startsWith("r")) {
                                filterRarity = (filterRarity == null ? CardPrinted.Predicates.Presets.IS_RARE : Predicates.or(filterRarity, CardPrinted.Predicates.Presets.IS_RARE));
                            } else if (rarity.startsWith("M") || rarity.startsWith("m")) {
                                filterRarity = (filterRarity == null ? CardPrinted.Predicates.Presets.IS_MYTHIC_RARE : Predicates.or(filterRarity, CardPrinted.Predicates.Presets.IS_MYTHIC_RARE));
                            }
                        }
                    }
                }
            }
        }

        if (filterRules != null) {
            final Predicate<CardPrinted> rulesPrinted = Predicates.compose(filterRules, CardPrinted.FN_GET_RULES);
            filters = Predicates.and(filters, rulesPrinted);
        }
        if (filterRarity != null) {
            filters = Predicates.and(filters, filterRarity);
        }
        return filters;
    }

    /**
     * The name.
     * 
     * @return the name
     */
    @Override
    public String getName() {
        return description;
    }

    /**
     * A QuestRewardCardChooser ought to always be resolved to an actual card, hence no images.
     * 
     * @return an empty string
     */
    @Override
    public String getImageFilename() {
        return "";
    }

    /**
     * The item type.
     * 
     * @return item type
     */
    @Override
    public String getItemType() {
        switch (type) {
            case playerCards:
                return "duplicate card";
            case predicateFilter: default:
                return "chosen card";
        }
    }

    /**
     * Get type as enum.
     * @return enum, item type.
     */
    public poolType getType() {
        return type;
    }

    /**
     * Produces a list of options to choose from.
     * 
     * @return a List<CardPrinted> or null if could not create a list.
     */
    public final List<CardPrinted> getChoices() {
        if (type == poolType.playerCards) {
            final ItemPool<CardPrinted> playerCards = Singletons.getModel().getQuest().getAssets().getCardPool();
            if (!playerCards.isEmpty()) { // Maybe a redundant check since it's hard to win a duel without any cards...

                List<CardPrinted> cardChoices = new ArrayList<CardPrinted>();
                for (final Map.Entry<CardPrinted, Integer> card : playerCards) {
                    cardChoices.add(card.getKey());
                }
                Collections.sort(cardChoices);

                return Collections.unmodifiableList(cardChoices);
            }

        } else if (type == poolType.predicateFilter) {
            List<CardPrinted> cardChoices = new ArrayList<CardPrinted>();

            for (final CardPrinted card : Iterables.filter(CardDb.instance().getAllCards(), predicates)) {
                cardChoices.add(card);
            }
            Collections.sort(cardChoices);

            return Collections.unmodifiableList(cardChoices);
        } else {
            throw new RuntimeException("Unknown QuestRewardCardType: " + type);
        }
        return null;
    }
}

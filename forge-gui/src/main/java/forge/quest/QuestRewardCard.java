package forge.quest;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.model.FModel;

public abstract class QuestRewardCard implements IQuestRewardCard {

    protected String buildDescription(final String [] input) {
        final String defaultDescription = "a card";
        if (input == null || input.length < 1) {
            return defaultDescription;
        }

        String buildDesc = null;

        for (final String s : input) {
            if (s.startsWith("desc:") || s.startsWith("Desc:")) {
                final String[] tmp = s.split(":");
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

    protected Predicate<PaperCard> buildPredicates(final String [] input) {
        if (input == null || input.length < 1) {
            return null;
        }

        Predicate<PaperCard> filters = FModel.getQuest().getFormat().getFilterPrinted();
        Predicate<CardRules> filterRules = null;
        Predicate<PaperCard> filterRarity = null;

        for (final String s : input) {
            if (s.startsWith("sets:") || s.startsWith("Sets:")) {
                final String[] tmp = s.split(":");
                if (tmp.length > 1) {
                    final String [] setcodes = tmp[1].split(",");
                    if (setcodes.length > 0) {
                        final List<String> sets = new ArrayList<String>();
                        for (final String code : setcodes) {
                            if (FModel.getMagicDb().getEditions().contains(code)) {
                                // System.out.println("Set " + code + " was found!");
                                sets.add(code);
                            }
                            // else { System.out.println("Unknown set code " + code); }
                        }
                        if (sets.size() > 0) {
                            filters = IPaperCard.Predicates.printedInSets(sets, true);
                        }
                    }
                }
            } else if (s.startsWith("rules:") || s.startsWith("Rules:")) {
                final String[] tmp = s.split(":");
                if (tmp.length > 1) {
                    final String [] ruleCodes = tmp[1].split(",");
                    if (ruleCodes.length > 0) {
                        for (final String rule : ruleCodes) {
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
                    final String [] rarityCodes = tmp[1].split(",");
                    if (rarityCodes.length > 0) {
                        for (final String rarity : rarityCodes) {
                            if (rarity.startsWith("C") || rarity.startsWith("c")) {
                                filterRarity = (filterRarity == null ? IPaperCard.Predicates.Presets.IS_COMMON : Predicates.or(filterRarity, IPaperCard.Predicates.Presets.IS_COMMON));
                            } else if (rarity.startsWith("U") || rarity.startsWith("u")) {
                                filterRarity = (filterRarity == null ? IPaperCard.Predicates.Presets.IS_UNCOMMON : Predicates.or(filterRarity, IPaperCard.Predicates.Presets.IS_UNCOMMON));
                            } else if (rarity.startsWith("R") || rarity.startsWith("r")) {
                                filterRarity = (filterRarity == null ? IPaperCard.Predicates.Presets.IS_RARE : Predicates.or(filterRarity, IPaperCard.Predicates.Presets.IS_RARE));
                            } else if (rarity.startsWith("M") || rarity.startsWith("m")) {
                                filterRarity = (filterRarity == null ? IPaperCard.Predicates.Presets.IS_MYTHIC_RARE : Predicates.or(filterRarity, IPaperCard.Predicates.Presets.IS_MYTHIC_RARE));
                            }
                        }
                    }
                }
            }
        }

        if (filterRules != null) {
            final Predicate<PaperCard> rulesPrinted = Predicates.compose(filterRules, PaperCard.FN_GET_RULES);
            filters = Predicates.and(filters, rulesPrinted);
        }
        if (filterRarity != null) {
            filters = Predicates.and(filters, filterRarity);
        }
        return filters;
    }

}
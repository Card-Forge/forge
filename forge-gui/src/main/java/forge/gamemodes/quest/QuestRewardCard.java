package forge.gamemodes.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import forge.card.CardRules;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.model.FModel;

public abstract class QuestRewardCard implements IQuestRewardCard {

    protected String buildDescription(final String [] input) {
        final String defaultDescription = "a card";
        if (input == null || input.length < 1) {
            return defaultDescription;
        }

        StringBuilder buildDesc = null;

        for (final String s : input) {
            if (s.startsWith("desc:") || s.startsWith("Desc:")) {
                final String[] tmp = s.split(":");
                if (tmp.length > 1) {
                    buildDesc = new StringBuilder(tmp[1]);
                } else {
                    buildDesc = new StringBuilder();
                }
            } else if (buildDesc != null) {
                if (s.contains(":")) {
                    return buildDesc.toString();
                } else {
                    buildDesc.append(" ").append(s);
                }
            }
        }

        if (buildDesc != null) {
            return buildDesc.toString();
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
                        final List<String> sets = new ArrayList<>();
                        for (final String code : setcodes) {
                            if (FModel.getMagicDb().getEditions().contains(code)) {
                                // System.out.println("Set " + code + " was found!");
                                sets.add(code);
                            }
                            // else { System.out.println("Unknown set code " + code); }
                        }
                        if (sets.size() > 0) {
                            filters = PaperCardPredicates.printedInSets(sets, true);
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
                                filterRules = filterRules == null ? newRule : filterRules.and(newRule);
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
                                filterRarity = filterRarity == null ? PaperCardPredicates.IS_COMMON : filterRarity.or(PaperCardPredicates.IS_COMMON);
                            } else if (rarity.startsWith("U") || rarity.startsWith("u")) {
                                filterRarity = filterRarity == null ? PaperCardPredicates.IS_UNCOMMON : filterRarity.or(PaperCardPredicates.IS_UNCOMMON);
                            } else if (rarity.startsWith("R") || rarity.startsWith("r")) {
                                filterRarity = filterRarity == null ? PaperCardPredicates.IS_RARE : filterRarity.or(PaperCardPredicates.IS_RARE);
                            } else if (rarity.startsWith("M") || rarity.startsWith("m")) {
                                filterRarity = filterRarity == null ? PaperCardPredicates.IS_MYTHIC_RARE : filterRarity.or(PaperCardPredicates.IS_MYTHIC_RARE);
                            }
                        }
                    }
                }
            }
        }

        if (filterRules != null) {
            final Predicate<PaperCard> rulesPrinted = PaperCardPredicates.fromRules(filterRules);
            filters = filters.and(rulesPrinted);
        }
        if (filterRarity != null) {
            filters = filters.and(filterRarity);
        }
        return filters;
    }

    @Override
    public String getImageKey(boolean altState) {
        return null;
    }
}
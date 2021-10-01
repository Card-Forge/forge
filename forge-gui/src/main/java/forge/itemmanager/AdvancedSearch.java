package forge.itemmanager;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.CardType;
import forge.card.CardType.CoreType;
import forge.card.CardType.Supertype;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.game.GameFormat;
import forge.game.keyword.Keyword;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestRegion;
import forge.gamemodes.quest.QuestSpellShop;
import forge.gamemodes.quest.QuestWorld;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.UiCommand;
import forge.gui.interfaces.IButton;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.model.FModel;
import forge.util.CardTranslation;
import forge.util.Localizer;

public class AdvancedSearch {
    public enum FilterOption {
        NONE("lblNone", null, null, null),
        CARD_NAME("lblName", PaperCard.class, FilterOperator.STRINGS_OPS, new StringEvaluator<PaperCard>() {
            @Override
            protected String getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<String> getItemValues(PaperCard input) {
                Set<String> names = new HashSet<>();
                names.add(input.getName());
                names.add(CardTranslation.getTranslatedName(input.getName()));
                CardSplitType cardSplitType = input.getRules().getSplitType();
                if (cardSplitType != CardSplitType.None && cardSplitType != CardSplitType.Split) {
                    if (input.getRules().getOtherPart() != null) {
                        names.add(input.getRules().getOtherPart().getName());
                        names.add(CardTranslation.getTranslatedName(input.getRules().getOtherPart().getName()));
                    }
                }
                return names;
            }
        }),
        CARD_RULES_TEXT("lblRulesText", PaperCard.class, FilterOperator.STRINGS_OPS, new StringEvaluator<PaperCard>() {
            @Override
            protected String getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<String> getItemValues(PaperCard input) {
                Set<String> names = new HashSet<>();
                names.add(input.getRules().getOracleText());
                names.add(CardTranslation.getTranslatedOracle(input.getName()));
                CardSplitType cardSplitType = input.getRules().getSplitType();
                if (cardSplitType != CardSplitType.None && cardSplitType != CardSplitType.Split) {
                    if (input.getRules().getOtherPart() != null) {
                        names.add(input.getRules().getOtherPart().getOracleText());
                        names.add(CardTranslation.getTranslatedOracle(input.getRules().getOtherPart().getName()));
                    }
                }
                return names;
            }
        }),
        CARD_KEYWORDS("lblKeywords", PaperCard.class, FilterOperator.COLLECTION_OPS, new CustomListEvaluator<PaperCard, Keyword>(Keyword.getAllKeywords()) {
            @Override
            protected Keyword getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<Keyword> getItemValues(PaperCard input) {
                CardSplitType cardSplitType = input.getRules().getSplitType();
                if (cardSplitType != CardSplitType.None && cardSplitType != CardSplitType.Split) {
                    Set<Keyword> keywords = new HashSet<>();
                    if (input.getRules().getOtherPart() != null) {
                        PaperCard otherPart = FModel.getMagicDb().getCommonCards().getCard(input.getRules().getOtherPart().getName());
                        if (otherPart != null) {
                            keywords.addAll(Keyword.getKeywordSet(otherPart));
                            keywords.addAll(Keyword.getKeywordSet(input));
                        }
                    }
                    return keywords;
                }
                return Keyword.getKeywordSet(input);
            }
        }),
        CARD_SET("lblSet", PaperCard.class, FilterOperator.SINGLE_LIST_OPS, new CustomListEvaluator<PaperCard, CardEdition>(FModel.getMagicDb().getSortedEditions(), CardEdition.FN_GET_CODE) {
            @Override
            protected CardEdition getItemValue(PaperCard input) {
                return FModel.getMagicDb().getCardEdition(input.getEdition());
            }
        }),
        CARD_FORMAT("lblFormat", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<PaperCard, GameFormat>((List<GameFormat>)FModel.getFormats().getFilterList()) {
            @Override
            protected GameFormat getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<GameFormat> getItemValues(PaperCard input) {
                return FModel.getFormats().getAllFormatsOfCard(input);
            }
        }),
        CARD_PLANE("lblPlane", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<PaperCard, ConquestPlane>(ImmutableList.copyOf(FModel.getPlanes())) {
            @Override
            protected ConquestPlane getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<ConquestPlane> getItemValues(PaperCard input) {
                return ConquestPlane.getAllPlanesOfCard(input);
            }
        }),
        CARD_REGION("lblRegion", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<PaperCard, ConquestRegion>(ConquestRegion.getAllRegions()) {
            @Override
            protected ConquestRegion getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<ConquestRegion> getItemValues(PaperCard input) {
                return ConquestRegion.getAllRegionsOfCard(input);
            }
        }),
        CARD_QUEST_WORLD("lblQuestWorld", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<PaperCard, QuestWorld>(ImmutableList.copyOf(FModel.getWorlds())) {
            @Override
            protected QuestWorld getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<QuestWorld> getItemValues(PaperCard input) {
                return QuestWorld.getAllQuestWorldsOfCard(input);
            }
        }),
        CARD_COLOR("lblColor", PaperCard.class, FilterOperator.COMBINATION_OPS, new ColorEvaluator<PaperCard>() {
            @Override
            protected MagicColor.Color getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<MagicColor.Color> getItemValues(PaperCard input) {
                return input.getRules().getColor().toEnumSet();
            }
        }),
        CARD_COLOR_IDENTITY("lblColorIdentity", PaperCard.class, FilterOperator.COMBINATION_OPS, new ColorEvaluator<PaperCard>() {
            @Override
            protected MagicColor.Color getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<MagicColor.Color> getItemValues(PaperCard input) {
                return input.getRules().getColorIdentity().toEnumSet();
            }
        }),
        CARD_COLOR_COUNT("lblColorCount", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<PaperCard>(0, 5) {
            @Override
            protected Integer getItemValue(PaperCard input) {
                return input.getRules().getColor().countColors();
            }
        }),
        CARD_TYPE("lblType", PaperCard.class, FilterOperator.COMBINATION_OPS, new CustomListEvaluator<PaperCard, String>(CardType.getCombinedSuperAndCoreTypes()) {
            @Override
            protected String getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<String> getItemValues(PaperCard input) {
                final CardType type = input.getRules().getType();
                final Set<String> types = new HashSet<>();
                CardSplitType cardSplitType = input.getRules().getSplitType();
                if (cardSplitType != CardSplitType.None && cardSplitType != CardSplitType.Split) {
                    if (input.getRules().getOtherPart() != null) {
                        for (Supertype supertype : input.getRules().getOtherPart().getType().getSupertypes()) {
                            types.add(supertype.name());
                        }
                        for (CoreType coreType : input.getRules().getOtherPart().getType().getCoreTypes()) {
                            types.add(coreType.name());
                        }
                        for (Supertype supertype : input.getRules().getMainPart().getType().getSupertypes()) {
                            types.add(supertype.name());
                        }
                        for (CoreType coreType : input.getRules().getMainPart().getType().getCoreTypes()) {
                            types.add(coreType.name());
                        }
                        return types;
                    }
                }
                for (Supertype t : type.getSupertypes()) {
                    types.add(t.name());
                }
                for (CoreType t : type.getCoreTypes()) {
                    types.add(t.name());
                }
                return types;
            }
        }),
        CARD_SUB_TYPE("lblSubtype", PaperCard.class, FilterOperator.COMBINATION_OPS, new CustomListEvaluator<PaperCard, String>(CardType.getSortedSubTypes()) {
            @Override
            protected String getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<String> getItemValues(PaperCard input) {
                CardSplitType cardSplitType = input.getRules().getSplitType();
                if (cardSplitType != CardSplitType.None && cardSplitType != CardSplitType.Split) {
                    if (input.getRules().getOtherPart() != null) {
                        Set<String> subtypes = new HashSet<>();
                        for (String subs : input.getRules().getOtherPart().getType().getSubtypes()) {
                            subtypes.add(subs);
                        }
                        for (String subs : input.getRules().getMainPart().getType().getSubtypes()) {
                            subtypes.add(subs);
                        }
                        return subtypes;
                    }
                }
                return (Set<String>)input.getRules().getType().getSubtypes();
            }
        }),
        CARD_CMC("lblCMC", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<PaperCard>(0, 20) {
            @Override
            protected Integer getItemValue(PaperCard input) {
                return input.getRules().getManaCost().getCMC();
            }
        }),
        CARD_GENERIC_COST("lblGenericCost", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<PaperCard>(0, 20) {
            @Override
            protected Integer getItemValue(PaperCard input) {
                return input.getRules().getManaCost().getGenericCost();
            }
        }),
        CARD_POWER("lblPower", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<PaperCard>(0, 20) {
            @Override
            protected Integer getItemValue(PaperCard input) {
                CardRules rules = input.getRules();
                if (rules.getType().isCreature()) {
                    return rules.getIntPower();
                }
                return null;
            }
        }),
        CARD_TOUGHNESS("lblToughness", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<PaperCard>(0, 20) {
            @Override
            protected Integer getItemValue(PaperCard input) {
                CardRules rules = input.getRules();
                if (rules.getType().isCreature()) {
                    return rules.getIntToughness();
                }
                return null;
            }
        }),
        CARD_MANA_COST("lblManaCost", PaperCard.class, FilterOperator.STRING_OPS, new StringEvaluator<PaperCard>() {
            @Override
            protected String getItemValue(PaperCard input) {
                return input.getRules().getManaCost().toString();
            }
        }),
        CARD_RARITY("lblRarity", PaperCard.class, FilterOperator.SINGLE_LIST_OPS, new CustomListEvaluator<PaperCard, CardRarity>(Arrays.asList(CardRarity.FILTER_OPTIONS), CardRarity.FN_GET_LONG_NAME, CardRarity.FN_GET_LONG_NAME) {
            @Override
            protected CardRarity getItemValue(PaperCard input) {
                return input.getRarity();
            }
        }),
        CARD_FIRST_PRINTING("lblFirstPrinting", PaperCard.class, FilterOperator.BOOLEAN_OPS, new BooleanEvaluator<PaperCard>() {
            @Override
            protected Boolean getItemValue(PaperCard input) {
                List<PaperCard> cards = FModel.getMagicDb().getCommonCards().getAllCards(input.getName());
                if (cards.size() <= 1) { return true; }

                Collections.sort(cards, FModel.getMagicDb().getEditions().CARD_EDITION_COMPARATOR);
                return cards.get(0) == input;
            }
        }),
        INVITEM_NAME("lblName", InventoryItem.class, FilterOperator.STRING_OPS, new StringEvaluator<InventoryItem>() {
            @Override
            protected String getItemValue(InventoryItem input) {
                return input.getName();
            }
        }),
        INVITEM_RULES_TEXT("lblRulesText", InventoryItem.class, FilterOperator.STRING_OPS, new StringEvaluator<InventoryItem>() {
            @Override
            protected String getItemValue(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return "";
                }
                return ((PaperCard)input).getRules().getOracleText();
            }
        }),
        INVITEM_KEYWORDS("lblKeywords", InventoryItem.class, FilterOperator.COLLECTION_OPS, new CustomListEvaluator<InventoryItem, Keyword>(Keyword.getAllKeywords()) {
            @Override
            protected Keyword getItemValue(InventoryItem input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<Keyword> getItemValues(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return new HashSet<>();
                }
                return Keyword.getKeywordSet((PaperCard)input);
            }
        }),
        INVITEM_SET("lblSet", InventoryItem.class, FilterOperator.SINGLE_LIST_OPS, new CustomListEvaluator<InventoryItem, CardEdition>(FModel.getMagicDb().getSortedEditions(), CardEdition.FN_GET_CODE) {
            @Override
            protected CardEdition getItemValue(InventoryItem input) {
                if (input instanceof PaperCard) {
                    CardEdition set = FModel.getMagicDb().getEditions().get(((PaperCard)input).getEdition());
                    if (set == null)  // try custom set
                        set = FModel.getMagicDb().getCustomEditions().get(((PaperCard)input).getEdition());
                    return set;
                } else if (input instanceof SealedProduct) {
                    return FModel.getMagicDb().getEditions().get(((SealedProduct)input).getEdition());
                } else {
                    return CardEdition.UNKNOWN;
                }
            }
        }),
        INVITEM_FORMAT("lblFormat", InventoryItem.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<InventoryItem, GameFormat>((List<GameFormat>)FModel.getFormats().getFilterList()) {
            @Override
            protected GameFormat getItemValue(InventoryItem input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<GameFormat> getItemValues(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return new HashSet<>();
                }
                return FModel.getFormats().getAllFormatsOfCard((PaperCard)input);
            }
        }),
        INVITEM_PLANE("lblPlane", InventoryItem.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<InventoryItem, ConquestPlane>(ImmutableList.copyOf(FModel.getPlanes())) {
            @Override
            protected ConquestPlane getItemValue(InventoryItem input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<ConquestPlane> getItemValues(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return new HashSet<>();
                }
                return ConquestPlane.getAllPlanesOfCard((PaperCard)input);
            }
        }),
        INVITEM_REGION("lblRegion", InventoryItem.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<InventoryItem, ConquestRegion>(ConquestRegion.getAllRegions()) {
            @Override
            protected ConquestRegion getItemValue(InventoryItem input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<ConquestRegion> getItemValues(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return new HashSet<>();
                }
                return ConquestRegion.getAllRegionsOfCard((PaperCard)input);
            }
        }),
        INVITEM_QUEST_WORLD("lblQuestWorld", InventoryItem.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<InventoryItem, QuestWorld>(ImmutableList.copyOf(FModel.getWorlds())) {
            @Override
            protected QuestWorld getItemValue(InventoryItem input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<QuestWorld> getItemValues(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return new HashSet<>();
                }
                return QuestWorld.getAllQuestWorldsOfCard(((PaperCard)input));
            }
        }),
        INVITEM_COLOR("lblColor", InventoryItem.class, FilterOperator.COMBINATION_OPS, new ColorEvaluator<InventoryItem>() {
            @Override
            protected MagicColor.Color getItemValue(InventoryItem input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<MagicColor.Color> getItemValues(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return new HashSet<>();
                }
                return ((PaperCard)input).getRules().getColor().toEnumSet();
            }
        }),
        INVITEM_COLOR_IDENTITY("lblColorIdentity", InventoryItem.class, FilterOperator.COMBINATION_OPS, new ColorEvaluator<InventoryItem>() {
            @Override
            protected MagicColor.Color getItemValue(InventoryItem input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<MagicColor.Color> getItemValues(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return new HashSet<>();
                }
                return ((PaperCard)input).getRules().getColorIdentity().toEnumSet();
            }
        }),
        INVITEM_COLOR_COUNT("lblColorCount", InventoryItem.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<InventoryItem>(0, 5) {
            @Override
            protected Integer getItemValue(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return 0;
                }
                return ((PaperCard)input).getRules().getColor().countColors();
            }
        }),
        INVITEM_TYPE("lblType", InventoryItem.class, FilterOperator.COMBINATION_OPS, new CustomListEvaluator<InventoryItem, String>(CardType.getCombinedSuperAndCoreTypes()) {
            @Override
            protected String getItemValue(InventoryItem input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<String> getItemValues(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return new HashSet<>();
                }
                final CardType type = ((PaperCard)input).getRules().getType();
                final Set<String> types = new HashSet<>();
                for (Supertype t : type.getSupertypes()) {
                    types.add(t.name());
                }
                for (CoreType t : type.getCoreTypes()) {
                    types.add(t.name());
                }
                return types;
            }
        }),
        INVITEM_SUB_TYPE("lblSubtype", InventoryItem.class, FilterOperator.COMBINATION_OPS, new CustomListEvaluator<InventoryItem, String>(CardType.getSortedSubTypes()) {
            @Override
            protected String getItemValue(InventoryItem input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<String> getItemValues(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return new HashSet<>();
                }
                return (Set<String>)((PaperCard)input).getRules().getType().getSubtypes();
            }
        }),
        INVITEM_CMC("lblCMC", InventoryItem.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<InventoryItem>(0, 20) {
            @Override
            protected Integer getItemValue(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return 0;
                }
                return ((PaperCard)input).getRules().getManaCost().getCMC();
            }
        }),
        INVITEM_GENERIC_COST("lblGenericCost", InventoryItem.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<InventoryItem>(0, 20) {
            @Override
            protected Integer getItemValue(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return 0;
                }
                return ((PaperCard)input).getRules().getManaCost().getGenericCost();
            }
        }),
        INVITEM_POWER("lblPower", InventoryItem.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<InventoryItem>(0, 20) {
            @Override
            protected Integer getItemValue(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return null;
                }
                CardRules rules = ((PaperCard)input).getRules();
                if (rules.getType().isCreature()) {
                    return rules.getIntPower();
                }
                return null;
            }
        }),
        INVITEM_TOUGHNESS("lblToughness", InventoryItem.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<InventoryItem>(0, 20) {
            @Override
            protected Integer getItemValue(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return null;
                }
                CardRules rules = ((PaperCard)input).getRules();
                if (rules.getType().isCreature()) {
                    return rules.getIntToughness();
                }
                return null;
            }
        }),
        INVITEM_MANA_COST("lblManaCost", InventoryItem.class, FilterOperator.STRING_OPS, new StringEvaluator<InventoryItem>() {
            @Override
            protected String getItemValue(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return "";
                }
                return ((PaperCard)input).getRules().getManaCost().toString();
            }
        }),
        INVITEM_FIRST_PRINTING("lblFirstPrinting", InventoryItem.class, FilterOperator.BOOLEAN_OPS, new BooleanEvaluator<InventoryItem>() {
            @Override
            protected Boolean getItemValue(InventoryItem input) {
                List<PaperCard> cards = FModel.getMagicDb().getCommonCards().getAllCards(input.getName());
                if (cards.size() <= 1) { return true; }

                Collections.sort(cards, FModel.getMagicDb().getEditions().CARD_EDITION_COMPARATOR);
                return cards.get(0) == input;
            }
        }),
        INVITEM_RARITY("lblRarity", InventoryItem.class, FilterOperator.SINGLE_LIST_OPS, new CustomListEvaluator<InventoryItem, CardRarity>(Arrays.asList(CardRarity.FILTER_OPTIONS), CardRarity.FN_GET_LONG_NAME, CardRarity.FN_GET_LONG_NAME) {
            @Override
            protected CardRarity getItemValue(InventoryItem input) {
                if (!(input instanceof PaperCard)) {
                    return CardRarity.Special;
                }
                return ((PaperCard)input).getRarity();
            }
        }),
        INVITEM_BUY_PRICE("lblBuyPrice", InventoryItem.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<InventoryItem>(0, 10000000) {
            @Override
            protected Integer getItemValue(InventoryItem input) {
                return (Integer) QuestSpellShop.fnPriceGet.apply(new AbstractMap.SimpleEntry<InventoryItem, Integer>(input, 1));
            }
        }),
        INVITEM_SELL_PRICE("lblSellPrice", InventoryItem.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<InventoryItem>(0, 10000000) {
            @Override
            protected Integer getItemValue(InventoryItem input) {
                return (Integer) QuestSpellShop.fnPriceSellGet.apply(new AbstractMap.SimpleEntry<InventoryItem, Integer>(input, 1));
            }
        }),
        INVITEM_USED_IN_QUEST_DECKS("lblUsedInQuestDecks", InventoryItem.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<InventoryItem>(0, 100) {
            @Override
            protected Integer getItemValue(InventoryItem input) {
                return (Integer) Integer.parseInt((String) QuestSpellShop.fnDeckGet.apply(new AbstractMap.SimpleEntry<InventoryItem, Integer>(input, 1)));
            }
        }),
        DECK_NAME("lblName", DeckProxy.class, FilterOperator.STRING_OPS, new StringEvaluator<DeckProxy>() {
            @Override
            protected String getItemValue(DeckProxy input) {
                return input.getName();
            }
        }),
        DECK_FOLDER("lblFolder", DeckProxy.class, FilterOperator.STRING_OPS, new StringEvaluator<DeckProxy>() {
            @Override
            protected String getItemValue(DeckProxy input) {
                return input.getPath();
            }
        }),
        DECK_FAVORITE("ttFavorite", DeckProxy.class, FilterOperator.BOOLEAN_OPS, new BooleanEvaluator<DeckProxy>() {
            @Override
            protected Boolean getItemValue(DeckProxy input) {
                return input.isFavoriteDeck();
            }
        }),
        DECK_FORMAT("lblFormat", DeckProxy.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<DeckProxy, GameFormat>((List<GameFormat>)FModel.getFormats().getFilterList()) {
            @Override
            protected GameFormat getItemValue(DeckProxy input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<GameFormat> getItemValues(DeckProxy input) {
                return input.getExhaustiveFormats();
            }
        }),
        DECK_QUEST_WORLD("lblQuestWorld", DeckProxy.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<DeckProxy, QuestWorld>(ImmutableList.copyOf(FModel.getWorlds())) {
            @Override
            protected QuestWorld getItemValue(DeckProxy input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<QuestWorld> getItemValues(DeckProxy input) {
                return QuestWorld.getAllQuestWorldsOfDeck(input.getDeck());
            }
        }),
        DECK_COLOR("lblColor", DeckProxy.class, FilterOperator.COMBINATION_OPS, new ColorEvaluator<DeckProxy>() {
            @Override
            protected MagicColor.Color getItemValue(DeckProxy input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<MagicColor.Color> getItemValues(DeckProxy input) {
                return input.getColor().toEnumSet();
            }
        }),
        DECK_COLOR_IDENTITY("lblColorIdentity", DeckProxy.class, FilterOperator.COMBINATION_OPS, new ColorEvaluator<DeckProxy>() {
            @Override
            protected MagicColor.Color getItemValue(DeckProxy input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<MagicColor.Color> getItemValues(DeckProxy input) {
                return input.getColorIdentity().toEnumSet();
            }
        }),
        DECK_COLOR_COUNT("lblColorCount", DeckProxy.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<DeckProxy>(0, 5) {
            @Override
            protected Integer getItemValue(DeckProxy input) {
                return input.getColor().countColors();
            }
        }),
        DECK_AVERAGE_CMC("lblAverageCMC", DeckProxy.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<DeckProxy>(0, 20) {
            @Override
            protected Integer getItemValue(DeckProxy input) {
                return input.getAverageCMC();
            }
        }),
        DECK_MAIN("lblMainDeck", DeckProxy.class, FilterOperator.DECK_CONTENT_OPS, new DeckContentEvaluator<DeckProxy>() {
            @Override
            protected Map<String, Integer> getItemValue(DeckProxy input) {
                return input.getDeck().getMain().toNameLookup();
            }
        }),
        DECK_SIDEBOARD("lblSideboard", DeckProxy.class, FilterOperator.DECK_CONTENT_OPS, new DeckContentEvaluator<DeckProxy>() {
            @Override
            protected Map<String, Integer> getItemValue(DeckProxy input) {
                CardPool sideboard = input.getDeck().get(DeckSection.Sideboard);
                if (sideboard != null) {
                    sideboard.toNameLookup();
                }
                return null;
            }
        }),
        DECK_MAIN_SIZE("lblMainDeckSize", DeckProxy.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<DeckProxy>(40, 250) {
            @Override
            protected Integer getItemValue(DeckProxy input) {
                return input.getMainSize();
            }
        }),
        DECK_SIDE_SIZE("lblSideboardSize", DeckProxy.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<DeckProxy>(0, 15) {
            @Override
            protected Integer getItemValue(DeckProxy input) {
                return Math.min(input.getSideSize(), 0);
            }
        }),
        COMMANDER_NAME("lblName", ConquestCommander.class, FilterOperator.STRING_OPS, new StringEvaluator<ConquestCommander>() {
            @Override
            protected String getItemValue(ConquestCommander input) {
                return input.getName();
            }
        }),
        COMMANDER_ORIGIN("lblOrigin", ConquestCommander.class, FilterOperator.SINGLE_LIST_OPS, new CustomListEvaluator<ConquestCommander, ConquestPlane>(ImmutableList.copyOf(FModel.getPlanes())) {
            @Override
            protected ConquestPlane getItemValue(ConquestCommander input) {
                return input.getOriginPlane();
            }
        }),
        COMMANDER_COLOR("lblColor", ConquestCommander.class, FilterOperator.COMBINATION_OPS, new ColorEvaluator<ConquestCommander>() {
            @Override
            protected MagicColor.Color getItemValue(ConquestCommander input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<MagicColor.Color> getItemValues(ConquestCommander input) {
                return input.getCard().getRules().getColorIdentity().toEnumSet();
            }
        }),
        COMMANDER_COLOR_COUNT("lblColorCount", ConquestCommander.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<ConquestCommander>(0, 5) {
            @Override
            protected Integer getItemValue(ConquestCommander input) {
                return input.getCard().getRules().getColorIdentity().countColors();
            }
        }),
        COMMANDER_DECK_AVERAGE_CMC("lblDeckAverageCMC", ConquestCommander.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<ConquestCommander>(0, 20) {
            @Override
            protected Integer getItemValue(ConquestCommander input) {
                return DeckProxy.getAverageCMC(input.getDeck());
            }
        }),
        COMMANDER_DECK_CONTENTS("lblDeckContents", ConquestCommander.class, FilterOperator.DECK_CONTENT_OPS, new DeckContentEvaluator<ConquestCommander>() {
            @Override
            protected Map<String, Integer> getItemValue(ConquestCommander input) {
                return input.getDeck().getMain().toNameLookup();
            }
        }),
        COMMANDER_DECK_SIZE("lblDeckSize", DeckProxy.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<DeckProxy>(40, 250) {
            @Override
            protected Integer getItemValue(DeckProxy input) {
                return input.getMainSize();
            }
        });

        private final String name;
        private final Class<? extends InventoryItem> type;
        private final FilterOperator[] operatorOptions;
        private final FilterEvaluator<? extends InventoryItem, ?> evaluator;

        FilterOption(String name0, Class<? extends InventoryItem> type0, FilterOperator[] operatorOptions0, FilterEvaluator<? extends InventoryItem, ?> evaluator0) {
            name = Localizer.getInstance().getMessage(name0);
            type = type0;
            operatorOptions = operatorOptions0;
            evaluator = evaluator0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum FilterOperator {
        //Boolean operators
        IS_TRUE("lblIsTrue", "%1$s is true", FilterValueCount.ZERO, new OperatorEvaluator<Boolean>() {
            @Override
            public boolean apply(Boolean input, List<Boolean> values) {
                if (input != null) {
                    return input.booleanValue();
                }
                return false;
            }
        }),
        IS_FALSE("lblIsFalse", "%1$s is false", FilterValueCount.ZERO, new OperatorEvaluator<Boolean>() {
            @Override
            public boolean apply(Boolean input, List<Boolean> values) {
                if (input != null) {
                    return !input.booleanValue();
                }
                return false;
            }
        }),

        //Numeric operators
        EQUALS("lblEqual", "%1$s = %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() == values.get(0).intValue();
                }
                return false;
            }
        }),
        NOT_EQUALS("lblNotEqual", "%1$s <> %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() != values.get(0).intValue();
                }
                return true;
            }
        }),
        GREATER_THAN("lblGreaterThan", "%1$s > %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() > values.get(0).intValue();
                }
                return false;
            }
        }),
        LESS_THAN("lblLessThan", "%1$s < %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() < values.get(0).intValue();
                }
                return false;
            }
        }),
        GT_OR_EQUAL("lblGreaterThanOrEqual", "%1$s >= %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() >= values.get(0).intValue();
                }
                return false;
            }
        }),
        LT_OR_EQUAL("lblLessThanOrEqual", "%1$s <= %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() <= values.get(0).intValue();
                }
                return false;
            }
        }),
        BETWEEN_INCLUSIVE("lblBetweenInclusive", "%2$d <= %1$s <= %3$d", FilterValueCount.TWO, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    int inputValue = input.intValue();
                    return values.get(0).intValue() <= inputValue && inputValue <= values.get(1).intValue();
                }
                return false;
            }
        }),
        BETWEEN_EXCLUSIVE("lblBetweenExclusive", "%2$d < %1$s < %3$d", FilterValueCount.TWO, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    int inputValue = input.intValue();
                    return values.get(0).intValue() < inputValue && inputValue < values.get(1).intValue();
                }
                return false;
            }
        }),

        //String operators
        CONTAINS("lblContains", "%1$s contains '%2$s'", FilterValueCount.ONE, new OperatorEvaluator<String>() {
            @Override
            public boolean apply(String input, List<String> values) {
                if (input != null) {
                    return input.toLowerCase().indexOf(values.get(0).toLowerCase()) != -1;
                }
                return false;
            }
            @Override
            public boolean apply(Set<String> inputs, List<String> values) {
                if (inputs != null && !inputs.isEmpty() && !values.isEmpty()) {
                    for (String input : inputs) {
                        if (apply(input, values)) {
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            }
        }),
        STARTS_WITH("lblStartsWith", "%1$s starts with '%2$s'", FilterValueCount.ONE, new OperatorEvaluator<String>() {
            @Override
            public boolean apply(String input, List<String> values) {
                if (input != null) {
                    return input.toLowerCase().startsWith(values.get(0).toLowerCase());
                }
                return false;
            }
            @Override
            public boolean apply(Set<String> inputs, List<String> values) {
                if (inputs != null && !inputs.isEmpty() && !values.isEmpty()) {
                    for (String input : inputs) {
                        if (apply(input, values)) {
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            }
        }),
        ENDS_WITH("lblEndsWith", "%1$s ends with '%2$s'", FilterValueCount.ONE, new OperatorEvaluator<String>() {
            @Override
            public boolean apply(String input, List<String> values) {
                if (input != null) {
                    return input.toLowerCase().endsWith(values.get(0).toLowerCase());
                }
                return false;
            }
            @Override
            public boolean apply(Set<String> inputs, List<String> values) {
                if (inputs != null && !inputs.isEmpty() && !values.isEmpty()) {
                    for (String input : inputs) {
                        if (apply(input, values)) {
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            }
        }),

        //Custom list operators
        IS_EXACTLY("lblIsExactly", "%1$s is %2$s", FilterValueCount.MANY, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                if (input != null && values.size() == 1) {
                    return input.equals(values.get(0));
                }
                return false;
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null && inputs.size() == values.size()) {
                    for (Object value : values) {
                        if (!inputs.contains(value)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }),
        IS_ANY("lblIsAnyOf", "%1$s is %2$s", FilterValueCount.MANY_OR, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                if (input != null) {
                    for (Object value : values) {
                        if (input.equals(value)) {
                            return true;
                        }
                    }
                }
                return false;
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null) {
                    for (Object value : values) {
                        if (inputs.contains(value)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }),
        CONTAINS_ANY("lblContainsAnyOf", "%1$s contains %2$s", FilterValueCount.MANY_OR, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                if (input != null) {
                    for (Object value : values) {
                        if (input.equals(value)) {
                            return true;
                        }
                    }
                }
                return false;
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null) {
                    for (Object value : values) {
                        if (inputs.contains(value)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }),
        CONTAINS_ALL("lblContainsAllOf", "%1$s contains %2$s", FilterValueCount.MANY_AND, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                if (input != null && values.size() == 1) {
                    return input.equals(values.get(0));
                }
                return false;
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null) {
                    for (Object value : values) {
                        if (!inputs.contains(value)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }),
        CONTAIN_ANY("lblContainAnyOf", "%1$s contain %2$s", FilterValueCount.MANY_OR, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                throw new RuntimeException("shouldn't be called with a single input");
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null) {
                    for (Object value : values) {
                        if (inputs.contains(value)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }),
        CONTAIN_ALL("lblContainAllOf", "%1$s contain %2$s", FilterValueCount.MANY_AND, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                throw new RuntimeException("shouldn't be called with a single input");
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null) {
                    for (Object value : values) {
                        if (!inputs.contains(value)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }),

        //Deck content operators
        CONTAINS_CARD("lblContainsCard", "%1$s contains %2$s", FilterValueCount.ONE, new OperatorEvaluator<Map<PaperCard, Integer>>() {
            @Override
            public boolean apply(Map<PaperCard, Integer> input, List<Map<PaperCard, Integer>> values) {
                if (input != null) {
                    Entry<PaperCard, Integer> entry = values.get(0).entrySet().iterator().next();
                    return input.containsKey(entry.getKey());
                }
                return false;
            }
        }),
        CONTAINS_X_COPIES_OF_CARD("lblContainsXCopiesCard", "%1$s contains %3$d %2$s", FilterValueCount.TWO, new OperatorEvaluator<Map<PaperCard, Integer>>() {
            @Override
            public boolean apply(Map<PaperCard, Integer> input, List<Map<PaperCard, Integer>> values) {
                if (input != null) {
                    Entry<PaperCard, Integer> entry = values.get(0).entrySet().iterator().next();
                    return input.get(entry.getKey()) == entry.getValue();
                }
                return false;
            }
        });

        public static final FilterOperator[] BOOLEAN_OPS = new FilterOperator[] {
            IS_TRUE, IS_FALSE
        };
        public static final FilterOperator[] NUMBER_OPS = new FilterOperator[] {
            EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GT_OR_EQUAL, LT_OR_EQUAL, BETWEEN_INCLUSIVE, BETWEEN_EXCLUSIVE
        };
        public static final FilterOperator[] STRING_OPS = new FilterOperator[] {
            CONTAINS, STARTS_WITH, ENDS_WITH
        };
        public static final FilterOperator[] STRINGS_OPS = new FilterOperator[] {
            CONTAINS, STARTS_WITH, ENDS_WITH
        };
        public static final FilterOperator[] SINGLE_LIST_OPS = new FilterOperator[] {
            IS_ANY
        };
        public static final FilterOperator[] MULTI_LIST_OPS = new FilterOperator[] {
            IS_ANY
        };
        public static final FilterOperator[] COMBINATION_OPS = new FilterOperator[] {
            IS_EXACTLY, CONTAINS_ANY, CONTAINS_ALL
        };
        public static final FilterOperator[] COLLECTION_OPS = new FilterOperator[] {
            CONTAIN_ANY, CONTAIN_ALL
        };
        public static final FilterOperator[] DECK_CONTENT_OPS = new FilterOperator[] {
            CONTAINS_CARD, CONTAINS_X_COPIES_OF_CARD
        };

        private final String caption, formatStr;
        private final FilterValueCount valueCount;
        private final OperatorEvaluator<?> evaluator;

        FilterOperator(String caption0, String formatStr0, FilterValueCount valueCount0, OperatorEvaluator<?> evaluator0) {
            caption = Localizer.getInstance().getMessage(caption0);
            formatStr = formatStr0;
            valueCount = valueCount0;
            evaluator = evaluator0;
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    private enum FilterValueCount {
        ZERO,
        ONE,
        TWO,
        MANY,
        MANY_OR,
        MANY_AND
    }

    private static abstract class OperatorEvaluator<V> {
        protected abstract boolean apply(V input, List<V> values);

        protected boolean apply(Set<V> inputs, List<V> values) {
            return false; //available for options that have multiple inputs
        }
    }

    private static abstract class FilterEvaluator<T extends InventoryItem, V> {
        @SuppressWarnings("unchecked")
        public final Filter<T> createFilter(FilterOption option, FilterOperator operator) {
            final List<V> values = getValues(option, operator);
            if (values == null || values.isEmpty()) {
                return null;
            }

            String caption = getCaption(values, option, operator);

            final OperatorEvaluator<V> evaluator = (OperatorEvaluator<V>) operator.evaluator;
            Predicate<T> predicate = new Predicate<T>() {
                @Override
                public boolean apply(T input) {
                    return evaluator.apply(getItemValue(input), values);
                }
            };

            final FilterOperator[][] manyValueOperators = { FilterOperator.MULTI_LIST_OPS,
                    FilterOperator.COMBINATION_OPS, FilterOperator.COLLECTION_OPS, FilterOperator.STRINGS_OPS };
            for (FilterOperator[] oper : manyValueOperators) {
                if (option.operatorOptions == oper) {
                    predicate = new Predicate<T>() {
                        @Override
                        public boolean apply(T input) {
                            return evaluator.apply(getItemValues(input), values);
                        }
                    };
                    break;
                }
            }
            return new Filter<>(option, operator, caption, predicate);
        }

        protected abstract List<V> getValues(FilterOption option, FilterOperator operator);
        protected abstract String getCaption(List<V> values, FilterOption option, FilterOperator operator);
        protected abstract V getItemValue(T input);

        protected Set<V> getItemValues(T input) { //available for options that have multiple inputs
            return null;
        }
    }

    private static abstract class BooleanEvaluator<T extends InventoryItem> extends FilterEvaluator<T, Boolean> {
        public BooleanEvaluator() {
        }

        @Override
        protected List<Boolean> getValues(FilterOption option, FilterOperator operator) {
            List<Boolean> values = new ArrayList<>();
            values.add(operator == FilterOperator.IS_TRUE); //just always add a single boolean value so other logic works
            return values;
        }

        @Override
        protected String getCaption(List<Boolean> values, FilterOption option, FilterOperator operator) {
            return String.format(operator.formatStr, option.name);
        }
    }

    private static abstract class NumericEvaluator<T extends InventoryItem> extends FilterEvaluator<T, Integer> {
        private final int min, max;

        public NumericEvaluator(int min0, int max0) {
            min = min0;
            max = max0;
        }

        @Override
        protected List<Integer> getValues(FilterOption option, FilterOperator operator) {
            String message;
            if (operator.valueCount == FilterValueCount.ONE) {
                message = option.name + " " + operator.caption + " ?";
            }
            else {
                message = "? " + operator.caption.replace("|", " " + option.name + " ");
            }
            Integer lowerBound = SGuiChoose.getInteger(message, min, max);
            if (lowerBound == null) { return null; }

            final List<Integer> values = new ArrayList<>();
            values.add(lowerBound);

            if (operator.valueCount == FilterValueCount.TWO) { //prompt for upper bound if needed
                message = lowerBound + message.substring(1) + " ?";
                int upperBoundMin = lowerBound;
                if (operator == FilterOperator.BETWEEN_EXCLUSIVE) {
                    upperBoundMin += 2; //if exclusive, ensure it's possible to have numbers in between
                }
                Integer upperBound = SGuiChoose.getInteger(message, upperBoundMin, max);
                if (upperBound == null) { return null; }

                values.add(upperBound);
            }
            return values;
        }

        @Override
        protected String getCaption(List<Integer> values, FilterOption option, FilterOperator operator) {
            if (operator.valueCount == FilterValueCount.TWO) {
                return String.format(operator.formatStr, option.name, values.get(0), values.get(1));
            }
            return String.format(operator.formatStr, option.name, values.get(0));
        }
    }

    private static abstract class StringEvaluator<T extends InventoryItem> extends FilterEvaluator<T, String> {
        private String initialInput = "";

        public StringEvaluator() {
        }

        @Override
        protected List<String> getValues(FilterOption option, FilterOperator operator) {
            String message = option.name + " " + operator.caption + " ?";
            String value = SOptionPane.showInputDialog("", message, null, initialInput);
            if (value == null) { return null; }

            initialInput = value; //store value as initial input for next time

            List<String> values = new ArrayList<>();
            values.add(value);
            return values;
        }

        @Override
        protected String getCaption(List<String> values, FilterOption option, FilterOperator operator) {
            return String.format(operator.formatStr, option.name, values.get(0));
        }
    }

    private static abstract class CustomListEvaluator<T extends InventoryItem, V> extends FilterEvaluator<T, V> {
        private final Collection<V> choices;
        private final Function<V, String> toShortString, toLongString;

        public CustomListEvaluator(Collection<V> choices0) {
            this(choices0, null, null);
        }
        public CustomListEvaluator(Collection<V> choices0, Function<V, String> toShortString0) {
            this(choices0, toShortString0, null);
        }
        public CustomListEvaluator(Collection<V> choices0, Function<V, String> toShortString0, Function<V, String> toLongString0) {
            choices = choices0;
            toShortString = toShortString0;
            toLongString = toLongString0;
        }

        @Override
        protected List<V> getValues(FilterOption option, FilterOperator operator) {
            int max = choices.size();
            String message = option.name + " " + operator.caption + " ?";
            return SGuiChoose.getChoices(message, 0, max, choices, null, toLongString);
        }

        @Override
        protected String getCaption(List<V> values, FilterOption option, FilterOperator operator) {
            String valuesStr;
            switch (operator.valueCount) {
            case MANY:
                valuesStr = formatValues(values, " ", " ");
                break;
            case MANY_OR:
                valuesStr = formatValues(values, ", ", " or ");
                break;
            case MANY_AND:
            default:
                valuesStr = formatValues(values, ", ", " and ");
                break;
            }
            return String.format(operator.formatStr, option.name, valuesStr);
        }

        protected String formatValues(List<V> values, String delim, String finalDelim) {
            int valueCount = values.size();
            switch (valueCount) {
            case 1:
                return formatValue(values.get(0));
            case 2:
                return formatValue(values.get(0)) + finalDelim + formatValue(values.get(1));
            default:
                int lastValueIdx = valueCount - 1;
                StringBuilder result = new StringBuilder(formatValue(values.get(0)));
                for (int i = 1; i < lastValueIdx; i++) {
                    result.append(delim).append(formatValue(values.get(i)));
                }
                result.append(delim.trim()).append(finalDelim).append(formatValue(values.get(lastValueIdx)));
                return result.toString();
            }
        }

        protected String formatValue(V value) {
            if (toShortString == null) {
                return value.toString();
            }
            return toShortString.apply(value);
        }
    }

    private static abstract class ColorEvaluator<T extends InventoryItem> extends CustomListEvaluator<T, MagicColor.Color> {
        public ColorEvaluator() {
            super(Arrays.asList(MagicColor.Color.values()), MagicColor.FN_GET_SYMBOL);
        }

        @Override
        protected String getCaption(List<MagicColor.Color> values, FilterOption option, FilterOperator operator) {
            if (operator == FilterOperator.IS_EXACTLY) {
                //handle special case for formatting colors with no spaces in between for is exactly operator
                return String.format(operator.formatStr, option.name, formatValues(values, "", ""));
            }
            return super.getCaption(values, option, operator);
        }
    }

    private static abstract class DeckContentEvaluator<T extends InventoryItem> extends FilterEvaluator<T, Map<String, Integer>> {
        public DeckContentEvaluator() {
        }

        @Override
        protected List<Map<String, Integer>> getValues(FilterOption option, FilterOperator operator) {
            String message = option.name + " " + operator.caption + " ?";
            PaperCard card = SGuiChoose.oneOrNone(message, FModel.getMagicDb().getCommonCards().getUniqueCards());
            if (card == null) { return null; }

            Integer amount = -1;
            if (operator == FilterOperator.CONTAINS_X_COPIES_OF_CARD) { //prompt for quantity if needed
                amount = SGuiChoose.getInteger(Localizer.getInstance().getMessage("lblHowManyCopiesOfN", CardTranslation.getTranslatedName(card.getName())), 0, 4);
                if (amount == null) { return null; }
            }

            Map<String, Integer> map = new HashMap<>();
            map.put(card.getName(), amount);

            List<Map<String, Integer>> values = new ArrayList<>();
            values.add(map);
            return values;
        }

        @Override
        protected String getCaption(List<Map<String, Integer>> values, FilterOption option, FilterOperator operator) {
            Entry<String, Integer> entry = values.get(0).entrySet().iterator().next();
            if (operator == FilterOperator.CONTAINS_X_COPIES_OF_CARD) {
                return String.format(operator.formatStr, option.name, entry.getKey(), entry.getValue());
            }
            return String.format(operator.formatStr, option.name, entry.getKey());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends InventoryItem> Filter<T> getFilter(Class<? super T> type, Filter<T> editFilter, boolean reselectOption) {
        final FilterOption option;
        final FilterOption defaultOption = editFilter == null ? null : editFilter.option;
        if (defaultOption == null || reselectOption) {
            //build list of filter options based on ItemManager type
            List<FilterOption> options = new ArrayList<>();
            if (editFilter != null) {
                options.add(FilterOption.NONE); //provide option to clear existing filter
            }
            for (FilterOption opt : FilterOption.values()) {
                if (opt.type == type) {
                    options.add(opt);
                }
            }
            option = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblSelectAFilterType"), options, defaultOption, null);
            if (option == null) { return editFilter; }
        }
        else {
            option = defaultOption;
        }

        if (option == FilterOption.NONE) { return null; } //allow user to clear filter by selecting "(none)"

        final FilterOperator operator;
        if (option.operatorOptions.length > 1) {
            final FilterOperator defaultOperator = option == defaultOption ? editFilter.operator : null;
            operator = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblSelectOperatorFor", option.name), option.operatorOptions, defaultOperator, null);
            if (operator == null) { return editFilter; }
        }
        else {
            operator = option.operatorOptions[0];
        }

        Filter<T> filter = (Filter<T>)option.evaluator.createFilter(option, operator);
        if (filter == null) {
            filter = editFilter;
        }
        return filter;
    }

    public static class Filter<T extends InventoryItem> {
        private final FilterOption option;
        private final FilterOperator operator;
        private final String caption;
        private final Predicate<T> predicate;

        private Filter(FilterOption option0, FilterOperator operator0, String caption0, Predicate<T> predicate0) {
            option = option0;
            operator = operator0;
            caption = caption0;
            predicate = predicate0;
        }

        public boolean canMergeCaptionWith(Filter<T> filter) {
            return filter != null && option == filter.option && operator == filter.operator;
        }

        public String extractValuesFromCaption() {
            //build empty caption to determine index where values would begin
            String emptyCaption = operator.formatStr.replace("%1$s", option.name);
            emptyCaption = emptyCaption.replaceAll("'?%\\d\\$[sd]'?", ""); //strip other arguments
            return caption.substring(emptyCaption.length());
        }

        public FilterOption getOption() {
            return option;
        }

        public FilterOperator getOperator() {
            return operator;
        }

        public Predicate<T> getPredicate() {
            return predicate;
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    public interface IFilterControl<T extends InventoryItem> {
        IButton getBtnNotBeforeParen();
        IButton getBtnOpenParen();
        IButton getBtnNotAfterParen();
        IButton getBtnFilter();
        IButton getBtnCloseParen();
        IButton getBtnAnd();
        IButton getBtnOr();
        Filter<T> getFilter();
        void setFilter(Filter<T> filter0);
        Class<? super T> getGenericType();
    }

    public static class Model<T extends InventoryItem> {
        private static final String EMPTY_FILTER_TEXT = Localizer.getInstance().getMessage("lblSelectingFilter");

        private final List<Object> expression = new ArrayList<>();
        private final List<IFilterControl<T>> controls = new ArrayList<>();
        private IButton label;

        public Model() {
        }

        public Predicate<T> getPredicate() {
            if (isEmpty()) {
                return Predicates.alwaysTrue();
            }
            return getPredicatePiece(new ExpressionIterator());
        }

        @SuppressWarnings("unchecked")
        private Predicate<T> getPredicatePiece(ExpressionIterator iterator) {
            Predicate<T> pred = null;
            Predicate<T> predPiece = null;
            Operator operator = null;
            boolean applyNot = false;

            for (; iterator.hasNext(); iterator.next()) {
                Object piece = iterator.get();
                if (piece.equals(Operator.OPEN_PAREN)) {
                    predPiece = getPredicatePiece(iterator.next());
                }
                else if (piece.equals(Operator.CLOSE_PAREN)) {
                    return pred;
                }
                else if (piece.equals(Operator.AND)) {
                    operator = Operator.AND;
                    continue;
                }
                else if (piece.equals(Operator.OR)) {
                    operator = Operator.OR;
                    continue;
                }
                else if (piece.equals(Operator.NOT)) {
                    applyNot = !applyNot;
                    continue;
                }
                else {
                    predPiece = ((AdvancedSearch.Filter<T>) piece).getPredicate();
                }
                if (applyNot) {
                    predPiece = Predicates.not(predPiece);
                    applyNot = false;
                }
                if (pred == null) {
                    pred = predPiece;
                }
                else if (operator == Operator.AND) {
                    pred = Predicates.and(pred, predPiece);
                }
                else if (operator == Operator.OR) {
                    pred = Predicates.or(pred, predPiece);
                }
                operator = null;
            }
            return pred;
        }

        public boolean isEmpty() {
            return expression.isEmpty();
        }

        public void reset() {
            expression.clear();
            controls.clear();
            updateLabel();
        }

        @SuppressWarnings("serial")
        public void addFilterControl(final IFilterControl<T> control) {
            control.getBtnFilter().setText(EMPTY_FILTER_TEXT);
            control.getBtnFilter().setCommand(new UiCommand() {
                @Override
                public void run() {
                    editFilterControl(control, null);
                }
            });
            controls.add(control);
        }

        public void removeFilterControl(IFilterControl<T> control) {
            int index = controls.indexOf(control);
            if (index > 0) {
                IFilterControl<T> prevControl = controls.get(index - 1);
                prevControl.getBtnAnd().setSelected(control.getBtnAnd().isSelected());
                prevControl.getBtnOr().setSelected(control.getBtnOr().isSelected());
            }
            controls.remove(index);
        }

        public void editFilterControl(final IFilterControl<T> control, final Runnable onChange) {
            FThreads.invokeInBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    final Filter<T> filter = getFilter(control.getGenericType(), control.getFilter(), onChange == null); //reselect option if no change handler passed
                    if (control.getFilter() != filter) {
                        FThreads.invokeInEdtLater(new Runnable() {
                            @Override
                            public void run() {
                                control.setFilter(filter);
                                if (filter != null) {
                                    control.getBtnFilter().setText(GuiBase.getInterface().encodeSymbols(filter.toString(), false));

                                    if (filter.getOption() == FilterOption.CARD_KEYWORDS) {
                                        //the first time the user selects keywords, preload keywords for all cards
                                        Runnable preloadTask = Keyword.getPreloadTask();
                                        if (preloadTask != null) {
                                            GuiBase.getInterface().runBackgroundTask(Localizer.getInstance().getMessage("lblLoadingKeywords"), preloadTask);
                                        }
                                    }
                                }
                                else {
                                    control.getBtnFilter().setText(EMPTY_FILTER_TEXT);
                                }
                                if (onChange != null) {
                                    onChange.run();
                                }
                            }
                        });
                    }
                }
            });
        }

        public Iterable<IFilterControl<T>> getControls() {
            return controls;
        }

        public void setLabel(IButton label0) {
            label = label0;
            updateLabel();
        }

        @SuppressWarnings("unchecked")
        public void updateLabel() {
            if (label == null) { return; }

            StringBuilder builder = new StringBuilder();
            builder.append("Filter: ");
            if (expression.isEmpty()) {
                builder.append("(none)");
            }
            else {
                int prevFilterEndIdx = -1;
                AdvancedSearch.Filter<T> filter, prevFilter = null;
                for (Object piece : expression) {
                    if (piece instanceof AdvancedSearch.Filter) {
                        filter = (AdvancedSearch.Filter<T>)piece;
                        if (filter.canMergeCaptionWith(prevFilter)) {
                            //convert boolean operators between filters to lowercase
                            builder.replace(prevFilterEndIdx, builder.length(), builder.substring(prevFilterEndIdx).toLowerCase());
                            //append only values for filter
                            builder.append(filter.extractValuesFromCaption());
                        }
                        else {
                            builder.append(filter);
                        }
                        prevFilter = filter;
                        prevFilterEndIdx = builder.length();
                    }
                    else {
                        if (piece.equals(Operator.OPEN_PAREN) || piece.equals(Operator.CLOSE_PAREN)) {
                            prevFilter = null; //prevent merging filters with parentheses in between
                        }
                        builder.append(piece);
                    }
                }
            }
            label.setText(GuiBase.getInterface().encodeSymbols(builder.toString(), false));
            label.setToolTipText(getTooltip());
        }

        public String getTooltip() {
            if (expression.isEmpty()) { return ""; }

            StringBuilder builder = new StringBuilder();
            builder.append("Filter:\n");

            StringBuilder indent = new StringBuilder();

            for (Object piece : expression) {
                if (piece.equals(Operator.CLOSE_PAREN) && (indent.length() > 0)) {
                    indent = new StringBuilder(indent.substring(2)); //trim an indent level when a close paren is hit
                }
                builder.append("\n").append(indent).append(piece.toString().trim());
                if (piece.equals(Operator.OPEN_PAREN)) {
                    indent.append("  "); //add an indent level when an open paren is hit
                }
            }
            return GuiBase.getInterface().encodeSymbols(builder.toString(), false);
        }

        public void updateExpression() {
            expression.clear();

            for (IFilterControl<T> control : controls) {
                if (control.getFilter() == null) { continue; } //skip any blank filters

                if (control.getBtnNotBeforeParen().isSelected()) {
                    expression.add(Operator.NOT);
                }
                if (control.getBtnOpenParen().isSelected()) {
                    expression.add(Operator.OPEN_PAREN);
                }
                if (control.getBtnNotAfterParen().isSelected()) {
                    expression.add(Operator.NOT);
                }

                expression.add(control.getFilter());

                if (control.getBtnCloseParen().isSelected()) {
                    expression.add(Operator.CLOSE_PAREN);
                }
                if (control.getBtnAnd().isSelected()) {
                    expression.add(Operator.AND);
                }
                else if (control.getBtnOr().isSelected()) {
                    expression.add(Operator.OR);
                }
            }

            updateLabel();
        }

        private class ExpressionIterator {
            private int index;
            private boolean hasNext() {
                return index < expression.size();
            }
            private ExpressionIterator next() {
                index++;
                return this;
            }
            private Object get() {
                return expression.get(index);
            }
        }

        private enum Operator {
            AND(" AND "),
            OR(" OR "),
            NOT("NOT "),
            OPEN_PAREN("("),
            CLOSE_PAREN(")");

            private final String token;

            Operator(String token0) {
                token = token0;
            }

            public String toString() {
                return token;
            }
        }
    }
}

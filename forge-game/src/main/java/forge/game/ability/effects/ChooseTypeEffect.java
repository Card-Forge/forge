package forge.game.ability.effects;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.card.CardType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;

public class ChooseTypeEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (!sa.usesTargeting()) {
            sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));
            sb.append(" chooses a ").append(sa.getParam("Type").toLowerCase()).append(" type.");
        } else {
            sb.append("Please improve the stack description.");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final String type = sa.getParam("Type");
        final List<String> invalidTypes = sa.hasParam("InvalidTypes") ? Arrays.asList(sa.getParam("InvalidTypes").split(",")) : new ArrayList<>();
        final List<String> validTypes = new ArrayList<>();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (sa.hasParam("ValidTypes")) {
            validTypes.addAll(Arrays.asList(sa.getParam("ValidTypes").split(",")));
        }

        if (validTypes.isEmpty()) {
            switch (type) {
            case "Card":
                validTypes.addAll(CardType.getAllCardTypes());
                break;
            case "Creature":
                if (sa.hasParam("TypesFromDefined")) {
                    for (final Card c : AbilityUtils.getDefinedCards(card, sa.getParam("TypesFromDefined"), sa)) {
                        validTypes.addAll(c.getType().getCreatureTypes());
                    }
                } else if (sa.hasParam("MostPrevalentInDefinedZone")) {
                    final String[] info = sa.getParam("MostPrevalentInDefinedZone").split("_");
                    final Player definedP = AbilityUtils.getDefinedPlayers(sa.getHostCard(), info[0], sa).get(0);
                    final ZoneType z = info.length > 1 ? ZoneType.smartValueOf(info[1]) : ZoneType.Battlefield;
                    CardCollectionView zoneCards = definedP.getCardsIn(z);
                    for (String s : CardFactoryUtil.getMostProminentCreatureType(zoneCards)) {
                        validTypes.add(s);
                    }
                } else {
                    validTypes.addAll(CardType.getAllCreatureTypes());
                }
                break;
            case "Basic Land":
                validTypes.addAll(CardType.getBasicTypes());
                break;
            case "Land":
                validTypes.addAll(CardType.getAllLandTypes());
                break;
            case "Planeswalker":
                validTypes.addAll(CardType.getAllWalkerTypes());
                break;
            case "CreatureInTargetedDeck":
                for (final Player p : tgtPlayers) {
                    for (Card c : p.getAllCards()) {
                        if (c.getType().getCreatureTypes() != null) {
                            for (String s : c.getType().getCreatureTypes()) {
                                if (!validTypes.contains(s)) {
                                    validTypes.add(s);
                                }
                            }
                        }
                    }
                }
            }
        }

        validTypes.removeAll(invalidTypes);

        if (sa.hasParam("Note") && card.hasAnyNotedType()) {
            for (String noted : card.getNotedTypes()) {
                validTypes.remove(noted);
            }
        }

        if (validTypes.isEmpty() && sa.hasParam("Note")) {
            // OK to end up with no choices/have nothing new to note
        } else if (!validTypes.isEmpty()) {
            for (final Player p : tgtPlayers) {
                String choice;
                Player noNotify = p;
                if (sa.hasParam("AtRandom")) {
                    choice = Aggregates.random(validTypes);
                    noNotify = null;
                } else {
                    choice = p.getController().chooseSomeType(type, sa, validTypes, invalidTypes);
                }

                p.getGame().getAction().notifyOfValue(sa, p, choice, noNotify);

                if (sa.hasParam("Note")) {
                    card.addNotedType(choice);
                    if (!sa.hasParam("ChooseNoted")) {
                        continue;
                    }
                }
                if (sa.hasParam("ChooseType2")) {
                    card.setChosenType2(choice);
                } else {
                    card.setChosenType(choice);
                }
            }
        } else {
            throw new InvalidParameterException(sa.getHostCard() + "'s ability resulted in no types to choose from");
        }
    }
}

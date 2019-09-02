package forge.game.card;

import com.google.common.collect.Iterables;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Direction;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class CardProperty {

    public static boolean cardHasProperty(Card card, String property, Player sourceController, Card source,
            SpellAbility spellAbility) {
        final Game game = card.getGame();
        final Combat combat = game.getCombat();
        // lki can't be null but it does return this
        final Card lki = game.getChangeZoneLKIInfo(card);
        final Player controller = lki.getController();

        // by name can also have color names, so needs to happen before colors.
        if (property.startsWith("named")) {
            String name = TextUtil.fastReplace(property.substring(5), ";", ","); // for some legendary cards
            return card.sharesNameWith(name);
        } else if (property.startsWith("notnamed")) {
            return !card.sharesNameWith(property.substring(8));
        } else if (property.startsWith("sameName")) {
            return card.sharesNameWith(source);
        } else if (property.equals("NamedCard")) {
            return card.sharesNameWith(source.getNamedCard());
        } else if (property.equals("NamedByRememberedPlayer")) {
            if (!source.hasRemembered()) {
                final Card newCard = game.getCardState(source);
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Player) {
                        if (!card.sharesNameWith(((Player) o).getNamedCard())) {
                            return false;
                        }
                    }
                }
            }
            for (final Object o : source.getRemembered()) {
                if (o instanceof Player) {
                    if (!card.sharesNameWith(((Player) o).getNamedCard())) {
                        return false;
                    }
                }
            }
        } else if (property.equals("Permanent")) {
            return !card.isInstant() && !card.isSorcery();
        } else if (property.equals("Historic")) {
            return card.isHistoric();
        } else if (property.startsWith("CardUID_")) {// Protection with "doesn't remove effect"
            return card.getId() == Integer.parseInt(property.split("CardUID_")[1]);
        } else if (property.equals("ChosenCard")) {
            return source.hasChosenCard(card);
        } else if (property.equals("nonChosenCard")) {
            return !source.hasChosenCard(card);
        } else if (property.equals("DoubleFaced")) {
            return card.isDoubleFaced();
        } else if (property.equals("Flip")) {
            return card.isFlipCard();
        } else if (property.equals("Split")) {
            return card.isSplitCard();
        } else if (property.equals("NotSplit")) {
            return !card.isSplitCard();
        } else if (property.startsWith("leftcmc") || property.startsWith("rightcmc")) {
            int x;
            int y = 0;
            String rhs = "";

            if (property.startsWith("leftcmc")) {
                rhs = property.substring(9);
                y = card.getCMC(Card.SplitCMCMode.LeftSplitCMC);
            } else if (property.startsWith("rightcmc")) {
                rhs = property.substring(10);
                y = card.getCMC(Card.SplitCMCMode.RightSplitCMC);
            }

            try {
                x = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                x = AbilityUtils.calculateAmount(source, rhs, spellAbility);
            }

            return Expressions.compare(y, property, x);
        } else if (property.startsWith("YouCtrl")) {
            return controller.equals(sourceController);
        } else if (property.startsWith("YourTeamCtrl")) {
            return controller.getTeam() == sourceController.getTeam();
        } else if (property.startsWith("YouDontCtrl")) {
            return !controller.equals(sourceController);
        } else if (property.startsWith("OppCtrl")) {
            return controller.getOpponents().contains(sourceController);
        } else if (property.startsWith("ChosenCtrl")) {
            return controller.equals(source.getChosenPlayer());
        } else if (property.startsWith("DefenderCtrl")) {
            if (!game.getPhaseHandler().inCombat()) {
                return false;
            }
            if (property.endsWith("ForRemembered")) {
                if (!source.hasRemembered()) {
                    return false;
                }
                return combat.getDefendingPlayerRelatedTo((Card) source.getFirstRemembered()) == controller;
            } else {
                return combat.getDefendingPlayerRelatedTo(source) == controller;
            }
        } else if (property.startsWith("DefendingPlayer")) {
            Player p = property.endsWith("Ctrl") ? controller : card.getOwner();
            if (!game.getPhaseHandler().inCombat()) {
                return false;
            }
            return combat.isPlayerAttacked(p);
        } else if (property.startsWith("EnchantedPlayer")) {
            Player p = property.endsWith("Ctrl") ? controller : card.getOwner();
            final Object o = source.getEntityAttachedTo();
            if (o instanceof Player) {
                return p.equals(o);
            } else { // source not enchanting a player
                return false;
            }
        } else if (property.startsWith("EnchantedController")) {
            Player p = property.endsWith("Ctrl") ? controller : card.getOwner();
            final Object o = source.getEntityAttachedTo();
            if (o instanceof Card) {
                return p.equals(((Card) o).getController());
            } else { // source not enchanting a card
                return false;
            }
        } else if (property.startsWith("RememberedPlayer")) {
            Player p = property.endsWith("Ctrl") ? controller : card.getOwner();
            if (!source.hasRemembered()) {
                final Card newCard = game.getCardState(source);
                if (!newCard.isRemembered(p)) {
                    return false;
                }
            }

            return source.isRemembered(p);
        } else if (property.startsWith("nonRememberedPlayerCtrl")) {
            if (!source.hasRemembered()) {
                final Card newCard = game.getCardState(source);
                if (newCard.isRemembered(controller)) {
                    return false;
                }
            }

            return !source.isRemembered(controller);
        } else if (property.equals("TargetedPlayerCtrl")) {
            boolean foundTargetingSA = false;
            for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    foundTargetingSA = true;
                    for (final Player p : saTargeting.getTargets().getTargetPlayers()) {
                        if (!controller.equals(p)) {
                            return false;
                        }
                    }
                }
            }
            if (!foundTargetingSA) {
                // FIXME: Something went wrong with detecting the SA that has a target, this can happen
                // e.g. when activating a SA on a card from another player's hand (e.g. opponent's Chandra's Fury
                // activated via Sen Triplets). Needs further investigation as to why this is happening, it might
                // cause issues elsewhere too.
                System.err.println("Warning: could not deduce a player target for TargetedPlayerCtrl for " + source + ", trying to locate it via CastSA...");
                SpellAbility castSA = source.getCastSA();
                while (castSA != null) {
                    if (!Iterables.isEmpty(castSA.getTargets().getTargetPlayers())) {
                        foundTargetingSA = true;
                        for (final Player p : castSA.getTargets().getTargetPlayers()) {
                            if (!controller.equals(p)) {
                                return false;
                            }
                        }
                    }
                    castSA = castSA.getSubAbility();
                }
                if (!foundTargetingSA) {
                    System.err.println("Warning: checking targets in CastSA did not yield any results as well, TargetedPlayerCtrl check failed.");
                }
            }
        } else if (property.equals("TargetedControllerCtrl")) {
            for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                final CardCollectionView cards = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                final List<SpellAbility> sas = AbilityUtils.getDefinedSpellAbilities(source, "Targeted", sa);
                for (final Card c : cards) {
                    final Player p = c.getController();
                    if (!controller.equals(p)) {
                        return false;
                    }
                }
                for (final SpellAbility s : sas) {
                    final Player p = s.getHostCard().getController();
                    if (!controller.equals(p)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("ActivePlayerCtrl")) {
            return game.getPhaseHandler().isPlayerTurn(controller);
        } else if (property.startsWith("NonActivePlayerCtrl")) {
            return !game.getPhaseHandler().isPlayerTurn(controller);
        } else if (property.startsWith("YouOwn")) {
            return card.getOwner().equals(sourceController);
        } else if (property.startsWith("YouDontOwn")) {
            return !card.getOwner().equals(sourceController);
        } else if (property.startsWith("OppOwn")) {
            return card.getOwner().getOpponents().contains(sourceController);
        } else if (property.equals("TargetedPlayerOwn")) {
            boolean foundTargetingSA = false;
            for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    foundTargetingSA = true;
                    for (final Player p : saTargeting.getTargets().getTargetPlayers()) {
                        if (!card.getOwner().equals(p)) {
                            return false;
                        }
                    }
                }
            }
            if (!foundTargetingSA) {
                // FIXME: Something went wrong with detecting the SA that has a target, needs investigation
                System.err.println("Warning: could not deduce a player target for TargetedPlayerOwn for " + source + ", trying to locate it via CastSA...");
                SpellAbility castSA = source.getCastSA();
                while (castSA != null) {
                    if (!Iterables.isEmpty(castSA.getTargets().getTargetPlayers())) {
                        foundTargetingSA = true;
                        for (final Player p : castSA.getTargets().getTargetPlayers()) {
                            if (!card.getOwner().equals(p)) {
                                return false;
                            }
                        }
                    }
                    castSA = castSA.getSubAbility();
                }
                if (!foundTargetingSA) {
                    System.err.println("Warning: checking targets in CastSA did not yield any results as well, TargetedPlayerOwn check failed.");
                }
            }
        } else if (property.startsWith("OwnedBy")) {
            final String valid = property.substring(8);
            if (!card.getOwner().isValid(valid, sourceController, source, spellAbility)) {
                final List<Player> lp = AbilityUtils.getDefinedPlayers(source, valid, spellAbility);
                return lp.contains(card.getOwner());
            }
        } else if (property.startsWith("ControlledBy")) {
            final String valid = property.substring(13);
            if (!controller.isValid(valid, sourceController, source, spellAbility)) {
                final List<Player> lp = AbilityUtils.getDefinedPlayers(source, valid, spellAbility);
                return lp.contains(controller);
            }
        } else if (property.startsWith("OwnerDoesntControl")) {
            return !card.getOwner().equals(controller);
        } else if (property.startsWith("ControllerControls")) {
            final String type = property.substring(18);
            if (type.startsWith("AtLeastAsMany")) {
                String realType = type.split("AtLeastAsMany")[1];
                CardCollectionView cards = CardLists.getType(controller.getCardsIn(ZoneType.Battlefield), realType);
                CardCollectionView yours = CardLists.getType(sourceController.getCardsIn(ZoneType.Battlefield), realType);
                return cards.size() >= yours.size();
            } else {
                final CardCollectionView cards = controller.getCardsIn(ZoneType.Battlefield);
                return !CardLists.getType(cards, type).isEmpty();
            }
        } else if (property.startsWith("Other")) {
            return !card.equals(source);
        } else if (property.startsWith("StrictlySelf")) {
            return card.equals(source) && card.getTimestamp() == source.getTimestamp();
        } else if (property.startsWith("Self")) {
            return card.equals(source);
        } else if (property.startsWith("ExiledWithSource")) {
            if (card.getExiledWith() == null) {
                return false;
            }

            Card host = source;
            //Static Abilites doesn't have spellAbility or OriginalHost
            if (spellAbility != null) {
                host = spellAbility.getOriginalHost();
                if (host == null) {
                    host = spellAbility.getHostCard();
                }
            }

            return card.getExiledWith().equals(host);
        } else if (property.equals("EncodedWithSource")) {
            return card.getEncodedCards().contains(source);
        } else if (property.equals("EffectSource")) {
            if (!source.isEmblem() && !source.getType().hasSubtype("Effect")) {
                return false;
            }

            return card.equals(source.getEffectSource());
        } else if (property.equals("CanBeSacrificedBy")) {
            return card.canBeSacrificedBy(spellAbility);
        } else if (property.startsWith("AttachedBy")) {
            return card.hasCardAttachment(source);
        } else if (property.equals("Attached")) {
            return source.hasCardAttachment(card);
        } else if (property.startsWith("AttachedTo")) {
            final String restriction = property.split("AttachedTo ")[1];
            if (restriction.equals("Targeted")) {
                if (!source.getCurrentState().getTriggers().isEmpty()) {
                    for (final Trigger t : source.getCurrentState().getTriggers()) {
                        final SpellAbility sa = t.getTriggeredSA();
                        final CardCollectionView cards = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                        for (final Card c : cards) {
                            if (card.getEquipping() != c && !c.equals(card.getEntityAttachedTo())) {
                                return false;
                            }
                        }
                    }
                } else {
                    for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                        final CardCollectionView cards = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                        for (final Card c : cards) {
                            if (card.getEquipping() == c || c.equals(card.getEntityAttachedTo())) { // handle multiple targets
                                return true;
                            }
                        }
                    }
                    return false;
                }
            } else {
                return (card.getEntityAttachedTo() != null && card.getEntityAttachedTo().isValid(restriction, sourceController, source, spellAbility));
            }
        } else if (property.equals("NameNotEnchantingEnchantedPlayer")) {
            Player enchantedPlayer = source.getPlayerAttachedTo();
            return enchantedPlayer != null && !enchantedPlayer.isEnchantedBy(card.getName());
        } else if (property.equals("NotAttachedTo")) {
            return !source.hasCardAttachment(card);
        } else if (property.startsWith("EnchantedBy")) {
            if (property.equals("EnchantedBy")) {
                return card.isEnchantedBy(source) || card.equals(source.getEntityAttachedTo());
            } else {
                final String restriction = property.split("EnchantedBy ")[1];
                switch (restriction) {
                    case "Imprinted":
                        for (final Card c : source.getImprintedCards()) {
                            if (!card.isEnchantedBy(c) && !card.equals(c.getEntityAttachedTo())) {
                                return false;
                            }
                        }
                        break;
                    case "Targeted":
                        for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                            final SpellAbility saTargeting = sa.getSATargetingCard();
                            if (saTargeting != null) {
                                for (final Card c : saTargeting.getTargets().getTargetCards()) {
                                    if (!card.isEnchantedBy(c) && !card.equals(c.getEntityAttachedTo())) {
                                        return false;
                                    }
                                }
                            }
                        }
                        break;
                    default:  // EnchantedBy Aura.Other
                        for (final Card aura : card.getEnchantedBy()) {
                            if (aura.isValid(restriction, sourceController, source, spellAbility)) {
                                return true;
                            }
                        }
                        return false;
                }
            }
        } else if (property.startsWith("NotEnchantedBy")) {
            if (property.substring(14).equals("Targeted")) {
                for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                    final SpellAbility saTargeting = sa.getSATargetingCard();
                    if (saTargeting != null) {
                        for (final Card c : saTargeting.getTargets().getTargetCards()) {
                            if (card.isEnchantedBy(c)) {
                                return false;
                            }
                        }
                    }
                }
            } else {
                return !card.isEnchantedBy(source);
            }
        } else if (property.startsWith("Enchanted")) {
            return source.equals(card.getEntityAttachedTo());
        } else if (property.startsWith("CanEnchant")) {
            final String restriction = property.substring(10);
            if (restriction.equals("Remembered")) {
                for (final Object rem : source.getRemembered()) {
                    if (!(rem instanceof Card) || !((Card) rem).canBeAttached(card))
                        return false;
                }
            } else if (restriction.equals("Source")) {
                return source.canBeAttached(card);
            }
        } else if (property.startsWith("CanBeEnchantedBy")) {
            if (property.substring(16).equals("Targeted")) {
                for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                    final SpellAbility saTargeting = sa.getSATargetingCard();
                    if (saTargeting != null) {
                        for (final Card c : saTargeting.getTargets().getTargetCards()) {
                            if (!card.canBeAttached(c)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (property.substring(16).equals("AllRemembered")) {
                for (final Object rem : source.getRemembered()) {
                    if (rem instanceof Card) {
                        final Card c = (Card) rem;
                        if (!card.canBeAttached(c)) {
                            return false;
                        }
                    }
                }
            } else {
                return card.canBeAttached(source);
            }
        } else if (property.startsWith("EquippedBy")) {
            if (property.substring(10).equals("Targeted")) {
                for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                    final SpellAbility saTargeting = sa.getSATargetingCard();
                    if (saTargeting != null) {
                        for (final Card c : saTargeting.getTargets().getTargetCards()) {
                            if (!card.hasCardAttachment(c)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (property.substring(10).equals("Enchanted")) {
                return source.getEnchantingCard() != null &&
                        card.hasCardAttachment(source.getEnchantingCard());
            } else {
                return card.hasCardAttachment(source);
            }
        } else if (property.startsWith("FortifiedBy")) {
            return card.hasCardAttachment(source);
        } else if (property.startsWith("CanBeAttachedBy")) {
            return card.canBeAttached(source);
        } else if (property.startsWith("Equipped")) {
            return source.hasCardAttachment(card);
        } else if (property.startsWith("Fortified")) {
            // FIXME TODO what property has this?
            return source.hasCardAttachment(card);
        } else if (property.startsWith("HauntedBy")) {
            return card.isHauntedBy(source);
        } else if (property.startsWith("notTributed")) {
            return !card.isTributed();
        } else if (property.startsWith("madness")) {
            return card.isMadness();
        } else if (property.contains("Paired")) {
            if (property.contains("With")) { // PairedWith
                return card.isPaired() && card.getPairedWith() == source;
            } else if (property.startsWith("Not")) {  // NotPaired
                return !card.isPaired();
            } else { // Paired
                return card.isPaired();
            }
        } else if (property.startsWith("Above")) { // "Are Above" Source
            final CardCollectionView cards = card.getOwner().getCardsIn(ZoneType.Graveyard);
            return cards.indexOf(source) < cards.indexOf(card);
        } else if (property.startsWith("DirectlyAbove")) { // "Are Directly Above" Source
            final CardCollectionView cards = card.getOwner().getCardsIn(ZoneType.Graveyard);
            return cards.indexOf(card) - cards.indexOf(source) == 1;
        } else if (property.startsWith("TopGraveyardCreature")) {
            CardCollection cards = CardLists.filter(card.getOwner().getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES);
            Collections.reverse(cards);
            return !cards.isEmpty() && card.equals(cards.get(0));
        } else if (property.startsWith("TopGraveyard")) {
            final CardCollection cards = new CardCollection(card.getOwner().getCardsIn(ZoneType.Graveyard));
            Collections.reverse(cards);
            if (property.substring(12).matches("[0-9][0-9]?")) {
                int n = Integer.parseInt(property.substring(12));
                int num = Math.min(n, cards.size());
                final CardCollection newlist = new CardCollection();
                for (int i = 0; i < num; i++) {
                    newlist.add(cards.get(i));
                }
                return !cards.isEmpty() && newlist.contains(card);
            } else {
                return !cards.isEmpty() && card.equals(cards.get(0));
            }
        } else if (property.startsWith("BottomGraveyard")) {
            final CardCollectionView cards = card.getOwner().getCardsIn(ZoneType.Graveyard);
            return !cards.isEmpty() && card.equals(cards.get(0));
        } else if (property.startsWith("TopLibrary")) {
            final CardCollectionView cards = card.getOwner().getCardsIn(ZoneType.Library);
            return !cards.isEmpty() && card.equals(cards.get(0));
        } else if (property.startsWith("Cloned")) {
            return (card.getCloneOrigin() != null) && card.getCloneOrigin().equals(source);
        } else if (property.startsWith("DamagedBy")) {
            if ((property.endsWith("Source") || property.equals("DamagedBy")) &&
                    !card.getReceivedDamageFromThisTurn().containsKey(source)) {
                return false;
            } else if (property.endsWith("Remembered")) {
                boolean matched = false;
                for (final Object obj : source.getRemembered()) {
                    if (!(obj instanceof Card)) {
                        continue;
                    }
                    matched |= card.getReceivedDamageFromThisTurn().containsKey(obj);
                }
                return matched;
            } else if (property.endsWith("Equipped")) {
                final Card equipee = source.getEquipping();
                return equipee != null && card.getReceivedDamageFromThisTurn().containsKey(equipee);
            } else if (property.endsWith("Enchanted")) {
                final Card equipee = source.getEnchantingCard();
                return equipee != null && card.getReceivedDamageFromThisTurn().containsKey(equipee);
            }
        } else if (property.startsWith("Damaged")) {
            return card.getDealtDamageToThisTurn().containsKey(source);
        } else if (property.startsWith("IsTargetingSource")) {
            for (final SpellAbility sa : card.getCurrentState().getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingCard();
                if (saTargeting != null) {
                    for (final Card c : saTargeting.getTargets().getTargetCards()) {
                        if (c.equals(source)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else if (property.startsWith("SharesCMCWith")) {
            if (property.equals("SharesCMCWith")) {
                return card.sharesCMCWith(source);
            } else {
                final String restriction = property.split("SharesCMCWith ")[1];
                CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                return !CardLists.filter(list, CardPredicates.sharesCMCWith(card)).isEmpty();
            }
        } else if (property.startsWith("SharesColorWith")) {
            if (property.equals("SharesColorWith")) {
                return card.sharesColorWith(source);
            } else {
                final String restriction = property.split("SharesColorWith ")[1];
                if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    return !CardLists.filter(list, CardPredicates.sharesColorWith(card)).isEmpty();
                }

                switch (restriction) {
                    case "TopCardOfLibrary":
                        final CardCollectionView cards = sourceController.getCardsIn(ZoneType.Library);
                        if (cards.isEmpty() || !card.sharesColorWith(cards.get(0))) {
                            return false;
                        }
                        break;
                    case "Equipped":
                        if (!source.isEquipment() || !source.isEquipping()
                                || !card.sharesColorWith(source.getEquipping())) {
                            return false;
                        }
                        break;
                    case "MostProminentColor":
                        byte mask = CardFactoryUtil.getMostProminentColors(game.getCardsIn(ZoneType.Battlefield));
                        if (!CardUtil.getColors(card).hasAnyColor(mask))
                            return false;
                        break;
                    case "LastCastThisTurn":
                        final List<Card> c = game.getStack().getSpellsCastThisTurn();
                        if (c.isEmpty() || !card.sharesColorWith(c.get(c.size() - 1))) {
                            return false;
                        }
                        break;
                    case "ActivationColor":
                        byte manaSpent = source.getColorsPaid();
                        if (!CardUtil.getColors(card).hasAnyColor(manaSpent)) {
                            return false;
                        }
                        break;
                    default:
                        for (final Card c1 : sourceController.getCardsIn(ZoneType.Battlefield)) {
                            if (c1.isValid(restriction, sourceController, source, spellAbility) && card.sharesColorWith(c1)) {
                                return true;
                            }
                        }
                        return false;
                }
            }
        } else if (property.startsWith("MostProminentColor")) {
            // MostProminentColor <color>
            // e.g. MostProminentColor black
            String[] props = property.split(" ");
            if (props.length == 1) {
                System.out.println("WARNING! Using MostProminentColor property without a color.");
                return false;
            }
            String color = props[1];

            byte mostProm = CardFactoryUtil.getMostProminentColors(game.getCardsIn(ZoneType.Battlefield));
            return ColorSet.fromMask(mostProm).hasAnyColor(MagicColor.fromName(color));
        } else if (property.startsWith("notSharesColorWith")) {
            if (property.equals("notSharesColorWith")) {
                return !card.sharesColorWith(source);
            } else {
                final String restriction = property.split("notSharesColorWith ")[1];
                for (final Card c : sourceController.getCardsIn(ZoneType.Battlefield)) {
                    if (c.isValid(restriction, sourceController, source, spellAbility) && card.sharesColorWith(c)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("sharesCreatureTypeWith")) {
            if (property.equals("sharesCreatureTypeWith")) {
                return card.sharesCreatureTypeWith(source);
            } else {
                final String restriction = property.split("sharesCreatureTypeWith ")[1];
                switch (restriction) {
                    case "TopCardOfLibrary":
                        final CardCollectionView cards = sourceController.getCardsIn(ZoneType.Library);
                        if (cards.isEmpty() || !card.sharesCreatureTypeWith(cards.get(0))) {
                            return false;
                        }
                        break;
                    case "Commander":
                        final List<Card> cmdrs = sourceController.getCommanders();
                        for (Card cmdr : cmdrs) {
                            if (card.sharesCreatureTypeWith(cmdr)) {
                                return true;
                            }
                        }
                        return false;
                    case "Enchanted":
                        for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                            final SpellAbility root = sa.getRootAbility();
                            Card c = source.getEnchantingCard();
                            if ((c == null) && (root != null)
                                    && (root.getPaidList("Sacrificed") != null)
                                    && !root.getPaidList("Sacrificed").isEmpty()) {
                                c = root.getPaidList("Sacrificed").get(0).getEnchantingCard();
                                if (!card.sharesCreatureTypeWith(c)) {
                                    return false;
                                }
                            }
                        }
                        break;
                    case "Equipped":
                        return source.isEquipping() && card.sharesCreatureTypeWith(source.getEquipping());
                    case "Remembered":
                        for (final Object rem : source.getRemembered()) {
                            if (rem instanceof Card) {
                                final Card c = (Card) rem;
                                if (card.sharesCreatureTypeWith(c)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    case "AllRemembered":
                        for (final Object rem : source.getRemembered()) {
                            if (rem instanceof Card) {
                                final Card c = (Card) rem;
                                if (!card.sharesCreatureTypeWith(c)) {
                                    return false;
                                }
                            }
                        }
                        break;
                    default:
                        boolean shares = false;
                        for (final Card c : sourceController.getCardsIn(ZoneType.Battlefield)) {
                            if (c.isValid(restriction, sourceController, source, spellAbility) && card.sharesCreatureTypeWith(c)) {
                                shares = true;
                            }
                        }
                        if (!shares) {
                            return false;
                        }
                        break;
                }
            }
        } else if (property.startsWith("sharesCardTypeWith")) {
            if (property.equals("sharesCardTypeWith")) {
                return card.sharesCardTypeWith(source);
            } else {
                final String restriction = property.split("sharesCardTypeWith ")[1];
                switch (restriction) {
                    case "Imprinted":
                        if (!source.hasImprintedCard() || !card.sharesCardTypeWith(Iterables.getFirst(source.getImprintedCards(), null))) {
                            return false;
                        }
                        break;
                    case "Remembered":
                        for (final Object rem : source.getRemembered()) {
                            if (rem instanceof Card) {
                                final Card c = (Card) rem;
                                if (card.sharesCardTypeWith(c)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    case "TriggeredCard":
                        final Object triggeringObject = source.getTriggeringObject(restriction.substring("Triggered".length()));
                        if (!(triggeringObject instanceof Card)) {
                            return false;
                        }
                        return card.sharesCardTypeWith((Card) triggeringObject);
                    case "EachTopLibrary":
                        final CardCollection cards = new CardCollection();
                        for (Player p : game.getPlayers()) {
                            final Card top = p.getCardsIn(ZoneType.Library).get(0);
                            cards.add(top);
                        }
                        for (Card c : cards) {
                            if (card.sharesCardTypeWith(c)) {
                                return true;
                            }
                        }
                        return false;
                }
            }
        } else if (property.equals("sharesPermanentTypeWith")) {
            return card.sharesPermanentTypeWith(source);
        } else if (property.equals("canProduceSameManaTypeWith")) {
            return card.canProduceSameManaTypeWith(source);
        } else if (property.startsWith("canProduceManaColor")) {
            final String color = property.split("canProduceManaColor ")[1];
            for (SpellAbility ma : card.getManaAbilities()) {
                if (ma.getManaPart().canProduce(MagicColor.toShortString(color))) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("canProduceMana")) {
            return !card.getManaAbilities().isEmpty();
        } else if (property.startsWith("sharesNameWith")) {
            if (property.equals("sharesNameWith")) {
                return card.sharesNameWith(source);
            } else {
                final String restriction = property.split("sharesNameWith ")[1];
                if (restriction.equals("YourGraveyard")) {
                    return !CardLists.filter(sourceController.getCardsIn(ZoneType.Graveyard), CardPredicates.sharesNameWith(card)).isEmpty();
                } else if (restriction.equals(ZoneType.Graveyard.toString())) {
                    return !CardLists.filter(game.getCardsIn(ZoneType.Graveyard), CardPredicates.sharesNameWith(card)).isEmpty();
                } else if (restriction.equals(ZoneType.Battlefield.toString())) {
                    return !CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.sharesNameWith(card)).isEmpty();
                } else if (restriction.equals("ThisTurnCast")) {
                    return !CardLists.filter(CardUtil.getThisTurnCast("Card", source), CardPredicates.sharesNameWith(card)).isEmpty();
                } else if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    return !CardLists.filter(list, CardPredicates.sharesNameWith(card)).isEmpty();
                } else if (restriction.equals("MovedToGrave")) {
                    for (final SpellAbility sa : source.getCurrentState().getNonManaAbilities()) {
                        final SpellAbility root = sa.getRootAbility();
                        if (root != null && (root.getPaidList("MovedToGrave") != null)
                                && !root.getPaidList("MovedToGrave").isEmpty()) {
                            final CardCollectionView cards = root.getPaidList("MovedToGrave");
                            for (final Card c : cards) {
                                String name = c.getName();
                                if (StringUtils.isEmpty(name)) {
                                    name = c.getPaperCard().getName();
                                }
                                if (card.getName().equals(name)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                } else if (restriction.equals("NonToken")) {
                    return !CardLists.filter(game.getCardsIn(ZoneType.Battlefield),
                            Presets.NON_TOKEN, CardPredicates.sharesNameWith(card)).isEmpty();
                } else if (restriction.equals("TriggeredCard")) {
                    if (spellAbility == null) {
                        System.out.println("Looking at TriggeredCard but no SA?");
                    } else {
                        Card triggeredCard = ((Card)spellAbility.getTriggeringObject("Card"));
                        return triggeredCard != null && card.sharesNameWith(triggeredCard);
                    }
                    return false;
                }
            }
        } else if (property.startsWith("doesNotShareNameWith")) {
            if (property.equals("doesNotShareNameWith")) {
                return !card.sharesNameWith(source);
            } else {
                final String restriction = property.split("doesNotShareNameWith ")[1];
                if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    return CardLists.filter(list, CardPredicates.sharesNameWith(card)).isEmpty();
                } else if (restriction.equals("YourGraveyard")) {
                    return CardLists.filter(sourceController.getCardsIn(ZoneType.Graveyard), CardPredicates.sharesNameWith(card)).isEmpty();
                } else if (restriction.equals("OtherYourBattlefield")) {
                    // Obviously it's going to share a name with itself, so consider that in the
                    CardCollection list = CardLists.filter(sourceController.getCardsIn(ZoneType.Battlefield), CardPredicates.sharesNameWith(card));

                    if (list.size() == 1) {
                        Card c = list.getFirst();
                        if (c.getTimestamp() == card.getTimestamp() && c.getId() == card.getId()) {
                            list.remove(card);
                        }
                    }
                    return list.isEmpty();
                }
            }
        } else if (property.startsWith("sharesControllerWith")) {
            if (property.equals("sharesControllerWith")) {
                return card.sharesControllerWith(source);
            } else {
                final String restriction = property.split("sharesControllerWith ")[1];
                if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    return !CardLists.filter(list, CardPredicates.sharesControllerWith(card)).isEmpty();
                }
            }
        } else if (property.startsWith("sharesOwnerWith")) {
            if (property.equals("sharesOwnerWith")) {
                return card.getOwner().equals(source.getOwner());
            } else {
                final String restriction = property.split("sharesOwnerWith ")[1];
                if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card c = (Card) rem;
                            if (!card.getOwner().equals(c.getOwner())) {
                                return false;
                            }
                        }
                    }
                }
            }
        } else if (property.startsWith("SecondSpellCastThisTurn")) {
            final List<Card> cards = CardUtil.getThisTurnCast("Card", source);
            if (cards.size() < 2)  {
                return false;
            }
            else return cards.get(1) == card;
        } else if (property.equals("ThisTurnCast")) {
            for (final Card c : CardUtil.getThisTurnCast("Card", source)) {
                if (card.equals(c)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("ThisTurnEntered")) {
            final String restrictions = property.split("ThisTurnEntered_")[1];
            final String[] res = restrictions.split("_");
            final ZoneType destination = ZoneType.smartValueOf(res[0]);
            ZoneType origin = null;
            if (res.length > 1 && res[1].equals("from")) {
                origin = ZoneType.smartValueOf(res[2]);
            }
            CardCollectionView cards = CardUtil.getThisTurnEntered(destination,
                    origin, "Card", source);
            return cards.contains(card);
        } else if (property.equals("DiscardedThisTurn")) {
            if (card.getTurnInZone() != game.getPhaseHandler().getTurn()) {
                return false;
            }

            CardCollectionView cards = CardUtil.getThisTurnEntered(ZoneType.Graveyard, ZoneType.Hand, "Card", source);
            return cards.contains(card) || card.getMadnessWithoutCast();
        } else if (property.startsWith("ControlledByPlayerInTheDirection")) {
            final String restrictions = property.split("ControlledByPlayerInTheDirection_")[1];
            final String[] res = restrictions.split("_");
            final Direction direction = Direction.valueOf(res[0]);
            Player p = null;
            if (res.length > 1) {
                for (Player pl : game.getPlayers()) {
                    if (pl.isValid(res[1], sourceController, source, spellAbility)) {
                        p = pl;
                        break;
                    }
                }
            } else {
                p = sourceController;
            }
            return p != null && controller.equals(game.getNextPlayerAfter(p, direction));
        } else if (property.startsWith("hasKeyword")) {
            // "withFlash" would find Flashback cards, add this to fix Mystical Teachings
            return card.hasKeyword(property.substring(10));
        } else if (property.startsWith("withFlashback")) {
            boolean fb = card.hasKeyword(Keyword.FLASHBACK);
            return fb;
        } else if (property.startsWith("with")) {
            // ... Card keywords
            if (property.startsWith("without") && card.hasStartOfUnHiddenKeyword(property.substring(7))) {
                return false;
            }
            return property.startsWith("without") || card.hasStartOfUnHiddenKeyword(property.substring(4));
        } else if (property.startsWith("tapped")) {
            return card.isTapped();
        } else if (property.startsWith("untapped")) {
            return card.isUntapped();
        } else if (property.startsWith("faceDown")) {
            return card.isFaceDown();
        } else if (property.startsWith("faceUp")) {
            return !card.isFaceDown();
        } else if (property.startsWith("manifested")) {
            return card.isManifested();
        } else if (property.startsWith("DrawnThisTurn")) {
            return card.getDrawnThisTurn();
        } else if (property.startsWith("enteredBattlefieldThisTurn")) {
            return card.getTurnInZone() == game.getPhaseHandler().getTurn();
        } else if (property.startsWith("notEnteredBattlefieldThisTurn")) {
            return card.getTurnInZone() != game.getPhaseHandler().getTurn();
        } else if (property.startsWith("firstTurnControlled")) {
            return card.isFirstTurnControlled();
        } else if (property.startsWith("notFirstTurnControlled")) {
            return !card.isFirstTurnControlled();
        } else if (property.startsWith("startedTheTurnUntapped")) {
            return card.hasStartedTheTurnUntapped();
        } else if (property.startsWith("cameUnderControlSinceLastUpkeep")) {
            return card.cameUnderControlSinceLastUpkeep();
        } else if (property.equals("attackedOrBlockedSinceYourLastUpkeep")) {
            return card.getDamageHistory().hasAttackedSinceLastUpkeepOf(sourceController)
                    || card.getDamageHistory().hasBlockedSinceLastUpkeepOf(sourceController);
        } else if (property.equals("blockedOrBeenBlockedSinceYourLastUpkeep")) {
            return card.getDamageHistory().hasBeenBlockedSinceLastUpkeepOf(sourceController)
                    || card.getDamageHistory().hasBlockedSinceLastUpkeepOf(sourceController);
        } else if (property.startsWith("dealtDamageToYouThisTurn")) {
            return card.getDamageHistory().getThisTurnDamaged().contains(sourceController);
        } else if (property.startsWith("dealtDamageToOppThisTurn")) {
            return card.hasDealtDamageToOpponentThisTurn();
        } else if (property.startsWith("dealtCombatDamageThisTurn ") || property.startsWith("notDealtCombatDamageThisTurn ")) {
            final String v = property.split(" ")[1];
            final List<GameEntity> list = card.getDamageHistory().getThisTurnCombatDamaged();
            boolean found = false;
            for (final GameEntity e : list) {
                if (e.isValid(v, sourceController, source, spellAbility)) {
                    found = true;
                    break;
                }
            }
            return found != property.startsWith("not");
        } else if (property.startsWith("dealtCombatDamageThisCombat ") || property.startsWith("notDealtCombatDamageThisCombat ")) {
            final String v = property.split(" ")[1];
            final List<GameEntity> list = card.getDamageHistory().getThisCombatDamaged();
            boolean found = false;
            for (final GameEntity e : list) {
                if (e.isValid(v, sourceController, source, spellAbility)) {
                    found = true;
                    break;
                }
            }
            return found != property.startsWith("not");
        } else if (property.startsWith("controllerWasDealtCombatDamageByThisTurn")) {
            return source.getDamageHistory().getThisTurnCombatDamaged().contains(controller);
        } else if (property.startsWith("controllerWasDealtDamageByThisTurn")) {
            return source.getDamageHistory().getThisTurnDamaged().contains(controller);
        } else if (property.startsWith("wasDealtDamageThisTurn")) {
            return !(card.getReceivedDamageFromThisTurn().keySet()).isEmpty();
        } else if (property.startsWith("dealtDamageThisTurn")) {
            return card.getTotalDamageDoneBy() != 0;
        } else if (property.startsWith("attackedThisTurn")) {
            return card.getDamageHistory().getCreatureAttackedThisTurn();
        } else if (property.startsWith("attackedLastTurn")) {
            return card.getDamageHistory().getCreatureAttackedLastTurnOf(controller);
        } else if (property.startsWith("blockedThisTurn")) {
            return card.getDamageHistory().getCreatureBlockedThisTurn();
        } else if (property.startsWith("notExertedThisTurn")) {
            return card.getExertedThisTurn() <= 0;
        } else if (property.startsWith("gotBlockedThisTurn")) {
            return card.getDamageHistory().getCreatureGotBlockedThisTurn();
        } else if (property.startsWith("notAttackedThisTurn")) {
            return !card.getDamageHistory().getCreatureAttackedThisTurn();
        } else if (property.startsWith("notAttackedLastTurn")) {
            return !card.getDamageHistory().getCreatureAttackedLastTurnOf(controller);
        } else if (property.startsWith("notBlockedThisTurn")) {
            return !card.getDamageHistory().getCreatureBlockedThisTurn();
        } else if (property.startsWith("greatestPower")) {
            CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            if (property.contains("ControlledBy")) {
                FCollectionView<Player> p = AbilityUtils.getDefinedPlayers(source, property.split("ControlledBy")[1], spellAbility);
                cards = CardLists.filterControlledBy(cards, p);
                if (!cards.contains(card)) {
                    return false;
                }
            }
            for (final Card crd : cards) {
                if (crd.getNetPower() > card.getNetPower()) {
                    return false;
                }
            }
        } else if (property.startsWith("yardGreatestPower")) {
            final CardCollectionView cards = CardLists.filter(sourceController.getCardsIn(ZoneType.Graveyard), Presets.CREATURES);
            for (final Card crd : cards) {
                if (crd.getNetPower() > card.getNetPower()) {
                    return false;
                }
            }
        } else if (property.startsWith("leastPower")) {
            final CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : cards) {
                if (crd.getNetPower() < card.getNetPower()) {
                    return false;
                }
            }
        } else if (property.startsWith("leastToughness")) {
            final CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : cards) {
                if (crd.getNetToughness() < card.getNetToughness()) {
                    return false;
                }
            }
        } else if (property.startsWith("greatestCMC_")) {
            CardCollectionView cards = game.getCardsIn(ZoneType.Battlefield);
            String prop = property.substring("greatestCMC_".length());
            if (prop.contains("ControlledBy")) {
                prop = prop.split("ControlledBy")[0];
                FCollectionView<Player> p = AbilityUtils.getDefinedPlayers(source, property.split("ControlledBy")[1], null);
                cards = CardLists.filterControlledBy(cards, p);
            }

            if ("NonLandPermanent".equals(prop)) {
                cards = CardLists.filter(cards, CardPredicates.Presets.NONLAND_PERMANENTS);
            } else {
                cards = CardLists.getType(cards, prop);
            }
            cards = CardLists.getCardsWithHighestCMC(cards);
            return cards.contains(card);
        } else if (property.startsWith("greatestRememberedCMC")) {
            CardCollection cards = new CardCollection();
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    cards.add(game.getCardState((Card) o));
                }
            }
            if (!cards.contains(card)) {
                return false;
            }
            cards = CardLists.getCardsWithHighestCMC(cards);
            return cards.contains(card);
        } else if (property.startsWith("lowestRememberedCMC")) {
            CardCollection cards = new CardCollection();
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    cards.add(game.getCardState((Card) o));
                }
            }
            if (!cards.contains(card)) {
                return false;
            }
            cards = CardLists.getCardsWithLowestCMC(cards);
            return cards.contains(card);
        }
        else if (property.startsWith("lowestCMC")) {
            final CardCollectionView cards = game.getCardsIn(ZoneType.Battlefield);
            for (final Card crd : cards) {
                if (!crd.isLand() && !crd.isImmutable()) {
                    // no check for SplitCard anymore
                    if (crd.getCMC() < card.getCMC()) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("enchanted")) {
            return card.isEnchanted();
        } else if (property.startsWith("unenchanted")) {
            return !card.isEnchanted();
        } else if (property.startsWith("enchanting")) {
            return card.isEnchanting();
        } else if (property.startsWith("equipped")) {
            return card.isEquipped();
        } else if (property.startsWith("unequipped")) {
            return !card.isEquipped();
        } else if (property.startsWith("equipping")) {
            return card.isEquipping();
        } else if (property.startsWith("notEquipping")) {
            return !card.isEquipping();
        } else if (property.startsWith("token")) {
            return card.isToken();
        } else if (property.startsWith("nonToken")) {
            return !card.isToken();
        } else if (property.startsWith("hasXCost")) {
            SpellAbility sa1 = card.getFirstSpellAbility();
            return sa1 == null || sa1.isXCost();
        } else if (property.startsWith("suspended")) {
            return card.hasSuspend();
        } else if (property.startsWith("delved")) {
            return source.getDelved().contains(card);
        } else if (property.startsWith("convoked")) {
            return source.getConvoked().contains(card);
        } else if (property.startsWith("exploited")) {
            return source.getExploited().contains(card);
        } else if (property.startsWith("unequalPT")) {
            return card.getNetPower() != card.getNetToughness();
        } else if (property.equals("powerGTtoughness")) {
            return card.getNetPower() > card.getNetToughness();
        } else if (property.equals("powerLTtoughness")) {
            return card.getNetPower() < card.getNetToughness();
        } else if (property.startsWith("power") || property.startsWith("toughness")
                || property.startsWith("cmc") || property.startsWith("totalPT")) {
            int x;
            int y = 0;
            String rhs = "";

            if (property.startsWith("power")) {
                rhs = property.substring(7);
                y = card.getNetPower();
            } else if (property.startsWith("toughness")) {
                rhs = property.substring(11);
                y = card.getNetToughness();
            } else if (property.startsWith("cmc")) {
                rhs = property.substring(5);
                y = card.getCMC();
            } else if (property.startsWith("totalPT")) {
                rhs = property.substring(10);
                y = card.getNetPower() + card.getNetToughness();
            }
            try {
                x = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                x = AbilityUtils.calculateAmount(source, rhs, spellAbility);
            }

            return Expressions.compare(y, property, x);
        }

        // syntax example: countersGE9 P1P1 or countersLT12TIME (greater number
        // than 99 not supported)
        /*
         * slapshot5 - fair warning, you cannot use numbers with 2 digits
         * (greater number than 9 not supported you can use X and the
         * SVar:X:Number$12 to get two digits. This will need a better fix, and
         * I have the beginnings of a regex below
         */
        else if (property.startsWith("counters")) {
            /*
             * Pattern p = Pattern.compile("[a-z]*[A-Z][A-Z][X0-9]+.*$");
             * String[] parse = ???
             * System.out.println("Parsing completed of: "+Property); for (int i
             * = 0; i < parse.length; i++) {
             * System.out.println("parse["+i+"]: "+parse[i]); }
             */

            // TODO get a working regex out of this pattern so the amount of
            // digits doesn't matter
            int number;
            final String[] splitProperty = property.split("_");
            final String strNum = splitProperty[1].substring(2);
            final String comparator = splitProperty[1].substring(0, 2);
            String counterType;
            try {
                number = Integer.parseInt(strNum);
            } catch (final NumberFormatException e) {
                number = CardFactoryUtil.xCount(source, source.getSVar(strNum));
            }
            counterType = splitProperty[2];

            final int actualnumber = card.getCounters(CounterType.getType(counterType));

            return Expressions.compare(actualnumber, comparator, number);
        }
        // These predicated refer to ongoing combat. If no combat happens, they'll return false (meaning not attacking/blocking ATM)
        else if (property.startsWith("attacking")) {
            if (null == combat) return false;
            if (property.equals("attacking"))    return combat.isAttacking(card);
            if (property.equals("attackingLKI")) return combat.isLKIAttacking(card);
            if (property.equals("attackingYou")) return combat.isAttacking(card, sourceController);
            if (property.equals("attackingYouOrYourPW"))  {
                Player defender = combat.getDefenderPlayerByAttacker(card);
                return sourceController.equals(defender);
            }
        } else if (property.startsWith("notattacking")) {
            return null == combat || !combat.isAttacking(card);
        } else if (property.equals("attackedThisCombat")) {
            return null != combat && card.getDamageHistory().getCreatureAttackedThisCombat();
        } else if (property.equals("blockedThisCombat")) {
            return null != combat && card.getDamageHistory().getCreatureBlockedThisCombat();
        } else if (property.equals("attackedBySourceThisCombat")) {
            if (null == combat) return false;
            final GameEntity defender = combat.getDefenderByAttacker(source);
            return !(defender instanceof Card) || card.equals(defender);
        } else if (property.startsWith("blocking")) {
            if (null == combat) return false;
            String what = property.substring("blocking".length());

            if (StringUtils.isEmpty(what)) return combat.isBlocking(card);
            if (what.startsWith("Source")) return combat.isBlocking(card, source) ;
            if (what.startsWith("CreatureYouCtrl")) {
                for (final Card c : CardLists.filter(sourceController.getCardsIn(ZoneType.Battlefield), Presets.CREATURES))
                    if (combat.isBlocking(card, c))
                        return true;
                return false;
            }
            if (what.startsWith("Remembered")) {
                for (final Object o : source.getRemembered()) {
                    if (o instanceof Card && combat.isBlocking(card, (Card) o)) {
                        return true;
                    }
                }
                return false;
            }
        } else if (property.startsWith("sharesBlockingAssignmentWith")) {
            if (null == combat) { return false; }
            if (null == combat.getAttackersBlockedBy(source) || null == combat.getAttackersBlockedBy(card)) { return false; }

            CardCollection sourceBlocking = new CardCollection(combat.getAttackersBlockedBy(source));
            CardCollection thisBlocking = new CardCollection(combat.getAttackersBlockedBy(card));
            return !Collections.disjoint(sourceBlocking, thisBlocking);
        } else if (property.startsWith("notblocking")) {
            return null == combat || !combat.isBlocking(card);
        }
        // Nex predicates refer to past combat and don't need a reference to actual combat
        else if (property.equals("blocked")) {
            return null != combat && combat.isBlocked(card);
        } else if (property.startsWith("blockedBySource")) {
            return null != combat && combat.isBlocking(source, card);
        } else if (property.startsWith("blockedThisTurn")) {
            return !card.getBlockedThisTurn().isEmpty();
        } else if (property.startsWith("blockedByThisTurn")) {
            return !card.getBlockedByThisTurn().isEmpty();
        } else if (property.startsWith("blockedValidThisTurn ")) {
            if (card.getBlockedThisTurn() == null) {
                return false;
            }

            String valid = property.split(" ")[1];
            for(Card c : card.getBlockedThisTurn()) {
                if (c.isValid(valid, card.getController(), source, spellAbility)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("blockedByValidThisTurn ")) {
            if (card.getBlockedByThisTurn() == null) {
                return false;
            }
            String valid = property.split(" ")[1];
            for(Card c : card.getBlockedByThisTurn()) {
                if (c.isValid(valid, card.getController(), source, spellAbility)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("blockedBySourceThisTurn")) {
            return source.getBlockedByThisTurn().contains(card);
        } else if (property.startsWith("blockedSource")) {
            return null != combat && combat.isBlocking(card, source);
        } else if (property.startsWith("isBlockedByRemembered")) {
            if (null == combat) return false;
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card && combat.isBlocking((Card) o, card)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("blockedRemembered")) {
            Card rememberedcard;
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    rememberedcard = (Card) o;
                    if (card.getBlockedThisTurn().contains(rememberedcard)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (property.startsWith("blockedByRemembered")) {
            Card rememberedcard;
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    rememberedcard = (Card) o;
                    if (card.getBlockedByThisTurn().contains(rememberedcard)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (property.startsWith("unblocked")) {
            return combat != null && combat.isUnblocked(card);
        } else if (property.equals("attackersBandedWith")) {
            if (card.equals(source)) {
                // You don't band with yourself
                return false;
            }
            AttackingBand band = combat == null ? null : combat.getBandOfAttacker(source);
            return band != null && band.getAttackers().contains(card);
        } else if (property.startsWith("kicked")) {
            if (property.equals("kicked")) {
                return card.getKickerMagnitude() != 0;
            } else {
                String s = property.split("kicked ")[1];
                if ("1".equals(s) && !card.isOptionalCostPaid(OptionalCost.Kicker1)) return false;
                return !"2".equals(s) || card.isOptionalCostPaid(OptionalCost.Kicker2);
            }
        } else if (property.startsWith("notkicked")) {
            return card.getKickerMagnitude() <= 0;
        } else if (property.startsWith("pseudokicked")) {
            if (property.equals("pseudokicked")) {
                return card.isOptionalCostPaid(OptionalCost.Generic);
            }
        } else if (property.startsWith("surged")) {
            if (card.getCastSA() == null) {
                return false;
            }
            return card.getCastSA().isSurged();
        } else if (property.startsWith("dashed")) {
            if (card.getCastSA() == null) {
                return false;
            }
            return card.getCastSA().isDash();
        } else if (property.startsWith("evoked")) {
            if (card.getCastSA() == null) {
                return false;
            }
            return card.getCastSA().isEvoke();
        } else if (property.startsWith("prowled")) {
            if (card.getCastSA() == null) {
                return false;
            }
            return card.getCastSA().isProwl();
        } else if (property.startsWith("spectacle")) {
            if (card.getCastSA() == null) {
                return false;
            }
            return card.getCastSA().isSpectacle();
        } else if (property.equals("HasDevoured")) {
            return !card.getDevouredCards().isEmpty();
        } else if (property.equals("HasNotDevoured")) {
            return card.getDevouredCards().isEmpty();
        } else if (property.equals("IsMonstrous")) {
            return card.isMonstrous();
        } else if (property.equals("IsNotMonstrous")) {
            return !card.isMonstrous();
        } else if (property.equals("IsUnearthed")) {
            return card.isUnearthed();
        } else if (property.equals("IsRenowned")) {
            return card.isRenowned();
        } else if (property.equals("IsNotRenowned")) {
            return !card.isRenowned();
        } else if (property.startsWith("RememberMap")) {
            System.out.println(source.getRememberMap());
            for (SpellAbility sa : source.getSpellAbilities()) {
                if (sa.getActivatingPlayer() == null) continue;
                for (Player p : AbilityUtils.getDefinedPlayers(source, property.split("RememberMap_")[1], sa)) {
                    if (source.getRememberMap() != null && source.getRememberMap().get(p) != null) {
                        if (source.getRememberMap().get(p).contains(card)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else if (property.equals("IsRemembered")) {
            return source.isRemembered(card);
        } else if (property.equals("IsNotRemembered")) {
            return !source.isRemembered(card);
        } else if (property.equals("IsImprinted")) {
            return source.hasImprintedCard(card);
        } else if (property.equals("IsNotImprinted")) {
            return !source.hasImprintedCard(card);
        } else if (property.equals("NoAbilities")) {
            return (card.getAbilityText().trim().equals("") || card.isFaceDown()) && (card.getUnhiddenKeywords().isEmpty());
        } else if (property.equals("HasCounters")) {
            return card.hasCounters();
        } else if (property.equals("NoCounters")) {
            return !card.hasCounters();
        } else if (property.startsWith("CastSa"))  {
            SpellAbility castSA = card.getCastSA();
            if (castSA == null) {
                return false;
            }
            String v = property.substring(7);
            return castSA.isValid(v, sourceController, source, spellAbility);
        } else if (property.equals("wasCast")) {
            return null != card.getCastFrom();
        } else if (property.equals("wasNotCast")) {
            return null == card.getCastFrom();
        } else if (property.startsWith("wasCastFrom")) {
            // How are we getting in here with a comma?
            final String strZone = property.split(",")[0].substring(11);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            return realZone == card.getCastFrom();
        } else if (property.startsWith("wasNotCastFrom")) {
            final String strZone = property.substring(14);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            return realZone != card.getCastFrom();
        } else if (property.startsWith("set")) {
            final String setCode = property.substring(3, 6);
            return card.getSetCode().equals(setCode);
        } else if (property.startsWith("inZone")) {
            final String strZone = property.substring(6);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            // lki last zone does fall back to this zone
            final Zone lkiZone = lki.getLastKnownZone();

            return lkiZone != null && lkiZone.is(realZone);
        } else if (property.startsWith("inRealZone")) {
            final String strZone = property.substring(10);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);

            return card.isInZone(realZone);
        } else if (property.equals("IsCommander")) {
            return card.isCommander();
        } else {
            // StringType done in CardState
            return card.getCurrentState().hasProperty(property, sourceController, source, spellAbility);
        }
        return true;
    }

}

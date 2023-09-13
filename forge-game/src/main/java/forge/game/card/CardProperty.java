package forge.game.card;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.*;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.AttackRequirement;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Expressions;
import forge.util.TextUtil;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CardProperty {

    public static boolean cardHasProperty(Card card, String property, Player sourceController, Card source, CardTraitBase spellAbility) {
        final Game game = card.getGame();
        final Combat combat = game.getCombat();
        // lki can't be null but it does return this
        final Card lki = game.getChangeZoneLKIInfo(card);
        final Player controller = lki.getController();

        // CR 702.25b if card is phased out it will not count unless specifically asked for
        if (card.isPhasedOut()) {
            if (property.startsWith("phasedOut")) {
                property = property.substring(9);
            } else {
                return false;
            }
        }

        // by name can also have color names, so needs to happen before colors.
        if (property.startsWith("named")) {
            String name = TextUtil.fastReplace(property.substring(5), ";", ","); // workaround for card name with ","
            name = TextUtil.fastReplace(name, "_", " ");
            if (!card.sharesNameWith(name)) {
                return false;
            }
        } else if (property.startsWith("notnamed")) {
            String name = TextUtil.fastReplace(property.substring(8), ";", ","); // workaround for card name with ","
            name = TextUtil.fastReplace(name, "_", " ");
            if (card.sharesNameWith(name)) {
                return false;
            }
        } else if (property.equals("NamedCard")) {
            if (!card.sharesNameWith(source.getNamedCard())) {
                return false;
            }
        } else if (property.equals("NamedCard2")) {
            if (!card.sharesNameWith(source.getNamedCard2())) {
                return false;
            }
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
        } else if (property.startsWith("BorderColor")) {
            if (!property.toUpperCase().contains(card.borderColor().toString())) {
                return false;
            }
        } else if (property.equals("Permanent")) {
            if (!card.isPermanent()) {
                return false;
            }
        } else if (property.equals("Historic")) {
            if (!card.isHistoric()) {
                return false;
            }
        } else if (property.startsWith("CardUID_")) {// Protection with "doesn't remove effect"
            if (card.getId() != Integer.parseInt(property.split("CardUID_")[1])) {
                return false;
            }
        } else if (property.startsWith("ChosenCard")) {
            CardCollectionView chosen = source.getChosenCards();
            int i = chosen.indexOf(card);
            if (i == -1) {
                return false;
            }
            if (property.contains("Strict") && !chosen.get(i).equalsWithTimestamp(card)) {
                return false;
            }
        } else if (property.equals("nonChosenCard")) {
            if (source.hasChosenCard(card)) {
                return false;
            }
        } else if (property.equals("ChosenSector")) {
            if (!source.getChosenSector().equals(card.getSector())) {
                return false;
            }
        } else if (property.equals("DifferentSector")) {
            if (source.getSector().equals(card.getSector())) {
                return false;
            }
        } else if (property.equals("DoubleFaced")) {
            if (!card.isDoubleFaced()) {
                return false;
            }
        } else if (property.equals("FrontSide")) {
            if (card.isBackSide()) {
                return false;
            }
        } else if (property.equals("BackSide")) {
            if (!card.isBackSide()) {
                return false;
            }
        } else if (property.equals("CanTransform")) {
            if (!card.isTransformable()) {
                return false;
            }
        } else if (property.equals("Transformed")) {
            if (!card.isTransformed()) {
                return false;
            }
        } else if (property.equals("Flip")) {
            if (!card.isFlipCard()) {
                return false;
            }
        } else if (property.equals("Split")) {
            if (!card.isSplitCard()) {
                return false;
            }
        } else if (property.equals("NotSplit")) {
            if (card.isSplitCard()) {
                return false;
            }
        } else if (property.equals("AdventureCard")) {
            if (!card.isAdventureCard()) {
                return false;
            }
        } else if (property.equals("IsRingbearer")) {
            if (!card.isRingBearer()) {
                return false;
            }
        } else if (property.equals("IsTriggerRemembered")) {
            boolean found = false;
            for (Object o : spellAbility.getTriggerRemembered()) {
                if (o instanceof Card) {
                    Card trigRem = (Card) o;
                    if (trigRem.equalsWithTimestamp(card)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                return false;
            }
        } else if (property.startsWith("YouCtrl")) {
            if (!controller.equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("YourTeamCtrl")) {
            if (controller.getTeam() != sourceController.getTeam()) {
                return false;
            }
        } else if (property.startsWith("YouDontCtrl")) {
            if (controller.equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("OppCtrl")) {
            if (!controller.getOpponents().contains(sourceController)) {
                return false;
            }
        } else if (property.startsWith("ChosenCtrl")) {
            if (!controller.equals(source.getChosenPlayer())) {
                return false;
            }
        } else if (property.startsWith("DefenderCtrl")) {
            if (!game.getPhaseHandler().inCombat()) {
                return false;
            }
            if (property.endsWith("ForRemembered")) {
                if (!source.hasRemembered()) {
                    return false;
                }
                if (combat.getDefendingPlayerRelatedTo((Card) source.getFirstRemembered()) != controller) {
                    return false;
                }
            } else {
                if (combat.getDefendingPlayerRelatedTo(source) != controller) {
                    return false;
                }
            }
        } else if (property.startsWith("OppProtect")) {
            if (card.getProtectingPlayer() == null
                    || !sourceController.getOpponents().contains(card.getProtectingPlayer())) {
                return false;
            }
        } else if (property.startsWith("ProtectedBy")) {
            if (card.getProtectingPlayer() == null) {
                return false;
            }
            final List<Player> lp = AbilityUtils.getDefinedPlayers(source, property.substring(12), spellAbility);
            if (!lp.contains(card.getProtectingPlayer())) {
                return false;
            }
        } else if (property.startsWith("DefendingPlayer")) {
            Player p = property.endsWith("Ctrl") ? controller : card.getOwner();
            if (!game.getPhaseHandler().inCombat()) {
                return false;
            }
            if (!combat.isPlayerAttacked(p)) {
                return false;
            }
        } else if (property.startsWith("EnchantedPlayer")) {
            Player p = property.endsWith("Ctrl") ? controller : card.getOwner();
            final Object o = source.getEntityAttachedTo();
            if (o instanceof Player) {
                if (!p.equals(o)) {
                    return false;
                }
            } else { // source not enchanting a player
                return false;
            }
        } else if (property.startsWith("EnchantedController")) {
            Player p = property.endsWith("Ctrl") ? controller : card.getOwner();
            final Object o = source.getEntityAttachedTo();
            if (o instanceof Card) {
                if (!p.equals(((Card) o).getController())) {
                    return false;
                }
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

            if (!source.isRemembered(p)) {
                return false;
            }
        } else if (property.startsWith("nonRememberedPlayerCtrl")) {
            if (!source.hasRemembered()) {
                final Card newCard = game.getCardState(source);
                if (newCard.isRemembered(controller)) {
                    return false;
                }
            }

            if (source.isRemembered(controller)) {
                return false;
            }
        } else if (property.equals("targetedBy")) {
            if (!(spellAbility instanceof SpellAbility)) {
                return false;
            }
            SpellAbility sp = (SpellAbility)spellAbility;
            if (!sp.isTargeting(card)) {
                return false;
            }
        } else if (property.equals("TargetedPlayerCtrl")) {
            if (!AbilityUtils.getDefinedPlayers(source, "TargetedPlayer", spellAbility).contains(controller)) {
                return false;
            }
        } else if (property.startsWith("ActivePlayerCtrl")) {
            if (!game.getPhaseHandler().isPlayerTurn(controller)) {
                return false;
            }
        } else if (property.startsWith("NonActivePlayerCtrl")) {
            if (game.getPhaseHandler().isPlayerTurn(controller)) {
                return false;
            }
        } else if (property.startsWith("YouOwn")) {
            if (!card.getOwner().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("YouDontOwn")) {
            if (card.getOwner().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("OppOwn")) {
            if (!card.getOwner().getOpponents().contains(sourceController)) {
                return false;
            }
        } else if (property.equals("TargetedPlayerOwn")) {
            if (!AbilityUtils.getDefinedPlayers(source, "TargetedPlayer", spellAbility).contains(card.getOwner())) {
                return false;
            }
        } else if (property.startsWith("OwnedBy")) {
            final String valid = property.substring(8);
            if (!card.getOwner().isValid(valid, sourceController, source, spellAbility)) {
                final List<Player> lp = AbilityUtils.getDefinedPlayers(source, valid, spellAbility);
                if (!lp.contains(card.getOwner())) {
                    return false;
                }
            }
        } else if (property.startsWith("ControlledBy")) {
            final String valid = property.substring(13);
            if (!controller.isValid(valid, sourceController, source, spellAbility)) {
                final List<Player> lp = AbilityUtils.getDefinedPlayers(source, valid, spellAbility);
                if (!lp.contains(controller)) {
                    return false;
                }
            }
        } else if (property.startsWith("OwnerDoesntControl")) {
            if (card.getOwner().equals(controller)) {
                return false;
            }
        } else if (property.startsWith("ControllerControls")) {
            final String type = property.substring(18);
            if (type.startsWith("More")) {
                String realType = type.split("More")[1];
                CardCollectionView cards = CardLists.getType(controller.getCardsIn(ZoneType.Battlefield), realType);
                CardCollectionView yours = CardLists.getType(sourceController.getCardsIn(ZoneType.Battlefield), realType);
                if (cards.size() <= yours.size()) {
                    return false;
                }
            } else if (type.startsWith("AtLeastAsMany")) {
                String realType = type.split("AtLeastAsMany")[1];
                CardCollectionView cards = CardLists.getType(controller.getCardsIn(ZoneType.Battlefield), realType);
                CardCollectionView yours = CardLists.getType(sourceController.getCardsIn(ZoneType.Battlefield), realType);
                if (cards.size() < yours.size()) {
                    return false;
                }
            } else {
                final CardCollectionView cards = controller.getCardsIn(ZoneType.Battlefield);
                if (type.contains("_")) {
                    final String[] parts = type.split("_", 2);
                    CardCollectionView found = CardLists.getType(cards, parts[0]);
                    final int num = AbilityUtils.calculateAmount(card, parts[1].substring(2), spellAbility);
                    if (!Expressions.compare(found.size(), parts[1].substring(0, 2), num)) {
                        return false;
                    }

                } else if (CardLists.getType(cards, type).isEmpty()) {
                    return false;
                }
            }
        } else if (property.startsWith("StrictlyOther")) {
            if (card.equalsWithTimestamp(source)) {
                return false;
            }
        } else if (property.startsWith("Other")) {
            if (card.equals(source)) {
                return false;
            }
        } else if (property.startsWith("StrictlySelf")) {
            if (!card.equalsWithTimestamp(source)) {
                return false;
            }
        } else if (property.startsWith("Self")) {
            if (!card.equals(source)) {
                return false;
            }
        } else if (property.startsWith("ExiledByYou")) {
            if (card.getExiledBy() == null) {
                return false;
            }
            if (!card.getExiledBy().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("ExiledWithSourceLKI")) {
            List<Card> exiled = card.getZone().getCardsAddedThisTurn(null);
            int idx = exiled.lastIndexOf(card);
            if (idx == -1) {
                return false;
            }
            Card lkiExiled = exiled.get(idx);

            if (lkiExiled.getExiledWith() == null) {
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
            if (!lkiExiled.getExiledWith().equalsWithTimestamp(host)) {
                return false;
            }
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
            if (!source.hasExiledCard(card) || !card.getExiledWith().equalsWithTimestamp(host)) {
                return false;
            }
        } else if (property.equals("ExiledWithEffectSource")) {
            if (card.getExiledWith() == null) {
                return false;
            }
            if (!card.getExiledWith().equalsWithTimestamp(source.getEffectSource())) {
                return false;
            }
        } else if (property.equals("EncodedWithSource")) {
            if (!card.getEncodedCards().contains(source)) {
                return false;
            }
        } else if (property.equals("EffectSource")) {
            if (!source.isImmutable()) {
                return false;
            }

            if (!card.equals(source.getEffectSource())) {
                return false;
            }
        } else if (property.equals("CanBeSacrificedBy") && spellAbility instanceof SpellAbility) {
            // used for Emerge and Offering, these are SpellCost, not effect
            if (!card.canBeSacrificedBy((SpellAbility) spellAbility, false)) {
                return false;
            }
        } else if (property.equals("Attached")) {
            if (!source.hasCardAttachment(card)) {
                return false;
            }
        } else if (property.startsWith("AttachedTo")) {
            final String restriction = property.split("AttachedTo ")[1];

            if (!card.isAttachedToEntity()) {
                return false;
            }

            if (!card.getEntityAttachedTo().isValid(restriction, sourceController, source, spellAbility)) {
                // only few cases need players
                if (!(restriction.contains("Player") ? AbilityUtils.getDefinedPlayers(source, restriction, spellAbility) : AbilityUtils.getDefinedCards(source, restriction, spellAbility)).contains(card.getEntityAttachedTo())) {
                    return false;
                }
            }
        } else if (property.equals("NameNotEnchantingEnchantedPlayer")) {
            Player enchantedPlayer = source.getPlayerAttachedTo();
            if (enchantedPlayer == null || enchantedPlayer.isEnchantedBy(card.getName())) {
                return false;
            }
        } else if (property.equals("NotAttachedTo")) {
            if (source.hasCardAttachment(card)) {
                return false;
            }
        } else if (property.startsWith("EnchantedBy")) {
            if (property.equals("EnchantedBy")) {
                if (!card.isEnchantedBy(source) && !card.equals(source.getEntityAttachedTo())) {
                    return false;
                }
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
                        for (final Card c : AbilityUtils.getDefinedCards(source, "Targeted", spellAbility)) {
                            if (!card.isEnchantedBy(c) && !card.equals(c.getEntityAttachedTo())) {
                                return false;
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
                for (final Card c : AbilityUtils.getDefinedCards(source, "Targeted", spellAbility)) {
                    if (card.isEnchantedBy(c)) {
                        return false;
                    }
                }
            } else {
                if (card.isEnchantedBy(source)) {
                    return false;
                }
            }
        } else if (property.startsWith("Enchanted")) {
            if (!source.equals(card.getEntityAttachedTo())) {
                return false;
            }
        } else if (property.startsWith("CanEnchant")) {
            final String restriction = property.substring(10);
            if (restriction.equals("EquippedBy")) {
                if (!source.isEquipping() || !source.getEquipping().canBeAttached(card, null)) return false;
            }
            if (restriction.equals("Remembered")) {
                for (final Object rem : source.getRemembered()) {
                    if (!(rem instanceof Card) || !((Card) rem).canBeAttached(card, null))
                        return false;
                }
            } else if (restriction.equals("Source")) {
                if (!source.canBeAttached(card, null)) return false;
            }
        } else if (property.startsWith("CanBeEnchantedBy")) {
            if (property.substring(16).equals("Targeted")) {
                for (final Card c : AbilityUtils.getDefinedCards(source, "Targeted", spellAbility)) {
                    if (!card.canBeAttached(c, null)) {
                        return false;
                    }
                }
            } else if (property.substring(16).equals("AllRemembered")) {
                for (final Object rem : source.getRemembered()) {
                    if (rem instanceof Card) {
                        final Card c = (Card) rem;
                        if (!card.canBeAttached(c, null)) {
                            return false;
                        }
                    }
                }
            } else {
                if (!card.canBeAttached(source, null)) {
                    return false;
                }
            }
        } else if (property.startsWith("EquippedBy") || property.startsWith("AttachedBy")) {
            if (property.substring(10).equals("Targeted")) {
                for (final Card c : AbilityUtils.getDefinedCards(source, "Targeted", spellAbility)) {
                    if (!card.hasCardAttachment(c)) {
                        return false;
                    }
                }
            } else if (property.substring(10).equals("Enchanted")) {
                if (source.getEnchantingCard() == null ||
                        !card.hasCardAttachment(source.getEnchantingCard())) {
                    return false;
                }
            } else {
                if (!card.hasCardAttachment(source)) {
                    return false;
                }
            }
        } else if (property.startsWith("FortifiedBy")) {
            if (!card.hasCardAttachment(source)) {
                return false;
            }
        } else if (property.startsWith("CanBeAttachedBy")) {
            if (!card.canBeAttached(source, null)) {
                return false;
            }
        } else if (property.startsWith("HauntedBy")) {
            if (!card.isHauntedBy(source)) {
                return false;
            }
        } else if (property.startsWith("notTributed")) {
            if (card.isTributed()) {
                return false;
            }
        } else if (property.startsWith("madness")) {
            if (!card.isMadness()) {
                return false;
            }
        } else if (property.contains("Paired")) {
            if (property.contains("With")) { // PairedWith
                if (!card.isPaired() || card.getPairedWith() != source) {
                    return false;
                }
            } else if (property.startsWith("Not")) {  // NotPaired
                if (card.isPaired()) {
                    return false;
                }
            } else { // Paired
                if (!card.isPaired()) {
                    return false;
                }
            }
        } else if (property.startsWith("Above")) { // "Are Above" Source
            final CardCollectionView cards = card.getOwner().getCardsIn(ZoneType.Graveyard);
            if (cards.indexOf(source) >= cards.indexOf(card)) {
                return false;
            }
        } else if (property.startsWith("DirectlyAbove")) { // "Are Directly Above" Source
            final CardCollectionView cards = card.getOwner().getCardsIn(ZoneType.Graveyard);
            if (cards.indexOf(card) - cards.indexOf(source) != 1) {
                return false;
            }
        } else if (property.startsWith("TopGraveyardCreature")) {
            CardCollection cards = CardLists.filter(card.getOwner().getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES);
            Collections.reverse(cards);
            if (cards.isEmpty() || !card.equals(cards.get(0))) {
                return false;
            }
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
                if (cards.isEmpty() || !newlist.contains(card)) {
                    return false;
                }
            } else {
                if (cards.isEmpty() || !card.equals(cards.get(0))) {
                    return false;
                }
            }
        } else if (property.startsWith("BottomGraveyard")) {
            final CardCollectionView cards = card.getOwner().getCardsIn(ZoneType.Graveyard);
            if (cards.isEmpty() || !card.equals(cards.get(0))) {
                return false;
            }
        } else if (property.startsWith("TopLibrary")) {
            final CardCollectionView cards = card.getOwner().getCardsIn(ZoneType.Library);
            if (cards.isEmpty() || !card.equals(cards.get(0))) {
                return false;
            }
        } else if (property.startsWith("BottomLibrary")) {
            CardCollection cards = new CardCollection(card.getOwner().getCardsIn(ZoneType.Library));
            if (property.startsWith("BottomLibrary_")) {
                cards = CardLists.getValidCards(cards, property.substring(14), sourceController, source, spellAbility);
            }
            Collections.reverse(cards);
            if (cards.isEmpty() || !card.equals(cards.get(0))) {
                return false;
            }
        } else if (property.startsWith("Cloned")) {
            if (card.getCloneOrigin() == null || !card.getCloneOrigin().equals(source)) {
                return false;
            }
        } else if (property.startsWith("SharesCMCWith")) {
            if (property.equals("SharesCMCWith")) {
                if (!card.sharesCMCWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("SharesCMCWith ")[1];
                CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                return Iterables.any(list, CardPredicates.sharesCMCWith(card));
            }
        } else if (property.startsWith("SharesColorWith")) {
            // if card is colorless, it can't share colors
            if (card.isColorless()) {
                return false;
            }
            if (property.equals("SharesColorWith")) {
                if (!card.sharesColorWith(source)) {
                    return false;
                }
            } else {
                // Special case to prevent list from comparing with itself
                if (property.startsWith("SharesColorWithOther")) {
                    final String restriction = property.split("SharesColorWithOther ")[1];
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    list.remove(card);
                    return Iterables.any(list, CardPredicates.sharesColorWith(card));
                }

                final String restriction = property.split("SharesColorWith ")[1];
                if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted") || restriction.startsWith("TopOfLibrary")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    return Iterables.any(list, CardPredicates.sharesColorWith(card));
                }

                switch (restriction) {
                    case "Equipped":
                        if (!source.isEquipment() || !source.isEquipping()
                                || !card.sharesColorWith(source.getEquipping())) {
                            return false;
                        }
                        break;
                    case "MostProminentColor":
                        byte mask = CardFactoryUtil.getMostProminentColors(game.getCardsIn(ZoneType.Battlefield));
                        if (!card.getColor().hasAnyColor(mask))
                            return false;
                        break;
                    case "LastCastThisTurn":
                        final List<Card> c = game.getStack().getSpellsCastThisTurn();
                        if (c.isEmpty() || !card.sharesColorWith(c.get(c.size() - 1))) {
                            return false;
                        }
                        break;
                    case "ActivationColor":
                        SpellAbilityStackInstance castSA = game.getStack().getInstanceMatchingSpellAbilityID((SpellAbility) spellAbility);
                        if (castSA == null) {
                            return false;
                        }
                        List<Mana> payingMana = castSA.getSpellAbility().getPayingMana();
                        // even if the cost was raised, we only care about mana from activation part
                        // since this can only be 1 currently with Protective Sphere, let's just assume it's the first shard spent for easy handling
                        if (payingMana.isEmpty() || !card.getColor().hasAnyColor(payingMana.get(0).getColor())) {
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
                if (card.sharesColorWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("notSharesColorWith ")[1];
                for (final Card c : sourceController.getCardsIn(ZoneType.Battlefield)) {
                    if (c.isValid(restriction, sourceController, source, spellAbility) && card.sharesColorWith(c)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("MostProminentCreatureTypeInLibrary")) {
            final CardCollectionView list = sourceController.getCardsIn(ZoneType.Library);
            for (String s : CardFactoryUtil.getMostProminentCreatureType(list)) {
                if (!card.getType().hasCreatureType(s)) {
                    return false;
                }
            }
        } else if (property.equals("Party")) {
            boolean isParty = false;
            Set<String> partyTypes = Sets.newHashSet("Cleric", "Rogue", "Warrior", "Wizard");
            Set<String> cTypes = card.getType().getCreatureTypes();
            for (String t : partyTypes) {
                if (cTypes.contains(t)) {
                    isParty = true;
                    break;
                }
            }
            return isParty;
        } else if (property.startsWith("sharesCreatureTypeWith")) {
            if (property.equals("sharesCreatureTypeWith")) {
                if (!card.sharesCreatureTypeWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesCreatureTypeWith ")[1];
                switch (restriction) {
                    case "Commander":
                        final List<Card> cmdrs = sourceController.getCommanders();
                        for (Card cmdr : cmdrs) {
                            cmdr = game.getCardState(cmdr);
                            // if your commander is in a hidden zone or phased out
                            // it's considered to have no creature types
                            if (cmdr.getZone().getZoneType().isHidden() || cmdr.isPhasedOut()) {
                                continue;
                            }
                            if (card.sharesCreatureTypeWith(cmdr)) {
                                return true;
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
                        if (!Iterables.any(AbilityUtils.getDefinedCards(source, restriction, spellAbility), CardPredicates.sharesCreatureTypeWith(card))) {
                            return false;
                        }
                        break;
                }
            }
        } else if (property.startsWith("sharesCardTypeWith")) {
            if (property.equals("sharesCardTypeWith")) {
                if (!card.sharesCardTypeWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesCardTypeWith ")[1];
                switch (restriction) {
                    case "Imprinted":
                        if (!source.hasImprintedCard() || !card.sharesCardTypeWith(Iterables.getFirst(source.getImprintedCards(), null))) {
                            return false;
                        }
                        break;
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
                    default:
                        if (!Iterables.any(AbilityUtils.getDefinedCards(source, restriction, spellAbility), CardPredicates.sharesCardTypeWith(card))) {
                            return false;
                        }
                }
            }
        } else if (property.startsWith("sharesLandTypeWith")) {
            final String restriction = property.split("sharesLandTypeWith ")[1];
            if (!Iterables.any(AbilityUtils.getDefinedCards(source, restriction, spellAbility), CardPredicates.sharesLandTypeWith(card))) {
                return false;
            }
        } else if (property.equals("sharesPermanentTypeWith")) {
            if (!card.sharesPermanentTypeWith(source)) {
                return false;
            }
        } else if (property.equals("canProduceSameManaTypeWith")) {
            if (!card.canProduceSameManaTypeWith(source)) {
                return false;
            }
        } else if (property.startsWith("canProduceManaColor")) {
            final String color = property.split("canProduceManaColor ")[1];
            for (SpellAbility ma : card.getManaAbilities()) {
                if (ma.canProduce(MagicColor.toShortString(color))) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("canProduceMana")) {
            return !card.getManaAbilities().isEmpty();
        } else if (property.startsWith("sameName")) {
            if (!card.sharesNameWith(source)) {
                return false;
            }
        } else if (property.startsWith("sharesNameWith")) {
            if (property.equals("sharesNameWith")) {
                if (!card.sharesNameWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesNameWith ")[1];
                if (restriction.equals("YourGraveyard")) {
                    return Iterables.any(sourceController.getCardsIn(ZoneType.Graveyard), CardPredicates.sharesNameWith(card));
                } else if (restriction.equals(ZoneType.Graveyard.toString())) {
                    return Iterables.any(game.getCardsIn(ZoneType.Graveyard), CardPredicates.sharesNameWith(card));
                } else if (restriction.equals(ZoneType.Battlefield.toString())) {
                    return Iterables.any(game.getCardsIn(ZoneType.Battlefield), CardPredicates.sharesNameWith(card));
                } else if (restriction.equals("ThisTurnCast")) {
                    return Iterables.any(CardUtil.getThisTurnCast("Card", source, spellAbility, sourceController), CardPredicates.sharesNameWith(card));
                } else if (restriction.equals("MovedToGrave")) {
                    if (!(spellAbility instanceof SpellAbility)) {
                        final SpellAbility root = ((SpellAbility) spellAbility).getRootAbility();
                        if (root != null && (root.getPaidList("MovedToGrave", true) != null)
                                && !root.getPaidList("MovedToGrave", true).isEmpty()) {
                            final CardCollectionView cards = root.getPaidList("MovedToGrave", true);
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
                    if (!(spellAbility instanceof SpellAbility)) {
                        System.out.println("Looking at TriggeredCard but no SA?");
                    } else {
                        Card triggeredCard = ((Card) ((SpellAbility) spellAbility).getRootAbility().getTriggeringObject(AbilityKey.Card));
                        if (triggeredCard != null && card.sharesNameWith(triggeredCard)) {
                            return true;
                        }
                    }
                    return false;
                } else {
                    if (!Iterables.any(AbilityUtils.getDefinedCards(source, restriction, spellAbility), CardPredicates.sharesNameWith(card))) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("doesNotShareNameWith")) {
            if (property.equals("doesNotShareNameWith")) {
                if (card.sharesNameWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("doesNotShareNameWith ")[1];
                if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    return !Iterables.any(list, CardPredicates.sharesNameWith(card));
                } else if (restriction.equals("YourGraveyard")) {
                    return !Iterables.any(sourceController.getCardsIn(ZoneType.Graveyard), CardPredicates.sharesNameWith(card));
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
                } else {
                    CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), restriction,
                            sourceController, source, spellAbility);
                    return !Iterables.any(list, CardPredicates.sharesNameWith(card));
                }
            }
        } else if (property.startsWith("sharesControllerWith")) {
            if (property.equals("sharesControllerWith")) {
                if (!card.sharesControllerWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesControllerWith ")[1];
                CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                if (!Iterables.any(list, CardPredicates.sharesControllerWith(card))) {
                    return false;
                }
            }
        } else if (property.startsWith("sharesOwnerWith")) {
            if (property.equals("sharesOwnerWith")) {
                if (!card.getOwner().equals(source.getOwner())) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesOwnerWith ")[1];
                CardCollection def = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                if (!Iterables.all(def, CardPredicates.isOwner(card.getOwner()))) {
                    return false;
                }
            }
        } else if (property.startsWith("SecondSpellCastThisTurn")) {
            final List<Card> cards = CardUtil.getThisTurnCast("Card", source, spellAbility, sourceController);
            if (cards.size() < 2) {
                return false;
            }
            if (!cards.get(1).equalsWithTimestamp(card)) {
                return false;
            }
        } else if (property.equals("ThisTurnCast")) {
            for (final Card c : CardUtil.getThisTurnCast("Card", source, spellAbility, sourceController)) {
                if (card.equals(c)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("EnteredUnder")) {
            Player u = card.getTurnInController();
            if (u == null) {
                return false;
            }
            final String valid = property.substring(13);
            if (!u.isValid(valid, sourceController, source, spellAbility)) {
                final List<Player> lp = AbilityUtils.getDefinedPlayers(source, valid, spellAbility);
                if (!lp.contains(u)) {
                    return false;
                }
            }
        } else if (property.equals("EnteredSinceYourLastTurn")) {
            if (card.getTurnInZone() <= sourceController.getLastTurnNr()) {
                return false;
            }
        } else if (property.startsWith("ThisTurnEnteredFrom")) {
            final String restrictions = property.split("ThisTurnEnteredFrom_")[1];
            final String[] res = restrictions.split("_");
            final ZoneType origin = ZoneType.smartValueOf(res[0]);

            if (card.getTurnInZone() != game.getPhaseHandler().getTurn()) {
                return false;
            }

            if (!card.getZone().isCardAddedThisTurn(card, origin)) {
                return false;
            }
        } else if (property.startsWith("ThisTurnEntered")) {
            // only check if it entered the Zone this turn
            if (card.getTurnInZone() != game.getPhaseHandler().getTurn()) {
                return false;
            }
            if (!property.equals("ThisTurnEntered")) { // to confirm specific zones / player
                final boolean your = property.contains("Your");
                final ZoneType where = ZoneType.smartValueOf(property.substring(your ? 19 : 15));
                final Zone z = sourceController.getZone(where);
                if (!z.getCardsAddedThisTurn(null).contains(card)) {
                    return false;
                }
                if (your) { // for corner cases of controlling other player
                    if (!card.getOwner().equals(sourceController)) {
                        return false;
                    }
                }
            }
        } else if (property.equals("NotThisTurnEntered")) {
            // only check if it entered the Zone this turn
            if (card.getTurnInZone() == game.getPhaseHandler().getTurn()) {
                return false;
            }
        } else if (property.equals("DiscardedThisTurn")) {
            if (card.getTurnInZone() != game.getPhaseHandler().getTurn()) {
                return false;
            }

            if (!card.wasDiscarded()) {
                return false;
            }
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
            if (p == null || !controller.equals(game.getNextPlayerAfter(p, direction))) {
                return false;
            }
        } else if (property.equals("hasABasicLandType")) {
            if (!card.hasABasicLandType()) {
                return false;
            }
        } else if (property.startsWith("hasKeyword")) {
            // "withFlash" would find Flashback cards, add this to fix Mystical Teachings
            if (!card.hasKeyword(property.substring(10))) {
                return false;
            }
        } else if (property.startsWith("with")) {
            // ... Card keywords
            if (property.startsWith("without") && card.hasStartOfUnHiddenKeyword(property.substring(7))) {
                return false;
            }
            if (!property.startsWith("without") && !card.hasStartOfUnHiddenKeyword(property.substring(4))) {
                return false;
            }
        } else if (property.equals("hasNonmanaAbilities")) {
            boolean hasAbilities = false;
            for(SpellAbility sa : card.getSpellAbilities()) {
                if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                    hasAbilities = true;
                    break;
                }
            }

            if (!hasAbilities) {
                return false;
            }
        } else if (property.startsWith("activated")) {
            if (!card.activatedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("tapped")) {
            if (!card.isTapped()) {
                return false;
            }
        } else if (property.startsWith("untapped")) {
            if (!card.isUntapped()) {
                return false;
            }
        } else if (property.startsWith("faceDown")) {
            if (!card.isFaceDown()) {
                return false;
            }
        } else if (property.startsWith("faceUp")) {
            if (card.isFaceDown()) {
                return false;
            }
        } else if (property.startsWith("phasedOut")) {
            if (!card.isPhasedOut()) {
                return false;
            }
        } else if (property.startsWith("phasedIn")) {
            if (card.isPhasedOut()) {
                return false;
            }
        } else if (property.startsWith("manifested")) {
            if (!card.isManifested()) {
                return false;
            }
        } else if (property.startsWith("DrawnThisTurn")) {
            if (!card.getDrawnThisTurn()) {
                return false;
            }
        } else if (property.startsWith("notDrawnThisTurn")) {
            if (card.getDrawnThisTurn()) {
                return false;
            }
        } else if (property.startsWith("FoughtThisTurn")) {
            if (!card.getFoughtThisTurn()) {
                return false;
            }
        } else if (property.startsWith("firstTurnControlled")) {
            if (!card.isFirstTurnControlled()) {
                return false;
            }
        } else if (property.startsWith("notFirstTurnControlled")) {
            if (card.isFirstTurnControlled()) {
                return false;
            }
        } else if (property.startsWith("startedTheTurnUntapped")) {
            if (!card.hasStartedTheTurnUntapped()) {
                return false;
            }
        } else if (property.startsWith("cameUnderControlSinceLastUpkeep")) {
            if (!card.cameUnderControlSinceLastUpkeep()) {
                return false;
            }
        } else if (property.equals("attackedOrBlockedSinceYourLastUpkeep")) {
            if (!card.getDamageHistory().hasAttackedSinceLastUpkeepOf(sourceController)
                    && !card.getDamageHistory().hasBlockedSinceLastUpkeepOf(sourceController)) {
                return false;
            }
        } else if (property.equals("blockedOrBeenBlockedSinceYourLastUpkeep")) {
            if (!card.getDamageHistory().hasBeenBlockedSinceLastUpkeepOf(sourceController)
                    && !card.getDamageHistory().hasBlockedSinceLastUpkeepOf(sourceController)) {
                return false;
            }
        } else if (property.startsWith("DamagedBy")) {
            String prop = property.substring("DamagedBy".length());
            CardCollection def = null;
            if (prop.startsWith(" ")) {
                def = AbilityUtils.getDefinedCards(source, prop.substring(1), spellAbility);
            }
            boolean found = false;
            for (Pair<Integer, Boolean> p : card.getDamageReceivedThisTurn()) {
                Card dmgSource = game.getDamageLKI(p).getLeft();
                if (def != null) {
                    for (Card c : def) {
                        if (dmgSource.equalsWithTimestamp(c)) {
                            found = true;
                        }
                    }
                }
                else if (prop.isEmpty() && dmgSource.equalsWithTimestamp(source)) {
                    found = true;
                } else if (dmgSource.isValid(prop.split(","), sourceController, source, spellAbility)) {
                    found = true;
                }
                if (found) {
                    break;
                }
            }
            if (!found) {
                return false;
            }
        } else if (property.startsWith("Damaged")) {
            boolean found = false;
            for (Pair<Integer, Boolean> p : source.getDamageReceivedThisTurn()) {
                if (game.getDamageLKI(p).getLeft().equalsWithTimestamp(card)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        } else if (property.startsWith("dealtCombatDamageThisCombat")) {
            if (card.getDamageHistory().getThisCombatDamaged().isEmpty()) {
                return false;
            }
        } else if (property.startsWith("dealtDamageToYouThisTurn")) {
            if (card.getDamageHistory().getDamageDoneThisTurn(null, true, null, "You", card, sourceController, spellAbility) == 0) {
                return false;
            }
        } else if (property.startsWith("dealtDamageToOppThisTurn")) {
            if (!card.hasDealtDamageToOpponentThisTurn()) {
                return false;
            }
        } else if (property.startsWith("dealtCombatDamage") || property.startsWith("notDealtCombatDamage")) {
            final String v = property.split(" ")[1];
            boolean found = card.getDamageHistory().getDamageDoneThisTurn(true, true, null, v, card, sourceController, spellAbility) > 0;

            if (found == property.startsWith("not")) {
                return false;
            }
        } else if (property.startsWith("controllerWasDealtCombatDamageByThisTurn")) {
            if (source.getDamageHistory().getDamageDoneThisTurn(true, true, null, "You", card, controller, spellAbility) == 0) {
                return false;
            }
        } else if (property.startsWith("controllerWasDealtDamageByThisTurn")) {
            if (source.getDamageHistory().getDamageDoneThisTurn(null, true, null, "You", card, controller, spellAbility) == 0) {
                return false;
            }
        } else if (property.startsWith("wasDealtDamageThisTurn")) {
            if (card.getAssignedDamage() == 0) {
                return false;
            }
        } else if (property.equals("wasDealtNonCombatDamageThisTurn")) {
            if (card.getAssignedDamage(false, null) == 0) {
                return false;
            }
        } else if (property.startsWith("wasDealtExcessDamageThisTurn")) {
            if (!card.hasBeenDealtExcessDamageThisTurn()) {
                return false;
            }
        } else if (property.startsWith("wasDealtDamageByThisGame")) {
            int idx = source.getDamageHistory().getThisGameDamaged().indexOf(card);
            if (idx == -1) {
                return false;
            }
            Card c = (Card) source.getDamageHistory().getThisGameDamaged().get(idx);
            if (!c.equalsWithTimestamp(game.getCardState(card))) {
                return false;
            }
        } else if (property.startsWith("dealtDamageThisTurn")) {
            if (card.getTotalDamageDoneBy() == 0) {
                return false;
            }
        } else if (property.startsWith("dealtDamagetoAny")) {
            return card.getDamageHistory().getHasdealtDamagetoAny();
        } else if (property.startsWith("attackedThisTurn")) {
            if (card.getDamageHistory().getCreatureAttacksThisTurn() == 0) {
                return false;
            }
        } else if (property.startsWith("attackedBattleThisTurn")) {
            if (!card.getDamageHistory().hasAttackedBattleThisTurn()) {
                return false;
            }
        } else if (property.startsWith("attackedYouThisTurn")) {
            if (!card.getDamageHistory().hasAttackedThisTurn(sourceController)) {
                return false;
            }
        } else if (property.startsWith("attackedLastTurn")) {
            return card.getDamageHistory().getCreatureAttackedLastTurnOf(controller);
        } else if (property.startsWith("blockedThisTurn")) {
            if (card.getBlockedThisTurn().isEmpty()) {
                return false;
            }
        } else if (property.startsWith("notBlockedThisTurn")) {
            if (!card.getBlockedThisTurn().isEmpty()) {
                return false;
            }
        } else if (property.startsWith("notExertedThisTurn")) {
            if (card.getExertedThisTurn() > 0) {
                return false;
            }
        } else if (property.startsWith("gotBlockedThisTurn")) {
            if (card.getBlockedByThisTurn().isEmpty()) {
                return false;
            }
        } else if (property.startsWith("notAttackedThisTurn")) {
            if (card.getDamageHistory().getCreatureAttacksThisTurn() > 0) {
                return false;
            }
        } else if (property.startsWith("notAttackedLastTurn")) {
            return !card.getDamageHistory().getCreatureAttackedLastTurnOf(controller);

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
            CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            if (property.contains("ControlledBy")) {
                FCollectionView<Player> p = AbilityUtils.getDefinedPlayers(source, property.split("ControlledBy")[1], spellAbility);
                cards = CardLists.filterControlledBy(cards, p);
                if (!cards.contains(card)) {
                    return false;
                }
            }
            for (final Card crd : cards) {
                if (crd.getNetPower() < card.getNetPower()) {
                    return false;
                }
            }
        } else if (property.startsWith("leastToughness")) {
            CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            if (property.contains("ControlledBy")) { // 4/25/2023 only used for adventure mode Death Ring
                FCollectionView<Player> p = AbilityUtils.getDefinedPlayers(source, property.split("ControlledBy")[1], spellAbility);
                cards = CardLists.filterControlledBy(cards, p);
                if (!cards.contains(card)) {
                    return false;
                }
            }
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
            if (!cards.contains(card)) {
                return false;
            }
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
            if (!cards.contains(card)) {
                return false;
            }
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
            if (!cards.contains(card)) {
                return false;
            }
        } else if (property.startsWith("lowestCMC")) {
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
            if (!card.isEnchanted()) {
                return false;
            }
        } else if (property.startsWith("unenchanted")) {
            if (card.isEnchanted()) {
                return false;
            }
        } else if (property.startsWith("enchanting")) {
            if (!card.isEnchanting()) {
                return false;
            }
        } else if (property.startsWith("equipped")) {
            if (!card.isEquipped()) {
                return false;
            }
        } else if (property.startsWith("unequipped")) {
            if (card.isEquipped()) {
                return false;
            }
        } else if (property.startsWith("equipping")) {
            if (!card.isEquipping()) {
                return false;
            }
        } else if (property.startsWith("notEquipping")) {
            if (card.isEquipping()) {
                return false;
            }
        } else if (property.startsWith("modified")) {
            if (!card.isModified()) {
                return false;
            }
        } else if (property.startsWith("token")) {
            if (!card.isToken() && !card.isTokenCard()) {
                return false;
            }
            // copied spell don't count
            if (property.contains("Created") && card.getCastSA() != null) {
                return false;
            }
        } else if (property.startsWith("nonToken")) {
            if (card.isToken() || card.isTokenCard()) {
                return false;
            }
        } else if (property.startsWith("copiedSpell")) {
            if (!card.isCopiedSpell()) {
                return false;
            }
        } else if (property.startsWith("nonCopiedSpell")) {
            if (card.isCopiedSpell()) {
                return false;
            }
        } else if (property.startsWith("hasXCost")) {
            ManaCost cost = card.getManaCost();
            if (cost == null || cost.countX() <= 0) {
                return false;
            }
        } else if (property.startsWith("suspended")) {
            if (!card.hasSuspend()) {
                return false;
            }
        } else if (property.startsWith("delved")) {
            if (!source.getDelved().contains(card)) {
                return false;
            }
        } else if (property.startsWith("convoked")) {
            if (!source.getConvoked().contains(card)) {
                return false;
            }
        } else if (property.startsWith("exploited")) {
            if (!source.getExploited().contains(card)) {
                return false;
            }
        } else if (property.startsWith("unequalPT")) {
            if (card.getNetPower() == card.getNetToughness()) {
                return false;
            }
        } else if (property.startsWith("equalPT")) {
            if (card.getNetPower() != card.getNetToughness()) {
                return false;
            }
        } else if (property.equals("powerGTtoughness")) {
            if (card.getNetPower() <= card.getNetToughness()) {
                return false;
            }
        } else if (property.equals("powerGTbasePower")) {
            if (card.getNetPower() <= card.getCurrentPower()) {
                return false;
            }
        } else if (property.equals("powerLTtoughness")) {
            if (card.getNetPower() >= card.getNetToughness()) {
                return false;
            }
        } else if (property.equals("cmcEven")) {
            if (card.getCMC() % 2 != 0) {
                return false;
            }
        } else if (property.equals("cmcOdd")) {
            if (card.getCMC() % 2 != 1) {
                return false;
            }
        } else if (property.equals("cmcChosenEvenOdd")) {
            if (!source.hasChosenEvenOdd()) {
                return false;
            }
            if ((card.getCMC() % 2 == 0) != (source.getChosenEvenOdd() == EvenOdd.Even)) {
                return false;
            }
        } else if (property.equals("cmcNotChosenEvenOdd")) {
            if (!source.hasChosenEvenOdd()) {
                return false;
            }
            if ((card.getCMC() % 2 == 0) == (source.getChosenEvenOdd() == EvenOdd.Even)) {
                return false;
            }
        } else if (property.equals("cmcChosen")) {
            if (!source.hasChosenNumber()) {
                return false;
            }
            if (card.getCMC() != source.getChosenNumber()) {
                return false;
            }
        } else if (property.startsWith("power") || property.startsWith("toughness") || property.startsWith("cmc")
                || property.startsWith("totalPT") || property.startsWith("numColors")
                || property.startsWith("basePower") || property.startsWith("baseToughness")) {
            int x;
            int y = 0;
            String rhs = "";

            if (property.startsWith("power")) {
                rhs = property.substring(7);
                y = card.getNetPower();
            } else if (property.startsWith("basePower")) {
                rhs = property.substring(11);
                y = card.getCurrentPower();
            } else if (property.startsWith("toughness")) {
                rhs = property.substring(11);
                y = card.getNetToughness();
            } else if (property.startsWith("baseToughness")) {
                rhs = property.substring(15);
                y = card.getCurrentToughness();
            } else if (property.startsWith("cmc")) {
                rhs = property.substring(5);
                y = card.getCMC();
            } else if (property.startsWith("totalPT")) {
                rhs = property.substring(10);
                y = card.getNetPower() + card.getNetToughness();
            } else if (property.startsWith("numColors")) {
                rhs = property.substring(11);
                y = card.getColor().countColors();
            }
            x = AbilityUtils.calculateAmount(source, rhs, spellAbility);

            if (!Expressions.compare(y, property, x)) {
                return false;
            }
        } else if (property.startsWith("ManaCost")) {
            if (!card.getManaCost().getShortString().equals(property.substring(8))) {
                return false;
            }
        } else if (property.equals("HasCounters")) {
            if (!card.hasCounters()) {
                return false;
            }
        } else if (property.equals("NoCounters")) {
            if (card.hasCounters()) {
                return false;
            }
        }
        else if (property.startsWith("counters")) {
            // syntax example: counters_GE9_P1P1 or counters_LT12_TIME
            final String[] splitProperty = property.split("_");
            final String strNum = splitProperty[1].substring(2);
            final String comparator = splitProperty[1].substring(0, 2);
            final String counterType = splitProperty[2];
            final int number = AbilityUtils.calculateAmount(source, strNum, spellAbility);

            final int actualnumber = card.getCounters(CounterType.getType(counterType));

            if (!Expressions.compare(actualnumber, comparator, number)) {
                return false;
            }
        }
        // These predicated refer to ongoing combat. If no combat happens, they'll return false (meaning not attacking/blocking ATM)
        else if (property.startsWith("attacking")) {
            if (null == combat) return false;
            // check this always first to make sure lki is only used when the card provides it
            if (!(property.contains("LKI") ? lki : card).isAttacking()) return false;
            if (property.equals("attacking")) return true;
            if (property.equals("attackingYou")) return combat.isAttacking(card, sourceController);
            if (property.equals("attackingSame")) {
                final GameEntity attacked = combat.getDefenderByAttacker(source);
                if (!combat.isAttacking(card, attacked)) {
                    return false;
                }
            }
            if (property.equals("attackingBattle")) {
                final GameEntity attacked = combat.getDefenderByAttacker(source);
                if (!(attacked instanceof Card)) {
                    return false;
                }
                if (!((Card) attacked).isBattle()) {
                    return false;
                }
            }
            if (property.startsWith("attackingYouOrYourPW")) {
                GameEntity defender = combat.getDefenderByAttacker(card);
                if (defender instanceof Card) {
                    // attack on a planeswalker that was removed from combat
                    if (!((Card) defender).isPlaneswalker()) {
                        return false;
                    }
                    defender = ((Card) defender).getController();
                }
                if (!sourceController.equals(defender)) {
                    return false;
                }
            }
            if (property.equals("attackingOpponent")) {
                Player defender = combat.getDefenderPlayerByAttacker(card);
                if (!sourceController.isOpponentOf(defender)) {
                    return false;
                }
            }
            if (property.startsWith("attacking ")) { // generic "attacking [DefinedGameEntity]"
                FCollection<GameEntity> defined = AbilityUtils.getDefinedEntities(source, property.split(" ")[1],
                        spellAbility);
                final GameEntity defender = combat.getDefenderByAttacker(card);
                if (!defined.contains(defender)) {
                    return false;
                }
            }
        } else if (property.startsWith("notattacking")) {
            return null == combat || !combat.isAttacking(card);
        } else if (property.startsWith("attackedThisCombat")) {
            if (null == combat || card.getDamageHistory().getCreatureAttackedThisCombat() == 0) {
                return false;
            }
            if (property.length() > 18) {
                int x = AbilityUtils.calculateAmount(source, property.substring(21), spellAbility);
                if (!Expressions.compare(card.getDamageHistory().getCreatureAttackedThisCombat(), property, x)) {
                    return false;
                }
            }
        } else if (property.equals("blockedThisCombat")) {
            if (null == combat || !card.getDamageHistory().getCreatureBlockedThisCombat()) {
                return false;
            }
        } else if (property.equals("attackedBySourceThisCombat")) {
            if (null == combat) return false;
            final GameEntity defender = combat.getDefenderByAttacker(source);
            if (defender instanceof Card && !card.equals(defender)) {
                return false;
            }
        } else if (property.startsWith("blocking")) {
            if (null == combat) return false;
            String what = property.substring("blocking".length());

            if (StringUtils.isEmpty(what)) return combat.isBlocking(card);
            if (what.startsWith("Source")) return combat.isBlocking(card, source);
            if (what.startsWith("CreatureYouCtrl")) {
                for (final Card c : sourceController.getCreaturesInPlay())
                    if (combat.isBlocking(card, c))
                        return true;
                return false;
            } else {
                for (Card c : AbilityUtils.getDefinedCards(source, what, spellAbility)) {
                    if (combat.isBlocking(card, c)) {
                        return true;
                    }
                }
                return false;
            }
        } else if (property.startsWith("sharesBlockingAssignmentWith")) {
            if (null == combat) {
                return false;
            }
            if (null == combat.getAttackersBlockedBy(source) || null == combat.getAttackersBlockedBy(card)) {
                return false;
            }

            CardCollection sourceBlocking = new CardCollection(combat.getAttackersBlockedBy(source));
            CardCollection thisBlocking = new CardCollection(combat.getAttackersBlockedBy(card));
            if (Collections.disjoint(sourceBlocking, thisBlocking)) {
                return false;
            }
        } else if (property.startsWith("notblocking")) {
            return null == combat || !combat.isBlocking(card);
        }
        // Nex predicates refer to past combat and don't need a reference to actual combat
        else if (property.equals("blocked")) {
            return null != combat && combat.isBlocked(card);
        } else if (property.startsWith("blockedBySourceThisTurn")) {
            return card.getBlockedByThisTurn().contains(source);
        } else if (property.startsWith("blockedBySourceLKI")) {
            return null != combat && combat.isBlocking(game.getChangeZoneLKIInfo(source), card);
        } else if (property.startsWith("blockedBySource")) {
            return null != combat && combat.isBlocking(source, card);
        } else if (property.startsWith("blockedThisTurn")) {
            return !card.getBlockedThisTurn().isEmpty();
        } else if (property.startsWith("blockedByThisTurn")) {
            return !card.getBlockedByThisTurn().isEmpty();
        } else if (property.startsWith("blockedValidThisTurn ")) {
            List<Card> blocked = card.getBlockedThisTurn();
            if (blocked.isEmpty()) {
                return false;
            }
            String valid = property.split(" ")[1];
            for (Card c : blocked) {
                if (c.isValid(valid, card.getController(), source, spellAbility)) {
                    return true;
                }
            }
            for (Card c : AbilityUtils.getDefinedCards(source, valid, spellAbility)) {
                if (blocked.contains(c)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("blockedByValidThisTurn ")) {
            List<Card> blocked = card.getBlockedByThisTurn();
            if (blocked.isEmpty()) {
                return false;
            }
            String valid = property.split(" ")[1];
            if (Iterables.any(blocked, CardPredicates.restriction(valid, card.getController(), source, spellAbility))) {
                return true;
            }
            for (Card c : AbilityUtils.getDefinedCards(source, valid, spellAbility)) {
                if (blocked.contains(c)) {
                    return true;
                }
            }
            return false;
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
            if (combat == null || !combat.isUnblocked(card)) {
                return false;
            }
        } else if (property.equals("attackersBandedWith")) {
            if (card.equals(source)) {
                // You don't band with yourself
                return false;
            }
            AttackingBand band = combat == null ? null : combat.getBandOfAttacker(source);
            if (band == null || !band.getAttackers().contains(card)) {
                return false;
            }
        } else if (property.equals("hadToAttackThisCombat")) {
            AttackRequirement e = combat == null ? null : combat.getAttackConstraints().getRequirements().get(card);
            if (e == null || !e.hasRequirement() || !e.getAttacker().equalsWithTimestamp(card)) {
                return false;
            }
        } else if (property.equals("couldAttackButNotAttacking")) {
            if (!game.getPhaseHandler().isPlayerTurn(controller)) return false;
            return CombatUtil.couldAttackButNotAttacking(combat, card);
        } else if (property.startsWith("kicked")) {
            // CR 607.2i check cost is linked
            if (AbilityUtils.isUnlinkedFromCastSA(spellAbility, card)) {
                return false;
            }
            if (property.equals("kicked")) {
                if (card.getKickerMagnitude() == 0) {
                    return false;
                }
            } else {
                String s = property.split("kicked ")[1];
                if ("1".equals(s) && !card.isOptionalCostPaid(OptionalCost.Kicker1)) return false;
                if ("2".equals(s) && !card.isOptionalCostPaid(OptionalCost.Kicker2)) return false;
            }
        } else if (property.startsWith("pseudokicked")) {
            if (property.equals("pseudokicked")) {
                if (!card.isOptionalCostPaid(OptionalCost.Generic)) return false;
            }
        } else if (property.equals("bargained")) {
            if (card.getCastSA() == null) {
                return false;
            }
            if (AbilityUtils.isUnlinkedFromCastSA(spellAbility, card)) {
                return false;
            }
            return card.getCastSA().isBargain();
        } else if (property.equals("surged")) {
            if (card.getCastSA() == null) {
                return false;
            }
            if (AbilityUtils.isUnlinkedFromCastSA(spellAbility, card)) {
                return false;
            }
            return card.getCastSA().isSurged();
        } else if (property.equals("blitzed")) {
            if (card.getCastSA() == null) {
                return false;
            }
            if (AbilityUtils.isUnlinkedFromCastSA(spellAbility, card)) {
                return false;
            }
            return card.getCastSA().isBlitz();
        } else if (property.equals("dashed")) {
            if (card.getCastSA() == null) {
                return false;
            }
            if (AbilityUtils.isUnlinkedFromCastSA(spellAbility, card)) {
                return false;
            }
            return card.getCastSA().isDash();
        } else if (property.equals("escaped")) {
            if (card.getCastSA() == null) {
                return false;
            }
            if (AbilityUtils.isUnlinkedFromCastSA(spellAbility, card)) {
                return false;
            }
            return card.getCastSA().isEscape();
        } else if (property.equals("evoked")) {
            if (card.getCastSA() == null) {
                return false;
            }
            if (AbilityUtils.isUnlinkedFromCastSA(spellAbility, card)) {
                return false;
            }
            return card.getCastSA().isEvoke();
        } else if (property.equals("prowled")) {
            if (card.getCastSA() == null) {
                return false;
            }
            if (AbilityUtils.isUnlinkedFromCastSA(spellAbility, card)) {
                return false;
            }
            return card.getCastSA().isProwl();
        } else if (property.equals("spectacle")) {
            if (card.getCastSA() == null) {
                return false;
            }
            if (AbilityUtils.isUnlinkedFromCastSA(spellAbility, card)) {
                return false;
            }
            return card.getCastSA().isSpectacle();
        } else if (property.equals("foretold")) {
            if (!card.isForetold()) {
                return false;
            }
        } else if (property.equals("HasDevoured")) {
            if (card.getDevouredCards().isEmpty()) {
                return false;
            }
        } else if (property.equals("HasNotDevoured")) {
            if (!card.getDevouredCards().isEmpty()) {
                return false;
            }
        } else if (property.equals("IsMonstrous")) {
            if (!card.isMonstrous()) {
                return false;
            }
        } else if (property.equals("IsNotMonstrous")) {
            if (card.isMonstrous()) {
                return false;
            }
        } else if (property.equals("IsUnearthed")) {
            if (!card.isUnearthed()) {
                return false;
            }
        } else if (property.equals("IsRenowned")) {
            if (!card.isRenowned()) {
                return false;
            }
        } else if (property.equals("IsNotRenowned")) {
            if (card.isRenowned()) {
                return false;
            }
        } else if (property.equals("IsRemembered")) {
            if (!source.isRemembered(card)) {
                return false;
            }
        } else if (property.equals("IsNotRemembered")) {
            if (source.isRemembered(card)) {
                return false;
            }
        } else if (property.equals("IsImprinted")) {
            if (!source.hasImprintedCard(card)) {
                return false;
            }
        } else if (property.equals("IsNotImprinted")) {
            if (source.hasImprintedCard(card)) {
                return false;
            }
        } else if (property.equals("IsGoaded")) {
            if (!card.isGoaded()) {
                return false;
            }
        } else if (property.equals("NoAbilities")) {
            if (!card.hasNoAbilities()) {
                return false;
            }
        } else if (property.equals("castKeyword")) {
            SpellAbility castSA = card.getCastSA();
            if (castSA == null) {
                return false;
            }
            // intrinsic keyword might be a new one when the zone changes
            if (castSA.isIntrinsic()) {
                // so just check if the static is intrinsic too
                if (!spellAbility.isIntrinsic()) {
                    return false;
                }
            } else {
                // otherwise check for keyword object
                return Objects.equals(castSA.getKeyword(), spellAbility.getKeyword());
            }
        } else if (property.startsWith("CastSa"))  {
            SpellAbility castSA = card.getCastSA();
            if (castSA == null) {
                return false;
            }
            String v = property.substring(7);
            if (!castSA.isValid(v, sourceController, source, spellAbility)) {
                return false;
            }
        } else if (property.startsWith("wasCastFrom")) {
            boolean byYou = property.contains("ByYou");
            String strZone = property.substring(11);
            Player zoneOwner = null;
            if (property.contains("Your")) {
                strZone = strZone.substring(4);
                zoneOwner = sourceController;
            }
            if (property.contains("Their")) {
                strZone = strZone.substring(5);
                zoneOwner = controller;
            }
            if (byYou) {
                strZone = strZone.substring(0, strZone.indexOf("ByYou", 0));
            }
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (card.getCastFrom() == null || (zoneOwner != null && !card.getCastFrom().getPlayer().equals(zoneOwner))
                    || (byYou && !sourceController.equals(card.getCastSA().getActivatingPlayer()))
                    || realZone != card.getCastFrom().getZoneType()) {
                return false;
            }
        } else if (property.startsWith("wasNotCastFrom")) {
            boolean byYou = property.contains("ByYou");
            String strZone = property.substring(14);
            Player zoneOwner = null;
            if (property.contains("Your")) {
                strZone = strZone.substring(4);
                zoneOwner = sourceController;
            }
            if (property.contains("Their")) {
                strZone = strZone.substring(5);
                zoneOwner = controller;
            }
            if (byYou) {
                strZone = strZone.substring(0, strZone.indexOf("ByYou", 0));
            }
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (card.getCastFrom() != null && (zoneOwner == null || card.getCastFrom().getPlayer().equals(zoneOwner))
                    && (!byYou || sourceController.equals(card.getCastSA().getActivatingPlayer()))
                    && realZone == card.getCastFrom().getZoneType()) {
                return false;
            }
        }  else if (property.startsWith("wasCast")) {
            if (!card.wasCast()) {
                return false;
            }
            if (property.contains("ByYou") && !sourceController.equals(card.getCastSA().getActivatingPlayer())) {
                return false;
            }
        } else if (property.equals("wasNotCast")) {
            if (card.wasCast()) {
                return false;
            }
        } else if (property.startsWith("set")) {
            final String setCode = property.substring(3, 6);
            if (card.getName().isEmpty()) {
                return false;
            }
            final PaperCard setCard = StaticData.instance().getCommonCards().getCardFromEditions(card.getName(),
                    CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
            if (setCard != null && !setCard.getEdition().equals(setCode)) {
                return false;
            }
        } else if (property.startsWith("inZone")) {
            final String strZone = property.substring(6);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            // lki last zone does fall back to this zone
            final Zone lkiZone = lki.getLastKnownZone();

            if (lkiZone == null || !lkiZone.is(realZone)) {
                return false;
            }
        } else if (property.startsWith("inRealZone")) {
            final String strZone = property.substring(10);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);

            if (!card.isInZone(realZone)) {
                return false;
            }
        } else if (property.equals("IsCommander")) {
            if (!card.isCommander()) {
                return false;
            }
        } else if (property.equals("IsNotCommander")) {
            if (card.isCommander()) {
                return false;
            }
        } else if (property.startsWith("NotedFor")) {
            final String key = property.substring("NotedFor".length());
            for (String note : sourceController.getNotesForName(key)) {
                if (note.equals("Name:" + card.getName())) {
                    return true;
                }
                if (note.equals("Id:" + card.getId())) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("Triggered")) {
            if (spellAbility instanceof SpellAbility) {
                final String key = property.substring(9);
                SpellAbility sa = (SpellAbility) spellAbility;
                Object o = sa.getRootAbility().getTriggeringObject(AbilityKey.fromString(key));
                boolean found = false;
                if (o != null) {
                    if (o instanceof CardCollection) {
                        found = ((CardCollection) o).contains(card);
                    } else {
                        found = card.equals(o);
                    }
                }
                if (!found) {
                    return false;
                }
            } else {
                return false;
            }
        } else if (property.startsWith("NotTriggered")) {
            final String key = property.substring("NotTriggered".length());
            if (spellAbility instanceof SpellAbility) {
                SpellAbility sa = (SpellAbility) spellAbility;
                if (card.equals(sa.getRootAbility().getTriggeringObject(AbilityKey.fromString(key)))) {
                    return false;
                }
            } else {
                return false;
            }
        } else if (property.startsWith("NotDefined")) {
            final String key = property.substring("NotDefined".length());
            if (Iterables.contains(AbilityUtils.getDefinedCards(source, key, spellAbility), card)) {
                return false;
            }
        } else if (property.equals("CanPayManaCost")) {
            if (!(spellAbility instanceof SpellAbility)) {
                return false;
            }
            final class CheckCanPayManaCost {
                private List<Mana> manaPaid;
                private List<ManaCostShard> manaCost;
                // check shards recursively
                boolean checkShard(int index) {
                    if (index >= manaCost.size()) {
                        return true;
                    }
                    ManaCostShard shard = manaCost.get(index);
                    // ignore X cost
                    if (shard == ManaCostShard.X) {
                        return checkShard(index + 1);
                    }
                    for (int i = 0; i < manaPaid.size(); i++) {
                        Mana mana = manaPaid.get(i);
                        if (shard.isColor(mana.getColor()) || (shard.isSnow() && mana.isSnow())) {
                            manaPaid.remove(i);
                            if (checkShard(index + 1)) {
                                return true;
                            }
                            manaPaid.add(i, mana);
                        }
                        if (shard.isGeneric() && !shard.isSnow()) {
                            // Handle 2 generic mana
                            if (shard.getCmc() == 2) {
                                manaCost.add(ManaCostShard.GENERIC);
                            }
                            manaPaid.remove(i);
                            if (checkShard(index + 1)) {
                                return true;
                            }
                            manaPaid.add(i, mana);
                            if (shard.getCmc() == 2) {
                                manaCost.remove(manaCost.size() - 1);
                            }
                        }
                    }
                    return false;
                }
                boolean check() {
                    manaPaid = Lists.newArrayList(((SpellAbility)spellAbility).getPayingMana());
                    manaCost = Lists.newArrayList(card.getManaCost());
                    Collections.sort(manaCost);
                    //It seems the above codes didn't add generic mana cost ?
                    //Add generic cost below to fix it.
                    int genericCost = card.getManaCost().getGenericCost();
                    while (genericCost-- > 0) {
                        manaCost.add(ManaCostShard.GENERIC);
                    }
                    return checkShard(0);
                }
            }
            return new CheckCanPayManaCost().check();
        } else {
            // StringType done in CardState
            if (!card.getCurrentState().hasProperty(property, sourceController, source, spellAbility)) {
                return false;
            }
        }
        return true;
    }

}

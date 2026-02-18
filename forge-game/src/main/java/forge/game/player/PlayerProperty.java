package forge.game.player;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import forge.util.TextUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerProperty {

    public static boolean playerHasProperty(Player player, String property, Player sourceController, Card source, CardTraitBase spellAbility) {
        Game game = player.getGame();
        if (property.equals("Activator")) {
            if (!player.equals(spellAbility.getHostCard().getController())) {
                return false;
            }
        } else if (property.equals("You")) {
            if (!player.equals(sourceController)) {
                return false;
            }
        } else if (property.equals("Opponent")) {
            if (player.equals(sourceController) || !player.isOpponentOf(sourceController)) {
                return false;
            }
        } else if (property.startsWith("OpponentOf ")) {
            final String v = property.split(" ")[1];
            final List<Player> players = AbilityUtils.getDefinedPlayers(source, v, spellAbility);
            for (final Player p : players) {
                if (player.equals(p) || !player.isOpponentOf(p)) {
                    return false;
                }
            }
        } else if (property.startsWith("PlayerUID_")) {
            if (player.getId() != Integer.parseInt(property.split("PlayerUID_")[1])) {
                return false;
            }
        } else if (property.equals("YourTeam")) {
            if (!player.sameTeam(sourceController)) {
                return false;
            }
        } else if (property.equals("Allies")) {
            if (player.equals(sourceController) || player.isOpponentOf(sourceController)) {
                return false;
            }
        } else if (property.equals("Active")) {
            if (!game.getPhaseHandler().isPlayerTurn(player)) {
                return false;
            }
        } else if (property.equals("NonActive")) {
            if (game.getPhaseHandler().isPlayerTurn(player)) {
                return false;
            }
        } else if (property.equals("OpponentToActive")) {
            final Player active = game.getPhaseHandler().getPlayerTurn();
            if (player.equals(active) || !player.isOpponentOf(active)) {
                return false;
            }
        } else if (property.equals("Other")) {
            if (player.equals(sourceController)) {
                return false;
            }
        } else if (property.equals("CardOwner")) {
            if (!player.equals(source.getOwner())) {
                return false;
            }
        } else if (property.equals("descended")) {
            if (player.getDescended() < 1) {
                return false;
            }
        } else if (property.equals("committedCrimeThisTurn")) {
            if (player.getCommittedCrimeThisTurn() < 1) return false;
        } else if (property.equals("isMonarch")) {
            if (!player.isMonarch()) {
                return false;
            }
        } else if (property.equals("hasInitiative")) {
            if (!player.hasInitiative()) {
                return false;
            }
        } else if (property.equals("hasBlessing")) {
            if (!player.hasBlessing()) {
                return false;
            }
        } else if (property.equals("CanBeEnchantedBy")) {
            if (!player.canBeAttached(source, null)) {
                return false;
            }
        } else if (property.startsWith("damageDoneSingleSource")) {
            String props = property.split(" ")[1];
            List<Integer> sourceDmg = game.getDamageDoneThisTurn(null, false, "Card.YouCtrl", null, source, sourceController, spellAbility);
            int maxDmg = sourceDmg.isEmpty() ? 0 : Collections.max(sourceDmg);
            if (!Expressions.compare(maxDmg, props.substring(0, 2), AbilityUtils.calculateAmount(source, props.substring(2), spellAbility))) {
                return false;
            }
        } else if (property.startsWith("wasDealtCombatDamageThisCombatBy ")) {
            String v = property.split(" ")[1];
            boolean found = false;

            final List<Card> cards = AbilityUtils.getDefinedCards(source, v, spellAbility);
            for (final Card card : cards) {
                if (card.getDamageHistory().getThisCombatDamaged().contains(player)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        } else if (property.startsWith("wasDealtDamageThisGameBy ")) {
            String v = property.split(" ")[1];
            boolean found = false;

            final List<Card> cards = AbilityUtils.getDefinedCards(source, v, spellAbility);
            for (final Card card : cards) {
                if (card.getDamageHistory().getThisGameDamaged().contains(player)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        } else if (property.startsWith("wasDealt")) {
            Boolean combat = null;
            if (property.contains("CombatDamage")) {
                combat = true;
            }
            String validCard = null;
            String comp = "GE";
            int right = 1;

            if (property.contains("ThisTurnBy")) {
                int idx = 2;
                String[] props = property.split(" ");
                if (property.contains("BySource")) {
                    idx--;
                } else {
                    validCard = props[1];
                }
                if (props.length > idx) {
                    comp = props[idx].substring(0, 2);
                    right = AbilityUtils.calculateAmount(source, props[idx].substring(2), spellAbility);
                }
            }
            int result;
            if (property.contains("BySource")) {
                result = source.getDamageHistory().getDamageDoneThisTurn(combat, false, property.contains("SourceTimes"), null, "You", source, player, spellAbility);
            } else {
                result = game.getDamageDoneThisTurn(combat, validCard == null, validCard, "You", source, player, spellAbility).size();
            }
            if (!Expressions.compare(result, comp, right)) {
                return false;
            }
        } else if (property.equals("attackedBySourceThisCombat")) {
            if (game.getCombat() == null || !player.equals(game.getCombat().getDefenderPlayerByAttacker(source))) {
                return false;
            }
        } else if (property.equals("attackedBySourceThisTurn")) {
            if (!source.getDamageHistory().hasAttackedThisTurn(player)) {
                return false;
            }
        } else if (property.equals("Attacking")) {
            if (game.getCombat() == null || !player.equals(game.getCombat().getAttackingPlayer())) {
                return false;
            }
        } else if (property.equals("Defending")) {
            if (game.getCombat() == null || !game.getCombat().getAttackersAndDefenders().values().contains(player)) {
                return false;
            }
        } else if (property.startsWith("LostLifeThisTurn")) {
            String comparator = "GE";
            int value = 1;

            if (!property.equals("LostLifeThisTurn")) {
                // Parse value from "LostLifeThisTurn GE3"
                String compareAndValue = property.split(" ")[1];
                comparator = compareAndValue.substring(0, 2); // This should typically be GE
                final String rightString = compareAndValue.substring(2);
                value = AbilityUtils.calculateAmount(source, rightString, spellAbility);
            }
            if (!Expressions.compare(player.getLifeLostThisTurn(), comparator, value)) {
                return false;
            }
        } else if (property.equals("TappedLandForManaThisTurn")) {
            if (!player.hasTappedLandForManaThisTurn()) {
                return false;
            }
        } else if (property.equals("CardsInHandAtBeginningOfTurn")) {
            if (player.getNumCardsInHandStartedThisTurnWith() <= 0) {
                return false;
            }
        } else if (property.equals("IsRemembered")) {
            if (!source.isRemembered(player)) {
                return false;
            }
        } else if (property.equals("IsRememberedOrController")) {
            boolean found = false;
            for (Object o : source.getRemembered()) {
                if (o instanceof Player) {
                    final Player p = (Player) o;
                    if (p.equals(player)) {
                        found = true;
                        break;
                    }
                } else if (o instanceof Card) {
                    final Card c = (Card) o;
                    if (c.getController().equals(player)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                return false;
            }
        } else if (property.equals("IsNotRemembered")) {
            if (source.isRemembered(player)) {
                return false;
            }
        } else if (property.equals("IsTriggerRemembered")) {
            boolean found = false;
            for (Object o : spellAbility.getTriggerRemembered()) {
                if (o instanceof Player) {
                    Player trigRem = (Player) o;
                    if (trigRem.equals(player)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                return false;
            }
        } else if (property.equals("EnchantedBy")) {
            if (!player.isEnchantedBy(source)) {
                return false;
            }
        } else if (property.equals("EnchantedController")) {
            Card enchanting = source.getEnchantingCard();
            if (enchanting == null || !player.equals(enchanting.getController())) {
                return false;
            }
        } else if (property.equals("Chosen")) {
            if (source.getChosenPlayer() == null || !source.getChosenPlayer().equals(player)) {
                return false;
            }
        } else if (property.equals("NotedDefender")) {
            String tracker = player.getDraftNotes().getOrDefault("Cogwork Tracker", "");

            return Arrays.asList(tracker.split(",")).contains(String.valueOf(player));
        } else if (property.startsWith("life")) {
            int life = player.getLife();
            int amount = AbilityUtils.calculateAmount(source, property.substring(6), spellAbility);

            if (!Expressions.compare(life, property, amount)) {
                return false;
            }
        } else if (property.equals("IsPoisoned")) {
            if (player.getPoisonCounters() <= 0) {
                return false;
            }
        } else if (property.equals("IsCorrupted")) {
            if (player.getPoisonCounters() <= 2) {
                return false;
            }
        } else if (property.equals("NoSpeed")) {
            if (!player.noSpeed()) {
                return false;
            }
        } else if (property.equals("MaxSpeed")) {
            if (!player.maxSpeed()) {
                return false;
            }
        } else if (property.equals("targetedBy")) {
            if (!(spellAbility instanceof SpellAbility)) {
                return false;
            }
            SpellAbility sp = (SpellAbility)spellAbility;
            if (!sp.getRootAbility().isTargeting(player)) {
                return false;
            }
        } else if (property.startsWith("controls")) {
            // this allows escaping _ with \ in case of complex restrictions (used on Turf War)
            List<String> type = new ArrayList<>();
            Pattern regex = Pattern.compile("(?:\\\\.|[^_\\\\]++)+");
            Matcher regexMatcher = regex.matcher(property.substring(8));
            while (regexMatcher.find()) {
                type.add(regexMatcher.group());
            }
            final CardCollectionView list = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), type.get(0).replace("\\_", "_"), sourceController, source, spellAbility);
            String comparator = type.size() > 1 ? type.get(1) : "GE";
            int y = type.size() > 1 ? AbilityUtils.calculateAmount(source, comparator.substring(2), spellAbility) : 1;
            if (!Expressions.compare(list.size(), comparator, y)) {
                return false;
            }
        } else if (property.startsWith("HasCardsIn")) { // HasCardsIn[zonetype]_[cardtype]_[comparator]
            final String[] type = property.substring(10).split("_");
            final CardCollectionView list = CardLists.getValidCards(player.getCardsIn(ZoneType.smartValueOf(type[0])), type[1], sourceController, source, spellAbility);
            String comparator = type[2];
            int y = AbilityUtils.calculateAmount(source, comparator.substring(2), spellAbility);
            if (!Expressions.compare(list.size(), comparator, y)) {
                return false;
            }
        } else if (property.startsWith("withMore")) {
            final String cardType = property.split("sThan")[0].substring(8);
            final Player controller = "Active".equals(property.split("sThan")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            final CardCollectionView oppList = CardLists.getType(player.getCardsIn(ZoneType.Battlefield), cardType);
            final CardCollectionView yourList = CardLists.getType(controller.getCardsIn(ZoneType.Battlefield), cardType);
            if (oppList.size() <= yourList.size()) {
                return false;
            }
        } else if (property.startsWith("withAtLeast")) {
            final String cardType = property.split("More")[1].split("sThan")[0];
            final int amount = Integer.parseInt(property.substring(11, 12));
            final Player controller = "Active".equals(property.split("sThan")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            final CardCollectionView oppList = CardLists.getType(player.getCardsIn(ZoneType.Battlefield), cardType);
            final CardCollectionView yourList = CardLists.getType(controller.getCardsIn(ZoneType.Battlefield), cardType);
            if (oppList.size() < yourList.size() + amount) {
                return false;
            }
        } else if (property.startsWith("hasMore")) {
            final Player controller = property.contains("Than") && "Active".equals(property.split("Than")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            if (property.substring(7).startsWith("Life") && player.getLife() <= controller.getLife()) {
                return false;
            } else if (property.substring(7).startsWith("CardsInHand")
                    && player.getCardsIn(ZoneType.Hand).size() <= controller.getCardsIn(ZoneType.Hand).size()) {
                return false;
            }
        } else if (property.startsWith("hasFewer")) {
            final String cardType = property.split("sIn")[0].substring(8);
            final Player controller = "Active".equals(property.split("Than")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            final ZoneType zt = property.substring(8).startsWith("CreaturesInYard") ? ZoneType.Graveyard : ZoneType.Battlefield;
            final CardCollectionView oppList = CardLists.getType(player.getCardsIn(zt), cardType);
            final CardCollectionView yourList = CardLists.getType(controller.getCardsIn(zt), cardType);
            if (oppList.size() >= yourList.size()) {
                return false;
            }
        } else if (property.startsWith("withMost")) {
            final String kind = property.substring(8);
            if (kind.equals("Life")) {
                int highestLife = player.getLife(); // Negative base just in case a few Lich's are running around
                for (final Player p : game.getPlayers()) {
                    if (p.getLife() > highestLife) {
                        highestLife = p.getLife();
                    }
                }
                if (player.getLife() != highestLife) {
                    return false;
                }
            }
            else if (kind.equals("PermanentInPlay")) {
                int typeNum = 0;
                List<Player> controlmost = new ArrayList<>();
                for (final Player p : game.getPlayers()) {
                    final int num = p.getCardsIn(ZoneType.Battlefield).size();
                    if (num > typeNum) {
                        typeNum = num;
                        controlmost.clear();
                    }
                    if (num == typeNum) {
                        controlmost.add(p);
                    }
                }

                if (controlmost.size() != 1 || !controlmost.contains(player)) {
                    return false;
                }
            }
            else if (kind.equals("CardsInHand")) {
                int largestHand = 0;
                Player withLargestHand = null;
                for (final Player p : game.getPlayers()) {
                    if (p.getCardsIn(ZoneType.Hand).size() > largestHand) {
                        largestHand = p.getCardsIn(ZoneType.Hand).size();
                        withLargestHand = p;
                    }
                }
                if (!player.equals(withLargestHand)) {
                    return false;
                }
            }
            else if (kind.startsWith("Type")) {
                String type = property.split("Type")[1];
                boolean checkOnly = false;
                if (type.endsWith("Only")) {
                    checkOnly = true;
                    type = TextUtil.fastReplace(type, "Only", "");
                }
                int typeNum = 0;
                List<Player> controlmost = new ArrayList<>();
                for (final Player p : game.getPlayers()) {
                    final int num = CardLists.getType(p.getCardsIn(ZoneType.Battlefield), type).size();
                    if (num > typeNum) {
                        typeNum = num;
                        controlmost.clear();
                    }
                    if (num == typeNum) {
                        controlmost.add(p);
                    }
                }
                if (checkOnly && controlmost.size() != 1) {
                    return false;
                }
                if (!controlmost.contains(player)) {
                    return false;
                }
            }
        } else if (property.startsWith("withLowest")) {
            if (property.substring(10).equals("Life")) {
                int lowestLife = player.getLife();
                List<Player> lowestlifep = new ArrayList<>();
                for (final Player p : game.getPlayers()) {
                    if (p.getLife() == lowestLife) {
                        lowestlifep.add(p);
                    } else if (p.getLife() < lowestLife) {
                        lowestLife = p.getLife();
                        lowestlifep.clear();
                        lowestlifep.add(p);
                    }
                }
                if (!lowestlifep.contains(player)) {
                    return false;
                }
            }
        } else if (property.startsWith("Triggered") || property.equals("OriginalHostRemembered")) {
            if (!AbilityUtils.getDefinedPlayers(source, property, spellAbility).contains(player)) {
                return false;
            }
        } else if (property.equals("castSpellThisTurn")) {
            if (player.getSpellsCastThisTurn() == 0) {
                return false;
            }
        } else if (property.equals("attackedWithCreaturesThisTurn")) {
            if (player.getCreaturesAttackedThisTurn().isEmpty()) {
                return false;
            }
        } else if (property.startsWith("wasAttackedThisTurnBy")) {
            String restriction = property.split(" ")[1];
            for (Card c : sourceController.getCreaturesAttackedThisTurn(player)) {
                if (c.isValid(restriction, sourceController, source, spellAbility)) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("attackedYouTheirCurrentTurn")) {
            if (!Iterables.contains(player.getAttackedPlayersMyTurn(), sourceController)) {
                return false;
            }
        } else if (property.startsWith("attackedYouCtrlTheirCurrentTurn")) {
            CardCollectionView cardsYouCtrl = CardLists.filter(sourceController.getCardsIn(ZoneType.Battlefield),
                    CardPredicates.isType(property.split("_")[1]));
            for (Card card : cardsYouCtrl) {
                if (!player.getCreaturesAttackedThisTurn(card).isEmpty()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("attackedYouTheirLastTurn")) {
            if (!player.getAttackedPlayersMyLastTurn().contains(sourceController)) {
                return false;
            }
        } else if (property.equals("BeenAttackedThisCombat")) {
            for (Player p : game.getRegisteredPlayers()) {
                if (p.getAttackedPlayersMyCombat().contains(player)) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("VenturedThisTurn")) {
            if (player.getVenturedThisTurn() < 1) {
                return false;
            }
        } else if (property.startsWith("Condition")) {
            if (AbilityUtils.playerXCount(Lists.newArrayList(player), property, source, spellAbility) == 0) {
                return false;
            }
        } else if (property.startsWith("NotedFor")) {
            final String key = property.substring("NotedFor".length());
            for (String note : player.getNotesForName(key)) {
                if (note.equals("Name:" + source.getName())) {
                    return true;
                }
                if (note.equals("Id:" + source.getId())) {
                    return true;
                }
            }
            return false;
        } else {
            // could print error msg for unknown property here, though it'd need to check that it's not "Any" case
            return false;
        }
        return true;
    }

}

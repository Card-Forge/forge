package forge.game.player;

import java.util.ArrayList;
import java.util.List;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import forge.util.TextUtil;

public class PlayerProperty {

    public static boolean playerHasProperty(Player player, String property, Player sourceController, Card source, CardTraitBase spellAbility) {

        Game game = player.getGame();
        if (property.equals("You")) {
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
            if (!player.equals(game.getPhaseHandler().getPlayerTurn())) {
                return false;
            }
        } else if (property.equals("NonActive")) {
            if (player.equals(game.getPhaseHandler().getPlayerTurn())) {
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
        } else if (property.equals("OtherThanSourceOwner")) {
            if (player.equals(source.getOwner())) {
                return false;
            }
        } else if (property.equals("CardOwner")) {
            if (!player.equals(source.getOwner())) {
                return false;
            }
        } else if (property.equals("isMonarch")) {
            if (!player.isMonarch()) {
                return false;
            }
        } else if (property.equals("isNotMonarch")) {
            if (player.isMonarch()) {
                return false;
            }
        } else if (property.equals("hasBlessing")) {
            if (!player.hasBlessing()) {
                return false;
            }
        } else if (property.startsWith("wasDealtCombatDamageThisCombatBy ")) {
            String v = property.split(" ")[1];

            int count = 1;
            if (v.contains("_AtLeast")) {
                count = Integer.parseInt(v.substring(v.indexOf("_AtLeast") + 8));
                v = v.substring(0, v.indexOf("_AtLeast")).replace("Valid:", "Valid ");
            }

            final List<Card> cards = AbilityUtils.getDefinedCards(source, v, spellAbility);
            int found = 0;
            for (final Card card : cards) {
                if (card.getDamageHistory().getThisCombatDamaged().containsKey(player)) {
                    found++;
                }
            }
            if (found < count) {
                return false;
            }
        } else if (property.startsWith("wasDealtDamageThisGameBy ")) {
            String v = property.split(" ")[1];

            int count = 1;
            if (v.contains("_AtLeast")) {
                count = Integer.parseInt(v.substring(v.indexOf("_AtLeast") + 8));
                v = TextUtil.fastReplace(v.substring(0, v.indexOf("_AtLeast")), "Valid:", "Valid ");
            }

            final List<Card> cards = AbilityUtils.getDefinedCards(source, v, spellAbility);
            int found = 0;
            for (final Card card : cards) {
                if (card.getDamageHistory().getThisGameDamaged().containsKey(player)) {
                    found++;
                }
            }
            if (found < count) {
                return false;
            }
        } else if (property.startsWith("wasDealtDamageThisTurnBy ")) {
            String v = property.split(" ")[1];
            int count = 1;

            if (v.contains("_AtLeast")) {
                count = Integer.parseInt(v.substring(v.indexOf("_AtLeast") + 8));
                v = TextUtil.fastReplace(v.substring(0, v.indexOf("_AtLeast")), "Valid:", "Valid ");
            }

            final List<Card> cards = AbilityUtils.getDefinedCards(source, v, spellAbility);
            int found = 0;
            for (final Card card : cards) {
                if (card.getDamageHistory().getThisTurnDamaged().containsKey(player)) {
                    found++;
                }
            }
            if (found < count) {
                return false;
            }
        } else if (property.startsWith("wasDealtCombatDamageThisTurnBy ")) {
            String v = property.split(" ")[1];

            int count = 1;
            if (v.contains("_AtLeast")) {
                count = Integer.parseInt(v.substring(v.indexOf("_AtLeast") + 8));
                v = TextUtil.fastReplace(v.substring(0, v.indexOf("_AtLeast")), "Valid:", "Valid ");
            }

            final List<Card> cards = AbilityUtils.getDefinedCards(source, v, spellAbility);

            int found = 0;
            for (final Card card : cards) {
                if (card.getDamageHistory().getThisTurnCombatDamaged().containsKey(player)) {
                    found++;
                }
            }
            if (found < count) {
                return false;
            }
        } else if (property.equals("attackedBySourceThisCombat")) {
            if (game.getCombat() == null || !player.equals(game.getCombat().getDefenderPlayerByAttacker(source))) {
                return false;
            }
        } else if (property.equals("wasDealtDamageThisTurn")) {
            if (player.getAssignedDamage() == 0) {
                return false;
            }
        }  else if (property.equals("Defending")) {
            if (!player.getGame().getCombat().getAttackersAndDefenders().values().contains(player)) {
                return false;
            }
        } else if (property.equals("wasDealtCombatDamageThisTurn")) {
            if (player.getAssignedCombatDamage() == 0) {
                return false;
            }
        } else if (property.equals("LostLifeThisTurn")) {
            if (player.getLifeLostThisTurn() <= 0) {
                return false;
            }
        } else if (property.equals("DeclaredAttackerThisTurn")) {
            if (player.getAttackersDeclaredThisTurn() <= 0) {
                return false;
            }
        } else if (property.equals("TappedLandForManaThisTurn")) {
            if (!player.hasTappedLandForManaThisTurn()) {
                return false;
            }
        } else if (property.equals("NoCardsInHandAtBeginningOfTurn")) {
            if (player.getNumCardsInHandStartedThisTurnWith() > 0) {
                return false;
            }
        } else if (property.equals("CardsInHandAtBeginningOfTurn")) {
            if (player.getNumCardsInHandStartedThisTurnWith() <= 0) {
                return false;
            }
        } else if (property.startsWith("WithCardsInHand")) {
            if (property.contains("AtLeast")) {
                int amount = Integer.parseInt(property.split("AtLeast")[1]);
                if (player.getCardsIn(ZoneType.Hand).size() < amount) {
                    return false;
                }
            }
        } else if (property.equals("IsRemembered")) {
            if (!source.isRemembered(player)) {
                return false;
            }
        } else if (property.equals("IsNotRemembered")) {
            if (source.isRemembered(player)) {
                return false;
            }
        } else if (property.equals("EnchantedBy")) {
            if (!player.isEnchantedBy(source)) {
                return false;
            }
        } else if (property.equals("EnchantedController")) {
            Card enchanting = source.getEnchantingCard();
            if (enchanting != null && !player.equals(enchanting.getController())) {
                return false;
            }
        } else if (property.equals("Chosen")) {
            if (source.getChosenPlayer() == null || !source.getChosenPlayer().equals(player)) {
                return false;
            }
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
        } else if (property.startsWith("controls")) {
            final String[] type = property.substring(8).split("_");
            final CardCollectionView list = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), type[0], sourceController, source, spellAbility);
            String comparator = type[1];
            int y = AbilityUtils.calculateAmount(source, comparator.substring(2), spellAbility);
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
            final CardCollectionView oppList = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            final CardCollectionView yourList = CardLists.filter(controller.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            if (oppList.size() <= yourList.size()) {
                return false;
            }
        } else if (property.startsWith("withAtLeast")) {
            final String cardType = property.split("More")[1].split("sThan")[0];
            final int amount = Integer.parseInt(property.substring(11, 12));
            final Player controller = "Active".equals(property.split("sThan")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            final CardCollectionView oppList = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            final CardCollectionView yourList = CardLists.filter(controller.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
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
            final CardCollectionView oppList = CardLists.filter(player.getCardsIn(zt), CardPredicates.isType(cardType));
            final CardCollectionView yourList = CardLists.filter(controller.getCardsIn(zt), CardPredicates.isType(cardType));
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
        } else if (property.startsWith("LessThanHalfStartingLifeTotal")) {
            if (player.getLife() >= (int) Math.ceil(player.getStartingLife() / 2.0)) {
                return false;
            }
        } else if (property.startsWith("Triggered")) {
            if (!AbilityUtils.getDefinedPlayers(source, property, spellAbility).contains(player)) {
                return false;
            }
        } else if (property.equals("castSpellThisTurn")) {
            if (player.getSpellsCastThisTurn() > 0) {
                return false;
            }
        } else if (property.equals("attackedWithCreaturesThisTurn")) {
            if (player.getCreaturesAttackedThisTurn().isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
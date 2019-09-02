package forge.game.player;

import java.util.ArrayList;
import java.util.List;

import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

public class PlayerProperty {

    public static boolean playerHasProperty(Player player, String property, Player sourceController, Card source, SpellAbility spellAbility) {

        Game game = player.getGame();
        if (property.equals("You")) {
            return player.equals(sourceController);
        } else if (property.equals("Opponent")) {
            return !player.equals(sourceController) && player.isOpponentOf(sourceController);
        } else if (property.startsWith("OpponentOf ")) {
            final String v = property.split(" ")[1];
            final List<Player> players = AbilityUtils.getDefinedPlayers(source, v, spellAbility);
            for (final Player p : players) {
                if (player.equals(p) || !player.isOpponentOf(p)) {
                    return false;
                }
            }
        } else if (property.equals("YourTeam")) {
            return player.sameTeam(sourceController);
        } else if (property.equals("Allies")) {
            return !player.equals(sourceController) && !player.isOpponentOf(sourceController);
        } else if (property.equals("Active")) {
            return player.equals(game.getPhaseHandler().getPlayerTurn());
        } else if (property.equals("NonActive")) {
            return !player.equals(game.getPhaseHandler().getPlayerTurn());
        } else if (property.equals("OpponentToActive")) {
            final Player active = game.getPhaseHandler().getPlayerTurn();
            return !player.equals(active) && player.isOpponentOf(active);
        } else if (property.equals("Other")) {
            return !player.equals(sourceController);
        } else if (property.equals("OtherThanSourceOwner")) {
            return !player.equals(source.getOwner());
        } else if (property.equals("isMonarch")) {
            return player.equals(game.getMonarch());
        } else if (property.equals("hasBlessing")) {
            return player.hasBlessing();
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
                if (card.getDamageHistory().getThisCombatDamaged().contains(player)) {
                    found++;
                }
            }
            return found >= count;
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
                if (card.getDamageHistory().getThisGameDamaged().contains(player)) {
                    found++;
                }
            }
            return found >= count;
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
                if (card.getDamageHistory().getThisTurnDamaged().contains(player)) {
                    found++;
                }
            }
            return found >= count;
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
                if (card.getDamageHistory().getThisTurnCombatDamaged().contains(player)) {
                    found++;
                }
            }
            return found >= count;
        } else if (property.equals("attackedBySourceThisCombat")) {
            return game.getCombat() != null && player.equals(game.getCombat().getDefenderPlayerByAttacker(source));
        } else if (property.equals("wasDealtDamageThisTurn")) {
            return player.getAssignedDamage() != 0;
        } else if (property.equals("wasDealtCombatDamageThisTurn")) {
            return player.getAssignedCombatDamage() != 0;
        } else if (property.equals("LostLifeThisTurn")) {
            return player.getLifeLostThisTurn() > 0;
        } else if (property.equals("DeclaredAttackerThisTurn")) {
            return player.getAttackersDeclaredThisTurn() > 0;
        } else if (property.equals("TappedLandForManaThisTurn")) {
            return player.hasTappedLandForManaThisTurn();
        } else if (property.equals("NoCardsInHandAtBeginningOfTurn")) {
            return player.getNumCardsInHandStartedThisTurnWith() <= 0;
        } else if (property.equals("CardsInHandAtBeginningOfTurn")) {
            return player.getNumCardsInHandStartedThisTurnWith() > 0;
        } else if (property.startsWith("WithCardsInHand")) {
            if (property.contains("AtLeast")) {
                int amount = Integer.parseInt(property.split("AtLeast")[1]);
                return player.getCardsIn(ZoneType.Hand).size() >= amount;
            }
        } else if (property.equals("IsRemembered")) {
            return source.isRemembered(player);
        } else if (property.equals("IsNotRemembered")) {
            return !source.isRemembered(player);
        } else if (property.equals("EnchantedBy")) {
            return player.isEnchantedBy(source);
        } else if (property.equals("Chosen")) {
            return source.getChosenPlayer() != null && source.getChosenPlayer().equals(player);
        } else if (property.startsWith("LifeEquals_")) {
            int life = AbilityUtils.calculateAmount(source, property.substring(11), null);
            return player.getLife() == life;
        } else if (property.equals("IsPoisoned")) {
            return player.getPoisonCounters() > 0;
        } else if (property.startsWith("controls")) {
            final String[] type = property.substring(8).split("_");
            final CardCollectionView list = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), type[0], sourceController, source);
            String comparator = type[1];
            String compareTo = comparator.substring(2);
            int y = StringUtils.isNumeric(compareTo) ? Integer.parseInt(compareTo) : 0;
            return Expressions.compare(list.size(), comparator, y);
        } else if (property.startsWith("withMore")) {
            final String cardType = property.split("sThan")[0].substring(8);
            final Player controller = "Active".equals(property.split("sThan")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            final CardCollectionView oppList = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            final CardCollectionView yourList = CardLists.filter(controller.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            return oppList.size() > yourList.size();
        } else if (property.startsWith("withAtLeast")) {
            final String cardType = property.split("More")[1].split("sThan")[0];
            final int amount = Integer.parseInt(property.substring(11, 12));
            final Player controller = "Active".equals(property.split("sThan")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            final CardCollectionView oppList = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            final CardCollectionView yourList = CardLists.filter(controller.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            System.out.println(yourList.size());
            return oppList.size() >= yourList.size() + amount;
        } else if (property.startsWith("hasMore")) {
            final Player controller = property.contains("Than") && "Active".equals(property.split("Than")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            if (property.substring(7).startsWith("Life") && player.getLife() <= controller.getLife()) {
                return false;
            } else return !property.substring(7).startsWith("CardsInHand")
                    || player.getCardsIn(ZoneType.Hand).size() > controller.getCardsIn(ZoneType.Hand).size();
        } else if (property.startsWith("hasFewer")) {
            final Player controller = "Active".equals(property.split("Than")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            final ZoneType zt = property.substring(8).startsWith("CreaturesInYard") ? ZoneType.Graveyard : ZoneType.Battlefield;
            final CardCollectionView oppList = CardLists.filter(player.getCardsIn(zt), Presets.CREATURES);
            final CardCollectionView yourList = CardLists.filter(controller.getCardsIn(zt), Presets.CREATURES);
            return oppList.size() < yourList.size();
        } else if (property.startsWith("withMost")) {
            final String kind = property.substring(8);
            if (kind.equals("Life")) {
                int highestLife = player.getLife(); // Negative base just in case a few Lich's are running around
                for (final Player p : game.getPlayers()) {
                    if (p.getLife() > highestLife) {
                        highestLife = p.getLife();
                    }
                }
                return player.getLife() == highestLife;
            }
            else if (kind.equals("PermanentInPlay")) {
                int typeNum = 0;
                List<Player> controlmost = new ArrayList<Player>();
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

                return controlmost.size() == 1 && controlmost.contains(player);
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
                return player.equals(withLargestHand);
            }
            else if (kind.startsWith("Type")) {
                String type = property.split("Type")[1];
                boolean checkOnly = false;
                if (type.endsWith("Only")) {
                    checkOnly = true;
                    type = TextUtil.fastReplace(type, "Only", "");
                }
                int typeNum = 0;
                List<Player> controlmost = new ArrayList<Player>();
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
                return controlmost.contains(player);
            }
        } else if (property.startsWith("withLowest")) {
            if (property.substring(10).equals("Life")) {
                int lowestLife = player.getLife();
                List<Player> lowestlifep = new ArrayList<Player>();
                for (final Player p : game.getPlayers()) {
                    if (p.getLife() == lowestLife) {
                        lowestlifep.add(p);
                    } else if (p.getLife() < lowestLife) {
                        lowestLife = p.getLife();
                        lowestlifep.clear();
                        lowestlifep.add(p);
                    }
                }
                return lowestlifep.contains(player);
            }
        } else if (property.startsWith("LessThanHalfStartingLifeTotal")) {
            return player.getLife() < (int) Math.ceil(player.getStartingLife() / 2.0);
        } else if (property.startsWith("Triggered")) {
            return AbilityUtils.getDefinedPlayers(source, property, spellAbility).contains(player);
        }
        return true;
    }

}

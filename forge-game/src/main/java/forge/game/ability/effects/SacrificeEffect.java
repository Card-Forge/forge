package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class SacrificeEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final Card card = sa.getHostCard();
        if (sa.hasParam("Echo")) {
            boolean isPaid;
            if (activator.hasKeyword("You may pay 0 rather than pay the echo cost for permanents you control.")
                    && activator.getController().confirmAction(sa, null, "Do you want to pay Echo {0}?")) {
                isPaid = true;
            } else {
                isPaid = activator.getController().payManaOptional(card, new Cost(sa.getParam("Echo"), true),
                    sa, "Pay Echo", ManaPaymentPurpose.Echo);
            }
            final Map<String, Object> runParams = Maps.newHashMap();
            runParams.put("EchoPaid", Boolean.valueOf(isPaid));
            runParams.put("Card", card);
            game.getTriggerHandler().runTriggerOld(TriggerType.PayEcho, runParams, false);
            if (isPaid || !card.getController().equals(activator)) {
                return;
            }
        } else if (sa.hasParam("CumulativeUpkeep")) {
            GameEntityCounterTable table = new GameEntityCounterTable();
            card.addCounter(CounterType.AGE, 1, activator, true, table);

            table.triggerCountersPutAll(game);

            Cost cumCost = new Cost(sa.getParam("CumulativeUpkeep"), true);
            Cost payCost = new Cost(ManaCost.ZERO, true);
            int n = card.getCounters(CounterType.AGE);
            
            // multiply cost
            for (int i = 0; i < n; ++i) {
                payCost.add(cumCost);
            }
            
            sa.setCumulativeupkeep(true);
            game.updateLastStateForCard(card);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Cumulative upkeep for ").append(card);
            
            boolean isPaid = activator.getController().payManaOptional(card, payCost, sa, sb.toString(), ManaPaymentPurpose.CumulativeUpkeep);
            final Map<String, Object> runParams = Maps.newHashMap();
            runParams.put("CumulativeUpkeepPaid", Boolean.valueOf(isPaid));
            runParams.put("Card", card);
            runParams.put("PayingMana", StringUtils.join(sa.getPayingMana(), ""));
            game.getTriggerHandler().runTriggerOld(TriggerType.PayCumulativeUpkeep, runParams, false);
            if (isPaid || !card.getController().equals(activator)) {
                return;
            }
        }

        // Expand Sacrifice keyword here depending on what we need out of it.
        final String num = sa.hasParam("Amount") ? sa.getParam("Amount") : "1";
        final int amount = AbilityUtils.calculateAmount(card, num, sa);
        final List<Player> tgts = getTargetPlayers(sa);
        final boolean devour = sa.hasParam("Devour");
        final boolean exploit = sa.hasParam("Exploit");

        String valid = sa.getParam("SacValid");
        if (valid == null) {
            valid = "Self";
        }

        String msg = sa.getParam("SacMessage");
        if (msg == null) {
            msg = valid;
        }

        final boolean destroy = sa.hasParam("Destroy");
        final boolean remSacrificed = sa.hasParam("RememberSacrificed");
        final String remSVar = sa.getParam("RememberSacrificedSVar");
        int countSacrificed = 0;
        CardZoneTable table = new CardZoneTable();

        if (valid.equals("Self") && game.getZoneOf(card) != null) {
            if (game.getZoneOf(card).is(ZoneType.Battlefield)) {
                if (game.getAction().sacrifice(card, sa, table) != null) {
                    countSacrificed++;
                    if (remSacrificed) {
                        card.addRemembered(card);
                    }
                }
            }
        }
        else {
            CardCollectionView choosenToSacrifice = null;
            for (final Player p : tgts) {
                CardCollectionView battlefield = p.getCardsIn(ZoneType.Battlefield);
                CardCollectionView validTargets = AbilityUtils.filterListByType(battlefield, valid, sa);
                if (!destroy) {
                    validTargets = CardLists.filter(validTargets, CardPredicates.canBeSacrificedBy(sa));
                }

                if (sa.hasParam("Random")) {
                    choosenToSacrifice = Aggregates.random(validTargets, Math.min(amount, validTargets.size()), new CardCollection());
                } else if (sa.hasParam("OptionalSacrifice") && !p.getController().confirmAction(sa, null, "Do you want to sacrifice?")) {
                    choosenToSacrifice = CardCollection.EMPTY;
                } else {
                    boolean isOptional = sa.hasParam("Optional");
                    boolean isStrict = sa.hasParam("StrictAmount");
                    int minTargets = isOptional ? 0 : amount;
                    boolean notEnoughTargets = isStrict && validTargets.size() < minTargets;
                    
                    if (!notEnoughTargets) {
                        choosenToSacrifice = destroy ? 
                            p.getController().choosePermanentsToDestroy(sa, minTargets, amount, validTargets, msg) :
                            p.getController().choosePermanentsToSacrifice(sa, minTargets, amount, validTargets, msg);
                    } else {
                        choosenToSacrifice = CardCollection.EMPTY;
                    }
                }

                if (choosenToSacrifice.size() > 1) {
                    choosenToSacrifice = GameActionUtil.orderCardsByTheirOwners(game, choosenToSacrifice, ZoneType.Graveyard);
                }

                for (Card sac : choosenToSacrifice) {
                    final Card lKICopy = CardUtil.getLKICopy(sac);
                    boolean wasSacrificed = !destroy && game.getAction().sacrifice(sac, sa, table) != null;
                    boolean wasDestroyed = destroy && game.getAction().destroy(sac, sa, true, table);
                    // Run Devour Trigger
                    if (devour) {
                        card.addDevoured(lKICopy);
                        final Map<String, Object> runParams = Maps.newHashMap();
                        runParams.put("Devoured", sac);
                        game.getTriggerHandler().runTriggerOld(TriggerType.Devoured, runParams, false);
                    }
                    if (exploit) {
                        card.addExploited(lKICopy);
                        final Map<String, Object> runParams = Maps.newHashMap();
                        runParams.put("Exploited", lKICopy);
                        runParams.put("Card", card);
                        game.getTriggerHandler().runTriggerOld(TriggerType.Exploited, runParams, false);
                    }
                    if (wasDestroyed || wasSacrificed) {
                    	countSacrificed++;
                    	if (remSacrificed) {
                    		card.addRemembered(lKICopy);
                    	}
                    }
                }
            }

            if (remSVar != null) {
            	card.setSVar(remSVar, String.valueOf(countSacrificed));
            	SpellAbility root = sa;
            	do {
            		root.setSVar(remSVar, String.valueOf(countSacrificed));
            		root = root.getSubAbility();
            	} while (root != null);
            }
        }

        table.triggerChangesZoneAll(game);
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgts = getTargetPlayers(sa);

        String valid = sa.getParam("SacValid");
        if (valid == null) {
            valid = "Self";
        }

        String num = sa.getParam("Amount");
        num = (num == null) ? "1" : num;
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), num, sa);

        if (valid.equals("Self")) {
            sb.append("Sacrifice ").append(sa.getHostCard().toString());
        } else if (valid.equals("Card.AttachedBy")) {
            final Card toSac = sa.getHostCard().getEnchantingCard();
            sb.append(toSac.getController()).append(" sacrifices ").append(toSac).append(".");
        } else {
            for (final Player p : tgts) {
                sb.append(p.getName()).append(" ");
            }

            String msg = sa.getParam("SacMessage");
            if (msg == null) {
                msg = valid;
            }

            if (sa.hasParam("Destroy")) {
                sb.append("Destroys ");
            } else {
                sb.append("Sacrifices ");
            }
            sb.append(amount).append(" ").append(msg).append(".");
        }

        return sb.toString();
    }
}

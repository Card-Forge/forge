package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.HashMap;
import java.util.List;

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
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("EchoPaid", Boolean.valueOf(isPaid));
            runParams.put("Card", card);
            game.getTriggerHandler().runTrigger(TriggerType.PayEcho, runParams, false);
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

        if (valid.equals("Self")) {
            if (game.getZoneOf(card).is(ZoneType.Battlefield)) {
                if (game.getAction().sacrifice(card, sa) != null) {
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
                }
                else {
                    boolean isOptional = sa.hasParam("Optional");
                    choosenToSacrifice = destroy ? 
                        p.getController().choosePermanentsToDestroy(sa, isOptional ? 0 : amount, amount, validTargets, msg) :
                        p.getController().choosePermanentsToSacrifice(sa, isOptional ? 0 : amount, amount, validTargets, msg);
                }

                for (Card sac : choosenToSacrifice) {
                    final Card lKICopy = CardUtil.getLKICopy(sac);
                    boolean wasSacrificed = !destroy && game.getAction().sacrifice(sac, sa) != null;
                    boolean wasDestroyed = destroy && game.getAction().destroy(sac, sa);
                    // Run Devour Trigger
                    if (devour) {
                        card.addDevoured(lKICopy);
                        final HashMap<String, Object> runParams = new HashMap<String, Object>();
                        runParams.put("Devoured", sac);
                        game.getTriggerHandler().runTrigger(TriggerType.Devoured, runParams, false);
                    }
                    if (exploit) {
                        final HashMap<String, Object> runParams = new HashMap<String, Object>();
                        runParams.put("Exploited", lKICopy);
                        runParams.put("Card", card);
                        game.getTriggerHandler().runTrigger(TriggerType.Exploited, runParams, false);
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

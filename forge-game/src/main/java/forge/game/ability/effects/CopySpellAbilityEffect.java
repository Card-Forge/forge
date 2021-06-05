package forge.game.ability.effects;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.Localizer;


public class CopySpellAbilityEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<SpellAbility> tgtSpells = getTargetSpells(sa);

        sb.append("Copy ");
        // TODO Someone fix this Description when Copying Charms
        final Iterator<SpellAbility> it = tgtSpells.iterator();
        while (it.hasNext()) {
            sb.append(it.next().getHostCard());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        int amount = 1;
        if (sa.hasParam("Amount")) {
            amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa);
        }
        if (amount > 1) {
            sb.append(amount).append(" times");
        }
        sb.append(".");
        // TODO probably add an optional "You may choose new targets..."
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        List<Player> controllers = Lists.newArrayList(sa.getActivatingPlayer());

        int amount = 1;
        if (sa.hasParam("Amount")) {
            amount = AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa);
        }

        if (sa.hasParam("Controller")) {
            controllers = AbilityUtils.getDefinedPlayers(card, sa.getParam("Controller"), sa);
        }


        final List<SpellAbility> tgtSpells = getTargetSpells(sa);


        if (tgtSpells.size() == 0 || amount == 0) {
            return;
        }

        boolean isOptional = sa.hasParam("Optional");

        for (Player controller : controllers) {

            List<SpellAbility> copies = Lists.newArrayList();

            SpellAbility chosenSA = controller.getController().chooseSingleSpellForEffect(tgtSpells, sa,
                    Localizer.getInstance().getMessage("lblSelectASpellCopy"), ImmutableMap.of());

            if (isOptional && !controller.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoyouWantCopyTheSpell", CardTranslation.getTranslatedName(chosenSA.getHostCard().getName())))) {
                continue;
            }

            if (sa.hasParam("CopyForEachCanTarget")) {
                // Find subability or rootability that has targets
                SpellAbility targetedSA = chosenSA;
                while (targetedSA != null) {
                    if (targetedSA.usesTargeting() && targetedSA.getTargets().size() != 0) {
                        break;
                    }
                    targetedSA = targetedSA.getSubAbility();
                }
                if (targetedSA == null) {
                    continue;
                }
                final List<GameEntity> candidates = targetedSA.getTargetRestrictions().getAllCandidates(targetedSA, true);
                if (sa.hasParam("CanTargetPlayer")) {
                    // Radiate
                    // Remove targeted players because getAllCandidates include all the valid players
                    for(Player p : targetedSA.getTargets().getTargetPlayers())
                        candidates.remove(p);

                    for (GameEntity o : candidates) {
                        SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA, controller);
                        resetFirstTargetOnCopy(copy, o, targetedSA);
                        copies.add(copy);
                    }
                } else {// Precursor Golem, Ink-Treader Nephilim
                    final String type = sa.getParam("CopyForEachCanTarget");
                    CardCollection valid = new CardCollection();
                    List<Player> players = Lists.newArrayList();
                    Player originalTargetPlayer = Iterables.getFirst(getTargetPlayers(chosenSA), null);
                    for (final GameEntity o : candidates) {
                        if (o instanceof Card) {
                            valid.add((Card) o);
                        } else if (o instanceof Player) {
                            final Player p = (Player) o;
                            if (p.equals(originalTargetPlayer))
                                continue;
                            if (p.isValid(type.split(","), chosenSA.getActivatingPlayer(), chosenSA.getHostCard(), sa)) {
                                players.add(p);
                            }
                        }
                    }
                    valid = CardLists.getValidCards(valid, type.split(","), chosenSA.getActivatingPlayer(), chosenSA.getHostCard(), sa);
                    Card originalTarget = Iterables.getFirst(getTargetCards(chosenSA), null);
                    valid.remove(originalTarget);

                    if (sa.hasParam("ChooseOnlyOne")) {
                        Card choice = controller.getController().chooseSingleEntityForEffect(valid, sa, Localizer.getInstance().getMessage("lblChooseOne"), null);
                        if (choice != null) {
                            valid = new CardCollection(choice);
                        }
                    }

                    for (final Card c : valid) {
                        SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA, controller);
                        resetFirstTargetOnCopy(copy, c, targetedSA);
                        copies.add(copy);
                    }
                    for (final Player p : players) {
                        SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA, controller);
                        resetFirstTargetOnCopy(copy, p, targetedSA);
                        copies.add(copy);
                    }
                }
            }
            else {
                for (int i = 0; i < amount; i++) {
                    SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA, controller);
                    if (sa.hasParam("MayChooseTarget")) {
                        copy.setMayChooseNewTargets(true);
                    }
                    if (sa.hasParam("RandomTarget")){
                        List<GameEntity> candidates = copy.getTargetRestrictions().getAllCandidates(chosenSA, true);
                        if (sa.hasParam("RandomTargetRestriction")) {
                            candidates.removeIf(new Predicate<GameEntity>() {
                                @Override
                                public boolean test(GameEntity c) {
                                    return !c.isValid(sa.getParam("RandomTargetRestriction").split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa);
                                }
                            });
                        }
                        GameEntity choice = Aggregates.random(candidates);
                        resetFirstTargetOnCopy(copy, choice, chosenSA);
                    }

                    // extra case for Epic to remove the keyword and the last part of the SpellAbility
                    if (sa.hasParam("Epic")) {
                        copy.getHostCard().removeIntrinsicKeyword("Epic");
                        SpellAbility sub = copy;
                        while (sub.getSubAbility() != null && !sub.hasParam("Epic")) {
                            sub = sub.getSubAbility();
                        }
                        if (sub != null) {
                            sub.getParent().setSubAbility(sub.getSubAbility());
                        }
                    }

                    copies.add(copy);
                }
            }

            if (copies.isEmpty()) {
                continue;
            }

            int addAmount = copies.size();
            final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(controller);
            repParams.put(AbilityKey.SpellAbility, chosenSA);
            repParams.put(AbilityKey.Amount, addAmount);

            switch (game.getReplacementHandler().run(ReplacementType.CopySpell, repParams)) {
            case NotReplaced:
                break;
            case Updated: {
                addAmount = (int) repParams.get(AbilityKey.Amount);
                break;
            }
            default:
                addAmount = 0;
            }

            if (addAmount <= 0) {
                continue;
            }
            int extraAmount = addAmount - copies.size();
            for (int i = 0; i < extraAmount; i++) {
                SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA, controller);
                // extra copies added with CopySpellReplacenment currently always has new choose targets
                copy.setMayChooseNewTargets(true);
                copies.add(copy);
            }

            controller.getController().orderAndPlaySimultaneousSa(copies);

            if (sa.hasParam("RememberCopies")) {
                card.addRemembered(copies);
            }
        }
    } // end resolve

    private void resetFirstTargetOnCopy(SpellAbility copy, GameEntity obj, SpellAbility targetedSA) {
        copy.resetFirstTarget(obj, targetedSA);
        AbilitySub subAb = copy.getSubAbility();
        while (subAb != null) {
            subAb.resetFirstTarget(obj, targetedSA);
            subAb = subAb.getSubAbility();
        }
    }

}

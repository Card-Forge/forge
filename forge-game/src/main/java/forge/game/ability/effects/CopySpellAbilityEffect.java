package forge.game.ability.effects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObjectPredicates;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantBeCopied;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.collect.FCollection;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


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
            sb.append(" ").append(Lang.getNumeral(amount)).append(" times");
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

        int amount = 1;
        if (sa.hasParam("Amount")) {
            amount = AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa);
        }

        List<SpellAbility> tgtSpells = getTargetSpells(sa);

        tgtSpells.removeIf(tgtSA -> StaticAbilityCantBeCopied.cantBeCopied(tgtSA.getHostCard()));

        if (tgtSpells.isEmpty() || amount == 0) {
            return;
        }

        List<Player> controllers = Lists.newArrayList(sa.getActivatingPlayer());
        if (sa.hasParam("Controller")) {
            controllers = AbilityUtils.getDefinedPlayers(card, sa.getParam("Controller"), sa);
        }

        boolean isOptional = sa.hasParam("Optional");

        for (Player controller : controllers) {
            List<SpellAbility> copies = Lists.newArrayList();

            List<SpellAbility> copySpells = tgtSpells;
            if (sa.hasParam("SingleChoice")) {
                SpellAbility chosenSA = controller.getController().chooseSingleSpellForEffect(tgtSpells, sa,
                        Localizer.getInstance().getMessage("lblSelectASpellCopy"), ImmutableMap.of());
                copySpells = Lists.newArrayList(chosenSA);
            }

            for (SpellAbility chosenSA : copySpells) {
                if (isOptional && !controller.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoyouWantCopyTheSpell", CardTranslation.getTranslatedName(chosenSA.getHostCard().getName())), null)) {
                    continue;
                }

                // CR 707.10d
                if (sa.hasParam("CopyForEachCanTarget")) {
                    // Find subability or rootability that has targets
                    SpellAbility targetedSA = chosenSA;
                    while (targetedSA != null) {
                        if (targetedSA.usesTargeting() && !targetedSA.getTargets().isEmpty()) {
                            break;
                        }
                        targetedSA = targetedSA.getSubAbility();
                    }
                    if (targetedSA == null) {
                        continue;
                    }

                    FCollection<GameEntity> all = new FCollection<>(Iterables.filter(targetedSA.getTargetRestrictions().getAllCandidates(targetedSA, true), GameObjectPredicates.restriction(sa.getParam("CopyForEachCanTarget").split(","), sa.getActivatingPlayer(), card, sa)));
                    // Remove targeted players because getAllCandidates include all the valid players
                    all.removeAll(getTargetPlayers(chosenSA));

                    if (sa.hasParam("ChooseOnlyOne")) { // Beamsplitter Mage
                        GameEntity choice = controller.getController().chooseSingleEntityForEffect(all, sa, Localizer.getInstance().getMessage("lblChooseOne"), null);
                        if (choice != null) {
                            SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA, controller);
                            if (changeToLegalTarget(copy, choice)) {
                                copies.add(copy);
                            }
                        }
                    } else {
                        for (final GameEntity ge : all) {
                            SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA, controller);
                            resetFirstTargetOnCopy(copy, ge, targetedSA);
                            copies.add(copy);
                        }
                    }
                } else if (sa.hasParam("DefinedTarget")) { // CR 707.10e
                    final List<GameEntity> tgts = AbilityUtils.getDefinedEntities(card, sa.getParam("DefinedTarget"), sa);
                    if (tgts.isEmpty()) {
                        continue;
                    }

                    FCollection<GameEntity>  newTgts = new FCollection<>();
                    for (GameEntity e : tgts) {
                        if (e instanceof Player) { // Zevlor
                            FCollection<GameEntity> choices = new FCollection<>(e);
                            choices.addAll(((Player) e).getCardsIn(ZoneType.Battlefield));
                            newTgts.add(controller.getController().chooseSingleEntityForEffect(choices, sa, Localizer.getInstance().getMessage("lblChooseOne"), null));
                        } else { // Ivy
                            newTgts.add(e);
                        }
                    }

                    for (GameEntity e : newTgts) {
                        SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA, controller);
                        if (changeToLegalTarget(copy, e)) {
                            copies.add(copy);
                        }
                    }
                } else {
                    for (int i = 0; i < amount; i++) {
                        SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA, controller);
                        if (sa.hasParam("IgnoreFreeze")) {
                            copy.putParam("IgnoreFreeze", "True");
                        }
                        if (sa.hasParam("MayChooseTarget")) {
                            copy.setMayChooseNewTargets(true);
                        }

                        if (sa.hasParam("RandomTarget")) {
                            List<GameEntity> candidates = copy.getTargetRestrictions().getAllCandidates(chosenSA, true);
                            if (sa.hasParam("RandomTargetRestriction")) {
                                candidates.removeIf(new Predicate<GameEntity>() {
                                    @Override
                                    public boolean test(GameEntity c) {
                                        return !c.isValid(sa.getParam("RandomTargetRestriction").split(","), sa.getActivatingPlayer(), card, sa);
                                    }
                                });
                            }
                            if (!candidates.isEmpty()) {
                                GameEntity choice = Aggregates.random(candidates);
                                resetFirstTargetOnCopy(copy, choice, chosenSA);
                            }
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
            }

            controller.getController().orderAndPlaySimultaneousSa(copies);

            if (sa.hasParam("RememberCopies")) {
                card.addRemembered(copies);
            }
        }
    }

    private boolean changeToLegalTarget(SpellAbility copy, GameEntity tgt) {
        // Find subability or rootability that has targets
        SpellAbility targetedSA = copy;
        while (targetedSA != null) {
            if (targetedSA.usesTargeting() && !targetedSA.getTargets().isEmpty()) {
                break;
            }
            targetedSA = targetedSA.getSubAbility();
        }
        if (targetedSA == null) {
            return false;
        }
        if (!targetedSA.canTarget(tgt)) {
            return false;
        }
        resetFirstTargetOnCopy(copy, tgt, targetedSA);
        return true;
    }

    private void resetFirstTargetOnCopy(SpellAbility copy, GameEntity obj, SpellAbility targetedSA) {
        SpellAbility subAb = copy;
        while (subAb != null) {
            subAb.resetFirstTarget(obj, targetedSA);
            subAb = subAb.getSubAbility();
        }
    }

}

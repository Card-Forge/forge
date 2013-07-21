package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import forge.Card;
import forge.CardLists;
import forge.ITargetable;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCombat;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ChooseSourceEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a source.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Game game = sa.getActivatingPlayer().getGame();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);


        List<Card> permanentSources = new ArrayList<Card>();
        List<Card> stackSources = new ArrayList<Card>();
        List<Card> referencedSources = new ArrayList<Card>();
        List<Card> commandZoneSources = new ArrayList<Card>();

        List<Card> sourcesToChooseFrom = new ArrayList<Card>();

        // Get the list of permanent cards
        permanentSources = game.getCardsIn(ZoneType.Battlefield);
        // A source can be a face-up card in the command zone
        commandZoneSources = new ArrayList<Card>();
        for (Card c : game.getCardsIn(ZoneType.Command)) {
            if (!c.isFaceDown()) {
                commandZoneSources.add(c);
            }
        }

        // Get the list of cards that produce effects on the stack

        for (SpellAbilityStackInstance stackinst : game.getStack()) {
            if (!stackSources.contains(stackinst.getSourceCard())) {
                stackSources.add(stackinst.getSourceCard());
            }
            // Get the list of cards that are referenced by effects on the stack
            if (null != stackinst.getSpellAbility().getTriggeringObjects()) {
                for (Object c : stackinst.getSpellAbility().getTriggeringObjects().values()) {
                    if (c instanceof Card) {
                        if (!stackSources.contains((Card) c)) {
                            referencedSources.add((Card) c);
                        }
                    }
                }
            }
            if (null != stackinst.getSpellAbility().getTargetCard()) {
                referencedSources.add(stackinst.getSpellAbility().getTargetCard());
            }
            // TODO: is this necessary?
            if (null != stackinst.getSpellAbility().getReplacingObjects()) {
                for (Object c : stackinst.getSpellAbility().getReplacingObjects().values()) {
                    if (c instanceof Card) {
                        if (!stackSources.contains((Card) c)) {
                            referencedSources.add((Card) c);
                        }
                    }
                }
            }
        }


        if (sa.hasParam("Choices")) {
            permanentSources = CardLists.getValidCards(permanentSources, sa.getParam("Choices"), host.getController(), host);

            stackSources = CardLists.getValidCards(stackSources, sa.getParam("Choices"), host.getController(), host);
            referencedSources = CardLists.getValidCards(referencedSources, sa.getParam("Choices"), host.getController(), host);
            commandZoneSources = CardLists.getValidCards(commandZoneSources, sa.getParam("Choices"), host.getController(), host);
        }
        if (sa.hasParam("TargetControls")) {
            permanentSources = CardLists.filterControlledBy(permanentSources, tgtPlayers.get(0));
            stackSources = CardLists.filterControlledBy(stackSources, tgtPlayers.get(0));
            referencedSources = CardLists.filterControlledBy(referencedSources, tgtPlayers.get(0));
            commandZoneSources = CardLists.filterControlledBy(commandZoneSources, tgtPlayers.get(0));
        }

        Card divPermanentSources = new Card();
        divPermanentSources.setName("--PERMANENTS:--");
        Card divStackSources = new Card();
        divStackSources.setName("--SPELLS ON THE STACK:--");
        Card divReferencedSources = new Card();
        divReferencedSources.setName("--OBJECTS REFERRED TO ON THE STACK:--");
        Card divCommandZoneSources = new Card();
        divCommandZoneSources.setName("--CARDS IN THE COMMAND ZONE:--");

        if (permanentSources.size() > 0) {
            sourcesToChooseFrom.add(divPermanentSources);
            sourcesToChooseFrom.addAll(permanentSources);
        }
        if (stackSources.size() > 0) {
            sourcesToChooseFrom.add(divStackSources);
            sourcesToChooseFrom.addAll(stackSources);
        }
        if (referencedSources.size() > 0) {
            sourcesToChooseFrom.add(divReferencedSources);
            sourcesToChooseFrom.addAll(referencedSources);
        }
        if (commandZoneSources.size() > 0) {
            sourcesToChooseFrom.add(divCommandZoneSources);
            sourcesToChooseFrom.addAll(commandZoneSources);
        }

        if (sourcesToChooseFrom.size() == 0) {
            return;
        }

        final String numericAmount = sa.getParamOrDefault("Amount", "1");
        final int validAmount = StringUtils.isNumeric(numericAmount) ? Integer.parseInt(numericAmount) : CardFactoryUtil.xCount(host, host.getSVar(numericAmount));

        for (final Player p : tgtPlayers) {
            final List<Card> chosen = new ArrayList<Card>();
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < validAmount; i++) {
                    if (p.isHuman()) {
                        final String choiceTitle = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : "Choose a source ";
                        Card o = null;
                        do {
                            o = GuiChoose.one(choiceTitle, sourcesToChooseFrom);
                        } while (o.equals(divPermanentSources) || o.equals(divStackSources) || o.equals(divReferencedSources) || o.equals(divCommandZoneSources));
                        chosen.add(o);
                        sourcesToChooseFrom.remove(o);

                    } else {
                        if ("NeedsPrevention".equals(sa.getParam("AILogic"))) {
                            final Player ai = sa.getActivatingPlayer();
                            if (!game.getStack().isEmpty()) {
                                Card choseCard = ChooseCardOnStack(sa, ai, game);
                                if (choseCard != null) {
                                    chosen.add(ChooseCardOnStack(sa, ai, game));
                                }
                            }

                            final Combat combat = game.getCombat();
                            if (chosen.isEmpty()) {
                                permanentSources = CardLists.filter(permanentSources, new Predicate<Card>() {
                                    @Override
                                    public boolean apply(final Card c) {
                                        if (combat == null || !combat.isAttacking(c, ai) || !combat.isUnblocked(c)) {
                                            return false;
                                        }
                                        return ComputerUtilCombat.damageIfUnblocked(c, ai, combat) > 0;
                                    }
                                });
                                chosen.add(ComputerUtilCard.getBestCreatureAI(permanentSources));
                            }
                        } else {
                            chosen.add(ComputerUtilCard.getBestAI(sourcesToChooseFrom));
                        }
                    }
                }
                host.setChosenCard(chosen);
                if (sa.hasParam("RememberChosen")) {
                    for (final Card rem : chosen) {
                        host.addRemembered(rem);
                    }
                }
            }
        }
    }

    private Card ChooseCardOnStack(SpellAbility sa, Player ai, Game game) {
        for (SpellAbilityStackInstance si : game.getStack()) {
            final Card source = si.getSourceCard();
            final SpellAbility abilityOnStack = si.getSpellAbility();
            
            if (sa.hasParam("Choices") && !abilityOnStack.getSourceCard().isValid(sa.getParam("Choices"), ai, sa.getSourceCard())) {
                continue;
            }
            final ApiType threatApi = abilityOnStack.getApi();
            if (threatApi != ApiType.DealDamage && threatApi != ApiType.DamageAll) {
                continue;
            }
    
            
            List<? extends ITargetable> objects = getTargets(abilityOnStack);
    
            if (!abilityOnStack.usesTargeting() && !abilityOnStack.hasParam("Defined") && abilityOnStack.hasParam("ValidPlayers")) 
                objects = AbilityUtils.getDefinedPlayers(source, abilityOnStack.getParam("ValidPlayers"), abilityOnStack);
            
            if (!objects.contains(ai) || abilityOnStack.hasParam("NoPrevention")) {
                continue;
            }
            int dmg = AbilityUtils.calculateAmount(source, abilityOnStack.getParam("NumDmg"), abilityOnStack);
            if (ComputerUtilCombat.predictDamageTo(ai, dmg, source, false) <= 0) {
                continue;
            }
            return source;
        }
        return null;
    }
}

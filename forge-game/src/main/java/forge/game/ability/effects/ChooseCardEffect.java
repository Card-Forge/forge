package forge.game.ability.effects;

import java.util.*;

import com.google.common.collect.Lists;
import forge.game.Direction;
import forge.game.player.DelayedReveal;
import forge.game.player.PlayerView;
import forge.util.CardTranslation;

import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;

public class ChooseCardEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int numCards = sa.hasParam("Amount") ?
                AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;

        sb.append(Lang.joinHomogenous(getTargetPlayers(sa))).append(" ");
        if (sa.hasParam("Mandatory")) {
            sb.append(getTargetPlayers(sa).size() == 1 ? "chooses " : "choose ");
        } else {
            sb.append("may choose ");
        }
        String desc = sa.getParamOrDefault("ChoiceDesc", "card");
        if (!desc.contains("card") && !desc.contains("control")) {
            desc = desc + " card";
        }
        sb.append(Lang.nounWithNumeralExceptOne(numCards, desc));
        if (sa.hasParam("FromDesc")) {
            sb.append(" from ").append(sa.getParam("FromDesc"));
        } else if (sa.hasParam("ChoiceZone") && sa.getParam("ChoiceZone").equals("Hand")) {
            sb.append(" in their hand");
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        CardCollection allChosen = new CardCollection();

        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        List<ZoneType> choiceZone = Lists.newArrayList(ZoneType.Battlefield);
        if (sa.hasParam("ChoiceZone")) {
            choiceZone = ZoneType.listValueOf(sa.getParam("ChoiceZone"));
        }
        CardCollectionView choices = sa.hasParam("AllCards") ? game.getCardsInGame() : game.getCardsIn(choiceZone);
        if (sa.hasParam("Choices")) {
            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host, sa);
        }
        if (sa.hasParam("TargetControls")) {
            choices = CardLists.filterControlledBy(choices, tgtPlayers.get(0));
        }
        if (sa.hasParam("DefinedCards")) {
            choices = AbilityUtils.getDefinedCards(host, sa.getParam("DefinedCards"), sa);
        }
        if (sa.hasParam("IncludeSpellsOnStack")) {
            CardCollectionView stack = game.getCardsIn(ZoneType.Stack);
            CardCollection combined = new CardCollection();
            combined.addAll(stack);
            combined.addAll(choices);
            choices = combined;
        }

        final String amountValue = sa.getParamOrDefault("Amount", "1");
        int validAmount;
        if (amountValue.equals("Random")) {
            validAmount = Aggregates.randomInt(0, choices.size());
        } else {
            validAmount = AbilityUtils.calculateAmount(host, amountValue, sa);
        }
        final int minAmount = sa.hasParam("MinAmount") ? Integer.parseInt(sa.getParam("MinAmount")) : validAmount;

        if (validAmount <= 0) {
            return;
        }

        boolean revealTitle = sa.hasParam("RevealTitle");
        for (Player p : tgtPlayers) {
            CardCollectionView pChoices = choices;
            CardCollection chosen = new CardCollection();
            if (!p.isInGame()) {
                p = getNewChooser(sa, activator, p);
            }
            if (sa.hasParam("ControlledByPlayer")) {
                final String param = sa.getParam("ControlledByPlayer");
                if (param.equals("Chooser")) {
                    pChoices = CardLists.filterControlledBy(pChoices, p);
                } else if (param.equals("Left") || param.equals("Right")) {
                    pChoices = CardLists.filterControlledBy(pChoices, game.getNextPlayerAfter(p,
                        Direction.valueOf(param)));
                } else {
                    pChoices = CardLists.filterControlledBy(pChoices, AbilityUtils.getDefinedPlayers(host, param, sa));
                }
            }
            boolean dontRevealToOwner = true;
            if (sa.hasParam("EachBasicType")) {
                // Get all lands,
                List<Card> land = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.LANDS);
                String eachBasic = sa.getParam("EachBasicType");
                if (eachBasic.equals("Controlled")) {
                    land = CardLists.filterControlledBy(land, p);
                }

                // Choose one of each BasicLand given special place
                for (final String type : CardType.getBasicTypes()) {
                    final CardCollectionView cl = CardLists.getType(land, type);
                    if (!cl.isEmpty()) {
                        final String prompt = Localizer.getInstance().getMessage("lblChoose") + " " + Lang.nounWithAmount(1, type);
                        Card c = p.getController().chooseSingleEntityForEffect(cl, sa, prompt, false, null);
                        if (c != null) {
                            chosen.add(c);
                        }
                    }
                }
            } else if (sa.hasParam("ChooseEach")) {
                final String s = sa.getParam("ChooseEach");
                final String[] types = s.equals("Party") ? new String[]{"Cleric","Thief","Warrior","Wizard"}
                     : s.split(" & ");
                for (final String type : types) {
                    CardCollection valids = CardLists.filter(pChoices, CardPredicates.isType(type));
                    if (!valids.isEmpty()) {
                        final String prompt = Localizer.getInstance().getMessage("lblChoose") + " " +
                                Lang.nounWithNumeralExceptOne(1, type);
                        Card c = p.getController().chooseSingleEntityForEffect(valids, sa, prompt, 
                            !sa.hasParam("Mandatory"), null);
                        if (c != null) {
                            chosen.add(c);
                        }
                    }
                }
            } else if (sa.hasParam("WithTotalPower")) {
                final int totP = AbilityUtils.calculateAmount(host, sa.getParam("WithTotalPower"), sa);
                CardCollection negativeCreats = CardLists.filterLEPower(p.getCreaturesInPlay(), -1);
                int negativeNum = Aggregates.sum(negativeCreats, CardPredicates.Accessors.fnGetNetPower);
                CardCollection creature = CardLists.filterLEPower(p.getCreaturesInPlay(), totP - negativeNum);
                CardCollection chosenPool = new CardCollection();
                int chosenP = 0;
                while (!creature.isEmpty()) {
                    Card c = p.getController().chooseSingleEntityForEffect(creature, sa,
                            Localizer.getInstance().getMessage("lblSelectCreatureWithTotalPowerLessOrEqualTo", (totP - chosenP - negativeNum))
                                    + "\r\n(" + Localizer.getInstance().getMessage("lblSelected") + ":" + chosenPool + ")\r\n(" + Localizer.getInstance().getMessage("lblTotalPowerNum", chosenP) + ")", chosenP <= totP, null);
                    if (c == null) {
                        if (p.getController().confirmAction(sa, PlayerActionConfirmMode.OptionalChoose, Localizer.getInstance().getMessage("lblCancelChooseConfirm"), null)) {
                            break;
                        }
                    } else {
                        chosenP += c.getNetPower();
                        chosenPool.add(c);
                        negativeCreats.remove(c);
                        negativeNum = Aggregates.sum(negativeCreats, CardPredicates.Accessors.fnGetNetPower);
                        creature = CardLists.filterLEPower(p.getCreaturesInPlay(), totP - chosenP - negativeNum);
                        creature.removeAll(chosenPool);
                    }
                }
                chosen.addAll(chosenPool);
            } else if (sa.hasParam("WithDifferentPowers")) {
                String restrict = sa.getParam("Choices");
                CardCollection chosenPool = new CardCollection();
                String title = Localizer.getInstance().getMessage("lblChooseCreature");
                Card choice = null;
                while (!pChoices.isEmpty() && chosenPool.size() < validAmount) {
                    boolean optional = chosenPool.size() >= minAmount;
                    CardCollection creature = (CardCollection) pChoices;
                    if (!chosenPool.isEmpty()) {
                        title = Localizer.getInstance().getMessage("lblChooseCreatureWithDiffPower");
                    }
                    choice = p.getController().chooseSingleEntityForEffect(creature, sa, title, optional, null);
                    if (choice == null) {
                        break;
                    }
                    chosenPool.add(choice);
                    restrict = restrict + (restrict.contains(".") ? "+powerNE" : ".powerNE") + choice.getNetPower();
                    pChoices = CardLists.getValidCards(pChoices, restrict, activator, host, sa);
                }
                if (choice != null) {
                    chosenPool.add(choice);
                }
                chosen.addAll(chosenPool);
            } else if (sa.hasParam("EachDifferentPower")) {
                List<Integer> powers = new ArrayList<>();
                CardCollection chosenPool = new CardCollection();
                for (Card c : pChoices) {
                    int pow = c.getNetPower();
                    if (!powers.contains(pow)) {
                        powers.add(c.getNetPower());
                    }
                }
                Collections.sort(powers);
                String re = sa.getParam("Choices");
                re = re + (re.contains(".") ? "+powerEQ" : ".powerEQ");
                for (int i : powers) {
                    String restrict = re + i;
                    CardCollection valids = CardLists.getValidCards(pChoices, restrict, activator, host, sa);
                    Card choice = p.getController().chooseSingleEntityForEffect(valids, sa,
                            Localizer.getInstance().getMessage("lblChooseCreatureWithXPower", i), false, null);
                    chosenPool.add(choice);
                }
                chosen.addAll(chosenPool);
            } else if (sa.hasParam("ControlAndNot")) {
                String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseCreature");
                // Targeted player (p) chooses N creatures that belongs to them
                CardCollection tgtPlayerCtrl = CardLists.filterControlledBy(pChoices, p);
                chosen.addAll(p.getController().chooseCardsForEffect(tgtPlayerCtrl, sa, title + " " + "you control", minAmount, validAmount,
                        !sa.hasParam("Mandatory"), null));
                // Targeted player (p) chooses N creatures that don't belong to them
                CardCollection notTgtPlayerCtrl = new CardCollection(pChoices);
                notTgtPlayerCtrl.removeAll(tgtPlayerCtrl);
                chosen.addAll(p.getController().chooseCardsForEffect(notTgtPlayerCtrl, sa, title + " " + "you don't control", minAmount, validAmount,
                        !sa.hasParam("Mandatory"), null));
            } else if (sa.hasParam("AtRandom") && !pChoices.isEmpty()) {
                // don't pass FCollection for direct modification, the Set part would get messed up
                chosen = new CardCollection(Aggregates.random(pChoices, validAmount));
                dontRevealToOwner = false;
            } else {
                String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseaCard") + " ";
                if (sa.hasParam("ChoiceTitleAppend")) {
                    String tag = "";
                    String value = sa.getParam("ChoiceTitleAppend");
                    if (value.startsWith("Defined ")) {
                        tag = AbilityUtils.getDefinedPlayers(host, value.substring(8), sa).toString();
                    } else if (value.equals("ChosenType")) {
                        tag = host.getChosenType();
                    }
                    if (!tag.equals("")) {
                        title = title + " (" + tag +")";
                    }
                }
                if (sa.hasParam("QuasiLibrarySearch")) {
                    Long controlTimestamp = null;
                    if (activator.equals(p)) {
                        Map.Entry<Long, Player> searchControlPlayer = p.getControlledWhileSearching();
                        if (searchControlPlayer != null) {
                            controlTimestamp = searchControlPlayer.getKey();
                            p.addController(controlTimestamp, searchControlPlayer.getValue());
                        }
                    }

                    final Player searched = AbilityUtils.getDefinedPlayers(host, sa.getParam("QuasiLibrarySearch"), sa).get(0);
                    final int fetchNum = Math.min(searched.getCardsIn(ZoneType.Library).size(), 4);
                    CardCollectionView shown = !p.hasKeyword("LimitSearchLibrary")
                            ? searched.getCardsIn(ZoneType.Library) : searched.getCardsIn(ZoneType.Library, fetchNum);
                    DelayedReveal delayedReveal = new DelayedReveal(shown, ZoneType.Library, PlayerView.get(searched),
                            CardTranslation.getTranslatedName(host.getName()) + " - " +
                                    Localizer.getInstance().getMessage("lblLookingCardIn") + " ");
                    Card choice = p.getController().chooseSingleEntityForEffect(pChoices, delayedReveal, sa, title,
                            !sa.hasParam("Mandatory"), p, null);
                    if (choice == null) {
                        return;
                    }
                    chosen.add(choice);

                    if (controlTimestamp != null) {
                        p.removeController(controlTimestamp);
                    }
                } else {
                    chosen.addAll(p.getController().chooseCardsForEffect(pChoices, sa, title, minAmount, validAmount,
                            !sa.hasParam("Mandatory"), null));
                }
            }
            if (sa.hasParam("Reveal") && !sa.hasParam("Secretly")) {
                game.getAction().reveal(chosen, p, dontRevealToOwner, revealTitle ? sa.getParam("RevealTitle") : 
                    Localizer.getInstance().getMessage("lblChosenCards") + " ", !revealTitle);
            }
            if (sa.hasParam("ChosenMap")) {
                host.addToChosenMap(p, chosen);
            }
            allChosen.addAll(chosen);
        }
        if (sa.hasParam("Reveal") && sa.hasParam("Secretly")) {
            for (final Player p : tgtPlayers) {
                game.getAction().reveal(allChosen, p, true, revealTitle ?
                        sa.getParam("RevealTitle") : Localizer.getInstance().getMessage("lblChosenCards") + " ", 
                        !revealTitle);
            }
        }
        host.setChosenCards(allChosen);
        if (sa.hasParam("ForgetOtherRemembered")) {
            host.clearRemembered();
        }
        if (sa.hasParam("RememberChosen")) {
            host.addRemembered(allChosen);
        }
        if (sa.hasParam("ForgetChosen")) {
            host.removeRemembered(allChosen);
        }
        if (sa.hasParam("ImprintChosen")) {
            host.addImprintedCards(allChosen);
        }
    }
}

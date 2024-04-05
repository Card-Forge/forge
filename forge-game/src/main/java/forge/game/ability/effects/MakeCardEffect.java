package forge.game.ability.effects;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.ICardFace;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.item.BoosterPack;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.Localizer;

public class MakeCardEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());
        final Card source = sa.getHostCard();
        final PlayerCollection players = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

        for (final Player player : players) {
            final Game game = player.getGame();

            List<ICardFace> faces = new ArrayList<>();
            List<PaperCard> pack = null;
            List<String> names = Lists.newArrayList();
            
            final String desc = sa.getParamOrDefault("OptionPrompt", "");
            if (sa.hasParam("Optional") && sa.hasParam("OptionPrompt") && //for now, OptionPrompt is needed
                    !player.getController().confirmAction(sa, null, Localizer.getInstance().getMessage(desc), null)) {
            		return;
            }
            if (sa.hasParam("Name")) {
                final String n = sa.getParam("Name");
                if (n.equals("ChosenName")) {
                    if (source.hasNamedCard()) {
                        names.addAll(source.getNamedCards());
                    } else {
                        System.err.println("Malformed MakeCard entry! - " + source.toString());
                    }
                } else {
                    names.add(n);
                }
            } else if (sa.hasParam("Names")) {
                List<String> nameList = Arrays.asList(sa.getParam("Names").split(","));
                for (String s : nameList) {
                    // Cardnames that include "," must use ";" instead here
                    s = s.replace(";", ",");
                    names.add(s);
                }
            } else if (sa.hasParam("DefinedName")) {
                final String def = sa.getParam("DefinedName");
                CardCollection cards = new CardCollection();
                if (def.equals("ChosenMap")) {
                    cards = source.getChosenMap().get(player);
                } else {
                    cards = AbilityUtils.getDefinedCards(source, def, sa);
                }
                for (final Card c : cards) {
                    names.add(c.getName());
                }
            } else if (sa.hasParam("Spellbook")) {
                faces.addAll(parseFaces(sa, "Spellbook"));
            } else if (sa.hasParam("Choices")) {
                faces.addAll(parseFaces(sa, "Choices"));
            } else if (sa.hasParam("Booster")) {
                SealedProduct.Template booster = Aggregates.random(StaticData.instance().getBoosters());
                pack = new BoosterPack(booster.getEdition(), booster).getCards();
                for (PaperCard pc : pack) {
                    ICardFace face = pc.getRules().getMainPart();
                    if (face != null)
                        faces.add(face);
                    else
                        throw new RuntimeException("MakeCardEffect didn't find card face by name: " + pc);
                }
            }

            if (!faces.isEmpty()) {
                if (sa.hasParam("Filter")) {
                    List<ICardFace> filtered = new ArrayList<>();
                    for (ICardFace face : faces) {
                        PaperCard pc = StaticData.instance().getCommonCards().getUniqueByName(face.getName());
                        if (Card.fromPaperCard(pc, player).isValid(sa.getParam("Filter"), player, source, sa)) filtered.add(face);
                    }
                    faces = filtered;
                    if (faces.isEmpty()) continue;
                }
                int i = sa.hasParam("SpellbookAmount") ?
                        AbilityUtils.calculateAmount(source, sa.getParam("SpellbookAmount"), sa) : 1;
                while (i > 0) {
                    String chosen;
                    if (sa.hasParam("AtRandom")) {
                        chosen = Aggregates.random(faces).getName();
                    } else {
                        final String sbName = sa.hasParam("SpellbookName") ? sa.getParam("SpellbookName") :
                                CardTranslation.getTranslatedName(source.getName());
                        final String message = sa.hasParam("Choices") ? 
                            Localizer.getInstance().getMessage("lblChooseaCard") :
                            Localizer.getInstance().getMessage("lblChooseFromSpellbook", sbName);
                        chosen = player.getController().chooseCardName(sa, faces, message);
                    }
                    names.add(chosen);
                    faces.remove(StaticData.instance().getCommonCards().getFaceByName(chosen));
                    i--;
                }
            }

            final boolean attach = sa.hasParam("AttachedTo");
            final ZoneType zone = attach ? ZoneType.Battlefield : 
                ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Library"));
            if (zone == null) return;

            final int amount = sa.hasParam("Amount") ?
                    AbilityUtils.calculateAmount(source, sa.getParam("Amount"), sa) : 1;

            CardCollection cards = new CardCollection();
            final CardZoneTable triggerList = CardZoneTable.getSimultaneousInstance(sa);
            final GameEntityCounterTable counterTable = new GameEntityCounterTable();
            final CardCollectionView lastStateBattlefield = triggerList.getLastStateBattlefield();
            CardCollection attachList = new CardCollection();

            if (attach) {
                attachList = AbilityUtils.getDefinedCards(source, sa.getParam("AttachedTo"), sa);
                if (attachList.isEmpty()) {
                    attachList = CardLists.getValidCards(lastStateBattlefield, 
                        sa.getParam("AttachedTo"), source.getController(), source, sa);
                }
                if (attachList.isEmpty()) return; // nothing to attach to
            }

            for (final String name : names) {
                int toMake = amount;
                if (!name.equals("")) {
                    while (toMake > 0) {
                        PaperCard pc;
                        if (pack != null) {
                            pc = Iterables.getLast(Iterables.filter(pack, IPaperCard.Predicates.name(name)));
                        } else {
                            pc = StaticData.instance().getCommonCards().getUniqueByName(name);
                        }
                        Card card = Card.fromPaperCard(pc, player);

                        if (sa.hasParam("TokenCard")) {
                            card.setTokenCard(true);
                        }
                        game.getAction().moveTo(ZoneType.None, card, sa, moveParams);
                        cards.add(card);
                        toMake--;
                        if (sa.hasParam("Tapped")) {
                            card.setTapped(true);
                        }
                    }
                }
            }

            CardCollection madeCards = new CardCollection();
            final boolean wCounter = sa.hasParam("WithCounter");
            final boolean battlefield = zone.equals(ZoneType.Battlefield);
            
            for (final Card c : cards) {
                if (wCounter && battlefield) {
                    c.addEtbCounter(CounterType.getType(sa.getParam("WithCounter")), 
                        AbilityUtils.calculateAmount(source, sa.getParamOrDefault("WithCounterNum", "1"), 
                        sa), player);
                }        
                if (attach) {
                    for (Card a : attachList) {
                        Card cc;
                        if (c.getZone().getZoneType().equals(ZoneType.None)) cc = c;
                        else { // make another copy
                            PaperCard next = StaticData.instance().getCommonCards().getUniqueByName(c.getName());
                            cc = Card.fromPaperCard(next, player);
                            game.getAction().moveTo(ZoneType.None, cc, sa, moveParams);
                        }
                        cc.attachToEntity(game.getCardState(a), sa, true);
                        game.getAction().moveTo(zone, cc, sa, moveParams);
                        triggerList.put(ZoneType.None, cc.getZone().getZoneType(), cc);
                        madeCards.add(finishMaking(sa, cc, source));
                    }
                } else {
                    final int libraryPos = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(source, 
                    sa.getParam("LibraryPosition"), sa) : 0;
                    final Card made = game.getAction().moveTo(zone, c, libraryPos, sa, moveParams);
                    if (wCounter && !battlefield) {
                        made.addCounter(CounterType.getType(sa.getParam("WithCounter")),
                                AbilityUtils.calculateAmount(source, sa.getParamOrDefault("WithCounterNum", 
                                "1"), sa), player, counterTable);
                    }
                    triggerList.put(ZoneType.None, made.getZone().getZoneType(), made);
                    madeCards.add(finishMaking(sa, made, source));            
                }
            }
            triggerList.triggerChangesZoneAll(game, sa);
            counterTable.replaceCounterEffect(game, sa, true);

            if (sa.hasParam("Reveal")) {
                game.getAction().reveal(cards, player, true);
            }

            if (sa.hasParam("Conjure")) {
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(player);
                runParams.put(AbilityKey.Cards, madeCards);
                runParams.put(AbilityKey.Cause, sa); //-- currently not needed
                game.getTriggerHandler().runTrigger(TriggerType.ConjureAll, runParams, false);
            }

            if (zone.equals(ZoneType.Library) && !sa.hasParam("LibraryPosition")) {
                player.shuffle(sa);
            }
        }
    }

    private List<ICardFace> parseFaces (final SpellAbility sa, final String param) {
        List<ICardFace> parsedFaces = new ArrayList<>();
        for (String s : sa.getParam(param).split(",")) {
            // Cardnames that include "," must use ";" instead (i.e. Tovolar; Dire Overlord)
            s = s.replace(";", ",");
            ICardFace face = StaticData.instance().getCommonCards().getFaceByName(s);
            if (face != null)
                parsedFaces.add(face);
            else
                throw new RuntimeException("MakeCardEffect didn't find card face by name: " + s);
        }
        return parsedFaces;
    }

    private Card finishMaking (final SpellAbility sa, final Card made, final Card source) {
        if (sa.hasParam("FaceDown")) made.turnFaceDown(true);
        if (sa.hasParam("RememberMade")) source.addRemembered(made);
        if (sa.hasParam("ImprintMade")) source.addImprintedCard(made);
        return made;
    }
}

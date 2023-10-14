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
                final CardCollection def = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedName"), sa);
                for (final Card c : def) {
                    names.add(c.getName());
                }
            } else if (sa.hasParam("Spellbook")) {
                List<String> spellbook = Arrays.asList(sa.getParam("Spellbook").split(","));
                for (String s : spellbook) {
                    // Cardnames that include "," must use ";" instead in Spellbook$ (i.e. Tovolar; Dire Overlord)
                    s = s.replace(";", ",");
                    ICardFace face = StaticData.instance().getCommonCards().getFaceByName(s);
                    if (face != null)
                        faces.add(face);
                    else
                        throw new RuntimeException("MakeCardEffect didn't find card face by name: " + s);
                }
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
                if (sa.hasParam("AtRandom")) {
                    names.add(Aggregates.random(faces).getName());
                } else {
                    names.add(player.getController().chooseCardName(sa, faces,
                            Localizer.getInstance().getMessage("lblChooseFromSpellbook",
                                    CardTranslation.getTranslatedName(source.getName()))));
                }
            }

            final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Library"));
            final int amount = sa.hasParam("Amount") ?
                    AbilityUtils.calculateAmount(source, sa.getParam("Amount"), sa) : 1;

            CardCollection cards = new CardCollection();

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

            final CardZoneTable triggerList = new CardZoneTable();
            GameEntityCounterTable counterTable = new GameEntityCounterTable();
            CardCollection madeCards = new CardCollection();
            for (final Card c : cards) {
                if (sa.hasParam("WithCounter") && zone != null && zone.equals(ZoneType.Battlefield)) {
                    c.addEtbCounter(CounterType.getType(sa.getParam("WithCounter")),
                            AbilityUtils.calculateAmount(source, sa.getParamOrDefault("WithCounterNum", "1"), sa),
                            player);
                }

                final int libraryPos = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(source, sa.getParam("LibraryPosition"), sa) : 0;
                Card made = game.getAction().moveTo(zone, c, libraryPos, sa, moveParams);

                if (sa.hasParam("WithCounter") && zone != null && !zone.equals(ZoneType.Battlefield)) {
                    made.addCounter(CounterType.getType(sa.getParam("WithCounter")),
                            AbilityUtils.calculateAmount(source, sa.getParamOrDefault("WithCounterNum", "1"), sa),
                            player, counterTable);
                }

                if (sa.hasParam("FaceDown")) {
                    made.turnFaceDown(true);
                }
                triggerList.put(ZoneType.None, made.getZone().getZoneType(), made);
                madeCards.add(made);
                if (sa.hasParam("RememberMade")) {
                    source.addRemembered(made);
                }
                if (sa.hasParam("ImprintMade")) {
                    source.addImprintedCard(made);
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
}

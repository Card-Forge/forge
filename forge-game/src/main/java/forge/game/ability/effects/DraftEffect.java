package forge.game.ability.effects;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

import java.util.*;

 public class DraftEffect extends SpellAbilityEffect {
     @Override
     protected String getStackDescription(SpellAbility sa) {
         final Card source = sa.getHostCard();
         final Player player = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa).get(0);

         final StringBuilder sb = new StringBuilder();

         sb.append(player).append(" drafts a card from ").append(source.getDisplayName()).append("'s spellbook.");

         return sb.toString();
     }

     @Override
     public void resolve(SpellAbility sa) {
         final Card source = sa.getHostCard();
         final Player player = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa).get(0);
         final Game game = player.getGame();
         List<String> spellbook = Arrays.asList(sa.getParam("Spellbook").split(","));
         final int numToDraft = AbilityUtils.calculateAmount(source, sa.getParamOrDefault("DraftNum", "1"), sa);
         Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
         moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
         moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());
         CardCollection drafted = new CardCollection();

         for (int i = 0; i < numToDraft; i++) {
             Collections.shuffle(spellbook);
             List<Card> draftOptions = new ArrayList<>();
             for (String name : spellbook.subList(0, 3)) {
                 // Cardnames that include "," must use ";" instead in Spellbook$ (i.e. Tovolar; Dire Overlord)
                 name = name.replace(";", ",");
                 Card cardOption = Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(name), player);
                 draftOptions.add(cardOption);
             }

             Card chosenCard = player.getController().chooseSingleCardForZoneChange(ZoneType.None, new ArrayList<ZoneType>(), sa, new CardCollection(draftOptions), null, Localizer.getInstance().getMessage("lblChooseCardDraft"), false, player);
             game.getAction().moveTo(ZoneType.None, chosenCard, sa, moveParams);
             drafted.add(chosenCard);
         }

         final CardZoneTable triggerList = new CardZoneTable();
         for (final Card c : drafted) {
             Card made = game.getAction().moveToHand(c, sa, moveParams);
             if (c != null) {
                 triggerList.put(ZoneType.None, made.getZone().getZoneType(), made);
             }
             if (sa.hasParam("RememberDrafted")) {
                 source.addRemembered(made);
             }
         }
         triggerList.triggerChangesZoneAll(game, sa);
     }
 }

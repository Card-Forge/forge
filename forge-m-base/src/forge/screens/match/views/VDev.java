package forge.screens.match.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameType;
import forge.game.PlanarDice;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.net.FServer;
import forge.player.HumanPlay;
import forge.screens.match.FControl;
import forge.screens.match.input.InputSelectCardsFromList;
import forge.screens.match.views.VHeader.HeaderDropDown;
import forge.toolbox.FFileChooser;
import forge.toolbox.FLabel;
import forge.toolbox.GuiChoose;
import forge.toolbox.GuiDialog;
import forge.utils.ForgePreferences;
import forge.utils.ForgePreferences.FPref;

public class VDev extends HeaderDropDown {
    private static final float INSETS_FACTOR = 0.025f;
    private static final float GAP_FACTOR = 0.01f;

    private List<DevLabel> devLabels = new ArrayList<DevLabel>();

    public VDev() {
        addDevLabel("Generate Mana", new Runnable() {
            @Override
            public void run() {
                generateMana();
            }
        });
        addDevLabel("Tutor for Card", new Runnable() {
            @Override
            public void run() {
                tutorForCard();
            }
        });
        addDevLabel("Add card to play", new Runnable() {
            @Override
            public void run() {
                addCardToBattlefield();
            }
        });
        addDevLabel("Add card to hand", new Runnable() {
            @Override
            public void run() {
                addCardToHand();
            }
        });
        addDevLabel("Set Player Life", new Runnable() {
            @Override
            public void run() {
                setPlayerLife();
            }
        });
        addDevLabel("Setup Game State", new Runnable() {
            @Override
            public void run() {
                final FFileChooser fc = new FFileChooser();
                if (!fc.show("Select Game State File")) {
                    return;
                }
                
                FControl.setupGameState(fc.getSelectedFile().getAbsolutePath());
            }
        });

        final ForgePreferences prefs = FModel.getPreferences();
        boolean unlimitedLands = prefs.getPrefBoolean(FPref.DEV_UNLIMITED_LAND);
        final DevLabel lblUnlimitedLands = addDevLabel("Play Unlimited Lands: " +
                (unlimitedLands ? "On" : "Off"), null, true, unlimitedLands);
        lblUnlimitedLands.setCommand(new Runnable() {
            @Override
            public void run() {
                boolean unlimitedLands = lblUnlimitedLands.isSelected();
                lblUnlimitedLands.setText("Play Unlimited Lands: " +
                        (unlimitedLands ? "On" : "Off"));

                for (Player p : FControl.getGame().getPlayers()) {
                    if (p.getLobbyPlayer() == FServer.getLobby().getGuiPlayer() ) {
                        p.canCheatPlayUnlimitedLands = unlimitedLands;
                    }
                }
                // probably will need to call a synchronized method to have the game thread see changed value of the variable

                prefs.setPref(FPref.DEV_UNLIMITED_LAND, String.valueOf(unlimitedLands));
                prefs.save();
            }
        });

        addDevLabel("Add Counter to Permanent", new Runnable() {
            @Override
            public void run() {
                addCounterToPermanent();
            }
        });
        addDevLabel("Tap Permanent", new Runnable() {
            @Override
            public void run() {
                tapPermanent();
            }
        });
        addDevLabel("Untap Permanent", new Runnable() {
            @Override
            public void run() {
                untapPermanent();
            }
        });
        addDevLabel("Rigged planar roll", new Runnable() {
            @Override
            public void run() {
                riggedPlanarRoll();
            }
        });
        addDevLabel("Planeswalk to", new Runnable() {
            @Override
            public void run() {
                planeswalkTo();
            }
        });
    }

    private DevLabel addDevLabel(final String text0, final Runnable command0) {
        return addDevLabel(text0, command0, false, false);
    }
    private DevLabel addDevLabel(final String text0, final Runnable command0, boolean selectable0, boolean selected0) {
        DevLabel devLabel = add(new DevLabel(text0, command0, selectable0, selected0));
        devLabels.add(devLabel);
        return devLabel;
    }

    @Override
    public int getCount() {
        return -1;
    }

    @Override
    public void update() {
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float x = visibleWidth * INSETS_FACTOR;
        float y = x;
        float dy = visibleHeight * GAP_FACTOR;

        int rowCount = (int)(devLabels.size() / 2);
        float labelWidth = (visibleWidth - 2 * x - dy) / 2;
        float labelHeight = (visibleHeight - y - x) / rowCount - dy;

        float startX = x;
        float dx = labelWidth + dy;
        dy += labelHeight;

        for (int i = 0; i < devLabels.size(); i++) {
            devLabels.get(i).setBounds(x, y, labelWidth, labelHeight);
            if (i % 2 == 0) {
                x += dx;
            }
            else {
                x = startX;
                y += dy;
            }
        }
        return new ScrollBounds(visibleWidth, visibleHeight);
    }

    private class DevLabel extends FLabel {
        public DevLabel(final String text0, final Runnable command0, boolean selectable0, boolean selected0) {
            super(new ButtonBuilder().text(text0).command(command0).fontSize(12)
                    .selectable(selectable0).selected(selected0));
        }
    }

    private static void generateMana() {
        final Card dummy = new Card(-777777);
        dummy.setOwner(FControl.getGame().getPhaseHandler().getPriorityPlayer());
        Map<String, String> produced = new HashMap<String, String>();
        produced.put("Produced", "W W W W W W W U U U U U U U B B B B B B B G G G G G G G R R R R R R R 7");
        final AbilityManaPart abMana = new AbilityManaPart(dummy, produced);
        FControl.getGame().getAction().invoke(new Runnable() {
            @Override
            public void run() {
                abMana.produceMana(null);
            }
        });
    }

    private static void tutorForCard() {
        Player pPriority = FControl.getGame().getPhaseHandler().getPriorityPlayer();
        if (pPriority == null) {
            GuiDialog.message("No player has priority now, can't tutor from their deck at the moment");
            return;
        }
        final List<Card> lib = pPriority.getCardsIn(ZoneType.Library);
        final Card card = GuiChoose.oneOrNone("Choose a card", lib);
        if (card == null) { return; }

        FControl.getGame().getAction().invoke(new Runnable() {
            @Override
            public void run() {
                FControl.getGame().getAction().moveToHand(card);
            }
        });
    }

    private static void addCounterToPermanent() {
        final Card card = GuiChoose.oneOrNone("Add counters to which card?", FControl.getGame().getCardsIn(ZoneType.Battlefield));
        if (card == null) { return; }

        final CounterType counter = GuiChoose.oneOrNone("Which type of counter?", CounterType.values());
        if (counter == null) { return; }

        final Integer count = GuiChoose.getInteger("How many counters?", 1, Integer.MAX_VALUE, 10);
        if (count == null) { return; }
        
        card.addCounter(counter, count, false);
    }

    private static void tapPermanent() {
        final Game game = FControl.getGame();
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                final List<Card> untapped = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Predicates.not(CardPredicates.Presets.TAPPED));
                InputSelectCardsFromList inp = new InputSelectCardsFromList(0, Integer.MAX_VALUE, untapped);
                inp.setCancelAllowed(true);
                inp.setMessage("Choose permanents to tap");
                inp.showAndWait();
                if (!inp.hasCancelled()) {
                    for (Card c : inp.getSelected()) {
                        c.tap();
                    }
                }
            }
        });
    }

    private static void untapPermanent() {
        final Game game = FControl.getGame();

        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                final List<Card> tapped = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.TAPPED);
                InputSelectCardsFromList inp = new InputSelectCardsFromList(0, Integer.MAX_VALUE, tapped);
                inp.setCancelAllowed(true);
                inp.setMessage("Choose permanents to untap");
                inp.showAndWait();
                if (!inp.hasCancelled()) {
                    for(Card c : inp.getSelected()) {
                        c.untap();
                    }
                }
            }
        });
    }

    private static void setPlayerLife() {
        final List<Player> players = FControl.getGame().getPlayers();
        final Player player = GuiChoose.oneOrNone("Set life for which player?", players);
        if (player == null) { return; }

        final Integer life = GuiChoose.getInteger("Set life to what?", 0);
        if (life == null) { return; }

        player.setLife(life, null);
    }

    private static void addCardToHand() {
        final List<Player> players = FControl.getGame().getPlayers();
        final Player p = GuiChoose.oneOrNone("Put card in hand for which player?", players);
        if (null == p) {
            return;
        }

        final List<PaperCard> cards =  Lists.newArrayList(FModel.getMagicDb().getCommonCards().getUniqueCards());
        Collections.sort(cards);

        // use standard forge's list selection dialog
        final IPaperCard c = GuiChoose.oneOrNone("Name the card", cards);
        if (c == null) {
            return;
        }

        FControl.getGame().getAction().invoke(new Runnable() { @Override public void run() {
            FControl.getGame().getAction().moveToHand(Card.fromPaperCard(c, p));
        }});
    }

    private static void addCardToBattlefield() {
        final List<Player> players = FControl.getGame().getPlayers();
        final Player p = GuiChoose.oneOrNone("Put card in play for which player?", players);
        if (null == p) {
            return;
        }

        final List<PaperCard> cards =  Lists.newArrayList(FModel.getMagicDb().getCommonCards().getUniqueCards());
        Collections.sort(cards);

        // use standard forge's list selection dialog
        final IPaperCard c = GuiChoose.oneOrNone("Name the card", cards);
        if (c == null) {
            return;
        }

        final Game game = FControl.getGame();
        game.getAction().invoke(new Runnable() {
            @Override public void run() {
                final Card forgeCard = Card.fromPaperCard(c, p);

                if (c.getRules().getType().isLand()) {
                    game.getAction().moveToPlay(forgeCard);
                } else {
                    final List<SpellAbility> choices = forgeCard.getBasicSpells();
                    if (choices.isEmpty()) {
                        return; // when would it happen?
                    }

                    final SpellAbility sa = choices.size() == 1 ? choices.get(0) : GuiChoose.oneOrNone("Choose", choices);
                    if (sa == null) {
                        return; // happens if cancelled
                    }

                    game.getAction().moveToHand(forgeCard); // this is really needed (for rollbacks at least)
                    // Human player is choosing targets for an ability controlled by chosen player.
                    sa.setActivatingPlayer(p);
                    HumanPlay.playSaWithoutPayingManaCost(game, sa, true);
                }
                game.getStack().addAllTirggeredAbilitiesToStack(); // playSa could fire some triggers
            }
        });
    }

    private static void riggedPlanarRoll() {
        final List<Player> players = FControl.getGame().getPlayers();
        final Player player = GuiChoose.oneOrNone("Which player should roll?", players);
        if (player == null) { return; }

        final PlanarDice res = GuiChoose.oneOrNone("Choose result", PlanarDice.values());
        if (res == null) { return; }

        System.out.println("Rigging planar dice roll: " + res.toString());

        //DBG
        //System.out.println("ActivePlanes: " + FControl.getGame().getActivePlanes());
        //System.out.println("CommandPlanes: " + FControl.getGame().getCardsIn(ZoneType.Command));

        FControl.getGame().getAction().invoke(new Runnable() {
            @Override
            public void run() {
                PlanarDice.roll(player, res);
            }
        });
    }

    private static void planeswalkTo() {
        final Game game = FControl.getGame();
        if (!game.getRules().hasAppliedVariant(GameType.Planechase)) { return; }
        final Player p = game.getPhaseHandler().getPlayerTurn();

        final List<PaperCard> allPlanars = new ArrayList<PaperCard>();
        for (PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
            if (c.getRules().getType().isPlane() || c.getRules().getType().isPhenomenon()) {
                allPlanars.add(c);
            }
        }
        Collections.sort(allPlanars);

        // use standard forge's list selection dialog
        final IPaperCard c = GuiChoose.oneOrNone("Name the card", allPlanars);
        if (c == null) { return; }
        final Card forgeCard = Card.fromPaperCard(c, p);

        forgeCard.setOwner(p);
        game.getAction().invoke(new Runnable() { 
            @Override
            public void run() {
                game.getAction().changeZone(null, p.getZone(ZoneType.PlanarDeck), forgeCard, 0);
                PlanarDice.roll(p, PlanarDice.Planeswalk);
            }
        });
    }
}

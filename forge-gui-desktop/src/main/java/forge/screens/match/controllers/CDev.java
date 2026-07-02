package forge.screens.match.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import com.google.common.collect.Lists;

import forge.gui.framework.ICDoc;
import forge.interfaces.IGameController;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.IDevListener;
import forge.screens.match.views.VDev;

/**
 * Controls the combat panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public final class CDev implements ICDoc {

    private final CMatchUI matchUI;
    private final VDev view;
    public CDev(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VDev(this);
        addListener(view);

        view.getLblUnlimitedLands().addMouseListener(onClick(this::togglePlayManyLandsPerTurn));
        view.getLblViewAll().addMouseListener(onClick(this::toggleViewAllCards));
        view.getLblGenerateMana().addMouseListener(onClick(this::generateMana));
        view.getLblSetupGame().addMouseListener(onClick(this::setupGameState));
        view.getLblDumpGame().addMouseListener(onClick(this::dumpGameState));
        view.getLblTutor().addMouseListener(onClick(this::tutorForCard));
        view.getLblCardToHand().addMouseListener(onClick(this::addCardToHand));
        view.getLblExileFromHand().addMouseListener(onClick(this::exileCardsFromHand));
        view.getLblCardToBattlefield().addMouseListener(onClick(this::addCardToBattlefield));
        view.getLblTokenToBattlefield().addMouseListener(onClick(this::addTokenToBattlefield));
        view.getLblCardToLibrary().addMouseListener(onClick(this::addCardToLibrary));
        view.getLblCardToGraveyard().addMouseListener(onClick(this::addCardToGraveyard));
        view.getLblCardToExile().addMouseListener(onClick(this::addCardToExile));
        view.getLblCastSpell().addMouseListener(onClick(this::castASpell));
        view.getLblRepeatAddCard().addMouseListener(onClick(this::repeatAddCard));
        view.getLblAddCounterPermanent().addMouseListener(onClick(this::addCounterToPermanent));
        view.getLblSubCounterPermanent().addMouseListener(onClick(this::removeCountersFromPermanent));
        view.getLblTapPermanent().addMouseListener(onClick(this::tapPermanent));
        view.getLblUntapPermanent().addMouseListener(onClick(this::untapPermanent));
        view.getLblSetLife().addMouseListener(onClick(this::setPlayerLife));
        view.getLblWinGame().addMouseListener(onClick(this::winGame));
        view.getLblExileFromPlay().addMouseListener(onClick(this::exileCardsFromPlay));
        view.getLblRemoveFromGame().addMouseListener(onClick(this::removeCardsFromGame));
        view.getLblRiggedRoll().addMouseListener(onClick(this::riggedPlanerRoll));
        view.getLblWalkTo().addMouseListener(onClick(this::planeswalkTo));
        view.getLblAskAI().addMouseListener(onClick(this::askAI));
        view.getLblAskSimulationAI().addMouseListener(onClick(this::askSimulationAI));
    }
    public IGameController getController() {
        return matchUI.getGameController();
    }
    public VDev getView() {
        return view;
    }

    private final List<IDevListener> listeners = Lists.newArrayListWithCapacity(2);
    public void addListener(final IDevListener listener) {
        listeners.add(listener);
    }

    private MouseListener onClick(Runnable r) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                r.run();
            }
        };
    }

    public void togglePlayManyLandsPerTurn() {
        final boolean newValue = !view.getLblUnlimitedLands().getToggled();
        getController().cheat().setCanPlayUnlimitedLands(newValue);
        update();
    }

    public void toggleViewAllCards() {
        final boolean newValue = !view.getLblViewAll().getToggled();
        getController().cheat().setViewAllCards(newValue);
        update();
    }

    public void generateMana() {
        getController().cheat().generateMana();
    }
    public void setupGameState() {
        getController().cheat().setupGameState();
    }
    public void dumpGameState() {
        getController().cheat().dumpGameState();
    }
    public void tutorForCard() {
        getController().cheat().tutorForCard();
    }
    public void addCardToHand() {
        getController().cheat().addCardToHand();
    }
    public void addCardToLibrary() {
        getController().cheat().addCardToLibrary();
    }
    public void addCardToGraveyard() {
        getController().cheat().addCardToGraveyard();
    }
    public void addCardToExile() {
        getController().cheat().addCardToExile();
    }

    public void castASpell() {
        getController().cheat().castASpell();
    }
    public void repeatAddCard() {
        getController().cheat().repeatLastAddition();
    }
    public void exileCardsFromHand() {
        getController().cheat().exileCardsFromHand();
    }
    public void addCounterToPermanent() {
        getController().cheat().addCountersToPermanent();
    }
    public void removeCountersFromPermanent() {
        getController().cheat().removeCountersFromPermanent();
    }
    public void tapPermanent() {
        getController().cheat().tapPermanents();
    }
    public void untapPermanent() {
        getController().cheat().untapPermanents();
    }
    public void setPlayerLife() {
        getController().cheat().setPlayerLife();
    }
    public void winGame() {
        getController().cheat().winGame();
    }
    public void addCardToBattlefield() {
        getController().cheat().addCardToBattlefield();
    }
    public void addTokenToBattlefield() {
        getController().cheat().addTokenToBattlefield();
    }
    public void exileCardsFromPlay() {
        getController().cheat().exileCardsFromBattlefield();
    }
    public void removeCardsFromGame() {
        getController().cheat().removeCardsFromGame();
    }
    public void riggedPlanerRoll() {
        getController().cheat().riggedPlanarRoll();
    }
    public void planeswalkTo() {
        getController().cheat().planeswalkTo();
    }
    public void askAI() {
        getController().cheat().askAI(false);
    }
    public void askSimulationAI() {
        getController().cheat().askAI(true);
    }

    //========== End mouse listener inits

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        final IGameController controller = getController();
        if (controller != null) {
            final boolean canPlayUnlimitedLands = controller.canPlayUnlimitedLands();
            final boolean mayLookAtAllCards = controller.mayLookAtAllCards();
            for (final IDevListener listener : listeners) {
                listener.update(canPlayUnlimitedLands, mayLookAtAllCards);
            }
        }
    }
}

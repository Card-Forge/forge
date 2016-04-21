package forge.tournament.system;

import com.google.common.collect.Lists;

import java.lang.reflect.Array;
import java.util.*;

@SuppressWarnings("serial")
public class TournamentSwiss extends AbstractTournament {
    // Basically allow each player to not repeat an opponent or have more than one bye
    // But have a limited number of rounds because RoundRobin isn't feasible
    //http://www.wizards.com/DCI/downloads/Swiss_Pairings.pdf
    public TournamentSwiss(int ttlRnds, int pairingAmount) {
        super(ttlRnds);
        this.playersInPairing = pairingAmount;
        // Don't initialize the tournament if no players are available
    }

    public TournamentSwiss(List<TournamentPlayer> allPlayers, int pairingAmount) {
        // Technically not 100% correct for huge tournaments, but yknow.. close enough.
        super((int)Math.ceil(Math.log(allPlayers.size())/Math.log(2)), allPlayers);
        this.playersInPairing = pairingAmount;
    }

    @Override
    public void generateActivePairings() {
        // Group by Score
        // all the X-0s play each other
        // all the X-1s play each other, etc
        // if an odd amount in a grouping, last gets bumped down to lower group
        // award a bye to the last player without a pairing

        int frontOfGroup = 0;
        boolean pairing = true;
        TournamentPlayer byePlayer = null;
        activeRound++;

        // Randomize players, then sort by scores
        Collections.shuffle(allPlayers);
        sortAllPlayers("swiss");

        if (allPlayers.size() % 2 == 1) {
            int i = allPlayers.size() - 1;
            while(byePlayer == null) {
                TournamentPlayer pl = allPlayers.get(i);
                if (pl.getByes() == 0) {
                    byePlayer = pl;
                }
                i--;
            }
        }

        int backOfGroup;
        List<TournamentPlayer> leftoverPlayers = new ArrayList<>();

        List<TournamentPlayer> groupPlayers = Lists.newArrayList(allPlayers);

        if (byePlayer != null) {
            groupPlayers.remove(byePlayer);

            TournamentPairing byePair = new TournamentPairing(activeRound, Lists.<TournamentPlayer>newArrayList(byePlayer));
            byePair.setBye(true);
            activePairings.add(byePair);
        }

        if (groupPlayers.isEmpty()) {
            return;
        }

        int score = groupPlayers.get(frontOfGroup).getScore();
        while(pairing) {
            for(backOfGroup = frontOfGroup+1; backOfGroup < groupPlayers.size(); backOfGroup++) {
                if (allPlayers.get(backOfGroup).getScore() < score) {
                    break;
                }
            }
            List<TournamentPlayer> plyrs = Lists.newArrayList();
            if (!groupPlayers.isEmpty())
                plyrs.addAll(groupPlayers.subList(frontOfGroup, backOfGroup));
            plyrs.addAll(0, leftoverPlayers);
            groupPlayers.removeAll(plyrs);

            if (this.activeRound < 3) {
                // When ties are impossible, the first two rounds can be paired simply
                // TODO Should games taking too long result in a tie?
                leftoverPlayers = pairSwissGroup(plyrs);
            } else {
                // Complex pairings,
                int playersToPair = plyrs.size();
                leftoverPlayers = pairComplexSwissGroup(plyrs);
                if (playersToPair == leftoverPlayers.size() && groupPlayers.isEmpty()) {
                    // Couldn't pair anyone!
                    System.out.println("Only players left already played each other. Force pairing?");
                    leftoverPlayers = pairSwissGroup(leftoverPlayers);
                }
            }

            if (leftoverPlayers.isEmpty() && groupPlayers.isEmpty()) {
                pairing = false;
            } else if (!groupPlayers.isEmpty()) {
                score = groupPlayers.get(frontOfGroup).getSwissScore();
            } else {
                score = leftoverPlayers.get(frontOfGroup).getSwissScore();
            }
        }
    }

    private List<TournamentPlayer> pairComplexSwissGroup(List<TournamentPlayer> players) {
        // Record all opponents of current group players, assigning pairs as they are allowable
        final HashMap<TournamentPlayer, HashSet<TournamentPlayer>> availableOpponents = new HashMap<>();
        int oppClashes = 0;
        List<TournamentPlayer> unpairedPlayers = Lists.newArrayList();

        for(TournamentPlayer tp : players) {
            HashSet<TournamentPlayer> opponents = new HashSet<>();
            List<Integer> prevOpps = tp.getPreviousOpponents();
            for(TournamentPlayer opp : players) {
                if (!prevOpps.contains(opp.getIndex()) && !opp.equals(tp)) {
                    opponents.add(opp);
                } else if (!opp.equals(tp)) {
                    oppClashes++;
                }
            }

            availableOpponents.put(tp, opponents);
        }
        oppClashes /= 2;

        if (oppClashes == 0) {
            return pairSwissGroup(players);
        }

        Collections.sort(players, new Comparator<TournamentPlayer>() {
            @Override
            public int compare(TournamentPlayer o1, TournamentPlayer o2) {
                return availableOpponents.get(o1).size() - availableOpponents.get(o2).size();
            }
        });

        while(players.size() > 1) {
            TournamentPlayer initialPlayer = players.get(0);
            players.remove(0);
            ArrayList<TournamentPlayer> pair = new ArrayList<>();

            HashSet<TournamentPlayer> opposing = availableOpponents.get(initialPlayer);
            for(TournamentPlayer opp : players) {
                if (opposing.contains(opp)) {
                    pair.add(opp);
                    break;
                }
            }

            for(TournamentPlayer opp : pair) {
                players.remove(opp);
            }

            if (pair.isEmpty()) {
                unpairedPlayers.add(initialPlayer);
            } else {
                pair.add(initialPlayer);
                TournamentPairing pairing = new TournamentPairing(activeRound, pair);
                activePairings.add(pairing);
            }
        }

        unpairedPlayers.addAll(players);
        return unpairedPlayers;
    }

    private List<TournamentPlayer> pairSwissGroup(List<TournamentPlayer> players) {
        // Simple pairing algorithm doesn't account for repairings
        ArrayList<TournamentPlayer> pair = new ArrayList<>();
        List<TournamentPlayer> unpairable = new ArrayList<>();

        for (TournamentPlayer player : players) {
            pair.add(player);

            if (pair.size() == this.playersInPairing) {
                TournamentPairing pairing = new TournamentPairing(activeRound, pair);
                activePairings.add(pairing);
                pair = new ArrayList<>();
            }
        }

        if (!pair.isEmpty())
            unpairable.addAll(pair);

        return unpairable;
    }



    @Override
    public boolean reportMatchCompletion(TournamentPairing pairing) {
        // Returns whether there are more matches left in this round
        finishMatch(pairing);

        List<Integer> oppIndexes = new ArrayList<>();

        for (TournamentPlayer tp : pairing.getPairedPlayers()) {
            if (pairing.isBye()) {
                tp.addBye();
            } else {
                oppIndexes.add(tp.getIndex());
                if (!tp.equals(pairing.getWinner())) {
                    tp.addLoss();
                } else {
                    tp.addWin();
                }
            }
        }

        for (TournamentPlayer tp : pairing.getPairedPlayers()) {
            for(Integer i : oppIndexes) {
                if (i != null && !i.equals(tp.getIndex())) {
                    tp.addOpponentIndex(i);
                }
            }
        }

        if (activePairings.isEmpty()) {
            completeRound();
            return false;
        }
        return true;
    }


    @Override
    public void endTournament() {
        this.activePairings.clear();
    }
}

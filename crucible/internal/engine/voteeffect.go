package engine

//enginelint:allow id ability game control effecthelpers card condition additional zone valid defined player parts

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// voteEffect is VoteEffect.java: starting with the activator, each Defined$
// or targeted player votes once for one option -- a Choices$ ability, a
// VoteCard$ card in Zone$ (default Battlefield), or a VotePlayer$ player
// (Other: anyone but the voter). Then, with EachVote$, each Choices$
// ability resolves once per vote for it, with that voter remembered.
// Otherwise the most-voted options win: a tie with VoteTiedAbility$ runs
// that instead; VoteSubAbility$ runs with the winners remembered; plain
// Choices$ runs each winning ability; StoreVoteNum$ with Choices$ runs
// every ability with VoteNum bound to its vote count.
//
// Java groups votes in a hash multimap, so its tie order and EachVote$
// order are arbitrary; this port uses the options' own order. Each player
// votes 1 + AdditionalVote$ times plus up to AdditionalOptionalVote$ more,
// and a ControlVote$ player casts every ballot (Layer 8, RulesMod).
type voteEffect struct{}

// controlVoter is Game.getControlVote: the player holding the latest
// ControlVote$ effect, if any player holds one.
func (g *Game) controlVoter() (PlayerID, bool) {
	best, found := NoPlayer, false
	var bestTS uint64
	for _, p := range g.Players() {
		ts, ok := g.Player(p).Rules.controlVote()
		if ok && (!found || ts > bestTS) {
			best, bestTS, found = p, ts, true
		}
	}
	return best, found
}

type voteOption struct {
	sub    int
	entity EntityID
}

func (voteEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Vote", "Condition", "UpTo"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	choices := additionalAbilities(a.Params, "Choices")
	var options []voteOption
	switch {
	case hasParam(a, "Choices"):
		for i := range choices {
			options = append(options, voteOption{sub: i})
		}
	case hasParam(a, "VoteCard"):
		spec, _ := a.Params.Param("VoteCard")
		zone := Battlefield
		if raw, ok := a.Params.Param("Zone"); ok {
			z, ok := ZoneByName(raw)
			if !ok {
				return fmt.Errorf("engine: Vote: Zone$ %q not resolvable", raw)
			}
			zone = z
		}
		parsed := valid.Parse(spec)
		for _, pid := range g.Players() {
			for _, id := range g.Zone(zone, pid).Cards() {
				if Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
					options = append(options, voteOption{sub: -1, entity: CardEntity(id)})
				}
			}
		}
	case hasParam(a, "VotePlayer"):
		raw, _ := a.Params.Param("VotePlayer")
		if raw == "Other" {
			raw = "Player"
		}
		players, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
		if err != nil {
			return fmt.Errorf("engine: Vote: %w", err)
		}
		for _, p := range players {
			options = append(options, voteOption{sub: -1, entity: PlayerEntity(p)})
		}
	}
	if len(options) == 0 {
		return nil
	}
	storeNum := hasParam(a, "StoreVoteNum")
	if storeNum && !hasParam(a, "Choices") {
		return fmt.Errorf("engine: Vote: StoreVoteNum$ without Choices$ not resolvable yet")
	}
	voters, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Vote: %w", err)
	}
	voters = rotateToFront(voters, a.Controller)
	raw, _ := a.Params.Param("VotePlayer")
	other := raw == "Other"
	controlVoter, controlled := g.controlVoter()
	if controlled && other {
		// VoteEffect.java:93-94 removes the controlling player, not the
		// voter, from a VotePlayer$ Other ballot: the voter could vote for
		// themselves and nobody for the controller. A Forge bug (PORT-8),
		// so the combination fails closed rather than copying it.
		return fmt.Errorf("engine: Vote: ControlVote$ with VotePlayer$ Other not resolvable (VoteEffect.java:93-94)")
	}
	counts := make([]int, len(options))
	var ballots []struct {
		option int
		voter  PlayerID
	}
	for _, p := range voters {
		if g.Player(p).Lost {
			continue
		}
		opts := make([]int, 0, len(options))
		for i, o := range options {
			if other && o.entity == PlayerEntity(p) {
				continue
			}
			opts = append(opts, i)
		}
		if len(opts) == 0 {
			continue
		}
		// VoteEffect.java:89-101: one vote, plus every AdditionalVote$,
		// plus up to every AdditionalOptionalVote$ -- p decides how many of
		// those (asked only when there is a choice to make) -- all cast by
		// the ControlVote$ player when one exists.
		rules := g.Player(p).Rules
		amount := 1 + rules.additionalVotes()
		if optional := rules.additionalOptionalVotes(); optional > 0 {
			extra := controller.ChooseNumber(g, p, a.Source, 0, optional)
			if extra < 0 || extra > optional {
				return fmt.Errorf("engine: Vote: %d additional votes out of range [0, %d]", extra, optional)
			}
			amount += extra
		}
		realVoter := p
		if controlled {
			realVoter = controlVoter
		}
		for range amount {
			pick, err := castVote(g, controller, realVoter, a, choices, options, opts)
			if err != nil {
				return err
			}
			counts[pick]++
			ballots = append(ballots, struct {
				option int
				voter  PlayerID
			}{pick, p})
		}
	}
	resolveSub := func(i int, amounts bool) error {
		child := *a
		if amounts {
			child.Amounts = withAmount(a.Amounts, "VoteNum", counts[i])
		}
		return g.resolveAdditional(&child, controller, choices[options[i].sub])
	}
	if hasParam(a, "EachVote") {
		for _, b := range ballots {
			if options[b.option].sub < 0 {
				return fmt.Errorf("engine: Vote: EachVote$ needs Choices$")
			}
			added := rememberAll(&source.Memory, []EntityID{PlayerEntity(b.voter)})
			if err := resolveSub(b.option, false); err != nil {
				return err
			}
			forgetAll(&source.Memory, added)
		}
		return nil
	}
	if storeNum {
		for i := range options {
			if err := resolveSub(i, true); err != nil {
				return err
			}
		}
		return nil
	}
	most, best := []int{}, 0
	for i, n := range counts {
		switch {
		case n == 0:
		case n > best:
			most, best = []int{i}, n
		case n == best:
			most = append(most, i)
		}
	}
	switch {
	case len(most) > 1 && len(additionalAbilities(a.Params, "VoteTiedAbility")) > 0:
		if err := g.resolveAdditionalKey(a, controller, "VoteTiedAbility"); err != nil {
			return err
		}
	case len(additionalAbilities(a.Params, "VoteSubAbility")) > 0:
		for _, i := range most {
			if options[i].sub >= 0 {
				return fmt.Errorf("engine: Vote: VoteSubAbility$ over Choices$ not resolvable yet")
			}
			source.Memory.Remember(options[i].entity)
		}
		if err := g.resolveAdditionalKey(a, controller, "VoteSubAbility"); err != nil {
			return err
		}
		source.Memory.ClearRemembered()
	case hasParam(a, "Choices"):
		for _, i := range most {
			if err := resolveSub(i, false); err != nil {
				return err
			}
		}
	}
	if hasParam(a, "RememberVotedObjects") {
		for i, n := range counts {
			if n > 0 && options[i].sub < 0 {
				source.Memory.Remember(options[i].entity)
			}
		}
	}
	return nil
}

// castVote asks p for one vote among opts (indices into options).
func castVote(g *Game, controller PlayerController, p PlayerID, a *Ability, choices []compile.SubRef, options []voteOption, opts []int) (int, error) {
	if options[opts[0]].sub >= 0 {
		names := make([]string, len(opts))
		for i, o := range opts {
			names[i] = choices[options[o].sub].SVar
		}
		picked := controller.ChooseAbilitiesForEffect(g, p, a.Source, names, 1)
		if len(picked) != 1 || picked[0] < 0 || picked[0] >= len(opts) {
			return 0, fmt.Errorf("engine: Vote: vote %v out of range", picked)
		}
		return opts[picked[0]], nil
	}
	ents := make([]EntityID, len(opts))
	for i, o := range opts {
		ents[i] = options[o].entity
	}
	chosen := controller.ChooseEntitiesForEffect(g, p, a.Source, ents, 1, 1)
	if err := checkChoice(chosen, ents, 1, 1); err != nil {
		return 0, fmt.Errorf("engine: Vote: %w", err)
	}
	for i, e := range ents {
		if e == chosen[0] {
			return opts[i], nil
		}
	}
	return 0, fmt.Errorf("engine: Vote: vote not an option")
}

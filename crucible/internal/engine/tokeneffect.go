package engine

//enginelint:allow ability action animate card condition control defined effecthelpers game id parts player token valid zone changecombatantseffect

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// tokenUnresolvedParams are TokenEffect/TokenEffectBase params this port
// does not model (TokenAttacking$ and TokenBlocking$ resolve,
// addToCombatAttacking and addToCombatBlocking): a counter table on entry
// (WithCountersType$), copied triggers (AddTriggersFrom$), chosen-type/-color
// prototypes (TokenTypes$/TokenColors$), what the token remembers
// (TokenRemembered$/CleanupForEach$/RememberOriginalTokens$), a shared zone
// table (ChangeZoneTable$) and the end-of-turn delayed sacrifice/exile
// (AtEOT$/AtEOTTrig$).
var tokenUnresolvedParams = [...]string{
	"WithCountersType",
	"WithCountersAmount", "AddTriggersFrom", "TokenTypes", "TokenColors", "TokenRemembered",
	"CleanupForEach", "RememberOriginalTokens", "ChangeZoneTable", "AtEOT", "AtEOTTrig",
	"Condition"}

// tokenEffect is TokenEffect.java: TokenAmount$ (default 1) of every
// TokenScript$ script for every TokenOwner$ player (getDefinedPlayersOrTargeted:
// TokenOwner$ when named, else the targets, else You; APNAP order),
// created in that order and entered one at a time, then ChangesZoneAll
// once for the batch (triggerChangesZoneAll).
type tokenEffect struct{}

func (tokenEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Token", tokenUnresolvedParams[:]...); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	amount, err := optionalAmount(g, a, "Token", "TokenAmount", 1)
	if err != nil || amount < 1 {
		return err
	}
	scripts, ok := a.Params.Param("TokenScript")
	if !ok {
		return fmt.Errorf("engine: Token: no TokenScript$")
	}
	owners, err := tokenOwners(g, a)
	if err != nil {
		return err
	}
	base := tokenSpec{Tapped: hasParam(a, "TokenTapped")}
	if base.HasPower = hasParam(a, "TokenPower"); base.HasPower {
		if base.Power, err = optionalAmount(g, a, "Token", "TokenPower", 0); err != nil {
			return err
		}
	}
	if base.HasToughness = hasParam(a, "TokenToughness"); base.HasToughness {
		if base.Toughness, err = optionalAmount(g, a, "Token", "TokenToughness", 0); err != nil {
			return err
		}
	}
	pump, err := tokenPumpKeywords(a, "Token")
	if err != nil {
		return err
	}

	var specs []tokenSpec
	for _, owner := range owners {
		if g.Player(owner).Lost {
			continue
		}
		for _, script := range strings.Split(scripts, ",") {
			def, err := tokenScript(g, strings.TrimSpace(script))
			if err != nil {
				return err
			}
			spec := base
			spec.Def, spec.Owner = def, owner
			n := g.tokensReplaced(controller, owner, def, amount)
			for i := 0; i < n; i++ {
				specs = append(specs, spec)
			}
		}
	}
	attachTo := NoCard
	hasAttach := false
	if raw, ok := a.Params.Param("AttachedTo"); ok {
		hasAttach = true
		if attachTo, err = tokenAttachHost(g, a, source, raw); err != nil {
			return err
		}
	}
	var created []CardID
	for _, spec := range specs {
		// CR 303.4i: an Aura token that cannot be attached to its host is not
		// created (TokenEffectBase.java:136).
		if hasAttach && attachTo != NoCard && !tokenCanAttach(g, a, spec, attachTo) && isAuraDef(spec.Def) {
			continue
		}
		id := g.createToken(controller, spec)
		created = append(created, id)
		if hasAttach && attachTo != NoCard && tokenCanAttach(g, a, spec, attachTo) && g.Card(id).Zone == Battlefield {
			g.attachTo(controller, id, attachTo)
		}
		if pump != nil {
			g.timestamp++
			g.addAnimate(animateRecord{
				Card: id, Timestamp: g.timestamp, Permanent: pump.permanent, AddKeywords: pump.keywords,
			})
		}
		// TokenEffectBase.java:188: tapped and attacking (CR 506.3b), or
		// blocking, each token as it is made.
		if err := g.addToCombatAttacking(controller, a, id, "TokenAttacking"); err != nil {
			return fmt.Errorf("engine: Token: %w", err)
		}
		if err := g.addToCombatBlocking(controller, a, id, "TokenBlocking"); err != nil {
			return fmt.Errorf("engine: Token: %w", err)
		}
		if hasParam(a, "RememberTokens") {
			source.Memory.Remember(CardEntity(id))
		}
		if hasParam(a, "ImprintTokens") {
			source.Memory.Imprint(id)
		}
		if hasParam(a, "RememberSource") {
			g.Card(id).Memory.Remember(CardEntity(a.Source))
		}
	}
	g.checkChangesZoneAllTriggers(controller, created, None, Battlefield)
	return nil
}

// tokenOwners is getDefinedPlayersOrTargeted(sa, "TokenOwner").
func tokenOwners(g *Game, a *Ability) ([]PlayerID, error) {
	var owners []PlayerID
	if raw, ok := a.Params.Param("TokenOwner"); ok {
		for _, d := range strings.Split(raw, " & ") {
			ps, err := definedPlayers(g, a.Controller, a.Source, d, a.refs())
			if err != nil {
				return nil, fmt.Errorf("engine: Token: %w", err)
			}
			owners = append(owners, ps...)
		}
	} else if hasParam(a, "ValidTgts") {
		for _, e := range a.Targets {
			if pid, ok := e.AsPlayer(); ok {
				owners = append(owners, pid)
			}
		}
	} else {
		owners = []PlayerID{a.Controller}
	}
	return g.inAPNAPOrder(owners), nil
}

// tokenPump is PumpKeywords$ with its PumpDuration$: keywords the created
// token gains, permanently when PumpDuration$ is absent (addPumpUntil
// registers nothing), until end of turn otherwise.
type tokenPump struct {
	keywords  []string
	permanent bool
}

// api names the effect in an error: Token and Clone share this grant
// (TokenEffectBase.addPumpUntil is called from both).
func tokenPumpKeywords(a *Ability, api string) (*tokenPump, error) {
	raw, ok := a.Params.Param("PumpKeywords")
	if !ok {
		return nil, nil
	}
	p := &tokenPump{keywords: strings.Split(raw, " & "), permanent: true}
	if d, ok := a.Params.Param("PumpDuration"); ok {
		if d == "UntilYourNextTurn" {
			return nil, fmt.Errorf("engine: %s: PumpDuration$ %q not resolvable yet", api, d)
		}
		p.permanent = false
	}
	return p, nil
}

// tokenAttachHost is the first card AttachedTo$ names (attachTokenTo reads the
// first defined entity); NoCard when it names none. A player host (an Aura on
// a player) is not resolvable here.
func tokenAttachHost(g *Game, a *Ability, source *Card, raw string) (CardID, error) {
	objects, err := definedEntities(g, a.Controller, source, raw, a.refs())
	if err != nil {
		return NoCard, fmt.Errorf("engine: Token: AttachedTo$: %w", err)
	}
	if len(objects) == 0 {
		return NoCard, nil
	}
	id, ok := objects[0].AsCard()
	if !ok {
		return NoCard, fmt.Errorf("engine: Token: AttachedTo$ %q names a player, not resolvable yet", raw)
	}
	return id, nil
}

// isAuraDef reports whether def is an Aura.
func isAuraDef(def *compile.Card) bool {
	return def != nil && def.Faces[0].Type.HasSubtype("Aura")
}

// tokenCanAttach is attachTokenTo's check: the token is an attachment, and an
// Aura's Enchant restriction and the host's protection allow it.
func tokenCanAttach(g *Game, a *Ability, spec tokenSpec, host CardID) bool {
	def := spec.Def
	if def == nil {
		return false
	}
	t := def.Faces[0].Type
	if !t.HasSubtype("Aura") && !t.HasSubtype("Equipment") && !t.HasSubtype("Fortification") {
		return false
	}
	if !t.HasSubtype("Aura") {
		return true
	}
	h := g.Card(host)
	if h.Zone != Battlefield {
		return false
	}
	if restriction, ok := enchantSpecOf(def); ok && !Matches(g, h, restriction, spec.Owner, a.Source) {
		return false
	}
	return true
}

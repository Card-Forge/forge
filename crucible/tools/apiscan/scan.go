// Params of every ability API, recovered from Forge's own call sites.

package main

import (
	"bufio"
	"errors"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// errUnread is returned by check when the corpus writes a param key that
// nothing reads and nothing excludes. The message is the report.
var errUnread = errors.New("unread param keys")

// exclusionRow matches one row of the deliberate-exclusion table in
// parity-matrix.md. Columns are item, kind, reason, revisit; only the first two
// are read, and only rows whose kind is the param-key vocabulary count.
var exclusionRow = regexp.MustCompile(
	"^\\|\\s*`?([A-Za-z0-9_]+)`?\\s*\\|\\s*([^|]*?)\\s*\\|")

// paramRead matches every way a param is read: the accessors on the ability,
// and the raw map lookups AbilityFactory and the handlers do before an ability
// object exists. The key is always a literal.
var paramRead = regexp.MustCompile(`(?:` +
	// The accessors on an ability.
	`getParam|hasParam|getParamOrDefault` +
	// The raw map, read before an ability object exists.
	`|(?:mapParams|params|map)\.(?:get|containsKey)` +
	`)\("([A-Za-z0-9_]+)"`)

// helperRead matches the helpers that take the param *name* as an argument
// rather than its value, so the key never appears next to getParam at all.
// `addToCombat(moved, sa, "TokenAttacking", "TokenBlocking")` is two params in
// one call.
var helperRead = regexp.MustCompile(
	`(?:getDefined[A-Za-z]*OrTargeted|getTargetedOrDefined[A-Za-z]*|addToCombat)\([^)]*?"([A-Za-z0-9_]+)"(?:\s*,\s*"([A-Za-z0-9_]+)")?`)

// keyVariable matches a param name bound to a variable before it is used, which
// is how AbilityFactory reads `Choices` and `ResultSubAbilities`.
var keyVariable = regexp.MustCompile(`\bString\s+key\s*=\s*"([A-Za-z0-9_]+)"`)

// validParamRead matches the triggers and replacements, which check a param
// against the event's own objects rather than reading it off the ability:
// `matchesValidParam("ValidExplorer", runParams.get(...))`.
var validParamRead = regexp.MustCompile(`matchesValidParam\(\s*"([A-Za-z0-9_]+)"`)

// additionalKeys captures the string literals of AbilityFactory's
// additionalAbilityKeys list, which is the one place a param name is data
// rather than a call site.
var additionalKeys = regexp.MustCompile(`(?s)additionalAbilityKeys\s*=\s*Lists\.newArrayList\((.*?)\);`)

// effectSuper matches an effect class's superclass, so a key read in
// TokenEffectBase counts as TokenEffect's own rather than going missing.
var effectSuper = regexp.MustCompile(`class\s+\w+\s+extends\s+(\w+)`)

// apiConstant matches one row of the ApiType enum: the API name and the effect
// class that implements it.
var apiConstant = regexp.MustCompile(`^\s{4}([A-Za-z][A-Za-z0-9_]*)\s*\((([A-Za-z0-9_]+)Effect)\.class`)

// sharedRoots are scanned whole for param reads. A key read anywhere in the
// rules engine is a key the language defines, whichever class happens to read
// it: the restriction, condition and targeting objects pull params off the same
// map the effect does, and the trigger and replacement handlers pull more.
//
// Narrowing this to a handful of base classes reports 326 keys as unknown that
// Forge reads perfectly well, which is a scan finding its own blind spot rather
// than a finding about the corpus.
var sharedRoots = []string{
	"forge-game/src/main/java/forge/game",
	"forge-ai/src/main/java/forge/ai",
	// The human controller reads params of its own -- prompts and titles the
	// engine never touches.
	"forge-gui/src/main/java/forge/player",
}

// effectsDir holds one class per ability API. It sits inside the first shared
// root, and is excluded from that walk: a key only its own effect reads is
// that API's vocabulary, not the language's, and folding the two together is
// what made every per-API set come out empty.
const effectsDir = "forge-game/src/main/java/forge/game/ability/effects"

// recordKeys lead a param map and say what the line is. They are structure, not
// parameters, and no code reads them with getParam.
var recordKeys = map[string]bool{
	"SP": true, "AB": true, "DB": true, "ST": true, "RE": true,
	"Mode": true, "Event": true,
}

func run(forge string, perAPI bool) error {
	apis, shared, err := readVocabulary(forge, perAPI)
	if err != nil {
		return err
	}

	// bufio.Writer keeps the first write error and returns it from Flush, so
	// the per-line results are dropped and the flush is what reports.
	out := bufio.NewWriterSize(os.Stdout, 1<<20)

	for _, key := range sorted(shared) {
		_, _ = fmt.Fprintf(out, "shared\t%s\n", key)
	}

	effects := filepath.Join(forge, effectsDir)
	for _, api := range apis {
		keys, err := readEffectParams(effects, api.class)
		if err != nil {
			// An API whose class lives elsewhere is reported, not guessed at.
			_, _ = fmt.Fprintf(out, "missing\t%s\t%s\n", api.name, api.class)
			continue
		}
		own := make([]string, 0, len(keys))
		for key := range keys {
			if !shared[key] {
				own = append(own, key)
			}
		}
		sort.Strings(own)
		_, _ = fmt.Fprintf(out, "api\t%s\t%s\t%s\n", api.name, api.class, strings.Join(own, ","))
	}
	return out.Flush()
}

// readVocabulary returns the API list and every param key Forge reads anywhere.
func readVocabulary(forge string, perAPI bool) ([]api, map[string]bool, error) {
	apis, err := readAPIs(filepath.Join(forge, "forge-game/src/main/java/forge/game/ability/ApiType.java"))
	if err != nil {
		return nil, nil, err
	}

	shared := map[string]bool{}
	for _, root := range sharedRoots {
		if err := filepath.WalkDir(filepath.Join(forge, root), func(path string, d fs.DirEntry, err error) error {
			if err != nil {
				return err
			}
			if d.IsDir() {
				if perAPI && filepath.Clean(path) == filepath.Clean(filepath.Join(forge, effectsDir)) {
					return filepath.SkipDir
				}
				return nil
			}
			if filepath.Ext(path) != ".java" {
				return nil
			}
			keys, err := readParams(path)
			if err != nil {
				return err
			}
			for key := range keys {
				shared[key] = true
			}
			return nil
		}); err != nil {
			return nil, nil, err
		}
	}
	for key := range recordKeys {
		shared[key] = true
	}
	extra, err := readAdditionalKeys(filepath.Join(forge,
		"forge-game/src/main/java/forge/game/ability/AbilityFactory.java"))
	if err != nil {
		return nil, nil, err
	}
	for _, key := range extra {
		shared[key] = true
	}
	return apis, shared, nil
}

type api struct{ name, class string }

func readAPIs(path string) ([]api, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	var out []api
	for _, line := range strings.Split(string(raw), "\n") {
		if m := apiConstant.FindStringSubmatch(line); m != nil {
			out = append(out, api{name: m[1], class: m[2]})
		}
	}
	return out, nil
}

// readAdditionalKeys reads the additionalAbilityKeys list, whose entries name
// params no call site mentions by name.
func readAdditionalKeys(path string) ([]string, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	m := additionalKeys.FindStringSubmatch(string(raw))
	if m == nil {
		return nil, fmt.Errorf("%s: additionalAbilityKeys not found", path)
	}
	var out []string
	for _, quoted := range regexp.MustCompile(`"([A-Za-z0-9_]+)"`).FindAllStringSubmatch(m[1], -1) {
		out = append(out, quoted[1])
	}
	return out, nil
}

func readParams(path string) (map[string]bool, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	keys := map[string]bool{}
	addParams(keys, string(raw))
	return keys, nil
}

// addParams adds every param key src reads to keys.
func addParams(keys map[string]bool, src string) {
	for _, re := range []*regexp.Regexp{paramRead, helperRead, keyVariable, validParamRead} {
		for _, m := range re.FindAllStringSubmatch(src, -1) {
			for _, key := range m[1:] {
				if key != "" {
					keys[key] = true
				}
			}
		}
	}
}

func sorted(set map[string]bool) []string {
	out := make([]string, 0, len(set))
	for key := range set {
		out = append(out, key)
	}
	sort.Strings(out)
	return out
}

// use is one param key written by one card, with the line that writes it.
type use struct{ key, card, api, line string }

// check compiles the whole corpus and reports every param key no Java code
// reads. Forge's param map is a TreeMap(CASE_INSENSITIVE_ORDER), so keys are
// compared case-insensitively here for the same reason.
func check(forge, corpus, types, allow string, perAPI bool) error {
	apis, shared, err := readVocabulary(forge, perAPI)
	if err != nil {
		return err
	}
	own, err := readOwnParams(forge, apis, shared)
	if err != nil {
		return err
	}
	known := map[string]bool{}
	for key := range shared {
		known[strings.ToLower(key)] = true
	}

	reg, err := loadTypes(types)
	if err != nil {
		return err
	}

	var uses []use
	cards := 0
	if err := filepath.WalkDir(corpus, func(path string, d fs.DirEntry, err error) error {
		if err != nil || d.IsDir() || filepath.Ext(path) != ".txt" {
			return err
		}
		raw, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		name := strings.TrimSuffix(filepath.Base(path), ".txt")
		card, err := carddb.ParseScript(reg, name, raw)
		if err != nil {
			return fmt.Errorf("%s: %w", path, err)
		}
		out, err := compile.Compile(card)
		if err != nil {
			return fmt.Errorf("%s: %w", path, err)
		}
		cards++
		for i := range out.Faces {
			for _, group := range [][]*compile.Ability{
				out.Faces[i].Abilities, out.Faces[i].Triggers,
				out.Faces[i].Statics, out.Faces[i].Replacements,
			} {
				for _, a := range group {
					collect(a, name, known, own, &uses)
				}
			}
		}
		return nil
	}); err != nil {
		return err
	}

	excluded, err := readExclusions(allow)
	if err != nil {
		return err
	}

	sort.Slice(uses, func(i, j int) bool {
		if uses[i].key != uses[j].key {
			return uses[i].key < uses[j].key
		}
		return uses[i].card < uses[j].card
	})

	unread := 0
	for _, u := range uses {
		if excluded[strings.ToLower(u.key)] {
			continue
		}
		unread++
		fmt.Fprintf(os.Stderr, "%s: %s writes %s$, which nothing reads\n  %s\n", u.card, u.api, u.key, u.line)
	}
	scope := "no Java code reads"
	if perAPI {
		scope = "the named effect does not read"
	}
	if unread > 0 {
		return fmt.Errorf("%w: %d uses of keys %s, over %d cards", errUnread, unread, scope, cards)
	}
	if len(uses) == 0 {
		fmt.Printf("apiscan: %d cards, no param key %s\n", cards, scope)
	} else {
		fmt.Printf("apiscan: %d cards, no param key %s outside the %d excluded\n", cards, scope, len(uses))
	}
	return nil
}

// collect walks an ability and its sub-abilities, appending every param key the
// API does not read.
func collect(a *compile.Ability, card string, known map[string]bool, own map[string]map[string]bool, uses *[]use) {
	if set, ok := own[a.Name]; ok {
		for _, p := range a.Params {
			key := strings.ToLower(p.Key)
			if known[key] || set[key] {
				continue
			}
			*uses = append(*uses, use{key: p.Key, card: card, api: a.Name, line: format(a)})
		}
	}
	for _, s := range a.Subs {
		collect(s.Ability, card, known, own, uses)
	}
}

// format rebuilds the script line an ability came from, so a report names the
// text the author would search for. The record key is the ability's first
// param, so the line needs no prefix of its own.
func format(a *compile.Ability) string {
	var b strings.Builder
	for i, p := range a.Params {
		if i > 0 {
			b.WriteString(" | ")
		}
		b.WriteString(p.Key)
		b.WriteString("$ ")
		b.WriteString(p.Value)
	}
	return b.String()
}

// readOwnParams returns, per API, the keys its effect class reads that the
// shared vocabulary does not already cover.
func readOwnParams(forge string, apis []api, shared map[string]bool) (map[string]map[string]bool, error) {
	effects := filepath.Join(forge, effectsDir)
	out := make(map[string]map[string]bool, len(apis))
	for _, a := range apis {
		set := map[string]bool{}
		keys, err := readEffectParams(effects, a.class)
		if err == nil {
			for key := range keys {
				if !shared[key] {
					set[strings.ToLower(key)] = true
				}
			}
		}
		out[a.name] = set
	}
	return out, nil
}

func loadTypes(path string) (*cardtype.Registry, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = f.Close() }()
	return cardtype.LoadRegistry(f)
}

// readExclusions reads the deliberate-exclusion table from parity-matrix.md.
// The table is the allowlist: a key listed there fails no gate, and a key not
// listed there fails this one.
func readExclusions(path string) (map[string]bool, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	out := map[string]bool{}
	for _, line := range strings.Split(string(raw), "\n") {
		m := exclusionRow.FindStringSubmatch(line)
		if m == nil || !strings.EqualFold(strings.TrimSpace(m[2]), "Ability param key") {
			continue
		}
		out[strings.ToLower(m[1])] = true
	}
	return out, nil
}

// readEffectParams reads one effect class and every ancestor of it that also
// lives in the effects directory. TokenEffect reads half its params through
// TokenEffectBase, and an API whose vocabulary stops at its own file is an API
// reported as not reading params it plainly reads.
func readEffectParams(effects, class string) (map[string]bool, error) {
	start := class
	keys, err := readParams(filepath.Join(effects, class+".java"))
	if err != nil {
		return nil, err
	}
	// Bounded rather than while(true): a malformed or cyclic extends chain
	// must not hang the build.
	for depth := 0; depth < 8; depth++ {
		raw, err := os.ReadFile(filepath.Join(effects, class+".java"))
		if err != nil {
			break
		}
		m := effectSuper.FindStringSubmatch(string(raw))
		if m == nil {
			break
		}
		class = m[1]
		parent, err := readParams(filepath.Join(effects, class+".java"))
		if err != nil {
			// The chain leaves the effects directory at SpellAbilityEffect,
			// which is shared and already counted.
			break
		}
		for key := range parent {
			keys[key] = true
		}
	}
	addHelperParams(effects, start, keys)
	return keys, nil
}

// staticCall matches a call to a static method of another class,
// `TokenEffectBase.addPumpUntil(sa, ...)`: the class and the method name.
var staticCall = regexp.MustCompile(`\b([A-Z][A-Za-z0-9_]*)\.([a-z][A-Za-z0-9_]*)\s*\(`)

// addHelperParams adds the params read by every static method of another
// effect class that class's own file calls. CloneEffect is no TokenEffectBase,
// yet reads PumpDuration$ through TokenEffectBase.addPumpUntil(sa, ...); the
// superclass chain alone reports it as a param Clone never reads. Only the
// called method's own body counts, never the rest of its class, and only one
// level deep: a helper's own helpers are not followed.
func addHelperParams(effects, class string, keys map[string]bool) {
	raw, err := os.ReadFile(filepath.Join(effects, class+".java"))
	if err != nil {
		return
	}
	for _, m := range staticCall.FindAllStringSubmatch(string(raw), -1) {
		if m[1] == class {
			continue
		}
		other, err := os.ReadFile(filepath.Join(effects, m[1]+".java"))
		if err != nil {
			continue
		}
		if body, ok := staticMethodBody(string(other), m[2]); ok {
			addParams(keys, body)
		}
	}
}

// staticMethodBody returns the body of the static method name declared in
// src, braces included, by counting braces from the declaration's first one.
// Braces inside string or character literals would miscount; a miscount only
// widens what counts as read, never narrows it.
func staticMethodBody(src, name string) (string, bool) {
	decl, err := regexp.Compile(`\bstatic\b[^;{=]*\b` + regexp.QuoteMeta(name) + `\s*\(`)
	if err != nil {
		return "", false
	}
	loc := decl.FindStringIndex(src)
	if loc == nil {
		return "", false
	}
	open := strings.IndexByte(src[loc[1]:], '{')
	if open < 0 {
		return "", false
	}
	start := loc[1] + open
	depth := 0
	for i := start; i < len(src); i++ {
		switch src[i] {
		case '{':
			depth++
		case '}':
			depth--
			if depth == 0 {
				return src[start : i+1], true
			}
		}
	}
	return "", false
}

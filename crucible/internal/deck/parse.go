// Ported from forge-core/src/main/java/forge/util/FileSection.java
// (parseSections), forge/deck/Deck.java (loadDeckSections) and
// forge/deck/CardPool.java (processCardList).
// Deviations recorded in docs/crucible/porting/port-log/deck-serializer.md.

package deck

import (
	"strconv"
	"strings"
)

// metadataHeader is the section holding `Key=Value` lines rather than cards.
const metadataHeader = "metadata"

// Parse reads a `.dck` file.
//
// The format is sections introduced by `[Name]`, with `[metadata]` holding
// `Key=Value` and every other known section holding card lines. A header that
// is not a deck section is recorded and skipped, which is what Forge does with
// the `[quest]`, `[shop]` and `[duel]` blocks its own tooling writes.
//
// A card line is an optional count, then the card request:
//
//	4 Lightning Bolt|2ED|1
//	Sol Ring
//
// Lines beginning with `;` or `#` are comments. Anything else that does not fit
// is skipped rather than rejected, which is why Parse returns no error: Java's
// card-line regex simply fails to match, and its metadata parser turns a line
// with no `=` into a key with an empty value. A decklist Forge opens must not
// be one Crucible refuses.
func Parse(name string, content []byte) *Deck {
	d := &Deck{
		Name:     name,
		Metadata: map[string]string{},
		Sections: map[Section][]Entry{},
	}

	var (
		current Section
		inKnown bool
		inMeta  bool
	)
	for _, raw := range strings.Split(string(content), "\n") {
		line := strings.TrimSpace(strings.TrimRight(raw, "\r"))
		if line == "" || line[0] == ';' || line[0] == '#' {
			continue
		}

		if header, ok := cutHeader(line); ok {
			inMeta = strings.EqualFold(header, metadataHeader)
			current, inKnown = SectionFromHeader(header)
			if !inMeta && !inKnown && !containsFold(d.UnknownSections, header) {
				d.UnknownSections = append(d.UnknownSections, header)
			}
			continue
		}

		switch {
		case inMeta:
			// A line with no '=' becomes a key with an empty value, which is
			// what FileSection.parse does. 1,945 decklists rely on it: their
			// Description spans several lines, and every line after the first
			// has no separator.
			key, value, _ := strings.Cut(line, "=")
			key = strings.TrimSpace(key)
			d.Metadata[key] = strings.TrimSpace(value)
			if strings.EqualFold(key, "Name") {
				d.Name = strings.TrimSpace(value)
			}
		case inKnown:
			entry, ok := parseEntry(line)
			if !ok {
				continue
			}
			d.Sections[current] = append(d.Sections[current], entry)
		}
	}
	return d
}

// cutHeader returns the text inside a `[...]` line.
func cutHeader(line string) (string, bool) {
	if len(line) >= 2 && line[0] == '[' && line[len(line)-1] == ']' {
		return line[1 : len(line)-1], true
	}
	return "", false
}

// parseEntry reads one card line. The count is optional and defaults to one,
// matching Java's `((\d+)\s+)?(.*?)` with a blank request rejected.
func parseEntry(line string) (Entry, bool) {
	count := 1
	request := line
	if digits, rest, ok := cutCount(line); ok {
		n, err := strconv.Atoi(digits)
		if err != nil {
			return Entry{}, false
		}
		count, request = n, rest
	}

	request = strings.TrimSpace(request)
	if request == "" {
		return Entry{}, false
	}

	name, rest, _ := strings.Cut(request, "|")
	edition, extra, _ := strings.Cut(rest, "|")
	return Entry{
		Count:   count,
		Name:    strings.TrimSpace(name),
		Edition: strings.TrimSpace(edition),
		Extra:   strings.TrimSpace(extra),
	}, true
}

// cutCount splits a leading run of digits followed by whitespace.
func cutCount(line string) (digits, rest string, ok bool) {
	i := 0
	for i < len(line) && line[i] >= '0' && line[i] <= '9' {
		i++
	}
	if i == 0 || i == len(line) || (line[i] != ' ' && line[i] != '\t') {
		return "", "", false
	}
	return line[:i], line[i:], true
}

func containsFold(xs []string, want string) bool {
	for _, x := range xs {
		if strings.EqualFold(x, want) {
			return true
		}
	}
	return false
}

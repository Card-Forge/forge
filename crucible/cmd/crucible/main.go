// Command crucible is the tool's entry point. Today it has one subcommand.
//
//	crucible corpus-coverage -decks <path>...
//
// Coverage is what ADR-0011 scopes the port by: a deck is playable when every
// card in it is supported, and "supported" is a moving target that starts here
// as "the card database has it". Effect-level support arrives at M6, and this
// command is where that check will live.
package main

import (
	"fmt"
	"os"
)

func main() {
	if len(os.Args) < 2 {
		usage()
		os.Exit(2)
	}

	var err error
	switch os.Args[1] {
	case "corpus-coverage":
		err = corpusCoverage(os.Args[2:])
	case "-h", "--help", "help":
		usage()
		return
	default:
		fmt.Fprintf(os.Stderr, "crucible: unknown command %q\n", os.Args[1])
		usage()
		os.Exit(2)
	}

	if err != nil {
		fmt.Fprintf(os.Stderr, "crucible: %v\n", err)
		os.Exit(1)
	}
}

func usage() {
	fmt.Fprint(os.Stderr, `usage: crucible <command> [flags]

commands:
  corpus-coverage   report which cards in a decklist the card database has
`)
}

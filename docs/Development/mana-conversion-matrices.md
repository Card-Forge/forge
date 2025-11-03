Spend as other mana: The easiest way would probably be to intercept mana costs before they are paid and replace them with different mana costs (so it only changes what you're asked to pay).

Effects are:
Celestial Dawn: You may spend white mana as though it were mana of any color. You may spend other mana only as though it were colorless mana. 
False Dawn: Until end of turn, you may spend white mana as though it were mana of any color.
Mycosynth Lattice: Players may spend mana as though it were mana of any color. 
North Star: For one spell this turn, you may spend mana as though it were mana of any color to pay that spell's mana cost.
Quicksilver Elemental: You may spend blue mana as though it were mana of any color to pay the activation costs of Quicksilver Elemental's abilities.
Sunglasses of Urza: You may spend white mana as though it were red mana. 

Generic mana costs can be paid by any mana... so that's not relevant to any of the conversion matrices.

The left side of the matrix is Mana in your ManaPool and the top of the matrix is ManaCosts.

For Colored or Colorless specific costs the tables will look like:

Identity Matrix

| | W | U | B | R | G | C |
| - | - | - | - | - | - | - |
W| X | O | O | O | O | O |
U| O | X | O | O | O | O |
B| O | O | X | O | O | O |
R| O | O | O | X | O | O |
G| O | O | O | O | X | O |
C| O | O | O | O | O | X |

By default, there are no restrictions on what mana can pay for what. So this gives us a restriction MatriX | Of

| | W | U | B | R | G | C |
| - | - | - | - | - | - | - |
W| X | X | X | X | X | X |
U| X | X | X | X | X | X |
B| X | X | X | X | X | X |
R| X | X | X | X | X | X |
G| X | X | X | X | X | X |
C| X | X | X | X | X | X |

Since these are the default matrices, we take too mutable matrices. An additive matrix (that starts out as the identity and ORs any modifiers to itself) and a restriction matrix that starts as completely unrestricted and ANDs any restrictions to itself. 

Once all of the modifiers have been assigned you take the additive matrix AND it to the restriction matrix and that's how you determine what Mana you have can pay for a Mana Cost.


Celestial Dawn modifies the table to look like this:

| | W | U | B | R | G | C |
| - | - | - | - | - | - | - |
W| X | X | X | X | X | O |
U| O | O | O | O | O | X |
B| O | O | O | O | O | X |
R| O | O | O | O | O | X |
G| O | O | O | O | O | X |
C| O | O | O | O | O | X |

The False Dawn table looks like this:

| | W | U | B | R | G | C |
| - | - | - | - | - | - | - |
W| X | X | X | X | X | O |
U| O | X | O | O | O | O |
B| O | O | X | O | O | O |
R| O | O | O | X | O | O |
G| O | O | O | O | X | O |
C| O | O | O | O | O | X |

Mycosynth Lattice

| | W | U | B | R | G | C |
| - | - | - | - | - | - | - |
W| X | X | X | X | X | O |
U| X | X | X | X | X | O |
B| X | X | X | X | X | O |
R| X | X | X | X | X | O |
G| X | X | X | X | X | O |
C| X | X | X | X | X | X |

North Star (although specific tO | One spell, so might be better in a different location)

| | W | U | B | R | G | C |
| - | - | - | - | - | - | - |
W| X | X | X | X | X | X |
U| X | X | X | X | X | X |
B| X | X | X | X | X | X |
R| X | X | X | X | X | X |
G| X | X | X | X | X | X |
C| X | X | X | X | X | X |


Quicksilver Elemental, the table is similar to False Dawn:

| | W | U | B | R | G | C |
| - | - | - | - | - | - | - |
W| X | O | O | O | O | O |
U| X | X | X | X | X | X |
B| O | O | X | O | O | O |
R| O | O | O | X | O | O |
G| O | O | O | O | X | O |
C| O | O | O | O | O | X |


Sunglasses of Urza

| | W | U | B | R | G | C |
| - | - | - | - | - | - | - |
W| X | O | O | X | O | O |
U| O | X | O | O | O | O |
B| O | O | X | O | O | O |
R| O | O | O | X | O | O |
G| O | O | O | O | X | O |
C| O | O | O | O | O | X |



RECAP:

1. Start with original matrix (each color/colorless being able to pay for itself)
2. OR all of the positive allowances to the matrix (which is all of them except for the restriction in Celestial Dawn)
3. AND that to the payment restriction (just second half of Celestial Dawn)
4. Apply the payment matrix when trying to pay mana.
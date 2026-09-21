# Railway interlocking design

## Model choice

The implementation follows a coloured Petri-net model. A train token contains its name, route and current route index. Each section place has capacity one. A movement transition consumes a train token from its current section and a free-capacity token for the next section, then returns the old section's free-capacity token.

## Valid routes

| Direction and type | Entry | Destination | Route |
| --- | ---: | ---: | --- |
| Southbound passenger | 1 | 8 | 1, 5, 8 |
| Southbound passenger | 1 | 9 | 1, 5, 9 |
| Northbound passenger | 9 | 2 | 9, 6, 2 |
| Northbound passenger | 10 | 2 | 10, 6, 2 |
| Southbound freight | 3 | 4 | 3, 4 |
| Southbound freight | 3 | 11 | 3, 7, 11 |
| Northbound freight | 4 | 3 | 4, 3 |
| Northbound freight | 11 | 3 | 11, 7, 3 |

## Safety rules

1. Sections 1 to 11 are one-safe: one section can hold at most one train.
2. Freight routes remain on sections 3, 4, 7 and 11. Passenger routes remain on sections 1, 2, 5, 6, 8, 9 and 10.
3. Movement is planned from a snapshot and applied atomically. A train may enter a section vacated in the same step only when the approved movements form a safe forward chain.
4. Opposing edge swaps and competing moves into one section are rejected.
5. Passenger movements 1 to 5 and 6 to 2 have priority over freight branch movements 3 to 4 and 4 to 3 at the western crossing.
6. Opposing use of the eastern 5-9-6 junction is serialised. The train already leaving section 9 is preferred because this releases the shared section.

## Progress and fairness

When two otherwise safe movements compete for one section, the train that has waited for more requested movement rounds is selected first. Request order breaks a remaining tie. The passenger-priority rule at the western crossing remains absolute because the brief specifies that passenger trains should always have priority there.

## Interface decisions

- All move arguments are validated before state changes, so an invalid name cannot cause a partial movement.
- Repeated names in one movement request are treated as one request and cannot move a train multiple sections.
- A train exits on the movement request after it reaches its destination section.
- Names remain reserved after exit so `getTrain` can continue to return `-1` for a known completed train.
- Null or empty train names and null movement arrays are rejected with `IllegalArgumentException`.

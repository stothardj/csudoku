# sudoku

A Clojure library designed to solve and generate sudoku boards of arbitrary dimensions.

## Terminolgy

- Square
    A single square in a sudoku board that will contain a single number once solved
- Box
    A group of sudoku squares which have the same uniqueness constraint as a row and column.
    In a standard sized sudoku board a box is 3x3.

## Usage

When in doubt look at how the tests call the functions.

### Basics

1) Width and Height

Before using this sudoku library you must first understand how it refers to the dimensions
of a sudoku board. Many functions require a width and height to be passed into it. The
internal representation does not store these dimensions and it is impossible in general
to derive the width and height from the number of numbers.

The width and height refer to the width and height of a single "box" of the sudoku board. So
a standard sized sudoku board has a width of 3 and a height of 3. It looks something like the
below where the dot represents a sudoku square.

- - - - - - - - - - - - - 
| . . . | . . . | . . . | 
| . . . | . . . | . . . | 
| . . . | . . . | . . . | 
- - - - - - - - - - - - - 
| . . . | . . . | . . . | 
| . . . | . . . | . . . | 
| . . . | . . . | . . . | 
- - - - - - - - - - - - - 
| . . . | . . . | . . . | 
| . . . | . . . | . . . | 
| . . . | . . . | . . . | 
- - - - - - - - - - - - - 

A board with a width of 3 and height of 2 looks like:

- - - - - - - - -
| . . . | . . . |
| . . . | . . . |
- - - - - - - - -
| . . . | . . . |
| . . . | . . . |
- - - - - - - - -
| . . . | . . . |
| . . . | . . . |
- - - - - - - - -

A board with a width of 2 and height of 3 looks like:

- - - - - - - - - -
| . . | . . | . . | 
| . . | . . | . . | 
| . . | . . | . . | 
- - - - - - - - - -
| . . | . . | . . | 
| . . | . . | . . | 
| . . | . . | . . | 
- - - - - - - - - -

From the above we observe that a sudoku board is always square, even in the case
where each box isn't. A board with width w and height h has a total of (w * h)^2
squares. Furthermore it should be obvious that without specifying the width and
height it would be impossible to tell whether 36 numbers should be formatted into
a 2x3 board or a 3x2 board.

2) Representation

Each function is careful to document what format it expects the board in. In general
there are two formats, an external representation and an internal representation. The
external representation is expected to be easier to use for clients of the library, while
the internal representation is used during solving and generating internally.

The external representation is simply a sequence of numbers representing the values in the
sudoku squaures. The numbers start at the top-left and go left-to-right then top-to-bottom
just as one reads English. An open square is represented as the number 0.

The internal representation is a map. The keys are indexes in the board. The values are
sets of numbers which could be in that sudoku square. A square with only one possible
value is said to be certain. It should never be necessary to inspect the internal
representation, you can transform to and from it and therefore use it as a blackbox.
That being said, I will not be changing the internal representation.

### Generating a board

Simply call (generate-board w h) where w and h are width and height respectively. This
generates only one board. The board is truly random and only has one solution.

The library unfortunately doesn't provide a way to quickly generate a bulk of truly random
boards that would be faster than calling generate-board multiple times, so a bulk call
is not provided.

generate-board returns a board in the internal representation. To transform it to a sequence
simply use (board-to-seq w h (generate-board w h))

### Solving a board

Note that solve-board expects an internal representation of the board. So use
(solve-board w h (create-board w h board))
if you have a sequence of numbers to represent the board. This returns a lazy sequence
of all solutions to the sudoku board. The order of solutions is arbitrary. The implementation
simply returns solutions in the order it finds them, something which could easily change if
the implementation is altered.

As a warning I wouldn't recommend trying to find all solutions to an empty standard sized
board unless you have a lot of computer time on your hands.

## License

Copyright © 2014 Jake Stothard

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

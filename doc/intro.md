# Introduction to sudoku

Usage of this library is described in the README. This is for anyone interested in
the implementation.

## Data representation

A sudoku square is represented as a set of possible numbers in that sudoku square. To begin
with a square has only one number if that number was given in the input board, or all numbers
which are allowed on the board. So #{1} is a sudoku square which certainly has the value 1
in it.

An index is represented as a tuple (two item vector) of row followed by column. So [r c].

A sudoku board is a map of indices to sudoku squares. So {[0 0] #{1} ...}.

## Solving

In sudoku we have groups of squares with some restrictions on them.

These restrictions are:
- No number can be in multiple squares in the group
- Every number must be somewhere in the group

It doesn't matter whether we are talking about rows, columns, or boxes. In fact we can
forget about the distinction between these and just always think about them as "groups".

An index is a key, the sudoku square the corresponding value. So when the code refers to
a "key group" it is refering to a sequence of indices with the above requirements. A key group
has a corresponding "value group" which is the sequence of squares you get from using the
keys on the sudoku board map.

Example: 
On a standard size sudoku board a key group would be the first row:
[[0 0] [0 1] [0 2] ... [0 8]]
The corresponding value group would be the sequence of numbers in the first row.

To solve a sudoku board you first need reducing strategies which reduce the set of
possibilities for sudoku squares based on some logic. A reducing strategy takes a value
group and returns a value group, maybe with some some possibilities removed from the
squares. Order of the sequence is preserved so that the values can later be added to the
map. Reducing strategies are clearly composable.

Example:
One reducing strategy is to take all sudoku squares with only one value, then remove
their values from all the other squares. So if one square is #{1} and another is #{1 2}
then we can change that other square to simply be #{2}.

Most boards which only have one solution can actually be solved just by using the main
reducing strategy in sudoku.core. Sometimes it needs to backtrack though. You won't find
any explicit backtracking in the code. There's no stack of possibilities or continuations.
Instead the code relies on a structure something like this:

(first (concat (might-find-solution board1) (lazy-seq (might-find-solution board2))))

The idea is that if you succeed in finding a solution using board1, then the lazy-seq
is never looked and the solution generated from board1 is used. If might-find-solution
with board1 returns nil, then the lazy-seq is realized in order to give a value to first.
In this way I rely on lazy sequences to provide a clean backtracking pattern.

To come up with board1 and board2 above the code will find a square with multiple
possibilties and generate board1 where the square has only one possibilty, and board2
where that possibility is removed. This splitting of the board is only used as a last
resort once the reducing strategy is unable to make progress.

Using reducing strategies and splitting the board, all solutions are found.

## Generating

The first step to generating a board is generating a solved board. This is essentially
finding a random solution to an empty board. Unfortunatly we can't just take the first
solution to calling solve-board on an empty board. solve-board produces an "arbitrary"
order, not a "random" one. To make sure we get a random solution we do two things differently
to what solve-board does here:

- When selecting a square to split the board on we don't use the first uncertain square we
  find. Instead we find all uncertain squares and select one of them at random.
- We don't reorder the results to put solutions we find first at the front. Doing so could
  prefer certain boards.

Otherwise the generate-solved-board code is very similar to the solve-board code.

Once a solved board is generated some squares need to be made open. First randomly
permute all indices. Then go through each on and try setting it to be an open square.
In the code I refer to it as punching a hole into that square, as its removing the number
from there. If doing so means there is still exactly one solution then do it. Otherwise
don't. This means that generating a standard sized sudoku board will require calling
solve-board 81 times! On my laptop this takes from 2.5 to 5 seconds. I've tried chunking
the whole punching and while I can reduce the number of times solve-board is called, it
ends up taking more time as it can get further off-track.
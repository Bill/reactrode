Just getting started here. See the tests.

## TODO

* write a many-generations test and add game state cleanup interface+policy+mechanism for "old" cells
* `GameOfLife.getCells()` "driver" flux `put()`s cells into the game state as a side-effect. Backpressure on that `put()` should suspend the flux.
* make a visualization: maybe an RSocket client for JavaScript or something
* enable BlockHound https://github.com/reactor/BlockHound

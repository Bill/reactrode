import rSocketClient from './rsocket-stuff'

const gridHeight = 400;
const gridWidth = 400;
const canvas = document.getElementById("myCanvas");
const context = canvas.getContext("2d");
context.fillStyle = "#FF0000";
const calculateLocally = false;

// These are non-const so they are swappable
var theGrid = createArray(gridWidth);
var mirrorGrid = createArray(gridWidth);

if (calculateLocally) {
    fillRandom(); //create the starting state for the grid by filling it with random cells
    function tick() { //main loop
        // console.time("loop");
        calculateGridLocally();
        drawGrid();
        // console.timeEnd("loop");
        requestAnimationFrame(tick);
    }
    tick(); //call main loop
} else {
    
    const FRAME_SIZE = gridHeight * gridWidth;
    var subscription; // to support re-requesting
    var requested = 0;
    
    rSocketClient.connect().subscribe(
        {
            onComplete: socket => {

                socket.requestStream({
                                         data: {},
                                         metadata: '/rsocket/all-generations'
                                     })
                    .subscribe(
                        {
                            onComplete: () => console.log('all-generations done'),
                            onError: error => console.error(error),
                            onNext: value => {
                                let y = value.data.coordinates.y;
                                let x = value.data.coordinates.x;
                                if (value.data.isAlive) {
                                    theGrid[y][x] = 1;
                                } else {
                                    theGrid[y][x] = 0;
                                }
                                if(--requested === 0) {
                                    drawGrid();
                                    requested = FRAME_SIZE;
                                    subscription.request(requested);
                                }
                            },
                            // Nothing happens until `request(n)` is called
                            onSubscribe: sub => {
                                console.log("onSubscribe() requesting ")
                                subscription = sub;
                                requested = FRAME_SIZE;
                                subscription.request(requested);
                            },
                        }
                    )
            },
            onError: error => console.error(error),
            onSubscribe: cancel => {/* call cancel() to abort */
            }
        });
}


function createArray(rows) { //creates a 2 dimensional array of required height
    var arr = [];
    for (var i = 0; i < rows; i++) {
        arr[i] = [];
    }
    return arr;
}

function fillRandom() { //fill the grid randomly
    // leave a border so we can see gliders
    for (var j = 100; j < gridHeight - 100; j++) { //iterate through rows
        for (var k = 100; k < gridWidth - 100; k++) { //iterate through columns
            theGrid[j][k] = Math.round(Math.random());
        }
    }
}

function drawGrid() { //draw the contents of the grid onto a canvas
    console.log("drawing")
    context.clearRect(0, 0, gridHeight, gridWidth); //this should clear the canvas ahead of each redraw
    for (var row = 0; row < gridHeight; row++) { //iterate through rows
        for (var col = 0; col < gridWidth; col++) { //iterate through columns
            if (theGrid[row][col] === 1) {
                context.fillRect(row, col, 1, 1);
            }
        }
    }
}

function calculateGridLocally() { //perform one iteration of grid update

    for (var row = 1; row < gridHeight - 1; row++) { //iterate through rows
        for (var col = 1; col < gridWidth - 1; col++) { //iterate through columns
            var totalCells = 0;
            //add up the total values for the surrounding cells
            totalCells += theGrid[row - 1][col - 1]; //top left
            totalCells += theGrid[row - 1][col]; //top center
            totalCells += theGrid[row - 1][col + 1]; //top right

            totalCells += theGrid[row][col - 1]; //middle left
            totalCells += theGrid[row][col + 1]; //middle right

            totalCells += theGrid[row + 1][col - 1]; //bottom left
            totalCells += theGrid[row + 1][col]; //bottom center
            totalCells += theGrid[row + 1][col + 1]; //bottom right

            //apply the rules to each cell
            switch (totalCells) {
                case 2:
                    mirrorGrid[row][col] = theGrid[row][col];

                    break;
                case 3:
                    mirrorGrid[row][col] = 1; //live

                    break;
                default:
                    mirrorGrid[row][col] = 0; //
            }
        }
    }

    //mirror edges to create wraparound effect

    for (var l = 1; l < gridHeight - 1; l++) { //iterate through rows
        //top and bottom
        mirrorGrid[l][0] = mirrorGrid[l][gridHeight - 3];
        mirrorGrid[l][gridHeight - 2] = mirrorGrid[l][1];
        //left and right
        mirrorGrid[0][l] = mirrorGrid[gridHeight - 3][l];
        mirrorGrid[gridHeight - 2][l] = mirrorGrid[1][l];

    }


    //swap grids
    var temp = theGrid;
    theGrid = mirrorGrid;
    mirrorGrid = temp;
}
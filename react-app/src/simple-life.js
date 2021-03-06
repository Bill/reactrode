import rSocketClient from './rsocket-stuff'

const height = 100;
const width = 100;
const frameSize = height * width;
const canvas = document.getElementById("myCanvas");
const context = canvas.getContext("2d");
context.fillStyle = "#FF0000";
const board = createArray(width);

var subscription; // to support re-requesting
var requested = 0;
var frameStartTime = new Date();

function makeRequest() {
    requested = frameSize;
    subscription.request(requested);
}

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
                        onNext: cell => {
                            // console.log(`got cell`);
                            // console.log(cell);
                            let row = cell.data.coordinates.y;
                            let col = cell.data.coordinates.x;
                            board[row][col] = cell.data.isAlive;
                            if (--requested === 0) {
                                draw();
                                makeRequest();
                                updateStatistics(cell.data.coordinates.generation);
                            }
                        },
                        // Nothing happens until `request(n)` is called
                        onSubscribe: sub => {
                            subscription = sub;
                            makeRequest();
                            console.log(`onSubscribe() requested ${frameSize}`)
                        },
                    }
                )
        },
        onError: error => console.error(error),
        onSubscribe: cancel => {/* call cancel() to abort */
        }
    });

function updateStatistics(generation) {
    const frameEndTime = new Date();
    const elapsedMillis = frameEndTime - frameStartTime;
    const framesPerSecond = (1/elapsedMillis)*1000;
    const cellsPerSecond = framesPerSecond * frameSize;
    renderStatistics(generation,framesPerSecond,cellsPerSecond);
    frameStartTime = new Date();
}

function renderStatistics(generation,framesPerSecond, cellsPerSecond) {
    document.getElementById('generation').innerHTML = generation;
    document.getElementById('frames-per-second').innerHTML = framesPerSecond.toFixed(1);
    document.getElementById('cells-per-second').innerHTML = cellsPerSecond.toFixed(0);
}

function createArray(rows) {
    const array = [];
    for (var row = 0; row < rows; ++row) {
        array[row] = [];
    }
    return array;
}

function draw() {
    context.clearRect(0, 0, width, height);
    for (var col = 0; col < width; ++col) {
        for (var row = 0; row < height; ++row) {
            if (board[row][col]) {
                context.fillRect(col, row, 1, 1);
            }
        }
    }
}
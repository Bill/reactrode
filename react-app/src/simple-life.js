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
                            let row = value.data.coordinates.y;
                            let col = value.data.coordinates.x;
                            if (value.data.isAlive) {
                                board[row][col] = 1;
                            } else {
                                board[row][col] = 0;
                            }
                            if (--requested === 0) {
                                draw();
                                requested = frameSize;
                                subscription.request(requested);
                            }
                        },
                        // Nothing happens until `request(n)` is called
                        onSubscribe: sub => {
                            console.log(`onSubscribe() requesting ${frameSize}`)
                            subscription = sub;
                            requested = frameSize;
                            subscription.request(requested);
                        },
                    }
                )
        },
        onError: error => console.error(error),
        onSubscribe: cancel => {/* call cancel() to abort */
        }
    });


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
            if (board[row][col] === 1) {
                context.fillRect(col, row, 1, 1);
            }
        }
    }
}
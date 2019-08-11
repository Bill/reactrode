import {
    RSocketClient,
    JsonSerializer, IdentitySerializer, BufferEncoders,
} from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';
import RSocketTcpClient from 'rsocket-tcp-client'

// Create an instance of a client
const client = new RSocketClient(
    {
        serializers: {
            data: JsonSerializer,
            metadata: IdentitySerializer
        },
        setup: {
            dataMimeType: 'application/json',
            keepAlive: 100000,
            lifetime: 100000,
            metadataMimeType: 'message/x.rsocket.routing.v0',
        },

        // transport: new RSocketWebSocketClient({url: 'ws://localhost:7000'})

        // Transports default to sending/receiving strings:
        // Use BufferEncoders to enable binary

//        transport: new RSocketTcpClient(
//            {host: '127.0.0.1', port: 7000}, // options to Node.js net.connect()
//            BufferEncoders,
//        ),

          transport: new RSocketWebSocketClient({url: 'ws://localhost:62722'})
    });

// const socket = await client.connect();

// socket.requestStream({
//   data: {},
//   metadata: 'all-generations'
// }).subscribe({
//   onSubscribe: cancel => {alert('got subscription!')}
// });

console.log("HERE!")

// Open the connection
client.connect().subscribe(
    {
        onComplete: socket => {
            socket.requestStream({
                                     data: {},
                                     metadata: 'all-generations'
                                 })
                .subscribe(
                    {
                        onComplete: () => console.log('all-generations done'),
                        onError: error => console.error(error),
                        onNext: value => console.log(value),
                        // Nothing happens until `request(n)` is called
                        onSubscribe: sub => sub.request(4),
                    }
                )
        },
        onError: error => console.error(error),
        onSubscribe: cancel => {/* call cancel() to abort */
        }
    });
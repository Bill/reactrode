import {
    RSocketClient,
    JsonSerializer, IdentitySerializer, BufferEncoders,
} from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';

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

          transport: new RSocketWebSocketClient({url: 'ws://localhost:50979'})
    });

// module.exports.client = client
export default client

// const socket = await client.connect();

// socket.requestStream({
//   data: {},
//   metadata: 'all-generations'
// }).subscribe({
//   onSubscribe: cancel => {alert('got subscription!')}
// });


import {
    RSocketClient,
    JsonSerializer, IdentitySerializer,
} from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';

const client = new RSocketClient(
    {
        serializers: {
            data: JsonSerializer,
            metadata: IdentitySerializer
        },
        setup: {
            keepAlive: 30000,
            lifetime: 180000,
            dataMimeType: 'application/json',
            metadataMimeType: 'message/x.rsocket.routing.v0',
        },

          transport: new RSocketWebSocketClient({url: 'ws://localhost:7000/rsocket'})
    });

export default client
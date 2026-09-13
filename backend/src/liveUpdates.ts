import WebSocket, { WebSocketServer } from 'ws';
import type { IncomingMessage } from 'http';
import type { Server } from 'http';

const UPSTREAM_URL = 'wss://8.229.22.124';

let upstreamWs: WebSocket | null = null;
let clientWss: WebSocketServer | null = null;

/** Connect once to the course server and relay every message to all Android clients. */
function connectUpstream() {
  upstreamWs = new WebSocket(UPSTREAM_URL, {
    rejectUnauthorized: false,
  });

  upstreamWs.on('open', () => {
    console.log('[liveUpdate] Connected to upstream WebSocket server');
  });

  upstreamWs.on('message', (data: WebSocket.RawData) => {
    const payload = data.toString();
    if (clientWss) {
      for (const client of clientWss.clients) {
        if (client.readyState === WebSocket.OPEN) {
          client.send(payload);
        }
      }
    }
  });

  upstreamWs.on('close', (code, reason) => {
    console.log(`[liveUpdates] Upstream closed (${code}: ${reason}). Reconnecting in 2 s…`);
    upstreamWs = null;
    setTimeout(connectUpstream, 2000);
  });

  upstreamWs.on('error', (err) => {
    console.error('[liveUpdates] Upstream error:', err.message);
  });
}

/**
 * Attach a WebSocket server to the existing HTTP server on the path /live-updates.
 * Call this once from index.ts after app.listen().
 */
export function attachLiveUpdate(httpServer: Server): void {
  clientWss = new WebSocketServer({ server: httpServer, path: '/live-updates' });

  clientWss.on('connection', (ws: WebSocket, req: IncomingMessage) => {
    console.log(`[liveUpdates] Android client connected from ${req.socket.remoteAddress}`);

    ws.on('close', () => {
      console.log('[liveUpdates] Android client disconnected');
    });

    ws.on('error', (err) => {
      console.error('[liveUpdates] Client socket error:', err.message);
    });
  });

  // Start the upstream connection once relay server ready
  connectUpstream();
}


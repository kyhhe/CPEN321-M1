import express, { type Express } from 'express';
import os from 'os';

export function createApp(): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  // Button 1 APIs
  app.get('/server-ip', (_req, res) => {
    res.json({ ip: process.env.PUBLIC_IP || getLocalIp() });
  });

  app.get('/server-time', (_req, res) => {
    res.json({ time: formatServerTime() });
  });

  app.get('/my-name', (_req, res) => {
    res.json({ 
      firstName: 'Kelly',
      lastName: 'He'
    });
  });

  // Default
  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}

function formatServerTime(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');

  const hh = pad(now.getHours());
  const mm = pad(now.getMinutes());
  const ss = pad(now.getSeconds());

  // GMT offset calculation
  const offsetMin = -now.getTimezoneOffset(); 
  const sign = offsetMin >= 0 ? '+' : '-';
  const offH = pad(Math.floor(Math.abs(offsetMin) / 60));
  const offM = pad(Math.abs(offsetMin) % 60);

  return `${hh}:${mm}:${ss} GMT${sign}${offH}:${offM}`;
}

// Returns IP address of server
function getLocalIp(): string {
  const nets = os.networkInterfaces();
  for (const name of Object.keys(nets)) {
    for (const net of nets[name] ?? []) {
      if (net.family === 'IPv4' && !net.internal) {
        return net.address;
      }
    }
  }
  return 'unknown';
}

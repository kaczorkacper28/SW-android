require('dotenv').config();

const express = require('express');
const cors = require('cors');
const { sendBroadcast, listChannels } = require('./broadcaster');

const app = express();
const PORT = Number(process.env.PORT || 3000);
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1506076988564050081';
const CLIENT_ID = process.env.DISCORD_CLIENT_ID || '1539443070037266572';
const BROADCASTER_ROLE_IDS = (process.env.BROADCASTER_ROLE_IDS || '').split(',').map(x => x.trim()).filter(Boolean);

app.use(cors());
app.use(express.json({ limit: '1mb' }));

const ranks = ['Kandydat','Młodszy Funkcjonariusz','Funkcjonariusz','Starszy Funkcjonariusz','Dowódca Zmiany','Zastępca Naczelnika','Inspektor Generalny'];

const broadcastHistory = [];

function isAuthorized(req) {
  if (BROADCASTER_ROLE_IDS.length === 0) return true;
  const roles = Array.isArray(req.body?.roleIds) ? req.body.roleIds : [];
  return roles.some(id => BROADCASTER_ROLE_IDS.includes(id));
}

app.get('/api/health', (req, res) => res.json({ status: 'ok', service: 'SW Android Backend', guildId: GUILD_ID, clientId: CLIENT_ID }));
app.get('/api/config', (req, res) => res.json({ guildId: GUILD_ID, clientId: CLIENT_ID, ranks }));

app.get('/api/discord/channels', async (req, res) => {
  try {
    const channels = await listChannels(GUILD_ID);
    res.json(channels.filter(c => c.type === 0 || c.type === 5).map(c => ({ id: c.id, name: c.name, type: c.type })));
  } catch (error) {
    res.status(error.status || 500).json({ error: error.message, details: error.details || null });
  }
});

app.post('/api/broadcaster/send', async (req, res) => {
  if (!isAuthorized(req)) return res.status(403).json({ error: 'FORBIDDEN', message: 'Brak uprawnień do Broadcastera.' });
  try {
    const { channelId, title, content, color, imageUrl, mention, authorId, authorName } = req.body;
    const message = await sendBroadcast({ channelId, title, content, color, imageUrl, mention });
    const entry = { messageId: message.id, channelId, title: title || 'Komunikat', authorId: authorId || null, authorName: authorName || 'Nieznany', sentAt: new Date().toISOString() };
    broadcastHistory.unshift(entry);
    if (broadcastHistory.length > 100) broadcastHistory.pop();
    res.status(201).json({ success: true, message, historyEntry: entry });
  } catch (error) {
    res.status(error.status || 500).json({ error: error.message, details: error.details || null });
  }
});

app.get('/api/broadcaster/history', (req, res) => res.json({ count: broadcastHistory.length, history: broadcastHistory }));

app.get('/api/officers', async (req, res) => {
  try {
    if (!process.env.DISCORD_BOT_TOKEN) return res.status(503).json({ error: 'DISCORD_BOT_TOKEN_NOT_CONFIGURED' });
    const response = await fetch(`https://discord.com/api/v10/guilds/${GUILD_ID}/members?limit=1000`, { headers: { Authorization: `Bot ${process.env.DISCORD_BOT_TOKEN}` } });
    const members = await response.json();
    if (!response.ok) return res.status(response.status).json({ error: 'DISCORD_API_ERROR', details: members });
    const officers = members.filter(m => m.user && !m.user.bot).map(m => ({ id:m.user.id, username:m.user.username, displayName:m.nick || m.user.global_name || m.user.username, avatar:m.user.avatar ? `https://cdn.discordapp.com/avatars/${m.user.id}/${m.user.avatar}.png` : null, roles:m.roles, joinedAt:m.joined_at }));
    res.json({ guildId:GUILD_ID, count:officers.length, officers });
  } catch (error) { res.status(500).json({ error:'BACKEND_ERROR', message:'Nie udało się pobrać członków Discord.' }); }
});

app.listen(PORT, '0.0.0.0', () => console.log(`SW Backend działa na porcie ${PORT}`));

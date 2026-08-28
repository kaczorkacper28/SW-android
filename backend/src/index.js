require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { sendBroadcast, listChannels, listMessages, sendMessage } = require('./broadcaster');

const app = express();
const PORT = Number(process.env.PORT || 3000);
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1506076988564050081';
const CLIENT_ID = process.env.DISCORD_CLIENT_ID || '1539443070037266572';
const BROADCASTER_ROLE_IDS = (process.env.BROADCASTER_ROLE_IDS || '').split(',').map(x => x.trim()).filter(Boolean);

app.use(cors());
app.use(express.json({ limit: '1mb' }));

const ranks = [
  'Kandydat',
  'Młodszy Funkcjonariusz',
  'Funkcjonariusz',
  'Starszy Funkcjonariusz',
  'Dowódca Zmiany',
  'Zastępca Naczelnika',
  'Inspektor Generalny'
];

const broadcastHistory = [];

function isAuthorized(req) {
  if (BROADCASTER_ROLE_IDS.length === 0) return true;
  const roles = Array.isArray(req.body?.roleIds) ? req.body.roleIds : [];
  return roles.some(id => BROADCASTER_ROLE_IDS.includes(id));
}

function discordError(status, data) {
  const code = data?.code;
  const message = data?.message;
  if (status === 401) return { error: 'DISCORD_UNAUTHORIZED', message: 'Token bota jest nieprawidłowy albo wygasł.', discordCode: code };
  if (status === 403) return { error: 'DISCORD_FORBIDDEN', message: 'Bot nie ma dostępu do serwera lub brakuje mu uprawnień.', discordCode: code, discordMessage: message };
  if (status === 404) return { error: 'DISCORD_NOT_FOUND', message: 'Nie znaleziono serwera Discord. Sprawdź DISCORD_GUILD_ID.', discordCode: code };
  return { error: 'DISCORD_API_ERROR', message: message || 'Discord API zwróciło błąd.', discordCode: code, details: data };
}

async function discordGet(path) {
  const response = await fetch(`https://discord.com/api/v10${path}`, {
    headers: { Authorization: `Bot ${process.env.DISCORD_BOT_TOKEN}` }
  });
  const data = await response.json();
  if (!response.ok) {
    const error = new Error(data?.message || 'Discord API error');
    error.status = response.status;
    error.details = data;
    throw error;
  }
  return data;
}

function normalizeRoleName(value) {
  return String(value || '')
    .replace(/ł/g, 'l').replace(/Ł/g, 'L')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9 ]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

function findRank(roleNames) {
  const roles = roleNames.map(normalizeRoleName);
  const rules = [
    { rank: 'Inspektor Generalny', words: ['inspektor generalny', 'inspektor general', 'inspektor gen', 'generalny inspektor'] },
    { rank: 'Zastępca Naczelnika', words: ['zastepca naczelnika', 'zastepca nacz', 'zast naczelnika'] },
    { rank: 'Dowódca Zmiany', words: ['dowodca zmiany', 'dowodca zm', 'dow zmiany'] },
    { rank: 'Starszy Funkcjonariusz', words: ['starszy funkcjonariusz', 'starszy funkc', 'st funkcjonariusz'] },
    { rank: 'Młodszy Funkcjonariusz', words: ['mlodszy funkcjonariusz', 'mlodszy funkc', 'ml funkcjonariusz'] },
    { rank: 'Funkcjonariusz', words: ['funkcjonariusz sw', 'funkcjonariusz'] },
    { rank: 'Kandydat', words: ['kandydat sw', 'kandydat'] }
  ];
  for (const rule of rules) {
    if (roles.some(role => rule.words.some(word => role === word || role.includes(word)))) return rule.rank;
  }
  return null;
}

function hasCitizenRole(roleNames) {
  return roleNames.map(normalizeRoleName).some(role =>
    role === 'obywatel' || role.startsWith('obywatel ') || role.endsWith(' obywatel') || role.includes(' obywatel ')
  );
}

function hasSwRole(roleNames, rank) {
  if (rank) return true;
  return roleNames.map(normalizeRoleName).some(role =>
    role === 'sw' || role.startsWith('sw ') || role.endsWith(' sw') || role.includes(' sw ') ||
    role.includes('sluzba wiezienna') || role.includes('funkcjonariusz') || role.includes('funkcjonarius') ||
    role.includes('kandydat')
  );
}

function hasAnyNonCitizenRole(roleNames) {
  return roleNames.some(role => {
    const normalized = normalizeRoleName(role);
    return normalized && normalized !== '@everyone' && !normalized.includes('obywatel');
  });
}

function isTechnicalRole(name) {
  const r = normalizeRoleName(name);
  if (!r || r === '@everyone') return true;
  const technical = [
    'bot', 'admin', 'administrator', 'moderator', 'mod', 'staff', 'owner', 'wlasciciel',
    'zarzad', 'zarzadca', 'developer', 'deweloper', 'technik', 'support', 'pomoc',
    'obywatel', 'booster', 'nitro', 'mute', 'muted', 'everyone', 'pracownik sw'
  ];
  return technical.some(x => r === x || r.startsWith(x + ' ') || r.endsWith(' ' + x));
}

function getActualSwRole(roleObjects, rank) {
  if (rank) return rank;
  const candidates = roleObjects
    .filter(r => r && !isTechnicalRole(r.name) && !normalizeRoleName(r.name).includes('obywatel'))
    .sort((a, b) => Number(b.position || 0) - Number(a.position || 0));
  return candidates[0]?.name || 'Brak przypisanej rangi SW';
}

app.get('/api/health', (req, res) => res.json({
  status: 'ok', service: 'SW Android Backend', guildId: GUILD_ID,
  clientId: CLIENT_ID, discordTokenConfigured: Boolean(process.env.DISCORD_BOT_TOKEN)
}));

app.get('/api/config', (req, res) => res.json({ guildId: GUILD_ID, clientId: CLIENT_ID, ranks }));

app.get('/api/discord/channels', async (req, res) => {
  try {
    const channels = await listChannels(GUILD_ID);
    res.json(channels
      .filter(c => c.type === 0 || c.type === 5)
      .map(c => ({ id: c.id, name: c.name, type: c.type, parentId: c.parent_id || null, position: c.position || 0 })));
  } catch (error) {
    res.status(error.status || 500).json(discordError(error.status || 500, error.details));
  }
});

app.get('/api/discord/messages', async (req, res) => {
  try {
    const channelId = String(req.query.channelId || '').trim();
    if (!channelId) return res.status(400).json({ error: 'CHANNEL_ID_REQUIRED', message: 'Podaj channelId.' });
    const messages = await listMessages(channelId, req.query.limit);
    res.json({ channelId, messages });
  } catch (error) {
    res.status(error.status || 500).json(discordError(error.status || 500, error.details));
  }
});

app.post('/api/discord/messages', async (req, res) => {
  try {
    const channelId = String(req.body?.channelId || '').trim();
    const content = String(req.body?.content || '').trim();
    if (!channelId || !content) return res.status(400).json({ error: 'CHANNEL_ID_AND_CONTENT_REQUIRED', message: 'Kanał i treść wiadomości są wymagane.' });
    const message = await sendMessage(channelId, content);
    res.status(201).json({ success: true, message });
  } catch (error) {
    res.status(error.status || 500).json(discordError(error.status || 500, error.details));
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
    res.status(error.status || 500).json(discordError(error.status || 500, error.details));
  }
});

app.get('/api/broadcaster/history', (req, res) => res.json({ count: broadcastHistory.length, history: broadcastHistory }));

app.get('/api/officers', async (req, res) => {
  try {
    if (!process.env.DISCORD_BOT_TOKEN) {
      return res.status(503).json({ error: 'DISCORD_BOT_TOKEN_NOT_CONFIGURED', message: 'Na backendzie nie ustawiono DISCORD_BOT_TOKEN.' });
    }

    const discordRoles = await discordGet(`/guilds/${GUILD_ID}/roles`);
    const roleMap = new Map(discordRoles.map(role => [role.id, role]));
    const allMembers = [];
    let after = null;

    for (let page = 0; page < 100; page++) {
      const url = new URL(`https://discord.com/api/v10/guilds/${GUILD_ID}/members`);
      url.searchParams.set('limit', '1000');
      if (after) url.searchParams.set('after', after);

      const response = await fetch(url, {
        headers: { Authorization: `Bot ${process.env.DISCORD_BOT_TOKEN}` }
      });
      const members = await response.json();

      if (!response.ok) return res.status(response.status).json(discordError(response.status, members));
      if (!Array.isArray(members) || members.length === 0) break;

      allMembers.push(...members);
      after = members[members.length - 1]?.user?.id || null;
      if (members.length < 1000 || !after) break;
    }

    const parsed = allMembers
      .filter(member => member.user && !member.user.bot)
      .map(member => {
        const roleObjects = (member.roles || []).map(id => roleMap.get(id)).filter(Boolean);
        const roleNames = roleObjects.map(role => role.name).filter(Boolean);
        const rank = findRank(roleNames);
        const citizen = hasCitizenRole(roleNames);
        const sw = hasSwRole(roleNames, rank);
        const fallback = hasAnyNonCitizenRole(roleNames);
        const actualRole = getActualSwRole(roleObjects, rank);
        return { member, roleObjects, roleNames, rank, actualRole, citizen, sw, fallback };
      });

    const officers = parsed
      .filter(item => !item.citizen)
      .filter(item => item.sw || item.fallback)
      .map(item => {
        const m = item.member;
        const realRank = item.actualRole;
        return {
          id: m.user.id,
          username: m.user.username,
          displayName: m.nick || m.user.global_name || m.user.username,
          avatar: m.user.avatar ? `https://cdn.discordapp.com/avatars/${m.user.id}/${m.user.avatar}.png` : null,
          roles: m.roles || [],
          roleNames: item.roleNames,
          rank: realRank,
          rankRole: realRank,
          allRoles: item.roleNames,
          status: 'Aktywny',
          joinedAt: m.joined_at || null
        };
      });

    const rankOrder = new Map(ranks.map((rank, index) => [rank, index]));
    officers.sort((a, b) => {
      const ar = rankOrder.has(a.rank) ? rankOrder.get(a.rank) : -1;
      const br = rankOrder.has(b.rank) ? rankOrder.get(b.rank) : -1;
      if (ar !== br) return br - ar;
      return a.displayName.localeCompare(b.displayName, 'pl', { sensitivity: 'base' });
    });

    const citizensExcluded = parsed.filter(item => item.citizen).length;

    res.json({
      guildId: GUILD_ID,
      count: officers.length,
      officers,
      meta: {
        membersScanned: allMembers.length,
        citizensExcluded,
        officersWithRank: officers.filter(o => o.rank !== 'Brak przypisanej rangi SW').length,
        officersWithoutRank: officers.filter(o => o.rank === 'Brak przypisanej rangi SW').length
      }
    });
  } catch (error) {
    console.error('Officers backend error:', error);
    res.status(error.status || 500).json(discordError(error.status || 500, error.details));
  }
});

app.listen(PORT, '0.0.0.0', () => console.log(`SW Backend działa na porcie ${PORT}`));

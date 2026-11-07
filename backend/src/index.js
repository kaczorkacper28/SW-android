'use strict';
const express = require('express');
const crypto = require('node:crypto');
const path = require('node:path');
const { createDiscordClient, problem, snowflake, text } = require('./broadcaster');
const ranks = ['Kandydat','Młodszy Funkcjonariusz','Funkcjonariusz','Starszy Funkcjonariusz','Dowódca Zmiany','Zastępca Naczelnika','Inspektor Generalny'];

function decodeAccessKey(value) {
  if (!value.startsWith('base64:')) return value;
  const encoded = value.slice(7);
  if (!encoded || !/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(encoded)) return '';
  const decoded = Buffer.from(encoded,'base64');
  if (decoded.toString('base64') !== encoded) return '';
  try { return new TextDecoder('utf-8',{ fatal:true }).decode(decoded); } catch { return ''; }
}
function discordError(status,data) {
  const discordCode = Number.isInteger(data?.code) ? data.code : undefined;
  if (status === 401) return { error:'DISCORD_UNAUTHORIZED', message:'Token bota jest nieprawidłowy albo wygasł.', discordCode };
  if (status === 403) return { error:'DISCORD_FORBIDDEN', message:'Bot nie ma dostępu do serwera lub brakuje mu uprawnień.', discordCode };
  if (status === 404) return { error:'DISCORD_NOT_FOUND', message:'Nie znaleziono kanału lub serwera Discord. Sprawdź konfigurację.', discordCode };
  if (status === 429) return { error:'DISCORD_RATE_LIMITED', message:'Discord ograniczył liczbę żądań. Odczekaj przed ponowną próbą.', discordCode };
  return { error:'DISCORD_API_ERROR', message:'Discord API zwróciło błąd.', discordCode };
}

function normalizeRoleName(value) {
  return String(value || '').replace(/ł/g,'l').replace(/Ł/g,'L').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-zA-Z0-9 ]/g,' ').replace(/\s+/g,' ').trim().toLowerCase();
}

function findRank(roleNames) {
  const roles = roleNames.map(normalizeRoleName);
  const rules = [
    {rank:'Inspektor Generalny',words:['inspektor generalny','inspektor general','inspektor gen','generalny inspektor']},
    {rank:'Zastępca Naczelnika',words:['zastepca naczelnika','zastepca nacz','zast naczelnika']},
    {rank:'Dowódca Zmiany',words:['dowodca zmiany','dowodca zm','dow zmiany']},
    {rank:'Starszy Funkcjonariusz',words:['starszy funkcjonariusz','starszy funkc','st funkcjonariusz']},
    {rank:'Młodszy Funkcjonariusz',words:['mlodszy funkcjonariusz','mlodszy funkc','ml funkcjonariusz']},
    {rank:'Funkcjonariusz',words:['funkcjonariusz sw','funkcjonariusz']},
    {rank:'Kandydat',words:['kandydat sw','kandydat']}
  ];
  for (const rule of rules) if (roles.some(role => rule.words.some(word => role===word || role.includes(word)))) return rule.rank;
  return null;
}

function hasCitizenRole(roleNames) {
  return roleNames.map(normalizeRoleName).some(role => role==='obywatel' || role.startsWith('obywatel ') || role.endsWith(' obywatel') || role.includes(' obywatel '));
}

function hasSwRole(roleNames, rank) {
  if (rank) return true;
  return roleNames.map(normalizeRoleName).some(role => role==='sw' || role.startsWith('sw ') || role.endsWith(' sw') || role.includes(' sw ') || role.includes('sluzba wiezienna') || role.includes('funkcjonariusz') || role.includes('funkcjonarius') || role.includes('kandydat'));
}

function hasAnyNonCitizenRole(roleNames) {
  return roleNames.some(role => { const normalized=normalizeRoleName(role); return normalized && normalized!=='everyone' && !normalized.includes('obywatel'); });
}

function isTechnicalRole(name) {
  const r=normalizeRoleName(name);
  if (!r || r==='@everyone') return true;
  const technical=['bot','admin','administrator','moderator','mod','staff','owner','wlasciciel','zarzad','zarzadca','developer','deweloper','technik','support','pomoc','obywatel','booster','nitro','mute','muted','everyone','pracownik sw'];
  return technical.some(x => r===x || r.startsWith(x+' ') || r.endsWith(' '+x));
}

function getActualSwRole(roleObjects, rank) {
  if (rank) return rank;
  const candidates=roleObjects.filter(r => r && !isTechnicalRole(r.name) && !normalizeRoleName(r.name).includes('obywatel')).sort((a,b)=>Number(b.position||0)-Number(a.position||0));
  return candidates[0]?.name || 'Brak przypisanej rangi SW';
}


function createApp({ env = process.env, fetchImpl = globalThis.fetch, timeoutMs = 10000 } = {}) {
  const app = express();
  app.disable('x-powered-by');
  const guildId = snowflake(env.DISCORD_GUILD_ID || '1506076988564050081','DISCORD_GUILD_ID');
  const clientId = snowflake(env.DISCORD_CLIENT_ID || '1539443070037266572','DISCORD_CLIENT_ID');
  const key = env.BACKEND_ACCESS_KEY || '';
  const validKey = typeof key === 'string' && key.length >= 12 && key.length <= 1024 && !/[\x00-\x1f\x7f-\x9f]/.test(key);
  const discord = createDiscordClient({ token:env.DISCORD_BOT_TOKEN, guildId, fetchImpl, timeoutMs });
  const broadcastHistory = [];
  app.use((req,res,next) => {
    res.set({ 'Cache-Control':'no-store', 'X-Content-Type-Options':'nosniff', 'X-Frame-Options':'DENY', 'Referrer-Policy':'no-referrer' });
    const origin = req.get('Origin');
    if (origin) {
      const localOrigins = [`http://127.0.0.1:${req.socket.localPort}`,`http://localhost:${req.socket.localPort}`];
      if (!localOrigins.includes(origin)) return res.status(403).json({ error:'ORIGIN_NOT_ALLOWED', message:'Przeglądarka musi korzystać z lokalnego adresu backendu.' });
      res.set({ 'Access-Control-Allow-Origin':origin, 'Vary':'Origin', 'Access-Control-Allow-Methods':'GET, POST, OPTIONS', 'Access-Control-Allow-Headers':'Content-Type, X-SW-Key' });
    }
    if (req.method === 'OPTIONS') return res.sendStatus(origin ? 204 : 405);
    next();
  });
  app.get('/api/health',(req,res) => res.json({ status:'ok', service:'SW Android Backend', guildId, clientId, discordTokenConfigured:Boolean(env.DISCORD_BOT_TOKEN), accessKeyConfigured:validKey }));
  app.get('/api/config',(req,res) => res.json({ guildId, clientId, ranks }));
  app.use('/api',(req,res,next) => {
    if (!validKey) return res.status(503).json({ error:'BACKEND_ACCESS_KEY_NOT_CONFIGURED', message:'Uruchom npm run configure w katalogu backendu, aby ustawić hasło połączenia.' });
    const submitted = decodeAccessKey(req.get('X-SW-Key') || '');
    const digest = value => crypto.createHash('sha256').update(value).digest();
    if (!crypto.timingSafeEqual(digest(submitted),digest(key))) return res.status(401).json({ error:'UNAUTHORIZED', message:'Brak poprawnego hasła połączenia z backendem.' });
    next();
  });
  app.use(express.json({ limit:'64kb', strict:true }));
  app.get(['/api/access','/api/session'],(req,res) => res.json({ authenticated:true }));

  app.get('/api/discord/channels',async(req,res,next) => {
    try {
      const channels = await discord.listChannels();
      res.json({ channels:channels.filter(c => c.type === 0 || c.type === 5).map(c => ({ id:c.id, name:c.name, type:c.type, parentId:c.parent_id || null, position:c.position || 0 })) });
    } catch(error) { next(error); }
  });
  app.get('/api/discord/messages',async(req,res,next) => {
    try {
      const channelId = req.query.channelId;
      if (!channelId) throw problem(400,'CHANNEL_ID_REQUIRED','Podaj channelId.');
      const messages = await discord.listMessages(channelId,req.query.limit == null ? 50 : req.query.limit);
      res.json({ channelId,messages });
    } catch(error) { next(error); }
  });
  app.post('/api/discord/messages',async(req,res,next) => {
    try {
      const { channelId,content } = req.body || {};
      if (!channelId || !content) throw problem(400,'CHANNEL_ID_AND_CONTENT_REQUIRED','Kanał i treść wiadomości są wymagane.');
      const message = await discord.sendMessage(channelId,content);
      res.status(201).json({ success:true,message });
    } catch(error) { next(error); }
  });
  app.post('/api/broadcaster/send',async(req,res,next) => {
    try {
      const input = req.body || {};
      const authorId = text(input.authorId,'Identyfikator autora',64) || null;
      const authorName = text(input.authorName,'Nazwa autora',120) || 'Nieznany';
      // Authority comes from the installation key, never from body.roleIds.
      const message = await discord.sendBroadcast(input);
      const entry = { messageId:message.id, channelId:input.channelId, title:input.title || 'Komunikat', authorId, authorName, sentAt:new Date().toISOString() };
      broadcastHistory.unshift(entry);
      if (broadcastHistory.length > 100) broadcastHistory.pop();
      res.status(201).json({ success:true,message,historyEntry:entry });
    } catch(error) { next(error); }
  });
  app.get('/api/broadcaster/history',(req,res) => res.json({ count:broadcastHistory.length, history:broadcastHistory }));

  app.get('/api/officers',async(req,res,next) => {
    try {
      const discordRoles = await discord.request(`/guilds/${guildId}/roles`);
      if (!Array.isArray(discordRoles)) throw problem(502,'DISCORD_INVALID_RESPONSE','Discord nie zwrócił listy ról.');
      const roleMap = new Map(discordRoles.map(role => [role.id,role]));
      const allMembers = [];
      let after = null;
      for (let page = 0; page < 100; page++) {
        const query = new URLSearchParams({ limit:'1000', ...(after ? { after } : {}) });
        const members = await discord.request(`/guilds/${guildId}/members?${query}`);
        if (!Array.isArray(members)) throw problem(502,'DISCORD_INVALID_RESPONSE','Discord nie zwrócił listy członków.');
        if (members.length === 0) break;
        allMembers.push(...members);
        after = members[members.length - 1]?.user?.id || null;
        if (members.length < 1000 || !after) break;
      }
      const parsed = allMembers.filter(member => member.user && !member.user.bot).map(member => {
        const roleObjects = (member.roles || []).map(id => roleMap.get(id)).filter(Boolean);
        const roleNames = roleObjects.map(role => role.name).filter(Boolean);
        const rank = findRank(roleNames);
        return { member,roleNames,rank,actualRole:getActualSwRole(roleObjects,rank),citizen:hasCitizenRole(roleNames),sw:hasSwRole(roleNames,rank),fallback:hasAnyNonCitizenRole(roleNames) };
      });
      const officers = parsed.filter(item => !item.citizen && (item.sw || item.fallback)).map(item => {
        const m = item.member, realRank = item.actualRole;
        return { id:m.user.id,username:m.user.username,displayName:m.nick || m.user.global_name || m.user.username,avatar:m.user.avatar ? `https://cdn.discordapp.com/avatars/${m.user.id}/${m.user.avatar}.png` : null,roles:m.roles || [],roleNames:item.roleNames,rank:realRank,rankRole:realRank,allRoles:item.roleNames,status:'Aktywny',joinedAt:m.joined_at || null };
      });
      const rankOrder = new Map(ranks.map((rank,index) => [rank,index]));
      officers.sort((a,b) => {
        const ar = rankOrder.has(a.rank) ? rankOrder.get(a.rank) : -1, br = rankOrder.has(b.rank) ? rankOrder.get(b.rank) : -1;
        return ar !== br ? br - ar : a.displayName.localeCompare(b.displayName,'pl',{ sensitivity:'base' });
      });
      res.json({ guildId,count:officers.length,officers,meta:{ membersScanned:allMembers.length,citizensExcluded:parsed.filter(item => item.citizen).length,officersWithRank:officers.filter(o => o.rank !== 'Brak przypisanej rangi SW').length,officersWithoutRank:officers.filter(o => o.rank === 'Brak przypisanej rangi SW').length } });
    } catch(error) { next(error); }
  });
  app.use((req,res) => res.status(404).json({ error:'NOT_FOUND',message:'Nie znaleziono endpointu API.' }));
  app.use((error,req,res,next) => {
    if (res.headersSent) return next(error);
    if (error.type === 'entity.too.large') return res.status(413).json({ error:'BODY_TOO_LARGE',message:'Żądanie przekracza limit 64 KB.' });
    if (error.type === 'entity.parse.failed') return res.status(400).json({ error:'INVALID_JSON',message:'Nieprawidłowy JSON.' });
    const status = Number.isInteger(error.status) && error.status >= 400 && error.status <= 599 ? error.status : 500;
    if (error.discord) return res.status(status).json(discordError(status,error.details));
    res.status(status).json({ error:error.code || 'INTERNAL_ERROR',message:error.code ? error.message : 'Błąd backendu SW.' });
  });
  return app;
}

async function startServer({ env = process.env,host = env.HOST || '127.0.0.1',port = Number(env.PORT || 3000),...options } = {}) {
  if (!Number.isInteger(port) || port < 0 || port > 65535) throw new Error('PORT musi być liczbą od 1 do 65535.');
  const app = createApp({ ...options,env });
  return new Promise((resolve,reject) => {
    const server = app.listen(port,host,() => { server.removeListener('error',reject); resolve(server); });
    server.once('error',reject);
  });
}
if (require.main === module) {
  require('dotenv').config({ path:path.join(__dirname,'..','.env') });
  startServer().then(server => {
    console.log(`SW Backend działa na ${server.address().address}:${server.address().port}`);
    const stop = () => server.close(() => process.exit(0));
    process.once('SIGINT',stop);
    process.once('SIGTERM',stop);
  }).catch(error => { console.error(error.message);process.exitCode = 1; });
}
module.exports = { createApp,startServer,decodeAccessKey,normalizeRoleName,findRank };

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

function isAuthorized(req) { if (BROADCASTER_ROLE_IDS.length === 0) return true; const roles = Array.isArray(req.body?.roleIds) ? req.body.roleIds : []; return roles.some(id => BROADCASTER_ROLE_IDS.includes(id)); }
function discordError(status, data) { const code=data?.code, message=data?.message; if(status===401)return {error:'DISCORD_UNAUTHORIZED',message:'Token bota jest nieprawidłowy albo wygasł.',discordCode:code}; if(status===403)return {error:'DISCORD_FORBIDDEN',message:'Bot nie ma dostępu do serwera lub brakuje mu uprawnień.',discordCode:code,discordMessage:message}; if(status===404)return {error:'DISCORD_NOT_FOUND',message:'Nie znaleziono serwera Discord. Sprawdź DISCORD_GUILD_ID.',discordCode:code}; return {error:'DISCORD_API_ERROR',message:message||'Discord API zwróciło błąd.',discordCode:code,details:data}; }
async function discordGet(path) { const response=await fetch(`https://discord.com/api/v10${path}`,{headers:{Authorization:`Bot ${process.env.DISCORD_BOT_TOKEN}`}}); const data=await response.json(); if(!response.ok){const e=new Error(data?.message||'Discord API error');e.status=response.status;e.details=data;throw e;} return data; }
function normalizeRoleName(value){return String(value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-zA-Z0-9ąćęłńóśźżĄĆĘŁŃÓŚŹŻ ]/g,' ').replace(/\s+/g,' ').trim().toLowerCase();}
function findRank(roleNames){const normalizedRoles=roleNames.map(normalizeRoleName); for(let i=ranks.length-1;i>=0;i--){const target=normalizeRoleName(ranks[i]);if(normalizedRoles.some(role=>role===target||role.includes(target)))return ranks[i];} const aliases=[['Inspektor Generalny',['inspektor generalny','inspektor gen','ig']],['Zastępca Naczelnika',['zastepca naczelnika','zastepca naczelnika sw','zastepca nacz']],['Dowódca Zmiany',['dowodca zmiany','dowodca zm','dz']],['Starszy Funkcjonariusz',['starszy funkcjonariusz','starszy funkc','st funkcjonariusz']],['Funkcjonariusz',['funkcjonariusz','funkcjonariusz sw']],['Młodszy Funkcjonariusz',['mlodszy funkcjonariusz','mlodszy funkc']],['Kandydat',['kandydat','kandydat sw']]]; for(const [rank,values] of aliases){if(normalizedRoles.some(role=>values.some(alias=>role===alias||role.includes(alias))))return rank;} return 'Brak stopnia';}

app.get('/api/health',(req,res)=>res.json({status:'ok',service:'SW Android Backend',guildId:GUILD_ID,clientId:CLIENT_ID,discordTokenConfigured:Boolean(process.env.DISCORD_BOT_TOKEN)}));
app.get('/api/config',(req,res)=>res.json({guildId:GUILD_ID,clientId:CLIENT_ID,ranks}));
app.get('/api/discord/channels',async(req,res)=>{try{const channels=await listChannels(GUILD_ID);res.json(channels.filter(c=>c.type===0||c.type===5).map(c=>({id:c.id,name:c.name,type:c.type})));}catch(e){res.status(e.status||500).json(discordError(e.status||500,e.details));}});
app.post('/api/broadcaster/send',async(req,res)=>{if(!isAuthorized(req))return res.status(403).json({error:'FORBIDDEN',message:'Brak uprawnień do Broadcastera.'});try{const {channelId,title,content,color,imageUrl,mention,authorId,authorName}=req.body;const message=await sendBroadcast({channelId,title,content,color,imageUrl,mention});const entry={messageId:message.id,channelId,title:title||'Komunikat',authorId:authorId||null,authorName:authorName||'Nieznany',sentAt:new Date().toISOString()};broadcastHistory.unshift(entry);if(broadcastHistory.length>100)broadcastHistory.pop();res.status(201).json({success:true,message,historyEntry:entry});}catch(e){res.status(e.status||500).json(discordError(e.status||500,e.details));}});
app.get('/api/broadcaster/history',(req,res)=>res.json({count:broadcastHistory.length,history:broadcastHistory}));

app.get('/api/officers',async(req,res)=>{try{if(!process.env.DISCORD_BOT_TOKEN)return res.status(503).json({error:'DISCORD_BOT_TOKEN_NOT_CONFIGURED',message:'Na backendzie nie ustawiono DISCORD_BOT_TOKEN.'});const discordRoles=await discordGet(`/guilds/${GUILD_ID}/roles`);const roleMap=new Map(discordRoles.map(role=>[role.id,role.name]));const allMembers=[];let after='0';for(let page=0;page<20;page++){const url=new URL(`https://discord.com/api/v10/guilds/${GUILD_ID}/members`);url.searchParams.set('limit','1000');if(after!=='0')url.searchParams.set('after',after);const response=await fetch(url,{headers:{Authorization:`Bot ${process.env.DISCORD_BOT_TOKEN}`}});const members=await response.json();if(!response.ok)return res.status(response.status).json(discordError(response.status,members));if(!Array.isArray(members)||members.length===0)break;allMembers.push(...members);if(members.length<1000)break;after=members[members.length-1].user.id;}

const officers=allMembers.filter(m=>{if(!m.user||m.user.bot)return false;const roleNames=(m.roles||[]).map(id=>roleMap.get(id)).filter(Boolean);const normalized=roleNames.map(normalizeRoleName);/* Obywatel nie jest funkcjonariuszem SW. */if(normalized.some(r=>r==='obywatel'||r==='obywatel sw'||r.startsWith('obywatel ')))return false;/* Pokazujemy tylko osoby posiadające rozpoznawalny stopień SW. */return findRank(roleNames)!=='Brak stopnia';}).map(m=>{const roleNames=(m.roles||[]).map(id=>roleMap.get(id)).filter(Boolean);return{id:m.user.id,username:m.user.username,displayName:m.nick||m.user.global_name||m.user.username,avatar:m.user.avatar?`https://cdn.discordapp.com/avatars/${m.user.id}/${m.user.avatar}.png`:null,roles:m.roles||[],roleNames,rank:findRank(roleNames),status:'Aktywny',joinedAt:m.joined_at||null};});
res.json({guildId:GUILD_ID,count:officers.length,officers});}catch(e){console.error('Officers backend error:',e);res.status(e.status||500).json(discordError(e.status||500,e.details));}});

app.listen(PORT,'0.0.0.0',()=>console.log(`SW Backend działa na porcie ${PORT}`));

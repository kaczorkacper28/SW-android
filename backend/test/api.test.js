'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { spawnSync } = require('node:child_process');
const path = require('node:path');
const { createApp, startServer, decodeAccessKey } = require('../src/index');

const guildId = '100000000000000001';
const clientId = '100000000000000002';
const channelId = '100000000000000010';
const key = 'Hasło połączenia testowego ąć !';
const token = 'fixture-discord-token-not-real';
const baseEnv = { DISCORD_GUILD_ID:guildId, DISCORD_CLIENT_ID:clientId, BACKEND_ACCESS_KEY:key, DISCORD_BOT_TOKEN:token };
const reply = (data,status=200) => new Response(JSON.stringify(data),{ status,headers:{ 'Content-Type':'application/json' } });

async function serverFor(t,{ env = {},fetchImpl = async() => { throw new Error('Unexpected external request'); },timeoutMs = 10000 } = {}) {
  const server = await startServer({ env:{ ...baseEnv,...env },port:0,fetchImpl,timeoutMs });
  t.after(() => new Promise(resolve => server.close(resolve)));
  return server;
}
function request(server,route,{ method='GET',body,raw,keyValue=key,headers={} } = {}) {
  return new Promise((resolve,reject) => {
    const data = raw === undefined ? body === undefined ? undefined : JSON.stringify(body) : raw;
    const allHeaders = { 'Content-Type':'application/json',...headers };
    if (keyValue !== null) allHeaders['X-SW-Key'] = 'base64:' + Buffer.from(keyValue,'utf8').toString('base64');
    if (data !== undefined) allHeaders['Content-Length'] = Buffer.byteLength(data);
    const req = http.request({ host:'127.0.0.1',port:server.address().port,path:route,method,headers:allHeaders },res => {
      let text = '';
      res.setEncoding('utf8');
      res.on('data',chunk => { text += chunk; });
      res.on('end',() => {
        try { resolve({ status:res.statusCode,body:text ? JSON.parse(text) : null,headers:res.headers }); }
        catch(error) { reject(error); }
      });
    });
    req.on('error',reject);
    req.end(data);
  });
}

test('backend import does not listen or load a dotenv file',() => {
  const result = spawnSync(process.execPath,['-e',`const net=require('net'),dotenv=require('dotenv');net.Server.prototype.listen=()=>{throw Error('listen on import')};dotenv.config=()=>{throw Error('dotenv on import')};require('./src/index');console.log('import-ok')`],{ cwd:path.join(__dirname,'..'),encoding:'utf8',timeout:3000 });
  assert.equal(result.status,0,result.stderr);
  assert.equal(result.stdout.trim(),'import-ok');
  assert.equal(typeof createApp,'function');
});

test('health and config are public, report configuration without exposing secrets, and bind only loopback',async t => {
  const server = await serverFor(t);
  assert.equal(server.address().address,'127.0.0.1');
  const health = await request(server,'/api/health',{ keyValue:null });
  assert.equal(health.status,200);
  assert.equal(health.body.status,'ok');
  assert.equal(health.body.discordTokenConfigured,true);
  assert.equal(health.body.accessKeyConfigured,true);
  const config = await request(server,'/api/config',{ keyValue:null });
  assert.equal(config.status,200);
  assert.equal(config.body.ranks.length,7);
  assert.equal(config.body.guildId,guildId);
  const exposed = JSON.stringify([health.body,config.body]);
  assert.ok(!exposed.includes(token) && !exposed.includes(key));
});

test('every private endpoint requires a configured installation key, not claimed Discord roles',async t => {
  let calls = 0;
  const server = await serverFor(t,{ fetchImpl:async() => { calls++;return reply([]); } });
  for (const route of ['/api/access','/api/session','/api/officers','/api/discord/channels','/api/discord/messages','/api/broadcaster/history']) {
    assert.equal((await request(server,route,{ keyValue:null })).status,401,route);
    assert.equal((await request(server,route,{ keyValue:'wrong-key' })).status,401,route);
  }
  assert.equal((await request(server,'/api/broadcaster/send',{ method:'POST',keyValue:'wrong-key',body:{ roleIds:['claimed-admin'],channelId,content:'Test' } })).status,401);
  assert.equal(calls,0);
  assert.deepEqual((await request(server,'/api/access')).body,{ authenticated:true });
  assert.deepEqual((await request(server,'/api/session')).body,{ authenticated:true });
  for (const configured of ['', 'too-short']) {
    const absent = await serverFor(t,{ env:{ BACKEND_ACCESS_KEY:configured } });
    assert.equal((await request(absent,'/api/access')).status,503);
    assert.equal((await request(absent,'/api/health',{ keyValue:null })).body.accessKeyConfigured,false);
  }
});

test('Unicode connection passwords roundtrip through canonical base64 and malformed encodings fail',async t => {
  assert.equal(decodeAccessKey('base64:' + Buffer.from(key).toString('base64')),key);
  for (const value of ['base64:%%%%','base64:','base64:/w==','base64:Zg=','base64:Zh==']) assert.equal(decodeAccessKey(value),'');
  assert.equal(decodeAccessKey('ascii-password'),'ascii-password');
  const server = await serverFor(t);
  assert.equal((await request(server,'/api/access')).status,200);
  assert.equal((await request(server,'/api/access',{ keyValue:null,headers:{ 'X-SW-Key':'base64:/w==' } })).status,401);
});

test('external browser origins are rejected and local preflight never uses wildcard CORS',async t => {
  const server = await serverFor(t);
  assert.equal((await request(server,'/api/access',{ headers:{ Origin:'https://outside.example' } })).status,403);
  const origin = `http://127.0.0.1:${server.address().port}`;
  const options = await request(server,'/api/access',{ method:'OPTIONS',headers:{ Origin:origin } });
  assert.equal(options.status,204);
  assert.equal(options.headers['access-control-allow-origin'],origin);
  assert.notEqual(options.headers['access-control-allow-origin'],'*');
  const native = await request(server,'/api/access');
  assert.equal(native.headers['access-control-allow-origin'],undefined);
});

test('missing Discord token returns actionable 503 and performs no remote calls',async t => {
  let calls = 0;
  const server = await serverFor(t,{ env:{ DISCORD_BOT_TOKEN:'' },fetchImpl:async() => { calls++;return reply([]); } });
  assert.equal((await request(server,'/api/health')).body.discordTokenConfigured,false);
  for (const route of ['/api/officers','/api/discord/channels',`/api/discord/messages?channelId=${channelId}`]) {
    const result = await request(server,route);
    assert.equal(result.status,503);
    assert.equal(result.body.error,'DISCORD_BOT_TOKEN_NOT_CONFIGURED');
  }
  assert.equal(calls,0);
  assert.equal((await request(server,'/api/access')).status,200);
});

test('officers preserve filtering, Polish ranks and descending seniority',async t => {
  const roles = [{ id:'a',name:'Młodszy Funkcjonariusz',position:2 },{ id:'b',name:'Inspektor Generalny',position:9 },{ id:'c',name:'Obywatel',position:1 },{ id:'d',name:'Komendant jednostki',position:10 }];
  const members = [
    { user:{ id:'1',username:'junior' },roles:['a'],nick:'Anna',joined_at:'2026-01-01' },
    { user:{ id:'2',username:'senior',avatar:'image' },roles:['b'] },
    { user:{ id:'3',username:'citizen' },roles:['c','a'] },
    { user:{ id:'4',username:'robot',bot:true },roles:['b'] },
    { user:{ id:'5',username:'custom' },roles:['d'] }
  ];
  const server = await serverFor(t,{ fetchImpl:async(url,options) => {
    assert.equal(options.headers.Authorization,`Bot ${token}`);
    assert.ok(options.signal instanceof AbortSignal);
    if (url.endsWith(`/guilds/${guildId}/roles`)) return reply(roles);
    assert.ok(url.includes(`/guilds/${guildId}/members?limit=1000`));
    return reply(members);
  } });
  const result = await request(server,'/api/officers');
  assert.equal(result.status,200);
  assert.equal(result.body.count,3);
  assert.equal(result.body.officers[0].rank,'Inspektor Generalny');
  assert.equal(result.body.officers[1].rank,'Młodszy Funkcjonariusz');
  assert.equal(result.body.officers[2].rank,'Komendant jednostki');
  assert.equal(result.body.meta.citizensExcluded,1);
  assert.equal(result.body.meta.membersScanned,5);
});

test('officers fetch the next page with the last member cursor',async t => {
  const first = Array.from({ length:1000 },(_,i) => ({ user:{ id:String(100000000000001000n + BigInt(i)),username:'fixture',bot:true },roles:[] }));
  const cursors = [];
  const server = await serverFor(t,{ fetchImpl:async url => {
    if (url.endsWith('/roles')) return reply([]);
    const cursor = new URL(url).searchParams.get('after');
    cursors.push(cursor);
    return reply(cursor ? [] : first);
  } });
  const result = await request(server,'/api/officers');
  assert.equal(result.status,200);
  assert.deepEqual(cursors,[null,first.at(-1).user.id]);
  assert.equal(result.body.meta.membersScanned,1000);
});

test('channels and messages preserve schemas and verify ownership before reading or sending',async t => {
  const operations = [];
  const server = await serverFor(t,{ fetchImpl:async(url,options) => {
    operations.push([url,options]);
    if (url.endsWith(`/guilds/${guildId}/channels`)) return reply([{ id:channelId,name:'rozkazy',type:0,position:2 },{ id:'voice',type:2 },{ id:'news',name:'aktualności',type:5,parent_id:'category' }]);
    if (url.endsWith(`/channels/${channelId}`)) return reply({ id:channelId,guild_id:guildId,type:0 });
    if (options.method === 'POST') return reply({ id:'message-id',...JSON.parse(options.body) },201);
    assert.ok(url.endsWith(`/channels/${channelId}/messages?limit=100`));
    return reply([{ id:'message-id',content:'Rozkaz' }]);
  } });
  const channels = await request(server,'/api/discord/channels');
  assert.equal(channels.body.channels.length,2);
  assert.deepEqual(channels.body.channels[0],{ id:channelId,name:'rozkazy',type:0,parentId:null,position:2 });
  const messages = await request(server,`/api/discord/messages?channelId=${channelId}&limit=999`);
  assert.equal(messages.status,200);
  assert.equal(messages.body.channelId,channelId);
  assert.equal(messages.body.messages[0].content,'Rozkaz');
  const sent = await request(server,'/api/discord/messages',{ method:'POST',body:{ channelId,content:' Wiadomość @everyone ' } });
  assert.equal(sent.status,201);
  assert.equal(sent.body.message.content,'Wiadomość @everyone');
  assert.deepEqual(sent.body.message.allowed_mentions,{ parse:[] });
  const messageRequests = operations.filter(([url]) => url.includes('/messages'));
  assert.equal(messageRequests.length,2);
  for (const [url] of messageRequests) assert.ok(operations.some(([previous]) => previous === url.slice(0,url.indexOf('/messages'))));
});

test('cross-guild and DM channels cannot be read or written by the shared bot',async t => {
  for (const guild_id of ['100000000000000099',undefined]) {
    const operations = [];
    const server = await serverFor(t,{ fetchImpl:async(url,options) => { operations.push([url,options]);return reply({ id:channelId,guild_id,type:0 }); } });
    const results = [
      await request(server,`/api/discord/messages?channelId=${channelId}`),
      await request(server,'/api/discord/messages',{ method:'POST',body:{ channelId,content:'Test' } }),
      await request(server,'/api/broadcaster/send',{ method:'POST',body:{ channelId,content:'Test' } })
    ];
    assert.ok(results.every(result => result.status === 403 && result.body.error === 'CHANNEL_OUTSIDE_GUILD'));
    assert.equal(operations.length,3);
    assert.ok(operations.every(([url,options]) => url.endsWith(`/channels/${channelId}`) && options.method !== 'POST'));
  }
});

test('broadcaster sends validated embeds and records only acknowledged messages in history',async t => {
  let payload;
  const server = await serverFor(t,{ fetchImpl:async(url,options) => {
    if (url.endsWith(`/channels/${channelId}`)) return reply({ guild_id:guildId,type:0 });
    payload = JSON.parse(options.body);
    return reply({ id:'broadcast-id' },201);
  } });
  const result = await request(server,'/api/broadcaster/send',{ method:'POST',body:{ channelId,title:'Komunikat',content:'Treść',color:0,imageUrl:'https://example.org/image.png',mention:'none',authorName:'Android',roleIds:[] } });
  assert.equal(result.status,201);
  assert.equal(payload.embeds[0].color,0);
  assert.equal(payload.embeds[0].description,'Treść');
  assert.deepEqual(payload.allowed_mentions,{ parse:[] });
  const history = await request(server,'/api/broadcaster/history');
  assert.equal(history.body.count,1);
  assert.equal(history.body.history[0].messageId,'broadcast-id');
  assert.equal(history.body.history[0].authorName,'Android');
});

test('input errors, malformed JSON and oversized bodies fail before remote calls',async t => {
  let calls = 0;
  const server = await serverFor(t,{ fetchImpl:async() => { calls++;return reply({}); } });
  const invalid = [
    ['/api/discord/messages',{ method:'POST',body:{ channelId:'../bad',content:'Test' } }],
    ['/api/discord/messages',{ method:'POST',body:{ channelId,content:'x'.repeat(2001) } }],
    ['/api/discord/messages?channelId='+channelId+'&limit=nope',{}],
    ['/api/broadcaster/send',{ method:'POST',body:{ channelId,content:'Test',color:-1 } }],
    ['/api/broadcaster/send',{ method:'POST',body:{ channelId,content:'Test',imageUrl:'file:///private' } }],
    ['/api/broadcaster/send',{ method:'POST',body:{ channelId,content:'Test',mention:'admin' } }],
    ['/api/discord/messages',{ method:'POST',raw:'{' }]
  ];
  for (const [route,options] of invalid) assert.equal((await request(server,route,options)).status,400);
  assert.equal((await request(server,'/api/discord/messages',{ method:'POST',body:{ channelId,content:'x'.repeat(70000) } })).status,413);
  assert.equal(calls,0);
});

test('failed or unconfirmed broadcast never appears as a successful history entry',async t => {
  for (const response of [reply({ code:50013 },403),reply({})]) {
    const server = await serverFor(t,{ fetchImpl:async url => url.endsWith(`/channels/${channelId}`) ? reply({ guild_id:guildId,type:0 }) : response });
    const result = await request(server,'/api/broadcaster/send',{ method:'POST',body:{ channelId,content:'Test' } });
    assert.ok([403,502].includes(result.status));
    assert.equal((await request(server,'/api/broadcaster/history')).body.count,0);
  }
});

test('Discord failures and timeouts produce bounded safe errors without exposing credentials',async t => {
  const rejected = await serverFor(t,{ fetchImpl:async() => reply({ code:50001,message:token,details:key },403) });
  const failure = await request(rejected,'/api/discord/channels');
  assert.equal(failure.status,403);
  assert.equal(failure.body.error,'DISCORD_FORBIDDEN');
  assert.ok(!JSON.stringify(failure.body).includes(token) && !JSON.stringify(failure.body).includes(key));
  const unavailable = await serverFor(t,{ fetchImpl:async() => { throw new Error(token+key); } });
  assert.equal((await request(unavailable,'/api/discord/channels')).body.error,'DISCORD_UNAVAILABLE');
  const timeout = await serverFor(t,{ timeoutMs:10,fetchImpl:async(url,{ signal }) => new Promise((resolve,reject) => signal.addEventListener('abort',() => reject(new Error('Timeout')),{ once:true })) });
  const timed = await request(timeout,'/api/discord/channels');
  assert.equal(timed.status,504);
  assert.equal(timed.body.error,'DISCORD_TIMEOUT');
});

'use strict';
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const os=require('node:os');
const dotenv=require('dotenv');
const {configure}=require('../scripts/configure');
function fixture(t){
 const directory=fs.mkdtempSync(path.join(os.tmpdir(),'sw-android-setup-'));
 fs.writeFileSync(path.join(directory,'.env.example'),'DISCORD_CLIENT_ID=1539443070037266572\nDISCORD_GUILD_ID=1506076988564050081\n');
 t.after(()=>fs.rmSync(directory,{recursive:true,force:true}));return directory;
}
function prompts(values){return async()=>{assert.ok(values.length,'Unexpected prompt');return values.shift()}}
test('Termux setup writes private backend config and never outputs supplied secrets',async t=>{
 const directory=fixture(t),secret='fikcyjne-hasło-12345';let output='';
 await configure({directory,ask:prompts(['','','',secret,secret]),output:{write(s){output+=s}}});
 const filename=path.join(directory,'.env'); const d=dotenv.parse(fs.readFileSync(filename));
 assert.equal(d.HOST,'127.0.0.1');assert.equal(d.PORT,'3000');assert.equal(d.DISCORD_CLIENT_ID,'1539443070037266572');assert.equal(d.DISCORD_GUILD_ID,'1506076988564050081');assert.equal(d.BACKEND_ACCESS_KEY,secret);assert.equal(d.DISCORD_BOT_TOKEN,'');assert.equal(fs.statSync(filename).mode&0o777,0o600);assert.ok(!output.includes(secret));
});
test('reconfiguration preserves token, password, comments and unrelated settings on blank answers',async t=>{
 const directory=fixture(t),filename=path.join(directory,'.env');const source='# retained\nDISCORD_BOT_TOKEN=FICTIONAL_TOKEN_ONLY\nDISCORD_CLIENT_ID=1539443070037266572\nDISCORD_GUILD_ID=1506076988564050081\nBACKEND_ACCESS_KEY="fictional-connection-secret"\nEXTRA="left#alone" # comment\nHOST=127.0.0.1\nPORT=3000\n';fs.writeFileSync(filename,source);
 await configure({directory,ask:prompts(['','','','']),output:{write(){}}});assert.equal(fs.readFileSync(filename,'utf8'),source);
});
test('mismatched passwords and cancellation leave configuration unchanged',async t=>{
 const directory=fixture(t),filename=path.join(directory,'.env');
 await assert.rejects(configure({directory,ask:prompts(['','','','fictional-password-1','fictional-password-2']),output:{write(){}}}),/Hasła nie są/);
 assert.equal(fs.existsSync(filename),false);
 await assert.rejects(configure({directory,ask:async()=>{throw Object.assign(new Error('Cancelled'),{code:'CANCELLED'})},output:{write(){}}}),/Cancelled/);assert.equal(fs.existsSync(filename),false);
});
test('setup refuses concurrent changes and symlink configurations',async t=>{
 const directory=fixture(t),filename=path.join(directory,'.env');let calls=0;
 await assert.rejects(configure({directory,ask:async()=>{calls++;if(calls===5)fs.writeFileSync(filename,'EXTRA=concurrent\n');return calls>=4?'fictional-password-123':''},output:{write(){}}}),/innym procesie/);assert.equal(fs.readFileSync(filename,'utf8'),'EXTRA=concurrent\n');
 fs.unlinkSync(filename);fs.writeFileSync(path.join(directory,'preserved'),'EXTRA=preserved\n');fs.symlinkSync(path.join(directory,'preserved'),filename);
 await assert.rejects(configure({directory,ask:prompts([]),output:{write(){}}}),/dowiązaniem/);assert.equal(fs.readFileSync(path.join(directory,'preserved'),'utf8'),'EXTRA=preserved\n');
});

"use strict";

const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");
const readline = require("node:readline");
const dotenv = require("dotenv");

function fail(message) { throw new Error(message); }

function quoteValue(value) {
  value = String(value);
  for (const quote of ["'", '"', "`"]) {
    if (!value.includes(quote)) {
      const encoded = `${quote}${value}${quote}`;
      if (dotenv.parse(`VALUE=${encoded}`).VALUE === value) return encoded;
    }
  }
  fail("Nie można bezpiecznie zapisać wartości w pliku .env.");
}

// Replace assignment values only. Keep comments, unrelated lines and line endings.
// Skipping entire quoted values prevents matching apparent assignments inside them.
function mergeEnv(source, updates) {
  const before = dotenv.parse(source);
  const wanted = new Map(Object.entries(updates));
  for (const key of wanted.keys()) if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) fail("Nieprawidłowa nazwa zmiennej.");
  const found = new Set();
  const edits = [];
  const assignments = /^[ \t]*(?:export[ \t]+)?([\w.-]+)(?:[ \t]*=[ \t]*|:[ \t]+)/gm;
  let match;
  while ((match = assignments.exec(source))) {
    const start = assignments.lastIndex;
    let end = start;
    const quote = source[start];
    if (["'", '"', "`"].includes(quote)) {
      end++;
      while (end < source.length) {
        if (source[end] === quote) {
          let slashes = 0;
          for (let i = end - 1; i > start && source[i] === "\\"; i--) slashes++;
          if (slashes % 2 === 0) break;
        }
        end++;
      }
      if (end >= source.length) fail("Plik .env zawiera niedomkniętą wartość. Zachowano go bez zmian.");
      end++;
    } else {
      while (end < source.length && !["\r", "\n", "#"].includes(source[end])) end++;
      while (end > start && /[ \t]/.test(source[end - 1])) end--;
    }
    const nextLine = source.indexOf("\n", end);
    assignments.lastIndex = nextLine < 0 ? source.length : nextLine + 1;
    if (wanted.has(match[1])) {
      found.add(match[1]);
      edits.push({ start, end, value: quoteValue(wanted.get(match[1])) });
    }
  }
  let result = source;
  for (const edit of edits.reverse()) result = result.slice(0, edit.start) + edit.value + result.slice(edit.end);
  const newline = source.includes("\r\n") ? "\r\n" : "\n";
  for (const [key, value] of wanted) if (!found.has(key)) {
    if (result && !result.endsWith("\n")) result += newline;
    result += `${key}=${quoteValue(value)}${newline}`;
  }
  const after = dotenv.parse(result);
  for (const [key, value] of wanted) if (after[key] !== String(value)) fail("Nie udało się bezpiecznie scalić pliku .env. Zachowano go bez zmian.");
  for (const [key, value] of Object.entries(before)) if (!wanted.has(key) && after[key] !== value) fail("Scalanie zmieniłoby inne ustawienia. Zachowano plik .env bez zmian.");
  return result;
}

function createPrompter({ input = process.stdin, output = process.stderr } = {}) {
  return ({ label, secret = false, maxLength = secret ? 4096 : 256 }) => {
    if (!input.isTTY || typeof input.setRawMode !== "function") fail("Otwórz terminal Termux i uruchom npm run configure bez przekierowania wejścia.");
    return new Promise((resolve, reject) => {
      const wasRaw = Boolean(input.isRaw);
      let value = "";
      let finished = false;
      const finish = (error) => {
        if (finished) return;
        finished = true;
        input.removeListener("keypress", onKey);
        input.removeListener("end", onEnd);
        input.removeListener("error", onError);
        process.removeListener("SIGINT", onSignal);
        process.removeListener("SIGTERM", onSignal);
        input.setRawMode(wasRaw);
        input.pause();
        output.write("\n");
        if (error) reject(error); else resolve(value);
      };
      const onSignal = () => finish(Object.assign(new Error("Przerwano konfigurację. Nie zapisano zmian."), { code: "CANCELLED" }));
      const onEnd = onSignal;
      const onError = () => finish(new Error("Nie można odczytać danych z terminala."));
      const onKey = (text, key = {}) => {
        if (key.ctrl && ["c", "d"].includes(key.name)) return onSignal();
        if (["return", "enter"].includes(key.name)) return finish();
        if (key.name === "backspace") {
          if (value) { value = Array.from(value).slice(0, -1).join(""); if (!secret) output.write("\b \b"); }
        } else if (key.ctrl && key.name === "u") {
          if (!secret) output.write("\b \b".repeat(Array.from(value).length));
          value = "";
        } else if (!key.ctrl && !key.meta && text && !/[\x00-\x1f\x7f-\x9f]/.test(text)) {
          if (value.length + text.length > maxLength) return finish(new Error("Wpisana wartość jest za długa. Nie zapisano zmian."));
          value += text;
          if (!secret) output.write(text);
        }
      };
      readline.emitKeypressEvents(input);
      input.on("keypress", onKey);
      input.once("end", onEnd);
      input.once("error", onError);
      process.once("SIGINT", onSignal);
      process.once("SIGTERM", onSignal);
      input.setRawMode(true);
      input.resume();
      output.write(label);
    });
  };
}

function readOptional(filename) {
  try {
    if (fs.lstatSync(filename).isSymbolicLink()) fail("Plik konfiguracji jest dowiązaniem. Skonfiguruj go ręcznie, aby zachować jego cel.");
    return fs.readFileSync(filename, "utf8");
  } catch (error) { if (error.code === "ENOENT") return null; throw error; }
}

function writePrivate(filename, content) {
  fs.mkdirSync(path.dirname(filename), { recursive: true, mode: 0o700 });
  const temporary = path.join(path.dirname(filename), `.${path.basename(filename)}.${process.pid}.${crypto.randomBytes(8).toString("hex")}.tmp`);
  try {
    fs.writeFileSync(temporary, content, { flag: "wx", mode: 0o600 });
    fs.renameSync(temporary, filename);
  } finally { fs.rmSync(temporary, { force: true }); }
}

function validateId(value, label) {
  if (!/^[0-9]{17,20}$/.test(value)) fail(`${label}: wymagane 17–20 cyfr.`);
  return value;
}

async function configure({ directory = path.resolve(__dirname, '..'), ask, output = process.stderr } = {}) {
  ask ||= createPrompter({ output });
  const target = path.join(directory, '.env');
  const original = readOptional(target);
  const saved = dotenv.parse(original || '');
  const template = dotenv.parse(fs.readFileSync(path.join(directory, '.env.example'), 'utf8'));
  output.write('Konfiguracja SW Android na tym telefonie. Token i hasło wpisuje się niewidocznie. Ctrl+C anuluje bez zapisu.\n');
  const tokenInput = await ask({label: saved.DISCORD_BOT_TOKEN ? 'Token Discord (Enter zachowuje zapisany): ' : 'Token Discord (Enter pomija połączenie z Discordem): ', secret:true});
  const token = tokenInput.trim() || saved.DISCORD_BOT_TOKEN || '';
  if (token.length > 4096 || /[\s\x00-\x1f\x7f]/.test(token)) fail('Token nie może zawierać białych znaków.');
  const clientDefault = saved.DISCORD_CLIENT_ID || template.DISCORD_CLIENT_ID;
  const guildDefault = saved.DISCORD_GUILD_ID || template.DISCORD_GUILD_ID;
  const client = validateId((await ask({label:`ID aplikacji [${clientDefault}]: `})).trim() || clientDefault, 'ID aplikacji');
  const guild = validateId((await ask({label:`ID serwera [${guildDefault}]: `})).trim() || guildDefault, 'ID serwera');
  const passwordInput = await ask({label:saved.BACKEND_ACCESS_KEY ? 'Hasło połączenia aplikacji (Enter zachowuje zapisane): ' : 'Hasło połączenia aplikacji (minimum 12 znaków): ',secret:true,maxLength:1024});
  const password = passwordInput || saved.BACKEND_ACCESS_KEY || '';
  if (password.length < 12 || password.length > 1024 || /[\x00-\x1f\x7f\r\n]/.test(password)) fail('Hasło połączenia musi mieć 12–1024 znaki, bez znaków sterujących.');
  if (passwordInput || !saved.BACKEND_ACCESS_KEY) {
    const confirmation = await ask({label:'Powtórz hasło połączenia: ',secret:true,maxLength:1024});
    if (confirmation !== password) fail('Hasła nie są takie same. Uruchom konfigurację ponownie; nie zapisano zmian.');
  }
  const updates = {};
  if (saved.DISCORD_CLIENT_ID !== client) updates.DISCORD_CLIENT_ID = client;
  if (saved.DISCORD_GUILD_ID !== guild) updates.DISCORD_GUILD_ID = guild;
  if (tokenInput.trim() || !saved.DISCORD_BOT_TOKEN) updates.DISCORD_BOT_TOKEN = token;
  if (passwordInput || !saved.BACKEND_ACCESS_KEY) updates.BACKEND_ACCESS_KEY = password;
  if (!saved.HOST) updates.HOST = '127.0.0.1';
  if (!saved.PORT) updates.PORT = '3000';
  const merged = mergeEnv(original || '',updates);
  if (readOptional(target) !== original) fail('Konfiguracja zmieniła się w innym procesie; nie nadpisano pliku.');
  writePrivate(target,merged);
  output.write('Konfiguracja zapisana. W aplikacji SW wpisz to samo hasło w Ustawieniach połączenia.\n');
  if (!token) output.write('Token Discord nie jest ustawiony: serwer uruchomi się, ale dane i wysyłanie na Discord wymagają tokena.\n');
  return {envPath:target};
}
if (require.main === module) configure().catch(error => {
  console.error(error.message);
  process.exitCode = error.code === 'CANCELLED' ? 130 : 1;
});
module.exports = {configure,mergeEnv,createPrompter};

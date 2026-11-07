'use strict';

const DISCORD_API = 'https://discord.com/api/v10';

function problem(status, code, message) { return Object.assign(new Error(message), { status, code }); }
function snowflake(value, name = 'channelId') {
  if (typeof value !== 'string' || !/^[0-9]{17,20}$/.test(value)) throw problem(400, 'INVALID_ID', `${name} musi zawierać 17–20 cyfr.`);
  return value;
}
function text(value, name, limit, required = false) {
  if (value == null && !required) return '';
  if (typeof value !== 'string' || value.length > limit || (required && !value.trim())) throw problem(400, 'INVALID_INPUT', `${name}: wymagany tekst do ${limit} znaków.`);
  return value.trim();
}

function createDiscordClient({ token, guildId, fetchImpl = globalThis.fetch, timeoutMs = 10000 } = {}) {
  snowflake(guildId, 'DISCORD_GUILD_ID');
  if (!Number.isInteger(timeoutMs) || timeoutMs < 1 || timeoutMs > 60000) throw new Error('Nieprawidłowy limit czasu Discord API.');

  async function request(route, options = {}) {
    if (!token) throw problem(503, 'DISCORD_BOT_TOKEN_NOT_CONFIGURED', 'Uruchom npm run configure w Termux i podaj token bota Discord.');
    const signal = AbortSignal.timeout(timeoutMs);
    let response, raw;
    try {
      response = await fetchImpl(`${DISCORD_API}${route}`, { ...options, signal, headers: { Authorization: `Bot ${token}`, 'Content-Type': 'application/json' } });
      raw = await response.text();
    } catch {
      if (signal.aborted) throw problem(504, 'DISCORD_TIMEOUT', 'Discord nie odpowiedział w wymaganym czasie.');
      throw problem(502, 'DISCORD_UNAVAILABLE', 'Nie można połączyć się z Discordem. Sprawdź połączenie internetowe.');
    }
    let data;
    try { data = raw ? JSON.parse(raw) : null; }
    catch { throw problem(502, 'DISCORD_INVALID_RESPONSE', 'Discord zwrócił nieprawidłową odpowiedź.'); }
    if (!response.ok) throw Object.assign(new Error('Discord API error'), { status: response.status, discord: true, details: data });
    return data;
  }

  async function checkedChannel(channelId) {
    snowflake(channelId);
    const channel = await request(`/channels/${channelId}`);
    if (!channel || channel.guild_id !== guildId) throw problem(403, 'CHANNEL_OUTSIDE_GUILD', 'Kanał nie należy do skonfigurowanego serwera SW.');
    if (![0, 5, 10, 11, 12].includes(channel.type)) throw problem(400, 'CHANNEL_NOT_TEXT', 'Wybrany kanał nie obsługuje wiadomości tekstowych.');
    return channel;
  }
  async function listChannels() {
    const channels = await request(`/guilds/${guildId}/channels`);
    if (!Array.isArray(channels)) throw problem(502, 'DISCORD_INVALID_RESPONSE', 'Discord nie zwrócił listy kanałów.');
    return channels;
  }
  async function listMessages(channelId, limit = 50) {
    const requested = Number(limit);
    if (!Number.isInteger(requested) || requested < 1) throw problem(400, 'INVALID_LIMIT', 'Limit wiadomości musi być dodatnią liczbą całkowitą.');
    await checkedChannel(channelId);
    const messages = await request(`/channels/${channelId}/messages?limit=${Math.min(requested, 100)}`);
    if (!Array.isArray(messages)) throw problem(502, 'DISCORD_INVALID_RESPONSE', 'Discord nie zwrócił listy wiadomości.');
    return messages;
  }
  function confirmedMessage(message) {
    if (!message || typeof message.id !== 'string' || !message.id) throw problem(502, 'DISCORD_INVALID_RESPONSE', 'Discord nie potwierdził identyfikatora wysłanej wiadomości. Sprawdź kanał przed ponowną próbą.');
    return message;
  }
  async function sendMessage(channelId, content) {
    content = text(content, 'Treść wiadomości', 2000, true);
    await checkedChannel(channelId);
    return confirmedMessage(await request(`/channels/${channelId}/messages`, { method: 'POST', body: JSON.stringify({ content, allowed_mentions: { parse: [] } }) }));
  }
  async function sendBroadcast({ channelId, title, content, color, imageUrl, mention } = {}) {
    title = text(title, 'Tytuł', 256) || '📢 Komunikat Służby Więziennej';
    content = text(content, 'Treść komunikatu', 4096, true);
    imageUrl = text(imageUrl, 'Adres obrazka', 2048);
    if (imageUrl) {
      let url;
      try { url = new URL(imageUrl); } catch { throw problem(400, 'INVALID_IMAGE_URL', 'Obrazek wymaga poprawnego adresu HTTP lub HTTPS.'); }
      if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) throw problem(400, 'INVALID_IMAGE_URL', 'Obrazek wymaga adresu HTTP lub HTTPS bez danych logowania.');
    }
    const selectedColor = color == null ? 3447003 : color;
    if (!Number.isInteger(selectedColor) || selectedColor < 0 || selectedColor > 0xffffff) throw problem(400, 'INVALID_COLOR', 'Kolor musi być liczbą od 0 do 16777215.');
    if (mention != null && !['', 'none', 'everyone', 'here'].includes(mention)) throw problem(400, 'INVALID_MENTION', 'Nieprawidłowy rodzaj wzmianki.');
    await checkedChannel(channelId);
    const embed = { title, description: content, color: selectedColor, footer: { text: 'Służba Więzienna • Broadcaster' }, timestamp: new Date().toISOString() };
    if (imageUrl) embed.image = { url: imageUrl };
    const explicitMention = ['everyone', 'here'].includes(mention);
    return confirmedMessage(await request(`/channels/${channelId}/messages`, { method: 'POST', body: JSON.stringify({ content: explicitMention ? `@${mention}` : '', embeds: [embed], allowed_mentions: { parse: explicitMention ? ['everyone'] : [] } }) }));
  }
  return { request, listChannels, listMessages, sendMessage, sendBroadcast };
}

function defaultClient() { return createDiscordClient({ token: process.env.DISCORD_BOT_TOKEN, guildId: process.env.DISCORD_GUILD_ID || '1506076988564050081' }); }
module.exports = {
  createDiscordClient, problem, snowflake, text,
  listChannels: () => defaultClient().listChannels(),
  listMessages: (...args) => defaultClient().listMessages(...args),
  sendMessage: (...args) => defaultClient().sendMessage(...args),
  sendBroadcast: (...args) => defaultClient().sendBroadcast(...args)
};

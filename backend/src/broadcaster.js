const DISCORD_API = 'https://discord.com/api/v10';

function requireToken() {
  if (!process.env.DISCORD_BOT_TOKEN) {
    const error = new Error('DISCORD_BOT_TOKEN_NOT_CONFIGURED');
    error.status = 503;
    throw error;
  }
}

async function discordRequest(path, options = {}) {
  requireToken();
  const response = await fetch(`${DISCORD_API}${path}`, {
    ...options,
    headers: {
      Authorization: `Bot ${process.env.DISCORD_BOT_TOKEN}`,
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  });

  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = { raw: text }; }

  if (!response.ok) {
    const error = new Error('DISCORD_API_ERROR');
    error.status = response.status;
    error.details = data;
    throw error;
  }
  return data;
}

async function sendBroadcast({ channelId, title, content, color, imageUrl, mention }) {
  if (!channelId || !content) {
    const error = new Error('CHANNEL_ID_AND_CONTENT_REQUIRED');
    error.status = 400;
    throw error;
  }

  const allowedMentions = { parse: [] };
  let messageContent = '';
  if (mention === 'everyone') {
    messageContent = '@everyone';
    allowedMentions.parse = ['everyone'];
  } else if (mention === 'here') {
    messageContent = '@here';
    allowedMentions.parse = ['everyone'];
  }

  const embed = {
    title: title || '📢 Komunikat Służby Więziennej',
    description: content,
    color: Number(color || 3447003),
    footer: { text: 'Służba Więzienna • Broadcaster' },
    timestamp: new Date().toISOString()
  };

  if (imageUrl) embed.image = { url: imageUrl };

  return discordRequest(`/channels/${channelId}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content: messageContent, embeds: [embed], allowed_mentions: allowedMentions })
  });
}

async function listChannels(guildId) {
  return discordRequest(`/guilds/${guildId}/channels`, { method: 'GET' });
}

module.exports = { sendBroadcast, listChannels };

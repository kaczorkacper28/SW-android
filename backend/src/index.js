require('dotenv').config();

const express = require('express');
const cors = require('cors');

const app = express();
const PORT = Number(process.env.PORT || 3000);
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1506076988564050081';
const CLIENT_ID = process.env.DISCORD_CLIENT_ID || '1539443070037266572';

app.use(cors());
app.use(express.json());

const ranks = [
  'Kandydat',
  'Młodszy Funkcjonariusz',
  'Funkcjonariusz',
  'Starszy Funkcjonariusz',
  'Dowódca Zmiany',
  'Zastępca Naczelnika',
  'Inspektor Generalny'
];

app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'SW Android Backend',
    guildId: GUILD_ID,
    clientId: CLIENT_ID
  });
});

app.get('/api/config', (req, res) => {
  res.json({
    guildId: GUILD_ID,
    clientId: CLIENT_ID,
    ranks
  });
});

app.get('/api/officers', async (req, res) => {
  if (!process.env.DISCORD_BOT_TOKEN) {
    return res.status(503).json({
      error: 'DISCORD_BOT_TOKEN_NOT_CONFIGURED',
      message: 'Backend działa, ale token bota Discord nie został skonfigurowany.'
    });
  }

  try {
    const response = await fetch(
      `https://discord.com/api/v10/guilds/${GUILD_ID}/members?limit=1000`,
      {
        headers: {
          Authorization: `Bot ${process.env.DISCORD_BOT_TOKEN}`
        }
      }
    );

    if (!response.ok) {
      const body = await response.text();
      return res.status(response.status).json({
        error: 'DISCORD_API_ERROR',
        details: body
      });
    }

    const members = await response.json();

    const officers = members
      .filter(member => member.user && !member.user.bot)
      .map(member => ({
        id: member.user.id,
        username: member.user.username,
        displayName: member.nick || member.user.global_name || member.user.username,
        avatar: member.user.avatar
          ? `https://cdn.discordapp.com/avatars/${member.user.id}/${member.user.avatar}.png`
          : null,
        roles: member.roles,
        joinedAt: member.joined_at
      }));

    res.json({
      guildId: GUILD_ID,
      count: officers.length,
      officers
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({
      error: 'BACKEND_ERROR',
      message: 'Nie udało się pobrać członków Discord.'
    });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`SW Backend działa na porcie ${PORT}`);
  console.log(`Guild: ${GUILD_ID}`);
});

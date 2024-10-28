# BardBot-Discord-bot
## Created with:
- [Discord4j](https://discord4j.com/)
- [lavalink-lavaplayer](https://github.com/lavalink-devs/lavaplayer) with with necessary for playing records from YouTube [lavalink-youtube-source](https://github.com/lavalink-devs/youtube-source)

## Details
For user convenience  BotToken, RefreshToken and prefix settings are stored in config.json. This is the only reason my project uses google gson.
My custom youtube search function requires jsoup to work (If you want to you can get rid of it).
Also you will probably want a logger for BardBot to find errors and get your OAuth RefreshToken which is needed if you want to play anything from YouTube. I use logback but you can use and configure anything else like log4j ([find SLF4J providers](https://www.slf4j.org/codes.html#noProviders)).

To host BardBot you need a BotToken which you will get after creating an application at [Discord Developer site](https://discord.com/developers/applications).
You will also need to generate a link to invite your bot and give him some permissions. 
Then you need to parse this token in your config.json, you should also choose your preferred prefix and write it in your config file. "-" is a default BardBot prefix.

You should also check if your dependencies are up to date and update them if needed.

After this you should check readme of [lavalink-devs/youtube-source](https://github.com/lavalink-devs/youtube-source) fo OAuth tokens. During first start logger should display link to generate your OAuth RefreshToken which you will need to parse in your config.json file. 
To use poToken you will need to edit some code.

## Usage
- (prefix)play (url or video title which will return first most popular youtube search for specified video)
- (prefix)help (this command will display all BardBot's commands)
- (prefix)skip (this command will play next track in queue)
- (prefix)queue (shows all queued videos in a list)
- (prefix)clear (clears all videos in a queue)
- (prefix)status (this command will display video thumbnail, author and time to end. This command is going to be reworked soon)
- (prefix)loop (off, single, all - turns off looping, makes BardBot repeatedly playing single track, makes BardBot repeatedly playing entire queue)

BardBot can also play urls from soundcloud, discord attachments and other sources specified in [lavalink-lavaplayer](https://github.com/lavalink-devs/lavaplayer).

## Warning
BardBot can stop working at any time if youtube decides to change something again, but luckily it has only broken once in 4 years of its life.
Also bot comes with logback logger and basic configuration of logback.xml which displays ALL traffic in console. This solution makes potential errors hard to spot. Ideally you should configure your logger to store all important messages in logs folder. For now unconfigured BardBot makes your console unreadable. (I will probably fix it soon)

I am also aware of how much bloated this bot is but in the span of 4 years many dependencies responsible for only playing music from YouTube urls have depreceated. That is the only reason I'm using [lavalink-lavaplayer](https://github.com/lavalink-devs/lavaplayer) with [lavalink-youtube-source](https://github.com/lavalink-devs/youtube-source) and I don't really have time to read their documentation, disable their searching features when YouTube is changing every month.

BardBot is my personal project it may have some errors, it wasn't tested much but I do what I can to make it work. I'm also not a professional programer so any tips, fixes or explanations are welcome.


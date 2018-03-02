# Project Bluestone (Goldmine)

This is Project Bluestone, the rewrite of Goldmine in Java.

# Installation and Setup
I won't go into *huge* detail here, refer to other guilds such as JDA's for that.

**Make sure Java 8 or 9 is installed before proceeding!**
You can download Java [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
Select the JDK instead of JRE or Server JRE.

Have patience, the first build will take a while.

## Windows
Run the following commands in a new instance of `cmd.exe` (Start -> cmd). Administrator is not required.
```bat
cd [DRAG bluestone FOLDER INTO WINDOW]
gradlew shadowJar
```
**Success!**
If everything went right and `BUILD SUCCESSFUL` is displayed, you're good to go.
Proceed to [Configuration](#configuration) to complete the setup.

If there are errors, open an issue on GitHub or join the Discord [support server](https://discord.gg/sYkwfxA).

## macOS
Run the following commands in a new instance of Terminal (Applications -> Utilities -> Terminal).
```bash
cd [DRAG bluestone FOLDER INTO WINDOW]
./gradlew shadowJar
```
**Success!**
If everything went right and `BUILD SUCCESSFUL` is displayed, you're good to go.
Proceed to [Configuration](#configuration) to complete the setup.

If there are errors, open an issue on GitHub or join the Discord [support server](https://discord.gg/sYkwfxA).

## Linux
Run the following commands in a new terminal window of your choice.
```bash
cd [path to bluestone folder]
./gradlew shadowJar
```
**Success!**
If everything went right and `BUILD SUCCESSFUL` is displayed, you're good to go.
Proceed to [Configuration](#configuration) to complete the setup.

If there are errors, open an issue on GitHub or join the Discord [support server](https://discord.gg/sYkwfxA).

# Configuration
You will need to create `config.json` for the bot to be able to start.

## Simple
Paste this into the `config.json` file:
```json
{
  "token": "[token here]"
}
```

Replace `[token here]` with your bot token.
You can obtain a token by going to the Discord [developers page](https://discordapp.com/developers/applications/me) and creating a new app. Fill in the details.

After that, simply click "Create a Bot User" as shown here: ![Create a Bot User](https://user-images.githubusercontent.com/7930239/36882728-66599c4e-1d8a-11e8-969c-e9904aac2268.png)
Confirm the action by clicking "Yes, do it!" in the popup.

Now click `click to reveal` which is after `Token:`, as shown: ![Click to Reveal Token](https://user-images.githubusercontent.com/7930239/36882771-b4e0fe3e-1d8a-11e8-9232-21bbfe34befc.png)

**DO NOT SHOW THE TOKEN TO __ANYONE__!** Keep this secret just like it's your Discord account's password.

Copy the token as shown, and paste it into the `config.json` file described above. ![Copy Token](https://user-images.githubusercontent.com/7930239/36882814-fb2eccae-1d8a-11e8-830c-a5d568aba1ed.png)

## Advanced
This is the template:
```json
{
  "token": "token",
  "shard_count": 2,
  "type": "bot",
  "db_url": "h2:./database",
  "db_user": "username",
  "db_pass": "password",
  "graphite_host": "localhost",
  "graphite_port": 2003,
  "keys": {
    "google": "",
    "discord_bots": "",
    "carbonitex": "",
    "imgflip": {
      "username": "root",
      "password": "toor"
    },
    "sentry": "https://public:private@host:port/1?environment=development&servername=laptop1"
  },
  "chatengine_url": "http://my-chatengine-server.com/ask"
}
```
### If you aren't filling in a value, **delete the line**! Empty values will cause errors.

Put your token in for the `token` key.
Then, choose the number of shards you want to use.
This is typically unnecessary, and serves no benefit,
unless you have 2,500 guilds or more the bot is serving in.

The `db_url` key is for the database connection.
If you don't want to use a server, `h2` is probably the best choice.
The format for h2 is `h2:./[database file name without extension]`.
The file is saved as `name.mv.db`, in the current working directory.
However, h2 can have a server.
If you do want to use a server, such as MySQL, you'll have to write your own URL.
It's just passed to JDBC, so any JDBC connection source without `jdbc:` will work.

Optionally, obtain a Google API key with the following features enabled:
 - Google Custom Search API
 - YouTube Data API v3

and put it in the JSON.

You may also provide Discord Bots and Carbonitex keys if you have valid ones.
However, invalid or empty keys will cause errors
whenever guild count is updated.

For more reliable memes, you should also provide Imgflip account credentials.

For error reporting, provide a Sentry DSN, optionally with `environment` and `servername` set.

At the very least, you must have an empty `keys` object.
If there is no `keys` object, or it is another data type, everything may explode.

**Finished?** Proceed to [Running](#running) to start the bot.

# Running
You can start the bot in two ways:
  - Double-clicking the `jar` file found in `[bluestone folder]/build/libs/bluestone-1.0-SNAPSHOT-all.jar`
  - Opening a terminal, navigating to the `bluestone` folder, and typing `java -jar build/libs/bluestone-1.0-SNAPSHOT-all.jar`

Feel free to automate the process however you want.

# Support
Is something not working?
[Open](https://github.com/kdrag0n/bluestone/issues/new) a GitHub issue describing your problem, or ask in our Discord [support server](https://discord.gg/sYkwfxA).

# Credits
 - The `Strftime` class from Apache Tomcat is included under the Apache 2.0 License.

# Project Bluestone

This is Project Bluestone, the rewrite of Goldmine in Java (and some Scala).

You will need to create `config.json` for the bot to be able to start.
This is the template:
```json
{
  "token": "token",
  "shard_count": 2,
  "type": "bot",
  "keys": {
    "google": "",
    "discord_bots": "",
    "carbonitex": ""
  }
}
```
Put your token in for the `token` key.
Then, choose the number of shards you want to use.
This is typically unnecessary, and serves no benefit,
unless you have 750 guilds or more the bot is serving in.

Optionally, obtain a Google API key with the following features enabled:
 - Google Custom Search API
 - YouTube Data API v3

and put it in the JSON.

You may also provide Discord Bots and Carbonitex keys if you have valid ones.
However, invalid or empty keys will cause errors
whenever guild count is updated.

### Enjoy!
Don't forget that this project is still experimental.

#Credits
 - The `Strftime` class from Apache Tomcat is included under the Apache 2.0 License.
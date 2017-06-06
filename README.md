# Project Bluestone

This is Project Bluestone, the rewrite of Goldmine in Java (and some Scala).

You will need to create `auth.json` for the bot to be able to start.
This is the template:
```json
{
  "token": "token",
  "shardCount": 2,
  "type": "bot",
  "keys": {
    "google": ""
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

### Enjoy!
Don't forget that this project is still experimental.

#Credits
 - The `Strftime` class from Apache Tomcat is included under the Apache 2.0 License.
# Project Bluestone

This is Project Bluestone, the rewrite of Goldmine in Java.

You will need to create `config.json` for the bot to be able to start.
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
    "carbonitex": ""
  },
  "chatengine_url": "http://my-chatengine-server.com/ask"
}
```
Put your token in for the `token` key.
Then, choose the number of shards you want to use.
This is typically unnecessary, and serves no benefit,
unless you have 750 guilds or more the bot is serving in.

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

### Enjoy!
Don't forget that this project is still experimental.

#Credits
 - The `Strftime` class from Apache Tomcat is included under the Apache 2.0 License.
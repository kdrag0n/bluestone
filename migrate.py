#!/usr/bin/env python3

import time
import json
import sys
import random
from datetime import datetime

header = f'''-- Goldmine-->Bluestone JSON-->SQL migration (for MySQL)
-- generated {datetime.now()}
SET NAMES 'utf8mb4';
START TRANSACTION;
ALTER DATABASE `bluestone` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
'''

table_creation = '''
CREATE TABLE IF NOT EXISTS `admins` (`userId` BIGINT NOT NULL , `lastUsername` VARCHAR(32) DEFAULT '' , PRIMARY KEY (`userId`) );
ALTER TABLE `admins` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `admins` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `guild_prefixes` (`guildId` BIGINT NOT NULL , `prefix` VARCHAR(32) NOT NULL , PRIMARY KEY (`guildId`) );
ALTER TABLE `guild_prefixes` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `guild_prefixes` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `guild_welcome_msgs`
(`guildId` BIGINT NOT NULL , `welcome` VARCHAR(2000) DEFAULT '[default]' NOT NULL , `leave` VARCHAR(2000) DEFAULT '[default]' NOT NULL ,
`welcomeEnabled` TINYINT(1) NOT NULL , `leaveEnabled` TINYINT(1) NOT NULL , PRIMARY KEY (`guildId`) );
ALTER TABLE `guild_welcome_msgs` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `guild_welcome_msgs` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `quotes`
(`id` VARCHAR(4) NOT NULL , `quote` VARCHAR(360) NOT NULL , `date` TIMESTAMP NOT NULL ,
`authorId` BIGINT NOT NULL , `authorName` VARCHAR(32) NOT NULL , PRIMARY KEY (`id`) );
ALTER TABLE `quotes` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `quotes` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

CREATE INDEX IF NOT EXISTS `quotes_authorId_idx` ON `quotes` ( `authorId` );

-- data statements
'''

footer = f'''
COMMIT;
-- End migration code'''

statements = []
mode = 'print'
in_path = None
out_path = None

if len(sys.argv) < 2:
    print(f'Usage: {sys.argv[0]} [json path] {"output path"}')
    exit(1)

in_path = sys.argv[1]

if len(sys.argv) > 2:
    mode = 'file'
    out_path = sys.argv[2]

with open(in_path, 'rb') as source:
    json = json.loads(source.read())

bool_true = [
    'yes',
    'yea',
    'ya',
    'yeah',
    1,
    '1',
    'true',
    'yep',
    'yeh',
    'y',
    'ye',
    't',
    'positive',
    'certainly',
    'totally',
    'definitely',
    'on',
    'uh-huh',
    'yes.',
    'yus',
    'true',
    True
]

id_chars = list('0123456789abcdef')
quote_used_ids = ['0000', '0001']

def sqlstr(orig: str) -> str:
    return orig.replace("'", "''")

# CONVERSION

for adm in json['bot_admins']:
    statements.append(f'''
INSERT INTO admins (userId)
VALUES ({adm});''')

for guild_id, properties in json['properties']['by_server'].items():
    welcome = True
    leave = True
    if 'broadcast_join' in properties:
        welcome = properties['broadcast_join'] in bool_true
    if 'broadcast_leave' in properties:
        leave = properties['broadcast_leave'] in bool_true

    statements.append(f'''
INSERT INTO guild_welcome_msgs (guildId, welcomeEnabled, leaveEnabled)
VALUES ({guild_id}, {int(welcome)}, {int(leave)});''')

    if 'command_prefix' in properties:
        statements.append(f'''
INSERT INTO guild_prefixes (guildId, prefix)
VALUES ({guild_id}, '{sqlstr(properties['command_prefix'][:32])}');''')

for quote in json['quotes']:
    date = datetime(quote['date'][2], quote['date'][0], quote['date'][1], hour=12, minute=30, second=30)
    sqldate = date.strftime('%Y-%m-%d %H:%M:%S')
    statements.append(f'''
INSERT INTO quotes (quote, date, authorId, authorName)
VALUES ('{sqlstr(quote['quote'][:360])}',
        '{sqldate}', {quote['author_ids'][0]}, '{sqlstr(quote['author'])}');''')

# END CONVERSION

result = header + table_creation + '\n'.join(statements) + footer

if mode == 'print':
    print(result)
else:
    print('Writing file...')
    with open(out_path, 'wb+') as out:
        out.write(result.encode('utf-8'))
    print(f'File {out_path} written!')

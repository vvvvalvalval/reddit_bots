
## PostgreSQL Setup

```
 [ val@robo: ~/projects/reddit_bots ]
$ pg_ctl init -D data/pg/
The files belonging to this database system will be owned by user "val".
This user must also own the server process.

The database cluster will be initialized with locale "en_US.UTF-8".
The default database encoding has accordingly been set to "UTF8".
The default text search configuration will be set to "english".

Data page checksums are disabled.

fixing permissions on existing directory data/pg ... ok
creating subdirectories ... ok
selecting default max_connections ... 100
selecting default shared_buffers ... 128MB
selecting dynamic shared memory implementation ... posix
creating configuration files ... ok
running bootstrap script ... ok
performing post-bootstrap initialization ... ok
syncing data to disk ... ok

WARNING: enabling "trust" authentication for local connections
You can change this by editing pg_hba.conf or using the option -A, or
--auth-local and --auth-host, the next time you run initdb.

Success. You can now start the database server using:

    /usr/local/Cellar/postgresql/9.6.3/bin/pg_ctl -D data/pg -l logfile start
```

```
createdb reddit_bots_dev_db
```
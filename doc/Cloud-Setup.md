# Setup on OVH

Chose an Ubuntu 16.04 VM.


## Installing OS dependencies

Installing Java and Clojure:

```
sudo apt-get update
sudo apt-get -y install curl git-core openjdk-8-jdk maven rlwrap
curl -O https://download.clojure.org/install/linux-install-1.10.1.536.sh
chmod +x linux-install-1.10.1.536.sh
sudo ./linux-install-1.10.1.536.sh
```

Installing PostgreSQL:

```
sudo apt-get -y install postgresql postgresql-contrib
```

This last command output:

```
Reading package lists... Done
Building dependency tree
Reading state information... Done
The following additional packages will be installed:
  libpq5 postgresql-9.5 postgresql-client-9.5 postgresql-client-common postgresql-common postgresql-contrib-9.5 ssl-cert sysstat
Suggested packages:
  postgresql-doc locales-all postgresql-doc-9.5 libdbd-pg-perl openssl-blacklist isag
The following NEW packages will be installed:
  libpq5 postgresql postgresql-9.5 postgresql-client-9.5 postgresql-client-common postgresql-common postgresql-contrib postgresql-contrib-9.5 ssl-cert sysstat
0 upgraded, 10 newly installed, 0 to remove and 24 not upgraded.
Need to get 4,862 kB of archives.
After this operation, 19.7 MB of additional disk space will be used.
Do you want to continue? [Y/n] Y
Get:1 http://nova.clouds.archive.ubuntu.com/ubuntu xenial-updates/main amd64 libpq5 amd64 9.5.19-0ubuntu0.16.04.1 [78.3 kB]
Get:2 http://nova.clouds.archive.ubuntu.com/ubuntu xenial-updates/main amd64 postgresql-client-common all 173ubuntu0.3 [28.4 kB]
Get:3 http://nova.clouds.archive.ubuntu.com/ubuntu xenial-updates/main amd64 postgresql-client-9.5 amd64 9.5.19-0ubuntu0.16.04.1 [874 kB]
Get:4 http://nova.clouds.archive.ubuntu.com/ubuntu xenial/main amd64 ssl-cert all 1.0.37 [16.9 kB]
Get:5 http://nova.clouds.archive.ubuntu.com/ubuntu xenial-updates/main amd64 postgresql-common all 173ubuntu0.3 [154 kB]
Get:6 http://nova.clouds.archive.ubuntu.com/ubuntu xenial-updates/main amd64 postgresql-9.5 amd64 9.5.19-0ubuntu0.16.04.1 [3,009 kB]
Get:7 http://nova.clouds.archive.ubuntu.com/ubuntu xenial-updates/main amd64 postgresql all 9.5+173ubuntu0.3 [5,392 B]
Get:8 http://nova.clouds.archive.ubuntu.com/ubuntu xenial-updates/main amd64 postgresql-contrib-9.5 amd64 9.5.19-0ubuntu0.16.04.1 [449 kB]
Get:9 http://nova.clouds.archive.ubuntu.com/ubuntu xenial-updates/main amd64 postgresql-contrib all 9.5+173ubuntu0.3 [5,400 B]
Get:10 http://nova.clouds.archive.ubuntu.com/ubuntu xenial-updates/main amd64 sysstat amd64 11.2.0-1ubuntu0.3 [242 kB]
Fetched 4,862 kB in 1s (4,349 kB/s)
Preconfiguring packages ...
Selecting previously unselected package libpq5:amd64.
(Reading database ... 60006 files and directories currently installed.)
Preparing to unpack .../libpq5_9.5.19-0ubuntu0.16.04.1_amd64.deb ...
Unpacking libpq5:amd64 (9.5.19-0ubuntu0.16.04.1) ...
Selecting previously unselected package postgresql-client-common.
Preparing to unpack .../postgresql-client-common_173ubuntu0.3_all.deb ...
Unpacking postgresql-client-common (173ubuntu0.3) ...
Selecting previously unselected package postgresql-client-9.5.
Preparing to unpack .../postgresql-client-9.5_9.5.19-0ubuntu0.16.04.1_amd64.deb ...
Unpacking postgresql-client-9.5 (9.5.19-0ubuntu0.16.04.1) ...
Selecting previously unselected package ssl-cert.
Preparing to unpack .../ssl-cert_1.0.37_all.deb ...
Unpacking ssl-cert (1.0.37) ...
Selecting previously unselected package postgresql-common.
Preparing to unpack .../postgresql-common_173ubuntu0.3_all.deb ...
Adding 'diversion of /usr/bin/pg_config to /usr/bin/pg_config.libpq-dev by postgresql-common'
Unpacking postgresql-common (173ubuntu0.3) ...
Selecting previously unselected package postgresql-9.5.
Preparing to unpack .../postgresql-9.5_9.5.19-0ubuntu0.16.04.1_amd64.deb ...
Unpacking postgresql-9.5 (9.5.19-0ubuntu0.16.04.1) ...
Selecting previously unselected package postgresql.
Preparing to unpack .../postgresql_9.5+173ubuntu0.3_all.deb ...
Unpacking postgresql (9.5+173ubuntu0.3) ...
Selecting previously unselected package postgresql-contrib-9.5.
Preparing to unpack .../postgresql-contrib-9.5_9.5.19-0ubuntu0.16.04.1_amd64.deb ...
Unpacking postgresql-contrib-9.5 (9.5.19-0ubuntu0.16.04.1) ...
Selecting previously unselected package postgresql-contrib.
Preparing to unpack .../postgresql-contrib_9.5+173ubuntu0.3_all.deb ...
Unpacking postgresql-contrib (9.5+173ubuntu0.3) ...
Selecting previously unselected package sysstat.
Preparing to unpack .../sysstat_11.2.0-1ubuntu0.3_amd64.deb ...
Unpacking sysstat (11.2.0-1ubuntu0.3) ...
Processing triggers for libc-bin (2.23-0ubuntu11) ...
Processing triggers for man-db (2.7.5-1) ...
Processing triggers for ureadahead (0.100.0-19.1) ...
Processing triggers for systemd (229-4ubuntu21.23) ...
Setting up libpq5:amd64 (9.5.19-0ubuntu0.16.04.1) ...
Setting up postgresql-client-common (173ubuntu0.3) ...
Setting up postgresql-client-9.5 (9.5.19-0ubuntu0.16.04.1) ...
update-alternatives: using /usr/share/postgresql/9.5/man/man1/psql.1.gz to provide /usr/share/man/man1/psql.1.gz (psql.1.gz) in auto mode
Setting up ssl-cert (1.0.37) ...
Setting up postgresql-common (173ubuntu0.3) ...
Adding user postgres to group ssl-cert

Creating config file /etc/postgresql-common/createcluster.conf with new version

Creating config file /etc/logrotate.d/postgresql-common with new version
Building PostgreSQL dictionaries from installed myspell/hunspell packages...
Removing obsolete dictionary files:
Setting up postgresql-9.5 (9.5.19-0ubuntu0.16.04.1) ...
Creating new cluster 9.5/main ...
  config /etc/postgresql/9.5/main
  data   /var/lib/postgresql/9.5/main
  locale en_US.UTF-8
  socket /var/run/postgresql
  port   5432
update-alternatives: using /usr/share/postgresql/9.5/man/man1/postmaster.1.gz to provide /usr/share/man/man1/postmaster.1.gz (postmaster.1.gz) in auto mode
Setting up postgresql (9.5+173ubuntu0.3) ...
Setting up postgresql-contrib-9.5 (9.5.19-0ubuntu0.16.04.1) ...
Setting up postgresql-contrib (9.5+173ubuntu0.3) ...
Setting up sysstat (11.2.0-1ubuntu0.3) ...

Creating config file /etc/default/sysstat with new version
update-alternatives: using /usr/bin/sar.sysstat to provide /usr/bin/sar (sar) in auto mode
Processing triggers for libc-bin (2.23-0ubuntu11) ...
Processing triggers for ureadahead (0.100.0-19.1) ...
Processing triggers for systemd (229-4ubuntu21.23) ...
```

## PostgreSQL role

Followed: https://www.digitalocean.com/community/tutorials/how-to-install-and-use-postgresql-on-ubuntu-16-04

```
ubuntu@vvvredditbot-instance-s1-2-bhs5:~$ sudo -i -u postgres
postgres@vvvredditbot-instance-s1-2-bhs5:~$ createuser --interactive
Enter name of role to add: redditbots_postgres_user
Shall the new role be a superuser? (y/n) y
postgres@vvvredditbot-instance-s1-2-bhs5:~$ createuser --interactive --pwprompt
Enter name of role to add: redditbots_pg_user
Enter password for new role:
Enter it again:
Shall the new role be a superuser? (y/n) y

ubuntu@vvvredditbot-instance-s1-2-bhs5:~$ sudo -u postgres createdb redditbots_pg_db
```

We can then connect with psql, via:

```
ubuntu@vvvredditbot-instance-s1-2-bhs5:~$ psql -h localhost -p 5432 -d redditbots_pg_db  -U redditbots_pg_user
Password for user redditbots_pg_user:
psql (9.5.19)
SSL connection (protocol: TLSv1.2, cipher: ECDHE-RSA-AES256-GCM-SHA384, bits: 256, compression: off)
Type "help" for help.

redditbots_pg_db=#
```


## Remote REPL

Note: on the remote machine, the commands are executed from the project's directory.

Creating a REPL output file:

```
ubuntu@vvvredditbot-instance-s1-2-bhs5:~/reddit_bots$ touch ../repl-out.txt
```

Then start an nREPL server:

```
ubuntu@vvvredditbot-instance-s1-2-bhs5:~/reddit_bots$ nohup clojure -A:dev:nREPL -J-Xms1g -J-Xmx1g -m nrepl.cmdline --port 8888  --middleware "[sc.nrepl.middleware/wrap-letsc]" </dev/null >>../repl-out.txt 2>&1 &
```

Then start an SSH tunnel on my dev laptop:

```
$ ssh -N -L 8888:localhost:8888 -i ~/.ssh/ovhcloud_id_rsa ubuntu@51.79.29.59
```

Which then enables to connect any nREPL client, e.g:

```
$ clj -Sdeps '{:deps {nrepl {:mvn/version "0.5.0"}}}' -m nrepl.cmdline --connect --host localhost --port 8888
```

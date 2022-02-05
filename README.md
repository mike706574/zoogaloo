# zoogaloo

A webapp.

## Development

### Starting up

Start the development database:

```
docker compose up
```

Start the frontend:

```
npx shadow-cljs watch frontend
```

Start the backend without a REPL:

```
lein run
```

Start the backend with a REPL:

```
lein repl
(load-file "dev/user.clj")
(reset)
```

## Connecting to the database

Connect to the local database:

```
PGPASSWORD=postgres psql -h localhost -p 5432 -d postgres -U postgres
```

Connect to the production database:

```
PGPASSWORD=ZOOGALOO_DB_PASSWORD psql -h ZOOGALOO_DB_HOST -d ZOOGALOO_DB_NAME -U $ZOOGALOO_DB_USER
```

Dump production database:

```
pg_dump -h $ZOOGALOO_DB_HOST -U $ZOOGALOO_DB_USER -W -F t --no-owner --no-acl $ZOOGALOO_DB_NAME > zoogaloo.tar
```

Restore production database over local database:

```
pg_restore -h localhost -d postgres -U postgres -p 5432 -c -W zoogaloo.tar
```

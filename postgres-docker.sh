docker run \
--name psql --rm -d -p 5433:5432 \
-e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" -e POSTGRES_DB=megalab_news -e POSTGRES_USER=eld \
postgres:14.6-alpine
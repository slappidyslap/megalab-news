## А что делать?

Все команды выполняются из bash

Если русские символы не отображаются: 
```bash
chcp.com 1251
```

Включить кэширование: 
```bash
export CACHE_ENABLED=true
```

Запустить через maven: 
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Как тестовые данные был использован следующий 
[dataset](https://www.kaggle.com/datasets/kaggle/kaggle-blog-winners-posts)

Выполнить следующий скрипт для импорта

```h2
INSERT INTO posts (title, content, created_date, description)
SELECT title, content, 
       FORMATDATETIME(publication_date, 'yyyy-MM-dd')::DATE AS created_date, 'lorem'
FROM CSVREAD('<dump.csv>')
LIMIT 30;
```
## Полезные ссылки

https://stackoverflow.com/questions/62045161/how-to-use-clob-with-hibernate-and-both-postgres-and-h2

https://www.baeldung.com/spring-data-jpa-named-entity-graphs

https://www.baeldung.com/jpa-cascade-types

https://habr.com/ru/company/haulmont/blog/653843/

https://www.baeldung.com/mapstruct

https://www.baeldung.com/jackson-json-view-annotation

https://www.baeldung.com/spring-data-jpa-pagination-sorting

https://reflectoring.io/unit-testing-spring-boot/

https://stackoverflow.com/questions/32984799/fetchmode-join-vs-subselect

https://stackoverflow.com/questions/65757777/how-to-fetch-multiple-lists-with-entitygraph

https://stackoverflow.com/questions/33279153/rest-api-file-ie-images-processing-best-practices

https://spring.io/guides/gs/uploading-files/

https://stackoverflow.com/questions/5245682/how-to-manage-the-transactionwhich-includes-file-io-when-an-ioexception-is-thr

https://www.freecodecamp.org/news/how-to-implement-cacheable-pagination-of-frequently-changing-content-c8ddc8269e81/

https://stackoverflow.com/questions/59897491/how-can-i-retrieve-sort-property-and-direction-from-a-spring-data-pageable

https://reflectoring.io/bean-validation-with-spring-boot/

https://sysout.ru/vvedenie-v-aop-v-spring-boot/

https://springdoc.org/v2/#getting-started

https://dzone.com/articles/best-performance-practices-for-hibernate-5-and-spr-2

https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/JpaRepository.html#getReferenceById(ID)

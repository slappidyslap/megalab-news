INSERT INTO posts (title, created_date, description, content)
VALUES ('hibernate 1', CURRENT_DATE,
        '1 chapter of hibernate', 'lorem'),
       ('hibernate 2', CURRENT_DATE - INTERVAL '1' YEAR,
        '2 chapter of hibernate', 'lorem');
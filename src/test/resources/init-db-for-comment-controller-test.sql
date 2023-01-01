INSERT INTO posts (post_id, title, content, description, created_date)
VALUES (1, '1 post', 'lorem', '', current_date),
       (2, '2 post', 'lorem', '', current_date);

INSERT INTO comments (comment_id, content, post_id, commentator_id, created_date)
VALUES (1, '1 c - 1 p', 1, 0, current_date),
       (2, '2 c - 1 p', 1, 0, current_date),
       (3, '1 c - 2 p', 2, 0, current_date),
       (4, '2 c - 2 p', 2, 0, current_date);

INSERT INTO comments (comment_id, content, post_id, parent_comment_id, commentator_id, created_date)
VALUES (5,'1 c - 1 c - 1 p', 1, 1, 0, current_date),
       (6,'1 c - 2 c - 2 p', 2, 4, 0, current_date);

ALTER TABLE comments ALTER COLUMN comment_id RESTART WITH 7;

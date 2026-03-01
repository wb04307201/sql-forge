CREATE TABLE IF NOT EXISTS score
(
    id integer NOT NULL,
    student_id integer NOT NULL,
    grade integer NOT NULL,
    PRIMARY KEY (id)
);

insert into score(id,student_id,grade)values(1,1,80);
insert into score(id,student_id,grade)values(2,2,90);
insert into score(id,student_id,grade)values(3,3,60);
insert into score(id,student_id,grade)values(4,4,70);
insert into score(id,student_id,grade)values(5,5,80);
insert into score(id,student_id,grade)values(6,6,65);
insert into score(id,student_id,grade)values(7,7,85);


SELECT * FROM score
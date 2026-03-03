DROP TABLE IF EXISTS student;
CREATE TABLE student (
                           id int unsigned NOT NULL AUTO_INCREMENT,
                           name varchar(100) NOT NULL,
                           sex varchar(5) NOT NULL,
                           PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3;

INSERT INTO student (id, name, sex) VALUES (1, '小明', '男');
INSERT INTO student (id, name, sex) VALUES (2, '小红', '女');
INSERT INTO student (id, name, sex) VALUES (3, '小邋遢', '男');
INSERT INTO student (id, name, sex) VALUES (4, '锐锐', '女');
INSERT INTO student (id, name, sex) VALUES (5, '王权富贵', '男');
INSERT INTO student (id, name, sex) VALUES (6, '李雷', '男');
INSERT INTO student (id, name, sex) VALUES (7, '韩梅梅', '女');


SELECT * FROM student
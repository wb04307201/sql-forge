package cn.wubo.sql.forge;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "MYSQL.student")
public class Student {

    @Id
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "sex")
    private String sex;
}

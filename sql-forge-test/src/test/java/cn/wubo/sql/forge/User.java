package cn.wubo.sql.forge;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Table(name = "users")
public class User {

    @Id
    private String id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "create")
    private LocalDate create;
}

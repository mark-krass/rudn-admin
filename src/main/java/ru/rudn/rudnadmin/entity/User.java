package ru.rudn.rudnadmin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import ru.rudn.rudnadmin.entity.vpn.Vpn;

import java.time.LocalDate;

@Entity
@Table(name = "rudn_user")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String name;

    @Column
    private String middleName;

    @Column
    private String lastname;

    @Column
    private LocalDate birthday;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Student student;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Vpn vpn;

}

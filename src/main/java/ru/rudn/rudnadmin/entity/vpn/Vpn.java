package ru.rudn.rudnadmin.entity.vpn;

import jakarta.persistence.*;
import lombok.*;
import ru.rudn.rudnadmin.entity.User;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Vpn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private String link;
}



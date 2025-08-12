package ru.rudn.rudnadmin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "direction_dictionary")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Direction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @NotBlank(message = "Название направления обязательно")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Код направления обязателен")
    @Column(nullable = false, unique = true)
    private String code;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

}

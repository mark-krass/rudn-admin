package ru.rudn.rudnadmin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.*;

@Entity
@Table(name = "group_dictionary")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @NotBlank(message = "Название группы обязательно")
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull(message = "Год обязателен")
    @Min(value = 1900, message = "Год должен быть не меньше 1900")
    @Max(value = 2200, message = "Год должен быть не больше 2200")
    @Column(nullable = false)
    private Short year;

    @NotNull(message = "Направление обязательно")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direction_id", nullable = false)
    private Direction direction;


}

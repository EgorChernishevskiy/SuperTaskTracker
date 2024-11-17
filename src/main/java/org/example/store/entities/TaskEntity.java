package org.example.store.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task")
public class TaskEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String description;

    private int position;

    @ManyToOne
    private TaskStateEntity taskState;
}
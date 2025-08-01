package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // 태그 고유 식별 번호

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name; // 태그 이름 (예: "요리", "자취", "그림", "여행" 등)
}

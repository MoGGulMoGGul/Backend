package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tip_tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // 고유 식별 번호

    @Column(name = "tip_no", nullable = false)
    private Long tipNo; // 연결된 꿀팁 번호 (tip.no)

    @Column(name = "tag_no", nullable = false)
    private Long tagNo; // 연결된 태그 번호 (tag.no)
}

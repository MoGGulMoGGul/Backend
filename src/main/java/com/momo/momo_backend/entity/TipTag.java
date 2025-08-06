package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tip_tag")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TipTag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tip_no", nullable = false)
    private Tip tip;          // ✔ Tip 엔티티와 연관

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_no", nullable = false)
    private Tag tag;          // ✔ Tag 엔티티와 연관
}

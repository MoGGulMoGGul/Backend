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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tip_no", nullable = false)
    private Tip tip;  // 연결된 꿀팁 번호 (tip.no)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_no", nullable = false)
    private Tag tag;  // 연결된 태그 번호 (tag.no)
}
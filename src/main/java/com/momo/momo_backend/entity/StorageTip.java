package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "storage_tip")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "storage_no", nullable = false)
    private Storage storage; // 꿀팁 번호 (tip.no)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tip_no", nullable = false)
    private Tip tip; // 보관함 번호 (storage.no)
}
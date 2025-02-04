package com.kir138.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private Long version; // Для оптимистичной блокировки

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}

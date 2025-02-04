package com.kir138.entity;

import com.kir138.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime timestamp;

    @ManyToOne
    private Account fromAccount;

    @ManyToOne
    private Account toAccount;
}

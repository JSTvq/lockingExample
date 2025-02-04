package com.kir138.service;

import com.kir138.entity.Account;
import com.kir138.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.RollbackException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.lang.module.ResolutionException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final EntityManagerFactory entityManagerFactory;

    // Конструктор получает репозиторий и создаёт фабрику сущностей.
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        // Указываем имя persistence-unit из файла persistence.xml или application.properties (если используется Spring Boot – это может настраиваться иначе)
        this.entityManagerFactory = Persistence.createEntityManagerFactory("your-persistence-unit");
    }

    // Метод run запускает заполнение таблиц и затем много-потоковую обработку переводов.
    public void run() {
        ExecutorService es = Executors.newFixedThreadPool(10);
        try {
            // Заполняем таблицы начальными данными. Метод persistFill мы реализуем ниже.
            persistFill();

            // Этот пример предполагает, что persistFill создала два счёта с id = 1 и id = 2.
            // Если id еще не установлены, можно их получить через accountRepository, но для демонстрации примем id как 1 и 2.
            Long fromAccountId = 1L;
            Long toAccountId = 2L;
            BigDecimal transferAmount = BigDecimal.TEN;

            // Запускаем 10 потоков, которые выполняют перевод.
            CountDownLatch latch = new CountDownLatch(10);
            for (int i = 0; i < 10; i++) {
                es.execute(() -> {
                    try {
                        transferMoney(fromAccountId, toAccountId, transferAmount);
                    } catch (Exception e) {
                        System.out.println("Ошибка во время перевода в потоке " + Thread.currentThread().getName() + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            es.shutdown();
            if (!es.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println("Некоторые потоки не завершились за отведенное время.");
            }
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            entityManagerFactory.close();
        }
    }

    // Метод перевода средств с использованием пессимистической блокировки.
    public void transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // Создаем отдельный EntityManager для этого потока, если потребуется прямая работа с JPA.
        // В данном примере accountRepository уже использует свой EM, поэтому преимущественно операции ведутся через него.
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            System.out.println("Начало перевода средств. Поток: " + Thread.currentThread().getName());

            // Получаем счет-отправитель с пессимистической блокировкой
            Account from = accountRepository.findByIdWithPessimisticLock(fromAccountId)
                    .orElseThrow(() -> new ArithmeticException());
            System.out.println("Заблокирован счет отправителя id=" + from.getId()
                    + " в потоке " + Thread.currentThread().getName());

            // Получаем счет-получатель с пессимистической блокировкой
            Account to = accountRepository.findByIdWithPessimisticLock(toAccountId)
                    .orElseThrow(() -> new ArithmeticException());
            System.out.println("Заблокирован счет получателя id=" + to.getId()
                    + " в потоке " + Thread.currentThread().getName());

            // Лог перед обновлением балансов
            System.out.println("Перед изменением балансов: счет-отправитель баланс = "
                    + from.getBalance() + ", счет-получатель баланс = " + to.getBalance());

            if (from.getBalance().compareTo(amount) < 0) {
                System.out.println("Недостаточно средств на счете отправителя id=" + from.getId());
                throw new ResolutionException();
            }

            from.setBalance(from.getBalance().subtract(amount));
            to.setBalance(to.getBalance().add(amount));

            // Сохраняем изменения в базе
            accountRepository.saveAll(List.of(from, to));

            System.out.println("После изменения балансов: счет-отправитель баланс = "
                    + from.getBalance() + ", счет-получатель баланс = " + to.getBalance() +
                    ". Перевод завершен в потоке " + Thread.currentThread().getName());
        } finally {
            em.close();
        }
    }

    // Метод, заполняющий таблицы начальными данными (создаем два счета с балансом 1000 каждый)
    public void persistFill() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            // Создаем два счета, начальный баланс - 1000
            Account account1 = new Account();
            Account account2 = new Account();
            em.persist(account1);
            em.persist(account2);

            em.getTransaction().commit();
            System.out.println("persistFill: Созданы счета с id: "
                    + account1.getId() + " и " + account2.getId());
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}

package com.evshare.repository;

import com.evshare.model.Charger;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChargerRepository extends JpaRepository<Charger, Long> {
    List<Charger> findByActiveTrue();
    List<Charger> findByActiveTrueAndCityContainingIgnoreCase(String city);

    // Serializes booking attempts for the same charger across application instances.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Charger c where c.id = :id and c.active = true")
    Optional<Charger> findActiveByIdForUpdate(@Param("id") Long id);
}

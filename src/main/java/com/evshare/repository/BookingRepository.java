package com.evshare.repository;
import com.evshare.model.Booking; import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDate; import java.util.List; import java.util.Optional;
public interface BookingRepository extends JpaRepository<Booking,Long> { List<Booking> findByChargerIdAndBookingDateAndStatusNot(Long id, LocalDate date, String status); Optional<Booking> findByPaymentOrderId(String orderId); }

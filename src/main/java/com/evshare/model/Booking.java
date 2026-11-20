package com.evshare.model;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalTime;
@Entity @Table(name="bookings")
public class Booking {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long chargerId; private String customerName; private String customerEmail; private LocalDate bookingDate; private LocalTime startTime; private LocalTime endTime; private BigDecimal estimatedUnits; private BigDecimal amount; private String status="CONFIRMED";
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getChargerId(){return chargerId;} public void setChargerId(Long v){chargerId=v;} public String getCustomerName(){return customerName;} public void setCustomerName(String v){customerName=v;} public String getCustomerEmail(){return customerEmail;} public void setCustomerEmail(String v){customerEmail=v;} public LocalDate getBookingDate(){return bookingDate;} public void setBookingDate(LocalDate v){bookingDate=v;} public LocalTime getStartTime(){return startTime;} public void setStartTime(LocalTime v){startTime=v;} public LocalTime getEndTime(){return endTime;} public void setEndTime(LocalTime v){endTime=v;} public BigDecimal getEstimatedUnits(){return estimatedUnits;} public void setEstimatedUnits(BigDecimal v){estimatedUnits=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}

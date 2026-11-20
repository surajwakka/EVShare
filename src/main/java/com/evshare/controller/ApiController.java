package com.evshare.controller;
import com.evshare.model.*; import com.evshare.repository.*;
import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
import java.math.*; import java.time.*; import com.evshare.service.PaytmPaymentService; import java.util.*; import org.springframework.transaction.annotation.Transactional;
@RestController @RequestMapping("/api")

public class ApiController {
 private final ChargerRepository chargers; private final BookingRepository bookings; private final PaytmPaymentService paytm;
 public ApiController(ChargerRepository c, BookingRepository b, PaytmPaymentService paytm){chargers=c;bookings=b;this.paytm=paytm;}
 @GetMapping("/chargers") public List<Charger> list(@RequestParam(required=false) String city){return (city==null||city.isBlank())?chargers.findByActiveTrue():chargers.findByActiveTrueAndCityContainingIgnoreCase(city);}
 @PostMapping("/chargers") public ResponseEntity<?> create(@Valid @RequestBody Charger c){if(c.getConditionGrade()!=null&&!Set.of("EXCELLENT","GOOD","FAIR","NEEDS_REPAIR").contains(c.getConditionGrade().toUpperCase(Locale.ROOT)))return ResponseEntity.badRequest().body(Map.of("error","Choose a valid charger condition grade."));if("NEEDS_REPAIR".equalsIgnoreCase(c.getConditionGrade()))return ResponseEntity.badRequest().body(Map.of("error","Chargers marked as needing repair cannot be listed for bookings."));if(c.getInstallationYear()!=null&&(c.getInstallationYear()<1990||c.getInstallationYear()>Year.now().getValue()))return ResponseEntity.badRequest().body(Map.of("error","Invalid installation year."));c.setHostVerified(false);c.setSafetyVerified(false);return ResponseEntity.status(201).body(chargers.save(c));}
 @GetMapping("/chargers/{id}") public Charger get(@PathVariable Long id){return chargers.findById(id).orElseThrow(()->new NoSuchElementException("Charger not found"));}
 @PostMapping("/bookings") @Transactional public ResponseEntity<?> book(@RequestBody Map<String,String> p){
  try { if (p == null || p.get("chargerId") == null || p.get("date") == null || p.get("startTime") == null || p.get("endTime") == null || p.get("customerName") == null || p.get("customerEmail") == null) return ResponseEntity.badRequest().body(Map.of("error", "Missing required booking details."));
   Long chargerId=Long.valueOf(p.get("chargerId")); Charger c=chargers.findActiveByIdForUpdate(chargerId).orElseThrow(()->new NoSuchElementException("Charger not found or unavailable")); if(c.getConditionGrade()!=null&&!Set.of("EXCELLENT","GOOD","FAIR","NEEDS_REPAIR").contains(c.getConditionGrade().toUpperCase(Locale.ROOT)))return ResponseEntity.badRequest().body(Map.of("error","Choose a valid charger condition grade."));if("NEEDS_REPAIR".equalsIgnoreCase(c.getConditionGrade()))return ResponseEntity.badRequest().body(Map.of("error","This charger is not available for bookings.")); LocalDate date=LocalDate.parse(p.get("date")); LocalTime start=LocalTime.parse(p.get("startTime")), end=LocalTime.parse(p.get("endTime"));
   if(!date.isAfter(LocalDate.now().minusDays(1))||!end.isAfter(start)||start.isBefore(LocalTime.MIN)||end.isAfter(LocalTime.MAX)) return ResponseEntity.badRequest().body(Map.of("error","Choose a valid future date and time range."));
   if(start.isBefore(LocalTime.parse(c.getAvailableFrom()))||end.isAfter(LocalTime.parse(c.getAvailableTo()))) return ResponseEntity.badRequest().body(Map.of("error","This time is outside the host's listed availability."));
   boolean conflict=bookings.findByChargerIdAndBookingDateAndStatusNot(c.getId(),date,"CANCELLED").stream().anyMatch(b->!start.isBefore(b.getEndTime())&&!end.isAfter(b.getStartTime()) || start.isBefore(b.getEndTime())&&end.isAfter(b.getStartTime()));
   if(conflict)return ResponseEntity.status(409).body(Map.of("error","That time slot is already booked. Please choose another."));
   BigDecimal hours=BigDecimal.valueOf(Duration.between(start,end).toMinutes()).divide(BigDecimal.valueOf(60),3,RoundingMode.HALF_UP);
   BigDecimal units=c.getPowerKw().multiply(hours).setScale(2,RoundingMode.HALF_UP); BigDecimal amount=units.multiply(c.getPricePerKwh()).setScale(2,RoundingMode.HALF_UP);
   Booking b=new Booking(); b.setChargerId(c.getId()); b.setCustomerName(p.get("customerName").trim()); b.setCustomerEmail(p.get("customerEmail").trim().toLowerCase(Locale.ROOT)); b.setBookingDate(date); b.setStartTime(start); b.setEndTime(end); b.setEstimatedUnits(units); b.setAmount(amount); b.setStatus("PENDING_PAYMENT"); b.setPaymentStatus("NOT_INITIATED"); b.setPaymentOrderId("EVS-"+UUID.randomUUID().toString().substring(0,12).toUpperCase());
   b=bookings.save(b); return ResponseEntity.status(201).body(Map.of("bookingId",b.getId(),"orderId",b.getPaymentOrderId(),"estimatedUnits",units,"amount",amount,"currency","INR","status",b.getStatus(),"paymentStatus",b.getPaymentStatus(),"message","Slot held pending Paytm payment. Final charging cost should use measured energy delivered."));
  } catch(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"Invalid booking details":e.getMessage()));}
 }
 @PostMapping("/payments/paytm/initiate") public ResponseEntity<?> initiatePaytm(@RequestBody Map<String,String> p){
  try { String orderId=p.get("orderId"); if(orderId==null||orderId.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","orderId is required")); return ResponseEntity.ok(paytm.initiate(orderId)); }
  catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("error",e.getMessage()));}
 }
 @PostMapping("/payments/paytm/verify") public ResponseEntity<?> verifyPaytm(@RequestBody Map<String,String> p){
  try { String orderId=p.get("orderId"); if(orderId==null||orderId.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","orderId is required")); return ResponseEntity.ok(paytm.verifyAndUpdate(orderId)); }
  catch(NoSuchElementException e){return ResponseEntity.notFound().build();}
  catch(IllegalStateException e){return ResponseEntity.status(502).body(Map.of("error",e.getMessage()));}
 }
 @PostMapping("/payments/paytm/callback") public ResponseEntity<?> paytmCallback(@RequestParam Map<String,String> params, @RequestBody(required=false) Map<String,String> body){
  String orderId=params.getOrDefault("ORDERID", body==null?null:body.get("ORDERID")); if(orderId==null) return ResponseEntity.badRequest().body(Map.of("error","Missing Paytm order id"));
  try { return ResponseEntity.ok(paytm.verifyAndUpdate(orderId)); } catch(Exception e){return ResponseEntity.status(502).body(Map.of("error","Payment status could not be verified; booking remains unconfirmed."));}
 }
 @GetMapping("/bookings/{id}") public Booking booking(@PathVariable Long id){return bookings.findById(id).orElseThrow(()->new NoSuchElementException("Booking not found"));}
}

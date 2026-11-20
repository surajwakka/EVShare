package com.evshare.service;

import com.evshare.model.Booking;
import com.evshare.repository.BookingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PaytmPaymentService {
    private final BookingRepository bookings;
    private final ObjectMapper mapper;
    private final RestClient http = RestClient.builder().build();
    @Value("${paytm.environment:STAGING}") private String environment;
    @Value("${paytm.mid:}") private String mid;
    @Value("${paytm.merchant-key:}") private String merchantKey;
    @Value("${paytm.website-name:WEBSTAGING}") private String websiteName;
    @Value("${paytm.callback-url:}") private String callbackUrl;
    @Value("${paytm.checkout-script:https://securestage.paytmpayments.com/merchantpgpui/checkoutjs/merchants/}") private String checkoutScript;

    public PaytmPaymentService(BookingRepository bookings, ObjectMapper mapper) { this.bookings=bookings; this.mapper=mapper; }

    public Map<String,Object> initiate(String orderId) {
        if (!configured()) throw new IllegalStateException("Paytm staging credentials are missing. Configure PAYTM_MID and PAYTM_MERCHANT_KEY.");
        Booking booking=bookings.findByPaymentOrderId(orderId).orElseThrow(()->new NoSuchElementException("Booking not found"));
        if ("PAID".equals(booking.getPaymentStatus())) throw new IllegalStateException("This booking is already paid.");
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("requestType","Payment"); body.put("mid",mid); body.put("websiteName",websiteName); body.put("orderId",orderId);
        if (callbackUrl != null && !callbackUrl.isBlank()) body.put("callbackUrl",callbackUrl);
        Map<String,String> amount=new LinkedHashMap<>(); amount.put("value",booking.getAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()); amount.put("currency","INR"); body.put("txnAmount",amount);
        Map<String,String> user=new LinkedHashMap<>(); user.put("custId","EVS_CUST_"+booking.getId()); user.put("email",booking.getCustomerEmail()); body.put("userInfo",user);
        try {
            String bodyJson=mapper.writeValueAsString(body);
            Map<String,Object> payload=new LinkedHashMap<>(); payload.put("body",body); payload.put("head",Map.of("signature",PaytmChecksum.generate(bodyJson,merchantKey)));
            String domain="https://securestage.paytmpayments.com";
            Map response=http.post().uri(domain+"/theia/api/v1/initiateTransaction?mid={mid}&orderId={orderId}",mid,orderId).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
            Map respBody=(Map)response.getOrDefault("body",Map.of());
            String token=(String)respBody.get("txnToken");
            if (token==null || token.isBlank()) throw new IllegalStateException("Paytm did not return a transaction token: "+respBody.getOrDefault("resultMsg","Unknown gateway response"));
            booking.setPaymentStatus("INITIATED"); bookings.save(booking);
            return Map.of("configured",true,"environment","STAGING","mid",mid,"orderId",orderId,"amount",booking.getAmount(),"txnToken",token,"checkoutScript",checkoutScript+mid+".js","message","Paytm staging transaction initialized.");
        } catch (RestClientException e) { throw new IllegalStateException("Could not connect to Paytm staging. Please retry.",e); }
        catch (Exception e) { if(e instanceof RuntimeException r) throw r; throw new IllegalStateException("Could not prepare Paytm request",e); }
    }

    @Transactional
    public Map<String,Object> verifyAndUpdate(String orderId) {
        if (!configured()) throw new IllegalStateException("Paytm credentials are not configured.");
        Booking booking=bookings.findByPaymentOrderId(orderId).orElseThrow(()->new NoSuchElementException("Booking not found"));
        try {
            Map<String,Object> body=new LinkedHashMap<>(); body.put("mid",mid); body.put("orderId",orderId);
            String bodyJson=mapper.writeValueAsString(body);
            Map<String,Object> payload=Map.of("body",body,"head",Map.of("signature",PaytmChecksum.generate(bodyJson,merchantKey)));
            String domain="https://securestage.paytmpayments.com";
            Map response=http.post().uri(domain+"/v3/order/status").contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
            Map respBody=(Map)response.getOrDefault("body",Map.of());
            Object resultInfo=respBody.get("resultInfo");
            String resultStatus=resultInfo instanceof Map ri?String.valueOf(ri.get("resultStatus")):String.valueOf(respBody.getOrDefault("STATUS",""));
            String txnId=String.valueOf(respBody.getOrDefault("txnId",""));
            String txnAmount=String.valueOf(respBody.getOrDefault("txnAmount",""));
            boolean amountMatches=false;
            try { amountMatches=new BigDecimal(txnAmount).compareTo(booking.getAmount())==0; } catch(Exception ignored) {}
            if ("TXN_SUCCESS".equals(resultStatus) && amountMatches) { booking.setPaymentStatus("PAID"); booking.setStatus("CONFIRMED"); booking.setPaytmTxnId(txnId); }
            else if ("TXN_FAILURE".equals(resultStatus)) { booking.setPaymentStatus("FAILED"); }
            bookings.save(booking);
            return Map.of("orderId",orderId,"paymentStatus",booking.getPaymentStatus(),"bookingStatus",booking.getStatus(),"gatewayStatus",resultStatus.isBlank()?"UNKNOWN":resultStatus,"amountVerified",amountMatches);
        } catch (RestClientException e) { throw new IllegalStateException("Could not verify payment with Paytm. Try again.",e); }
        catch (Exception e) { if(e instanceof RuntimeException r) throw r; throw new IllegalStateException("Payment verification failed",e); }
    }

    public boolean configured(){return mid!=null&&!mid.isBlank()&&merchantKey!=null&&!merchantKey.isBlank();}
    public boolean isStaging(){return !"PRODUCTION".equalsIgnoreCase(environment);}
}

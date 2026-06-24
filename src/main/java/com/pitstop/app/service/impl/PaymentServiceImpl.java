package com.pitstop.app.service.impl;

import com.pitstop.app.config.RazorpayClientProvider;
import com.pitstop.app.config.RazorpaySignatureVerifier;
import com.pitstop.app.constants.BookingStatus;
import com.pitstop.app.constants.PaymentStatus;
import com.pitstop.app.constants.PaymentType;
import com.pitstop.app.dto.InitiatePaymentResponse;
import com.pitstop.app.dto.PaymentVerifyRequest;
import com.pitstop.app.exception.BusinessException;
import com.pitstop.app.exception.ResourceNotFoundException;
import com.pitstop.app.model.Booking;
import com.pitstop.app.model.BookingStatusWithTimeStamp;
import com.pitstop.app.model.Payment;
import com.pitstop.app.repository.BookingRepository;
import com.pitstop.app.repository.PaymentRepository;
import com.pitstop.app.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final RazorpayClientProvider razorpayClientProvider;
    private final RazorpaySignatureVerifier razorpaySignatureVerifier;

    @Value("${razorpay.key-id}")
    private String razorpayKey;
    @Value("${razorpay.key-secret}")
    private String razorpaySecret;

    @Override
    public InitiatePaymentResponse initiatePayment(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if(booking.getCurrentStatus() != BookingStatus.BOOKED && booking.getCurrentStatus() != BookingStatus.ON_THE_WAY) {
            log.warn("Payment blocked because booking {} is in status {}",
                    bookingId, booking.getCurrentStatus());
            throw new BusinessException("Payment can only be initiated when booking is BOOKED or ON THE WAY.");
        }

        if (booking.getCurrentPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Booking already paid.");
        }

        log.info("Initiating payment for bookingId={} with currentStatus={}",
                bookingId, booking.getCurrentStatus());

        Optional<Payment> latestPayment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId);

        int nextAttempt = 1;
        if(latestPayment.isPresent()) {
            Payment lastPayment = latestPayment.get();
            nextAttempt = lastPayment.getAttemptNo() + 1;
        }

        //marking all old failed payments as EXPIRED
        List<Payment> allPayments = paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(bookingId);
        List<Payment> updatedList = new ArrayList<>();
        for(Payment old : allPayments) {
            if(old.getPaymentStatus() == PaymentStatus.FAILED) {
                old.setPaymentStatus(PaymentStatus.EXPIRED);
                old.setStatusUpdatedAt(Instant.now());
                updatedList.add(old);
            }
        }
        if(!updatedList.isEmpty()) {
            paymentRepository.saveAll(updatedList);
        }

        double amountInPaise = booking.getAmount() * 100;

        Payment newPayment = new Payment();
        newPayment.setBookingId(bookingId);
        newPayment.setUserId(booking.getAppUserId());
        newPayment.setWorkshopId(booking.getWorkshopUserId());
        newPayment.setAmount(amountInPaise);
        newPayment.setAttemptNo(nextAttempt);
        newPayment.setPaymentStatus(PaymentStatus.INITIATED);
        newPayment.setStatusUpdatedAt(Instant.now());
        newPayment.setCreatedAt(Instant.now());
        newPayment.setUpdatedAt(Instant.now());
        paymentRepository.save(newPayment);

        //razorpay order
        try {
            RazorpayClient razorpayClient = razorpayClientProvider.getClient();
            JSONObject razorpayRequest = new JSONObject();
            razorpayRequest.put("amount",amountInPaise);
            razorpayRequest.put("currency","INR");
            razorpayRequest.put("receipt",newPayment.getId());
            razorpayRequest.put("payment_capture",1);

            Order order = razorpayClient.orders.create(razorpayRequest);

            String razorpayOrderId = order.get("id");
            newPayment.setGatewayOrderId(razorpayOrderId);
            newPayment.setPaymentStatus(PaymentStatus.ORDER_CREATED);
            newPayment.setStatusUpdatedAt(Instant.now());
            paymentRepository.save(newPayment);

            return new InitiatePaymentResponse(
                newPayment.getId(),
                razorpayOrderId,
                amountInPaise,
                "INR",
                razorpayKey
            );
        } catch (RazorpayException e) {
            newPayment.setPaymentStatus(PaymentStatus.FAILED);
            newPayment.setUpdatedAt(Instant.now());
            paymentRepository.save(newPayment);
            throw new RuntimeException("Failed to create Razorpay order", e);
        }
    }

    @Override
    @Transactional
    public void verifyAndUpdatePayment(PaymentVerifyRequest request) {

        Payment payment = paymentRepository
                .findByGatewayOrderId(request.getGatewayOrderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found for order " + request.getGatewayOrderId()
                        ));

        // Prevent duplicate processing
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        boolean validSignature = razorpaySignatureVerifier.isSignatureValid(
                request.getGatewayOrderId(),
                request.getGatewayPaymentId(),
                request.getGatewaySignature()
        );

        if (!validSignature) {

            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setGatewayPaymentId(request.getGatewayPaymentId());
            payment.setGatewaySignature(request.getGatewaySignature());
            payment.setStatusUpdatedAt(Instant.now());

            paymentRepository.save(payment);

            throw new BusinessException("Invalid Razorpay signature. Payment tampering detected.");
        }

        // Mark payment successful
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setGatewayPaymentId(request.getGatewayPaymentId());
        payment.setGatewaySignature(request.getGatewaySignature());
        payment.setStatusUpdatedAt(Instant.now());

        paymentRepository.save(payment);

        // Update booking payment status
        Booking booking = bookingRepository.findById(payment.getBookingId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found for id: " + payment.getBookingId()
                        ));

        booking.setCurrentPaymentStatus(PaymentStatus.PAID);

        bookingRepository.save(booking);

    }
}

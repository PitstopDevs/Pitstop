package com.pitstop.app.service;

import com.pitstop.app.dto.BookingResponse;
import com.pitstop.app.model.Booking;

import java.util.List;

public interface BookingService {
    Booking saveBookingDetails(Booking booking);
    Booking getBookingById(String id);
    List<Booking> getAllBookings();
    void deleteBooking(String id);
    List<BookingResponse> getActiveBookingsForAppUser();

    BookingResponse viewBookingDetails(String bookingId);

    List<BookingResponse> getActiveBookingsForWorkshop();
}

package com.vaicomtudo.backend.boundary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.vaicomtudo.backend.config.AbstractIntegrationTest;
import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.Payment;
import com.vaicomtudo.backend.data.entity.Payment.PaymentStatus;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.PaymentRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class FakePaymentControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ListingRepository listingRepository;

    private User renter;
    private Listing testListing;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        // Create renter account
        Account renterAccount = new Account();
        renterAccount.setName("Test Renter");
        renterAccount.setEmail("renter@test.com");
        renterAccount.setPasswordHash("hashedpassword");
        renterAccount = accountRepository.save(renterAccount);

        renter = new User();
        renter.setAccount(renterAccount);
        renter.setBirthdate(LocalDate.of(1990, 1, 1));
        renter = userRepository.save(renter);

        // Create a listing (represents approved booking)
        testListing = new Listing();
        testListing.setOwner(renter);
        testListing.setTitle("Test Bike");
        testListing.setDescription("Test description");
        testListing.setPrice(BigDecimal.valueOf(30.00));
        testListing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        testListing.setVehicle(vehicle);
        testListing.setPickUpLocation("Aveiro Centro");
        testListing.setDropOffLocation("Aveiro Station");

        testListing = listingRepository.save(testListing);
        bookingId = testListing.getId();
    }

    @Test
    @DisplayName("Integration test: Complete payment successfully")
    @Requirement("VCT-367")
    @WithMockUser(username = "renter@test.com")
    void whenCompletePayment_thenReturnsPaymentInfo() throws Exception {
        String requestBody = """
            {
                "rentalFee": 30.00,
                "deposit": 50.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/bookings/{bookingId}/complete", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
            .andExpect(jsonPath("$.renterEmail").value("renter@test.com"))
            .andExpect(jsonPath("$.rentalFee").value(30.00))
            .andExpect(jsonPath("$.deposit").value(50.00))
            .andExpect(jsonPath("$.total").value(80.00))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.receiptMessage").value("A digital receipt was sent to renter@test.com"));
    }

    @Test
    @DisplayName("Integration test: Payment is persisted in database")
    @Requirement("VCT-367")
    @WithMockUser(username = "renter@test.com")
    void whenCompletePayment_thenPaymentIsPersisted() throws Exception {
        BigDecimal rentalFee = BigDecimal.valueOf(30.00);
        BigDecimal deposit = BigDecimal.valueOf(50.00);

        String requestBody = """
            {
                "rentalFee": 30.00,
                "deposit": 50.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/bookings/{bookingId}/complete", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk());

        // Verify payment was saved in database
        Payment payment = paymentRepository.findByBookingId(bookingId).orElseThrow();
        assert payment.getBookingId().equals(bookingId);
        assert payment.getRenterEmail().equals("renter@test.com");
        assert payment.getRentalFee().compareTo(rentalFee) == 0;
        assert payment.getDeposit().compareTo(deposit) == 0;
        assert payment.getTotal().compareTo(BigDecimal.valueOf(80.00)) == 0;
        assert payment.getStatus() == PaymentStatus.CONFIRMED;
        assert payment.getReceiptMessage().contains("A digital receipt was sent to");
    }

    @Test
    @DisplayName("Integration test: Get payment by booking ID")
    @Requirement("VCT-367")
    @WithMockUser(username = "renter@test.com")
    void whenGetPayment_thenReturnsPaymentInfo() throws Exception {
        // First create a payment
        BigDecimal rentalFee = BigDecimal.valueOf(30.00);
        BigDecimal deposit = BigDecimal.valueOf(50.00);

        Payment payment = Payment.builder()
            .bookingId(bookingId)
            .renter(renter)
            .renterEmail("renter@test.com")
            .rentalFee(rentalFee)
            .deposit(deposit)
            .total(BigDecimal.valueOf(80.00))
            .status(PaymentStatus.CONFIRMED)
            .receiptMessage("A digital receipt was sent to renter@test.com")
            .createdAt(java.time.LocalDateTime.now())
            .updatedAt(java.time.LocalDateTime.now())
            .build();
        paymentRepository.save(payment);

        // Then retrieve it
        mockMvc.perform(get("/api/v1/payments/bookings/{bookingId}", bookingId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
            .andExpect(jsonPath("$.renterEmail").value("renter@test.com"))
            .andExpect(jsonPath("$.rentalFee").value(30.00))
            .andExpect(jsonPath("$.deposit").value(50.00))
            .andExpect(jsonPath("$.total").value(80.00))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Integration test: Get non-existent payment returns 404")
    @Requirement("VCT-367")
    @WithMockUser(username = "renter@test.com")
    void whenGetNonExistentPayment_thenReturnsNotFound() throws Exception {
        UUID nonExistentBookingId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/payments/bookings/{bookingId}", nonExistentBookingId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration test: Update existing payment")
    @Requirement("VCT-367")
    @WithMockUser(username = "renter@test.com")
    void whenCompletePaymentTwice_thenUpdatesExistingPayment() throws Exception {
        // First payment
        String firstRequestBody = """
            {
                "rentalFee": 30.00,
                "deposit": 50.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/bookings/{bookingId}/complete", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(firstRequestBody))
            .andExpect(status().isOk());

        // Second payment with different amounts
        String secondRequestBody = """
            {
                "rentalFee": 40.00,
                "deposit": 60.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/bookings/{bookingId}/complete", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(secondRequestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rentalFee").value(40.00))
            .andExpect(jsonPath("$.deposit").value(60.00))
            .andExpect(jsonPath("$.total").value(100.00));

        // Verify only one payment exists in database
        assert paymentRepository.findByBookingId(bookingId).isPresent();
        assert paymentRepository.count() == 1;
    }

    @Test
    @DisplayName("Integration test: Complete payment requires authentication")
    @Requirement("VCT-367")
    void whenCompletePaymentWithoutAuth_thenReturnsUnauthorized() throws Exception {
        String requestBody = """
            {
                "rentalFee": 30.00,
                "deposit": 50.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/bookings/{bookingId}/complete", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration test: Payment includes rental fee and deposit")
    @Requirement("VCT-367")
    @WithMockUser(username = "renter@test.com")
    void whenCompletePayment_thenIncludesRentalFeeAndDeposit() throws Exception {
        String requestBody = """
            {
                "rentalFee": 25.50,
                "deposit": 75.25
            }
            """;

        mockMvc.perform(post("/api/v1/payments/bookings/{bookingId}/complete", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rentalFee").value(25.50))
            .andExpect(jsonPath("$.deposit").value(75.25))
            .andExpect(jsonPath("$.total").value(100.75));
    }

    @Test
    @DisplayName("Integration test: Payment status is confirmed after completion")
    @Requirement("VCT-367")
    @WithMockUser(username = "renter@test.com")
    void whenCompletePayment_thenStatusIsConfirmed() throws Exception {
        String requestBody = """
            {
                "rentalFee": 30.00,
                "deposit": 50.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/bookings/{bookingId}/complete", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Verify in database
        Payment payment = paymentRepository.findByBookingId(bookingId).orElseThrow();
        assert payment.getStatus() == PaymentStatus.CONFIRMED;
    }

    @Test
    @DisplayName("Integration test: Payment receipt message is generated")
    @Requirement("VCT-367")
    @WithMockUser(username = "renter@test.com")
    void whenCompletePayment_thenReceiptMessageIsGenerated() throws Exception {
        String requestBody = """
            {
                "rentalFee": 30.00,
                "deposit": 50.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/bookings/{bookingId}/complete", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.receiptMessage").exists())
            .andExpect(jsonPath("$.receiptMessage").value("A digital receipt was sent to renter@test.com"));
    }

    @Test
    @DisplayName("Integration test: Payment associates with renter user when exists")
    @Requirement("VCT-367")
    @WithMockUser(username = "renter@test.com")
    void whenCompletePayment_thenAssociatesWithRenter() throws Exception {
        String requestBody = """
            {
                "rentalFee": 30.00,
                "deposit": 50.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/bookings/{bookingId}/complete", bookingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk());

        // Verify payment is associated with renter in database
        Payment payment = paymentRepository.findByBookingId(bookingId).orElseThrow();
        assert payment.getRenter() != null;
        assert payment.getRenter().getId().equals(renter.getId());
    }
}

package com.fiap.workshop.inventory.application.usecase.reservation;

import com.fiap.workshop.inventory.application.dto.reservation.ReservationResponse;
import com.fiap.workshop.inventory.application.dto.reservation.ReserveStockCommand;
import com.fiap.workshop.inventory.domain.exception.InsufficientStockException;
import com.fiap.workshop.inventory.domain.exception.InvalidStateException;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.model.reservation.ReservedItem;
import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;
import com.fiap.workshop.inventory.domain.model.shared.Money;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.model.supply.SupplyType;
import com.fiap.workshop.inventory.domain.repository.StockReservationRepository;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationUseCasesTest {

    @Mock
    private StockReservationRepository reservationRepository;
    @Mock
    private SupplyRepository supplyRepository;

    private ReserveStockUseCase reserve;
    private ReleaseStockUseCase release;
    private ConsumeStockUseCase consume;
    private FindReservationUseCase find;

    private final UUID orderId = UUID.randomUUID();
    private Supply oil;
    private Supply filter;

    @BeforeEach
    void setUp() {
        reserve = new ReserveStockUseCase(reservationRepository, supplyRepository);
        release = new ReleaseStockUseCase(reservationRepository, supplyRepository);
        consume = new ConsumeStockUseCase(reservationRepository);
        find = new FindReservationUseCase(reservationRepository);
        oil = new Supply(UUID.randomUUID(), "OIL", "Oil", "d", SupplyType.CONSUMABLE, Money.of(50), 10, 2);
        filter = new Supply(UUID.randomUUID(), "FLT", "Filter", "d", SupplyType.PART, Money.of(25), 1, 2);
        lenient().when(reservationRepository.save(any(StockReservation.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(supplyRepository.save(any(Supply.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ReserveStockCommand command(ReserveStockCommand.Line... lines) {
        return new ReserveStockCommand(orderId, List.of(lines));
    }

    @Test
    void shouldReserveTakingTheStockOffTheShelf() {
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        when(supplyRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(oil));

        ReservationResponse response = reserve.execute(command(new ReserveStockCommand.Line(oil.getId(), 4)));

        assertEquals("RESERVED", response.status());
        assertEquals(6, oil.getStockQuantity());
        verify(supplyRepository).save(oil);
    }

    @Test
    void shouldRefuseWhenTheStockIsInsufficient() {
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        when(supplyRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(filter));

        assertThrows(InsufficientStockException.class,
                () -> reserve.execute(command(new ReserveStockCommand.Line(filter.getId(), 2))));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldRefuseAnUnknownSupply() {
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        when(supplyRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> reserve.execute(command(new ReserveStockCommand.Line(UUID.randomUUID(), 1))));
    }

    @Test
    void shouldReserveNothingForAnOrderWithoutSupplies() {
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        when(supplyRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of());

        ReservationResponse response = reserve.execute(command());

        assertEquals("RESERVED", response.status());
        assertEquals(0, response.items().size());
    }

    @Test
    void shouldReturnTheExistingReservationWithoutTouchingTheStock() {
        StockReservation existing = StockReservation.reserve(orderId, List.of(new ReservedItem(oil.getId(), 4)));
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(existing));

        ReservationResponse response = reserve.execute(command(new ReserveStockCommand.Line(oil.getId(), 4)));

        assertEquals(existing.getId(), response.id());
        verify(supplyRepository, never()).save(any());
    }

    @Test
    void shouldReleaseGivingTheStockBack() {
        StockReservation reservation = StockReservation.reserve(orderId, List.of(new ReservedItem(oil.getId(), 4)));
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(supplyRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(oil));

        release.execute(orderId);

        assertEquals(14, oil.getStockQuantity());
        assertEquals("RELEASED", reservation.getStatus().name());
    }

    @Test
    void shouldReleaseNothingWhenThereIsNoReservationOrItWasAlreadyReleased() {
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        release.execute(orderId);

        StockReservation released = StockReservation.reserve(orderId, List.of(new ReservedItem(oil.getId(), 4)));
        released.release();
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(released));
        release.execute(orderId);

        verify(supplyRepository, never()).save(any());
    }

    @Test
    void shouldNotReleaseAConsumedReservation() {
        StockReservation consumed = StockReservation.reserve(orderId, List.of(new ReservedItem(oil.getId(), 4)));
        consumed.consume();
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(consumed));

        assertThrows(InvalidStateException.class, () -> release.execute(orderId));
        verify(supplyRepository, never()).save(any());
    }

    @Test
    void shouldFailToReleaseWhenASupplyWasDeleted() {
        StockReservation reservation = StockReservation.reserve(orderId, List.of(new ReservedItem(oil.getId(), 4)));
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(supplyRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> release.execute(orderId));
    }

    @Test
    void shouldConsumeAReservationOnlyOnce() {
        StockReservation reservation = StockReservation.reserve(orderId, List.of(new ReservedItem(oil.getId(), 4)));
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(reservation));

        assertEquals("CONSUMED", consume.execute(orderId).status());
        assertEquals("CONSUMED", consume.execute(orderId).status());
        verify(reservationRepository).save(reservation);
    }

    @Test
    void shouldFailToConsumeWhatWasNeverReservedOrWasReleased() {
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> consume.execute(orderId));

        StockReservation released = StockReservation.reserve(orderId, List.of(new ReservedItem(oil.getId(), 4)));
        released.release();
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(released));
        assertThrows(InvalidStateException.class, () -> consume.execute(orderId));
    }

    @Test
    void shouldFindTheReservationOfAnOrder() {
        StockReservation reservation = StockReservation.reserve(orderId, List.of(new ReservedItem(oil.getId(), 4)));
        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(reservation));
        assertEquals(oil.getId(), find.findByServiceOrderId(orderId).items().get(0).supplyId());

        when(reservationRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> find.findByServiceOrderId(orderId));
    }
}

package net.java.cargotracker.interfaces.booking.facade.internal.assembler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import net.java.cargotracker.domain.model.cargo.Itinerary;
import net.java.cargotracker.domain.model.cargo.Leg;
import net.java.cargotracker.domain.model.location.Location;
import net.java.cargotracker.domain.model.location.LocationRepository;
import net.java.cargotracker.domain.model.location.UnLocode;
import net.java.cargotracker.domain.model.voyage.Voyage;
import net.java.cargotracker.domain.model.voyage.VoyageNumber;
import net.java.cargotracker.domain.model.voyage.VoyageRepository;
import net.java.cargotracker.interfaces.booking.facade.dto.RouteCandidate;

public class ItineraryCandidateDtoAssembler {

    private static final SimpleDateFormat DATE_FORMAT
            = new SimpleDateFormat("MM/dd/yyyy hh:mm a z");

    public RouteCandidate toDTO(Itinerary itinerary) {
        List<net.java.cargotracker.interfaces.booking.facade.dto.Leg> legDTOs = new ArrayList<>(
                itinerary.getLegs().size());
        for (Leg leg : itinerary.getLegs()) {
            legDTOs.add(toLegDTO(leg));
        }
        return new RouteCandidate(legDTOs);
    }

    protected net.java.cargotracker.interfaces.booking.facade.dto.Leg toLegDTO(
            Leg leg) {
        VoyageNumber voyageNumber = leg.getVoyage().getVoyageNumber();
        return new net.java.cargotracker.interfaces.booking.facade.dto.Leg(
                voyageNumber.getIdString(),
                leg.getLoadLocation().getUnLocode().getIdString(),
                leg.getLoadLocation().getName(),
                leg.getUnloadLocation().getUnLocode().getIdString(),
                leg.getUnloadLocation().getName(),
                leg.getLoadTime(),
                leg.getUnloadTime());
    }

    public Itinerary fromDTO(RouteCandidate routeCandidateDTO,
            VoyageRepository voyageRepository,
            LocationRepository locationRepository) {
        List<Leg> legs = new ArrayList<>(routeCandidateDTO.getLegs().size());

        for (net.java.cargotracker.interfaces.booking.facade.dto.Leg legDTO
                : routeCandidateDTO.getLegs()) {
            VoyageNumber voyageNumber = new VoyageNumber(
                    legDTO.getVoyageNumber());
            Voyage voyage = voyageRepository.find(voyageNumber);
            Location from = locationRepository.find(new UnLocode(legDTO
                    .getFromUnLocode()));
            Location to = locationRepository.find(new UnLocode(legDTO.getToUnLocode()));

            try {
                legs.add(new Leg(voyage, from, to,
                        DATE_FORMAT.parse(legDTO.getLoadTime()),
                        DATE_FORMAT.parse(legDTO.getUnloadTime())));
            } catch (ParseException ex) {
                throw new RuntimeException("Could not parse date", ex);
            }
        }

        return new Itinerary(legs);
    }
}

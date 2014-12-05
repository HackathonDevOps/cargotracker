package net.java.cargotracker.interfaces.booking.facade.dto;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * DTO for registering and routing a cargo.
 */
public class CargoRoute implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat DATE_FORMAT
            = new SimpleDateFormat("MM/dd/yyyy hh:mm a z");

    private final String trackingId;
    private final String origin;
    private final String finalDestination;
    private final String arrivalDeadline;
    private final boolean misrouted;
    private final List<Leg> legs;
    private final boolean claimed;
    private final String lastKnownLocation;
    private final String transportStatus;
    private String nextLocation;

    public CargoRoute(String trackingId, String origin, String finalDestination,
            Date arrivalDeadline, boolean misrouted, boolean claimed, String lastKnownLocation, String transportStatus) {
        this.trackingId = trackingId;
        this.origin = origin;
        this.finalDestination = finalDestination;
        this.arrivalDeadline = DATE_FORMAT.format(arrivalDeadline);
        this.misrouted = misrouted;
        this.claimed = claimed;
        this.lastKnownLocation = lastKnownLocation;
        this.transportStatus = transportStatus;
        this.legs = new ArrayList<>();
    }

    public String getTrackingId() {
        return trackingId;
    }

    public String getOrigin() {
        return origin;
    }

    public String getFinalDestination() {
        return finalDestination;
    }

    public void addLeg(
            String voyageNumber,
            String fromUnLocode, String fromName,
            String toUnLocode, String toName,
            Date loadTime, Date unloadTime) {
        legs.add(new Leg(voyageNumber,
                fromUnLocode, fromName,
                toUnLocode, toName,
                loadTime, unloadTime));
    }

    public List<Leg> getLegs() {
        return Collections.unmodifiableList(legs);
    }

    public boolean isMisrouted() {
        return misrouted;
    }

    public boolean isRouted() {
        return !legs.isEmpty();
    }

    public String getArrivalDeadline() {
        return arrivalDeadline;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public String getLastKnownLocation() {
        return this.lastKnownLocation;
    }

    public String getTransportStatus() {
        return this.transportStatus;
    }

    public String getNextLocation() {
        return this.nextLocation;
    }
}

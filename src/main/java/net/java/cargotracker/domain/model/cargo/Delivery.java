package net.java.cargotracker.domain.model.cargo;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import static net.java.cargotracker.domain.model.cargo.RoutingStatus.*;
import static net.java.cargotracker.domain.model.cargo.TransportStatus.*;
import net.java.cargotracker.domain.model.handling.HandlingEvent;
import net.java.cargotracker.domain.model.handling.HandlingHistory;
import net.java.cargotracker.domain.model.location.Location;
import net.java.cargotracker.domain.model.voyage.Voyage;
import net.java.cargotracker.domain.shared.DomainObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The actual transportation of the cargo, as opposed to the customer
 * requirement (RouteSpecification) and the plan (Itinerary).
 */
@Embeddable
public class Delivery implements Serializable {

    private static final long serialVersionUID = 1L;
    // Null object pattern.
    public static final Date ETA_UNKOWN = null;
    // Null object pattern
    public static final HandlingActivity NO_ACTIVITY = new HandlingActivity();
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_status")
    @NotNull
    private TransportStatus transportStatus;
    @ManyToOne
    @JoinColumn(name = "last_known_location_id")
    private Location lastKnownLocation;
    @ManyToOne
    @JoinColumn(name = "current_voyage_id")
    private Voyage currentVoyage;
    @NotNull
    private boolean misdirected;
    @Temporal(TemporalType.DATE)
    private Date eta;
    @Embedded
    private HandlingActivity nextExpectedActivity;
    @Column(name = "unloaded_at_dest")
    @NotNull
    private boolean isUnloadedAtDestination;
    @Enumerated(EnumType.STRING)
    @Column(name = "routing_status")
    @NotNull
    private RoutingStatus routingStatus;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "calculated_at")
    @NotNull
    private Date calculatedAt;
    @ManyToOne
    @JoinColumn(name = "last_event_id")
    private HandlingEvent lastEvent;

    public Delivery() {
        // Nothing to initialize
    }

    public Delivery(HandlingEvent lastEvent, Itinerary itinerary,
            RouteSpecification routeSpecification) {
        this.calculatedAt = new Date();
        this.lastEvent = lastEvent;

        this.misdirected = calculateMisdirectionStatus(itinerary);
        this.routingStatus = calculateRoutingStatus(itinerary,
                routeSpecification);
        this.transportStatus = calculateTransportStatus();
        this.lastKnownLocation = calculateLastKnownLocation();
        this.currentVoyage = calculateCurrentVoyage();
        this.eta = calculateEta(itinerary);
        this.nextExpectedActivity = calculateNextExpectedActivity(
                routeSpecification, itinerary);
        this.isUnloadedAtDestination = calculateUnloadedAtDestination(routeSpecification);
    }

    /**
     * Creates a new delivery snapshot to reflect changes in routing, i.e. when
     * the route specification or the itinerary has changed but no additional
     * handling of the cargo has been performed.
     */
    Delivery updateOnRouting(RouteSpecification routeSpecification,
            Itinerary itinerary) {
        Validate.notNull(routeSpecification, "Route specification is required");

        return new Delivery(this.lastEvent, itinerary, routeSpecification);
    }

    /**
     * Creates a new delivery snapshot based on the complete handling history of
     * a cargo, as well as its route specification and itinerary.
     *
     * @param routeSpecification route specification
     * @param itinerary itinerary
     * @param handlingHistory delivery history
     * @return An up to date delivery.
     */
    static Delivery derivedFrom(RouteSpecification routeSpecification,
            Itinerary itinerary, HandlingHistory handlingHistory) {
        Validate.notNull(routeSpecification, "Route specification is required");
        Validate.notNull(handlingHistory, "Delivery history is required");

        HandlingEvent lastEvent = handlingHistory
                .getMostRecentlyCompletedEvent();

        return new Delivery(lastEvent, itinerary, routeSpecification);
    }

    public TransportStatus getTransportStatus() {
        return transportStatus;
    }

    public void setTransportStatus(TransportStatus transportStatus) {
        this.transportStatus = transportStatus;
    }

    public Location getLastKnownLocation() {
        return DomainObjectUtils.nullSafe(lastKnownLocation, Location.UNKNOWN);
    }

    public void setLastKnownLocation(Location lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
    }

    public void setLastEvent(HandlingEvent lastEvent) {
        this.lastEvent = lastEvent;
    }

    public Voyage getCurrentVoyage() {
        return DomainObjectUtils.nullSafe(currentVoyage, Voyage.NONE);
    }

    /**
     * Check if cargo is misdirected.
     * <p/>
     * <ul>
     * <li>A cargo is misdirected if it is in a location that's not in the
     * itinerary.
     * <li>A cargo with no itinerary can not be misdirected.
     * <li>A cargo that has received no handling events can not be misdirected.
     * </ul>
     *
     * @return <code>true</code> if the cargo has been misdirected,
     */
    public boolean isMisdirected() {
        return misdirected;
    }

    public void setMisdirected(boolean misdirected) {
        this.misdirected = misdirected;
    }

    public Date getEstimatedTimeOfArrival() {
        if (eta != ETA_UNKOWN) {
            return new Date(eta.getTime());
        } else {
            return ETA_UNKOWN;
        }
    }

    public HandlingActivity getNextExpectedActivity() {
        return nextExpectedActivity;
    }

    /**
     * @return True if the cargo has been unloaded at the final destination.
     */
    public boolean isUnloadedAtDestination() {
        return isUnloadedAtDestination;
    }

    public void setUnloadedAtDestination(boolean isUnloadedAtDestination) {
        this.isUnloadedAtDestination = isUnloadedAtDestination;
    }

    public RoutingStatus getRoutingStatus() {
        return routingStatus;
    }

    public void setRoutingStatus(RoutingStatus routingStatus) {
        this.routingStatus = routingStatus;
    }

    /**
     * @return When this delivery was calculated.
     */
    public Date getCalculatedAt() {
        return new Date(calculatedAt.getTime());
    }

    public void setCalculatedAt(Date calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    private TransportStatus calculateTransportStatus() {
        if (lastEvent == null) {
            return NOT_RECEIVED;
        }

        switch (lastEvent.getType()) {
            case LOAD:
                return ONBOARD_CARRIER;
            case UNLOAD:
            case RECEIVE:
            case CUSTOMS:
                return IN_PORT;
            case CLAIM:
                return CLAIMED;
            default:
                return UNKNOWN;
        }
    }

    private Location calculateLastKnownLocation() {
        if (lastEvent != null) {
            return lastEvent.getLocation();
        } else {
            return null;
        }
    }

    private Voyage calculateCurrentVoyage() {
        if (getTransportStatus().equals(ONBOARD_CARRIER) && lastEvent != null) {
            return lastEvent.getVoyage();
        } else {
            return null;
        }
    }

    private boolean calculateMisdirectionStatus(Itinerary itinerary) {
        if (lastEvent == null) {
            return false;
        } else {
            return !itinerary.isExpected(lastEvent);
        }
    }

    private Date calculateEta(Itinerary itinerary) {
        if (onTrack()) {
            return itinerary.getFinalArrivalDate();
        } else {
            return ETA_UNKOWN;
        }
    }

    private HandlingActivity calculateNextExpectedActivity(
            RouteSpecification routeSpecification, Itinerary itinerary) {
        if (!onTrack()) {
            return NO_ACTIVITY;
        }

        if (lastEvent == null) {
            return new HandlingActivity(HandlingEvent.Type.RECEIVE,
                    routeSpecification.getOrigin());
        }

        switch (lastEvent.getType()) {
            case LOAD:
                for (Leg leg : itinerary.getLegs()) {
                    if (leg.getLoadLocation().sameIdentityAs(
                            lastEvent.getLocation())) {
                        return new HandlingActivity(HandlingEvent.Type.UNLOAD,
                                leg.getUnloadLocation(), leg.getVoyage());
                    }
                }

                return NO_ACTIVITY;

            case UNLOAD:
                for (Iterator<Leg> iterator = itinerary.getLegs().iterator(); iterator
                        .hasNext();) {
                    Leg leg = iterator.next();

                    if (leg.getUnloadLocation().sameIdentityAs(
                            lastEvent.getLocation())) {
                        if (iterator.hasNext()) {
                            Leg nextLeg = iterator.next();
                            return new HandlingActivity(HandlingEvent.Type.LOAD,
                                    nextLeg.getLoadLocation(), nextLeg.getVoyage());
                        } else {
                            return new HandlingActivity(HandlingEvent.Type.CLAIM,
                                    leg.getUnloadLocation());
                        }
                    }
                }

                return NO_ACTIVITY;

            case RECEIVE:
                Leg firstLeg = itinerary.getLegs().iterator().next();

                return new HandlingActivity(HandlingEvent.Type.LOAD,
                        firstLeg.getLoadLocation(), firstLeg.getVoyage());

            case CLAIM:
            default:
                return NO_ACTIVITY;
        }
    }

    private RoutingStatus calculateRoutingStatus(Itinerary itinerary,
            RouteSpecification routeSpecification) {
        if (itinerary == null || itinerary == Itinerary.EMPTY_ITINERARY) {
            return NOT_ROUTED;
        } else {
            if (routeSpecification.isSatisfiedBy(itinerary)) {
                return ROUTED;
            } else {
                return MISROUTED;
            }
        }
    }

    private boolean calculateUnloadedAtDestination(
            RouteSpecification routeSpecification) {
        return lastEvent != null
                && HandlingEvent.Type.UNLOAD.sameValueAs(lastEvent.getType())
                && routeSpecification.getDestination().sameIdentityAs(
                        lastEvent.getLocation());
    }

    private boolean onTrack() {
        return routingStatus.equals(ROUTED) && !misdirected;
    }

    private boolean sameValueAs(Delivery other) {
        return other != null
                && new EqualsBuilder()
                .append(this.transportStatus, other.transportStatus)
                .append(this.lastKnownLocation, other.lastKnownLocation)
                .append(this.currentVoyage, other.currentVoyage)
                .append(this.misdirected, other.misdirected)
                .append(this.eta, other.eta)
                .append(this.nextExpectedActivity,
                        other.nextExpectedActivity)
                .append(this.isUnloadedAtDestination,
                        other.isUnloadedAtDestination)
                .append(this.routingStatus, other.routingStatus)
                .append(this.calculatedAt, other.calculatedAt)
                .append(this.lastEvent, other.lastEvent).isEquals();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Delivery other = (Delivery) o;

        return sameValueAs(other);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(transportStatus)
                .append(lastKnownLocation).append(currentVoyage)
                .append(misdirected).append(eta).append(nextExpectedActivity)
                .append(isUnloadedAtDestination).append(routingStatus)
                .append(calculatedAt).append(lastEvent).toHashCode();
    }
}

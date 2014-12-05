package net.java.cargotracker.domain.model.cargo;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.validation.constraints.Size;

import net.java.cargotracker.domain.model.handling.HandlingEvent;
import net.java.cargotracker.domain.model.location.Location;

import org.apache.commons.lang3.Validate;
import org.eclipse.persistence.annotations.PrivateOwned;

@Embeddable
public class Itinerary implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Date END_OF_DAYS = new Date(Long.MAX_VALUE);
    // Null object pattern.
    public static final Itinerary EMPTY_ITINERARY = new Itinerary();
    // TODO Look into why cascade delete doesn't work.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cargo_id")
    // TODO Index this is in leg_index
    @OrderBy("loadTime")
    @PrivateOwned
    @Size(min = 1)
    private List<Leg> legs = Collections.emptyList();

    public Itinerary() {
        // Nothing to initialize.
    }

    public Itinerary(List<Leg> legs) {
        Validate.notEmpty(legs);
        Validate.noNullElements(legs);

        this.legs = legs;
    }

    public List<Leg> getLegs() {
        return Collections.unmodifiableList(legs);
    }

    /**
     * Test if the given handling event is expected when executing this
     * itinerary.
     */
    public boolean isExpected(HandlingEvent event) {
        if (legs.isEmpty()) {
            return true;
        }

        // TODO Convert this to a switch statement?
        if (event.getType() == HandlingEvent.Type.RECEIVE) {
            // Check that the first leg's origin is the event's location
            Leg leg = legs.get(0);
            return (leg.getLoadLocation().equals(event.getLocation()));
        }

        if (event.getType() == HandlingEvent.Type.LOAD) {
            // Check that the there is one leg with same load location and
            // voyage
            for (Leg leg : legs) {
                if (leg.getLoadLocation().sameIdentityAs(event.getLocation())
                        && leg.getVoyage().sameIdentityAs(event.getVoyage())) {
                    return true;
                }
            }

            return false;
        }

        if (event.getType() == HandlingEvent.Type.UNLOAD) {
            // Check that the there is one leg with same unload location and
            // voyage
            for (Leg leg : legs) {
                if (leg.getUnloadLocation().equals(event.getLocation())
                        && leg.getVoyage().equals(event.getVoyage())) {
                    return true;
                }
            }

            return false;
        }

        if (event.getType() == HandlingEvent.Type.CLAIM) {
            // Check that the last leg's destination is from the event's
            // location
            Leg leg = getLastLeg();

            return (leg.getUnloadLocation().equals(event.getLocation()));
        }

        // HandlingEvent.Type.CUSTOMS;
        return true;
    }

    Location getInitialDepartureLocation() {
        if (legs.isEmpty()) {
            return Location.UNKNOWN;
        } else {
            return legs.get(0).getLoadLocation();
        }
    }

    Location getFinalArrivalLocation() {
        if (legs.isEmpty()) {
            return Location.UNKNOWN;
        } else {
            return getLastLeg().getUnloadLocation();
        }
    }

    /**
     * @return Date when cargo arrives at final destination.
     */
    Date getFinalArrivalDate() {
        Leg lastLeg = getLastLeg();

        if (lastLeg == null) {
            return new Date(END_OF_DAYS.getTime());
        } else {
            return new Date(lastLeg.getUnloadTime().getTime());
        }
    }

    /**
     * @return The last leg on the itinerary.
     */
    Leg getLastLeg() {
        if (legs.isEmpty()) {
            return null;
        } else {
            return legs.get(legs.size() - 1);
        }
    }

    private boolean sameValueAs(Itinerary other) {
        return other != null && legs.equals(other.legs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Itinerary itinerary = (Itinerary) o;

        return sameValueAs(itinerary);
    }

    @Override
    public int hashCode() {
        return legs.hashCode();
    }

    @Override
    public String toString() {
        return "Itinerary{" + "legs=" + legs + '}';
    }
}

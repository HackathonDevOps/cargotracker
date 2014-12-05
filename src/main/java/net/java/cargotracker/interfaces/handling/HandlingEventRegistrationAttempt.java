package net.java.cargotracker.interfaces.handling;

import java.io.Serializable;
import java.util.Date;
import net.java.cargotracker.domain.model.cargo.TrackingId;
import net.java.cargotracker.domain.model.handling.HandlingEvent;
import net.java.cargotracker.domain.model.location.UnLocode;
import net.java.cargotracker.domain.model.voyage.VoyageNumber;

/**
 * This is a simple transfer object for passing incoming handling event
 * registration attempts to the proper registration procedure.
 *
 * It is used as a message queue element.
 */
public class HandlingEventRegistrationAttempt implements Serializable {

    private final Date registrationTime;
    private final Date completionTime;
    private final TrackingId trackingId;
    private final VoyageNumber voyageNumber;
    private final HandlingEvent.Type type;
    private final UnLocode unLocode;

    public HandlingEventRegistrationAttempt(Date registrationDate,
            Date completionDate, TrackingId trackingId,
            VoyageNumber voyageNumber, HandlingEvent.Type type, 
            UnLocode unLocode) {
        this.registrationTime = registrationDate;
        this.completionTime = completionDate;
        this.trackingId = trackingId;
        this.voyageNumber = voyageNumber;
        this.type = type;
        this.unLocode = unLocode;
    }

    public Date getCompletionTime() {
        return new Date(completionTime.getTime());
    }

    public TrackingId getTrackingId() {
        return trackingId;
    }

    public VoyageNumber getVoyageNumber() {
        return voyageNumber;
    }

    public HandlingEvent.Type getType() {
        return type;
    }

    public UnLocode getUnLocode() {
        return unLocode;
    }

    public Date getRegistrationTime() {
        return registrationTime;
    }

    @Override
    public String toString() {
        return "HandlingEventRegistrationAttempt{"
                + "registrationTime=" + registrationTime
                + ", completionTime=" + completionTime
                + ", trackingId=" + trackingId
                + ", voyageNumber=" + voyageNumber
                + ", type=" + type
                + ", unLocode=" + unLocode + '}';
    }
}
package net.java.cargotracker.interfaces.tracking.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.java.cargotracker.domain.model.cargo.Cargo;
import net.java.cargotracker.domain.model.cargo.Delivery;
import net.java.cargotracker.domain.model.cargo.HandlingActivity;
import net.java.cargotracker.domain.model.handling.HandlingEvent;
import net.java.cargotracker.domain.model.location.Location;
import net.java.cargotracker.domain.model.voyage.Voyage;

/**
 * View adapter for displaying a cargo in a tracking context.
 */
public class CargoTrackingViewAdapter {

    private static final SimpleDateFormat DATE_FORMAT
            = new SimpleDateFormat("MM/dd/yyyy hh:mm a z");

    private final Cargo cargo;
    private final List<HandlingEventViewAdapter> events;

    public CargoTrackingViewAdapter(Cargo cargo,
            List<HandlingEvent> handlingEvents) {
        this.cargo = cargo;
        this.events = new ArrayList<>(handlingEvents.size());

        for (HandlingEvent handlingEvent : handlingEvents) {
            events.add(new HandlingEventViewAdapter(handlingEvent));
        }
    }

    public String getTrackingId() {
        return cargo.getTrackingId().getIdString();
    }

    public String getOrigin() {
        return getDisplayText(cargo.getOrigin());
    }

    public String getDestination() {
        return getDisplayText(cargo.getRouteSpecification().getDestination());
    }

    /**
     * @return A formatted string for displaying the location.
     */
    private String getDisplayText(Location location) {
        return location.getName();
    }

    /**
     * @return A translated string describing the cargo status.
     */
    public String getStatusText() {
        Delivery delivery = cargo.getDelivery();

        switch (delivery.getTransportStatus()) {
            case IN_PORT:
                return "In port " + getDisplayText(delivery.getLastKnownLocation());
            case ONBOARD_CARRIER:
                return "Onboard voyage "
                        + delivery.getCurrentVoyage().getVoyageNumber().getIdString();
            case CLAIMED:
                return "Claimed";
            case NOT_RECEIVED:
                return "Not received";
            case UNKNOWN:
                return "Unknown";
            default:
                return "[Unknown status]"; // Should never happen.
        }
    }

    public boolean isMisdirected() {
        return cargo.getDelivery().isMisdirected();
    }

    public String getEta() {
        Date eta = cargo.getDelivery().getEstimatedTimeOfArrival();

        if (eta == null) {
            return "?";
        } else {
            return DATE_FORMAT.format(eta);
        }
    }

    public String getNextExpectedActivity() {
        HandlingActivity activity = cargo.getDelivery().getNextExpectedActivity();

        if ((activity == null) || (activity.isEmpty())) {
            return "";
        }

        String text = "Next expected activity is to ";
        HandlingEvent.Type type = activity.getType();

        if (type.sameValueAs(HandlingEvent.Type.LOAD)) {
            return text + type.name().toLowerCase() + " cargo onto voyage "
                    + activity.getVoyage().getVoyageNumber() + " in "
                    + activity.getLocation().getName();
        } else if (type.sameValueAs(HandlingEvent.Type.UNLOAD)) {
            return text + type.name().toLowerCase() + " cargo off of "
                    + activity.getVoyage().getVoyageNumber() + " in "
                    + activity.getLocation().getName();
        } else {
            return text + type.name().toLowerCase() + " cargo in "
                    + activity.getLocation().getName();
        }
    }

    /**
     * @return An unmodifiable list of handling event view adapters.
     */
    public List<HandlingEventViewAdapter> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Handling event view adapter component.
     */
    public class HandlingEventViewAdapter {

        private final HandlingEvent handlingEvent;

        public HandlingEventViewAdapter(HandlingEvent handlingEvent) {
            this.handlingEvent = handlingEvent;
        }

        public String getLocation() {
            return handlingEvent.getLocation().getName();
        }

        public String getTime() {
            return DATE_FORMAT.format(handlingEvent
                    .getCompletionTime());
        }

        public String getType() {
            return handlingEvent.getType().toString();
        }

        public String getVoyageNumber() {
            Voyage voyage = handlingEvent.getVoyage();
            return voyage.getVoyageNumber().getIdString();
        }

        public boolean isExpected() {
            return cargo.getItinerary().isExpected(handlingEvent);
        }

        public String getDescription() {
            switch (handlingEvent.getType()) {
                case LOAD:
                    return "Loaded onto voyage "
                            + handlingEvent.getVoyage().getVoyageNumber().getIdString()
                            + " in " + handlingEvent.getLocation().getName() + ", at "
                            + DATE_FORMAT.format(handlingEvent.getCompletionTime()) + ".";
                case UNLOAD:
                    return "Unloaded off voyage "
                            + handlingEvent.getVoyage().getVoyageNumber().getIdString()
                            + " in " + handlingEvent.getLocation().getName() + ", at "
                            + DATE_FORMAT.format(handlingEvent.getCompletionTime()) + ".";
                case RECEIVE:
                    return "Received in " + handlingEvent.getLocation().getName()
                            + ", at " + DATE_FORMAT.format(handlingEvent.getCompletionTime()) + ".";
                case CLAIM:
                    return "Claimed in " + handlingEvent.getLocation().getName()
                            + ", at " + DATE_FORMAT.format(handlingEvent.getCompletionTime()) + ".";
                case CUSTOMS:
                    return "Cleared customs in " + handlingEvent.getLocation().getName()
                            + ", at " + DATE_FORMAT.format(handlingEvent.getCompletionTime()) + ".";
                default:
                    return "[Unknown]";
            }
        }
    }
}

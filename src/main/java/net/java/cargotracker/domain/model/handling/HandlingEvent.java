package net.java.cargotracker.domain.model.handling;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import net.java.cargotracker.domain.model.cargo.Cargo;
import net.java.cargotracker.domain.model.location.Location;
import net.java.cargotracker.domain.model.voyage.Voyage;
import net.java.cargotracker.domain.shared.DomainObjectUtils;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * A HandlingEvent is used to register the event when, for instance, a cargo is
 * unloaded from a carrier at a some location at a given time.
 * <p/>
 * The HandlingEvent's are sent from different Incident Logging Applications
 * some time after the event occurred and contain information about the null {@link net.java.cargotracker.domain.model.cargo.TrackingId},
 * {@link net.java.cargotracker.domain.model.location.Location}, time stamp of
 * the completion of the event, and possibly, if applicable a
 * {@link net.java.cargotracker.domain.model.voyage.Voyage}.
 * <p/>
 * This class is the only member, and consequently the root, of the
 * HandlingEvent aggregate.
 * <p/>
 * HandlingEvent's could contain information about a {@link Voyage} and if so,
 * the event type must be either {@link Type#LOAD} or {@link Type#UNLOAD}.
 * <p/>
 * All other events must be of {@link Type#RECEIVE}, {@link Type#CLAIM} or
 * {@link Type#CUSTOMS}.
 */
@Entity
@NamedQuery(name = "HandlingEvent.findByTrackingId",
        query = "Select e from HandlingEvent e where e.cargo.trackingId = :trackingId")
public class HandlingEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue
    private Long id;
    @Enumerated(EnumType.STRING)
    @NotNull
    private Type type;
    @ManyToOne
    @JoinColumn(name = "voyage_id")
    private Voyage voyage;
    @ManyToOne
    @JoinColumn(name = "location_id")
    @NotNull
    private Location location;
    @Temporal(TemporalType.DATE)
    @NotNull
    @Column(name = "completionTime")
    private Date completionTime;
    @Temporal(TemporalType.DATE)
    @NotNull
    @Column(name = "registration")
    private Date registrationTime;
    @ManyToOne
    @JoinColumn(name = "cargo_id")
    @NotNull
    private Cargo cargo;

    /**
     * Handling event type. Either requires or prohibits a carrier movement
     * association, it's never optional.
     */
    public enum Type {

        // Loaded onto voyage from port location.
        LOAD(true),
        // Unloaded from voyage to port location
        UNLOAD(true),
        // Received by carrier
        RECEIVE(false),
        // Cargo claimed by recepient
        CLAIM(false),
        // Cargo went through customs
        CUSTOMS(false);
        private final boolean voyageRequired;

        /**
         * Private enum constructor.
         *
         * @param voyageRequired whether or not a voyage is associated with this
         * event type
         */
        private Type(boolean voyageRequired) {
            this.voyageRequired = voyageRequired;
        }

        /**
         * @return True if a voyage association is required for this event type.
         */
        public boolean requiresVoyage() {
            return voyageRequired;
        }

        /**
         * @return True if a voyage association is prohibited for this event
         * type.
         */
        public boolean prohibitsVoyage() {
            return !requiresVoyage();
        }

        public boolean sameValueAs(Type other) {
            return other != null && this.equals(other);
        }
    }

    public HandlingEvent() {
        // Nothing to initialize.
    }

    /**
     * @param cargo The cargo
     * @param completionTime completion time, the reported time that the event
     * actually happened (e.g. the receive took place).
     * @param registrationTime registration time, the time the message is
     * received
     * @param type type of event
     * @param location where the event took place
     * @param voyage the voyage
     */
    public HandlingEvent(Cargo cargo, Date completionTime,
            Date registrationTime, Type type, Location location, Voyage voyage) {
        Validate.notNull(cargo, "Cargo is required");
        Validate.notNull(completionTime, "Completion time is required");
        Validate.notNull(registrationTime, "Registration time is required");
        Validate.notNull(type, "Handling event type is required");
        Validate.notNull(location, "Location is required");
        Validate.notNull(voyage, "Voyage is required");

        if (type.prohibitsVoyage()) {
            throw new IllegalArgumentException(
                    "Voyage is not allowed with event type " + type);
        }

        this.voyage = voyage;
        this.completionTime = (Date) completionTime.clone();
        this.registrationTime = (Date) registrationTime.clone();
        this.type = type;
        this.location = location;
        this.cargo = cargo;
    }

    /**
     * @param cargo cargo
     * @param completionTime completion time, the reported time that the event
     * actually happened (e.g. the receive took place).
     * @param registrationTime registration time, the time the message is
     * received
     * @param type type of event
     * @param location where the event took place
     */
    public HandlingEvent(Cargo cargo, Date completionTime,
            Date registrationTime, Type type, Location location) {
        Validate.notNull(cargo, "Cargo is required");
        Validate.notNull(completionTime, "Completion time is required");
        Validate.notNull(registrationTime, "Registration time is required");
        Validate.notNull(type, "Handling event type is required");
        Validate.notNull(location, "Location is required");

        if (type.requiresVoyage()) {
            throw new IllegalArgumentException(
                    "Voyage is required for event type " + type);
        }

        this.completionTime = (Date) completionTime.clone();
        this.registrationTime = (Date) registrationTime.clone();
        this.type = type;
        this.location = location;
        this.cargo = cargo;
        this.voyage = null;
    }

    public Type getType() {
        return this.type;
    }

    public Voyage getVoyage() {
        return DomainObjectUtils.nullSafe(this.voyage, Voyage.NONE);
    }

    public Date getCompletionTime() {
        return new Date(this.completionTime.getTime());
    }

    public Date getRegistrationTime() {
        return new Date(this.registrationTime.getTime());
    }

    public Location getLocation() {
        return this.location;
    }

    public Cargo getCargo() {
        return this.cargo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HandlingEvent event = (HandlingEvent) o;

        return sameEventAs(event);
    }

    private boolean sameEventAs(HandlingEvent other) {
        return other != null
                && new EqualsBuilder().append(this.cargo, other.cargo)
                .append(this.voyage, other.voyage)
                .append(this.completionTime, other.completionTime)
                .append(this.location, other.location)
                .append(this.type, other.type).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(cargo).append(voyage)
                .append(completionTime).append(location).append(type)
                .toHashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("\n--- Handling event ---\n")
                .append("Cargo: ").append(cargo.getTrackingId()).append("\n")
                .append("Type: ").append(type).append("\n")
                .append("Location: ").append(location.getName()).append("\n")
                .append("Completed on: ").append(completionTime).append("\n")
                .append("Registered on: ").append(registrationTime)
                .append("\n");

        if (voyage != null) {
            builder.append("Voyage: ").append(voyage.getVoyageNumber())
                    .append("\n");
        }

        return builder.toString();
    }
}

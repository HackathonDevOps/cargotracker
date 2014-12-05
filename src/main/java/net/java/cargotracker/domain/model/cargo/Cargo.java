/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package net.java.cargotracker.domain.model.cargo;

import java.io.Serializable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import net.java.cargotracker.domain.model.handling.HandlingEvent;
import net.java.cargotracker.domain.model.handling.HandlingHistory;
import net.java.cargotracker.domain.model.location.Location;
import net.java.cargotracker.domain.shared.DomainObjectUtils;
import org.apache.commons.lang3.Validate;

/**
 * A Cargo. This is the central class in the domain model, and it is the root of
 * the Cargo-Itinerary-Leg-Delivery-RouteSpecification aggregate.
 *
 * A cargo is identified by a unique tracking id, and it always has an origin
 * and a route specification. The life cycle of a cargo begins with the booking
 * procedure, when the tracking id is assigned. During a (short) period of time,
 * between booking and initial routing, the cargo has no itinerary.
 *
 * The booking clerk requests a list of possible routes, matching the route
 * specification, and assigns the cargo to one route. The route to which a cargo
 * is assigned is described by an itinerary.
 *
 * A cargo can be re-routed during transport, on demand of the customer, in
 * which case a new route is specified for the cargo and a new route is
 * requested. The old itinerary, being a value object, is discarded and a new
 * one is attached.
 *
 * It may also happen that a cargo is accidentally misrouted, which should
 * notify the proper personnel and also trigger a re-routing procedure.
 *
 * When a cargo is handled, the status of the delivery changes. Everything about
 * the delivery of the cargo is contained in the Delivery value object, which is
 * replaced whenever a cargo is handled by an asynchronous event triggered by
 * the registration of the handling event.
 *
 * The delivery can also be affected by routing changes, i.e. when a the route
 * specification changes, or the cargo is assigned to a new route. In that case,
 * the delivery update is performed synchronously within the cargo aggregate.
 *
 * The life cycle of a cargo ends when the cargo is claimed by the customer.
 *
 * The cargo aggregate, and the entire domain model, is built to solve the
 * problem of booking and tracking cargo. All important business rules for
 * determining whether or not a cargo is misdirected, what the current status of
 * the cargo is (on board carrier, in port etc), are captured in this aggregate.
 */
@Entity
@NamedQueries({
    @NamedQuery(name = "Cargo.findAll",
            query = "Select c from Cargo c"),
    @NamedQuery(name = "Cargo.findByTrackingId",
            query = "Select c from Cargo c where c.trackingId = :trackingId")})
public class Cargo implements Serializable {

    private static final long serialVersionUID = 1L;
    // Auto-generated surrogate key
    @Id
    @GeneratedValue
    private Long id;
    @Embedded
    private TrackingId trackingId;
    @ManyToOne
    @JoinColumn(name = "origin_id", updatable = false)
    private Location origin;
    @Embedded
    private RouteSpecification routeSpecification;
    @Embedded // This should be nullable: https://java.net/jira/browse/JPA_SPEC-42
    private Itinerary itinerary;
    @Embedded
    private Delivery delivery;

    public Cargo() {
        // Nothing to initialize.
    }

    public Cargo(TrackingId trackingId, RouteSpecification routeSpecification) {
        Validate.notNull(trackingId, "Tracking ID is required");
        Validate.notNull(routeSpecification, "Route specification is required");

        this.trackingId = trackingId;
        // Cargo origin never changes, even if the route specification changes.
        // However, at creation, cargo orgin can be derived from the initial
        // route specification.
        this.origin = routeSpecification.getOrigin();
        this.routeSpecification = routeSpecification;

        this.delivery = Delivery.derivedFrom(this.routeSpecification,
                this.itinerary, HandlingHistory.EMPTY);
        this.itinerary = Itinerary.EMPTY_ITINERARY;
    }

    public TrackingId getTrackingId() {
        return trackingId;
    }

    public void setOrigin(Location origin) {
        this.origin = origin;
    }

    public Location getOrigin() {
        return origin;
    }

    public RouteSpecification getRouteSpecification() {
        return routeSpecification;
    }

    /**
     * @return The delivery. Never null.
     */
    public Delivery getDelivery() {
        return delivery;
    }

    /**
     * @return The itinerary. Never null.
     */
    public Itinerary getItinerary() {
        return DomainObjectUtils.nullSafe(this.itinerary,
                Itinerary.EMPTY_ITINERARY);
    }

    /**
     * Specifies a new route for this cargo.
     */
    public void specifyNewRoute(RouteSpecification routeSpecification) {
        Validate.notNull(routeSpecification, "Route specification is required");

        this.routeSpecification = routeSpecification;
        // Handling consistency within the Cargo aggregate synchronously
        this.delivery = delivery.updateOnRouting(this.routeSpecification,
                this.itinerary);
    }

    public void assignToRoute(Itinerary itinerary) {
        Validate.notNull(itinerary, "Itinerary is required for assignment");

        this.itinerary = itinerary;
        // Handling consistency within the Cargo aggregate synchronously
        this.delivery = delivery.updateOnRouting(this.routeSpecification,
                this.itinerary);
    }

    /**
     * Updates all aspects of the cargo aggregate status based on the current
     * route specification, itinerary and handling of the cargo.
     * <p/>
     * When either of those three changes, i.e. when a new route is specified
     * for the cargo, the cargo is assigned to a route or when the cargo is
     * handled, the status must be re-calculated.
     * <p/>
     * {@link RouteSpecification} and {@link Itinerary} are both inside the
     * Cargo aggregate, so changes to them cause the status to be updated
     * <b>synchronously</b>, but changes to the delivery history (when a cargo
     * is handled) cause the status update to happen <b>asynchronously</b> since
     * {@link HandlingEvent} is in a different aggregate.
     *
     * @param handlingHistory handling history
     */
    public void deriveDeliveryProgress(HandlingHistory handlingHistory) {
        this.delivery = Delivery.derivedFrom(getRouteSpecification(), getItinerary(),
                handlingHistory);
    }

    /**
     * @param object to compare
     * @return True if they have the same identity
     * @see #sameIdentityAs(Cargo)
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        Cargo other = (Cargo) object;
        return sameIdentityAs(other);
    }

    private boolean sameIdentityAs(Cargo other) {
        return other != null && trackingId.sameValueAs(other.trackingId);
    }

    /**
     * @return Hash code of tracking id.
     */
    @Override
    public int hashCode() {
        return trackingId.hashCode();
    }

    @Override
    public String toString() {
        return trackingId.toString();
    }
}

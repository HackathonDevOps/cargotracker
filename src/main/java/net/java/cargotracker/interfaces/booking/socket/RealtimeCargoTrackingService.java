package net.java.cargotracker.interfaces.booking.socket;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import net.java.cargotracker.infrastructure.events.cdi.CargoInspected;
import net.java.cargotracker.domain.model.cargo.Cargo;

/**
 * WebSocket service for tracking all cargoes in real time.
 *
 * @author Vijay Nair
 */
@Singleton
@ServerEndpoint("/tracking")
public class RealtimeCargoTrackingService {

    private static final Logger logger = Logger.getLogger(
            RealtimeCargoTrackingService.class.getName());

    private final Set<Session> sessions = new HashSet<>();

    @OnOpen
    public void onOpen(final Session session) {
        sessions.add(session);
    }

    @OnClose
    public void onClose(final Session session) {
        sessions.remove(session);
    }

    public void onCargoInspected(@Observes @CargoInspected Cargo cargo) {
        Writer writer = new StringWriter();

        try (JsonGenerator generator = Json.createGenerator(writer)) {
            generator
                    .writeStartObject()
                    .write("trackingId", cargo.getTrackingId().getIdString())
                    .write("origin", cargo.getOrigin().getName())
                    .write("destination", cargo.getRouteSpecification().getDestination().getName())
                    .write("lastKnownLocation", cargo.getDelivery().getLastKnownLocation().getName())
                    .write("transportStatus", cargo.getDelivery().getTransportStatus().toString())
                    .writeEnd();
        }

        String jsonValue = writer.toString();

        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendText(jsonValue);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Unable to publish WebSocket message", ex);
            }
        }

    }
}
package net.java.cargotracker.application;

import javax.validation.constraints.NotNull;
import net.java.cargotracker.domain.model.cargo.TrackingId;

public interface CargoInspectionService {

    /**
     * Inspect cargo and send relevant notifications to interested parties, for
     * example if a cargo has been misdirected, or unloaded at the final
     * destination.
     */
    public void inspectCargo(
            @NotNull(message = "Tracking ID is required") TrackingId trackingId);
}

package main.service;

import main.domain.Cargo;
import main.domain.Shipment;

public class ShipmentService {
    private final PricingService pricingService;
    private final PermissionService permissionService;
    private final ManifestRepository repository;
    private final NotificationService notificationService;

    public ShipmentService(PricingService pricingService, PermissionService permissionService,
                           ManifestRepository repository, NotificationService notificationService) {
        this.pricingService = pricingService;
        this.permissionService = permissionService;
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public String validateCalculatePrintSaveAndNotify(Shipment shipment) {
        if (!isCustomerAllowed(shipment)) {
            return "ERROR_CUSTOMER";
        }

        if (shipment.getCargo().isEmpty()) {
            return "ERROR_EMPTY";
        }

        double totalWeight = 0;
        double totalValue = 0;
        boolean hazardous = false;

        for (Cargo item : shipment.getCargo()) {
            totalWeight += item.getWeight();
            totalValue += item.getDeclaredValue();

            if (item.isHazardous()) {
                hazardous = true;
            }
        }

        if (totalWeight > shipment.getShip().getCapacity()) {
            return "ERROR_CAPACITY";
        }

        if (hazardous
                && !permissionService.canCarryHazardous(shipment.getShip())) {
            return "ERROR_PERMISSION";
        }

        return completeShipment(
                shipment,
                totalWeight,
                totalValue,
                hazardous);
    }

    private double calculateTotal(
            Shipment shipment,
            double totalWeight,
            double totalValue,
            boolean hazardous) {

        double total = pricingService.calculatePrice(
                totalWeight,
                totalValue,
                hazardous,
                shipment.getOrigin().getName(),
                shipment.getOrigin().getSector(),
                shipment.getOrigin().getSecurityLevel(),
                shipment.getDestination().getName(),
                shipment.getDestination().getSector(),
                shipment.getDestination().getSecurityLevel(),
                shipment.getCustomer().getLoyaltyYears(),
                shipment.getCustomer().isActive(),
                shipment.getCustomer().isSuspended(),
                shipment.getDepartureDate());

        total += pricingService.calculateInsurance(
                totalValue,
                hazardous,
                shipment.getCustomer());

        return total;
    }

    private String completeShipment(
            Shipment shipment,
            double totalWeight,
            double totalValue,
            boolean hazardous) {

        double total = calculateTotal(
                shipment,
                totalWeight,
                totalValue,
                hazardous);

        shipment.setTotal(total);
        shipment.setStatus("READY");

        repository.save(shipment);

        return buildConfirmation(shipment, total);
    }

    private boolean isCustomerAllowed(Shipment shipment) {
        return shipment.getCustomer().isActive()
                && !shipment.getCustomer().isSuspended();
    }

    private String buildConfirmation(Shipment shipment, double total) {
        String category = total > 2000 ? "PRIORITY" : "REGULAR";

        String output = category
                + " | "
                + shipment.getReference()
                + " | "
                + String.format("%.2f", total);

        return output
                + " | "
                + notificationService.confirmationFor(shipment);
    }
}

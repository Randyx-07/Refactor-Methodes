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
        if (shipment.getCustomer().isActive()) {
            if (!shipment.getCustomer().isSuspended()) {
                if (!shipment.getCargo().isEmpty()) {
                    double totalWeight = 0;
                    double totalValue = 0;
                    boolean hazardous = false;
                    for (Cargo item : shipment.getCargo()) {
                        totalWeight += item.getWeight();
                        totalValue += item.getDeclaredValue();
                        if (item.isHazardous()) hazardous = true;
                    }
                    if (totalWeight > shipment.getShip().getCapacity()) return "ERROR_CAPACITY";
                    if (hazardous && !permissionService.canCarryHazardous(shipment.getShip()))
                        return "ERROR_PERMISSION";

                    double total = calculateTotal(
                            shipment,
                            totalWeight,
                            totalValue,
                            hazardous);

                    shipment.setTotal(total);
                    shipment.setStatus("READY");
                    String category = total > 2000 ? "PRIORITY" : "REGULAR";
                    String output = category
                            + " | "
                            + shipment.getReference()
                            + " | "
                            + String.format("%.2f", total);

                    repository.save(shipment);

                    return output + " | " + notificationService.confirmationFor(shipment);
                } else {
                    return "ERROR_EMPTY";
                }
            } else {
                return "ERROR_CUSTOMER";
            }
        } else {
            return "ERROR_CUSTOMER";
        }
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
}

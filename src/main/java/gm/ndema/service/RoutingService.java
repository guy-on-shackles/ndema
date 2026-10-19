package gm.ndema.service;

import gm.ndema.model.Enums;
import gm.ndema.model.Institution;
import gm.ndema.repository.InstitutionRepository;
import org.springframework.stereotype.Service;

@Service
public class RoutingService {

    private final InstitutionRepository institutionRepository;

    public RoutingService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    public Institution route(Enums.Category category, String lga) {
        if (category == Enums.Category.ELECTRICITY_STREETLIGHT || category == Enums.Category.WATER_SUPPLY) {
            return institutionRepository.findByShortName("NAWEC").orElse(null);
        }
        if (category == Enums.Category.FLOODING_HAZARD) {
            return institutionRepository.findByShortName("NDMA").orElse(null);
        }

        // Municipal council routing based on LGA string check
        String upperLga = (lga != null) ? lga.toUpperCase() : "";
        if (upperLga.contains("BANJUL")) {
            return institutionRepository.findByShortName("BCC").orElse(null);
        }

        // Default to KMC for Kanifing, Sukuta, Brikama, Serrekunda, etc.
        return institutionRepository.findByShortName("KMC").orElse(null);
    }
}
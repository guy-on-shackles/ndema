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
            return find("NAWEC");
        }
        if (category == Enums.Category.FLOODING_HAZARD) {
            return find("NDMA");
        }

        // National-level categories go to their ministry or agency; if that institution is not in the
        // system (an admin may have removed or renamed it), fall back to the municipal council.
        Institution national = null;
        if (category == Enums.Category.AGRICULTURE) {
            national = find("MOA");
        } else if (category == Enums.Category.EDUCATION) {
            national = find("MOBSE");
        } else if (category == Enums.Category.HEALTHCARE) {
            national = find("MOH");
        } else if (category == Enums.Category.PUBLIC_SAFETY) {
            national = find("GPF");
        }
        if (national != null) {
            return national;
        }

        return council(lga);
    }

    // Municipal council routing based on the LGA text
    private Institution council(String lga) {
        String upperLga = (lga != null) ? lga.toUpperCase() : "";
        if (upperLga.contains("BANJUL")) {
            return find("BCC");
        }
        // Default to KMC for Kanifing, Sukuta, Brikama, Serrekunda, etc.
        return find("KMC");
    }

    private Institution find(String shortName) {
        return institutionRepository.findByShortName(shortName).orElse(null);
    }
}

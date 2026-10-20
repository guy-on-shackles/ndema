package gm.ndema.controller;

import gm.ndema.model.Complaint;
import gm.ndema.model.Enums;
import gm.ndema.repository.ComplaintRepository;
import gm.ndema.service.RoutingService;
import gm.ndema.service.UrgencyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

@Controller
public class UssdController {

    private final ComplaintRepository complaintRepository;
    private final RoutingService routingService;
    private final UrgencyService urgencyService;

    public UssdController(ComplaintRepository complaintRepository,
                          RoutingService routingService,
                          UrgencyService urgencyService) {
        this.complaintRepository = complaintRepository;
        this.routingService = routingService;
        this.urgencyService = urgencyService;
    }

    @GetMapping("/ussd-sim")
    public String simulatorPage() {
        return "ussd-sim";
    }

    @PostMapping("/ussd")
    @ResponseBody
    public String handleUssd(@RequestParam String text, @RequestParam(defaultValue = "2201234567") String phoneNumber) {
        String[] inputs = text.isBlank() ? new String[0] : text.split("\\*");

        if (inputs.length == 0) {
            return "CON Welcome to Ndema\n1. Report a problem\n2. Track a complaint";
        }

        if ("2".equals(inputs[0])) {
            if (inputs.length == 1) {
                return "CON Enter your tracking code (e.g. GM-2026-000001):";
            }
            if (inputs.length == 2) {
                String code = inputs[1].trim().toUpperCase();
                return complaintRepository.findByTrackingCode(code)
                        .map(c -> "END " + c.getTrackingCode() + "\nStatus: " + c.getStatus()
                                + "\nAgency: " + (c.getInstitution() != null ? c.getInstitution().getShortName() : "Pending"))
                        .orElse("END Tracking code not found. Check and try again.");
            }
        }

        if ("1".equals(inputs[0])) {
            if (inputs.length == 1) {
                return "CON Category:\n1. Roads/Drains\n2. Waste\n3. Water\n4. Electricity\n5. Public Safety\n6. Flooding\n7. Health\n8. Education\n9. Agriculture\n0. Other";
            }
            if (inputs.length == 2) {
                return "CON Area:\n1. Banjul\n2. Kanifing\n3. Brikama\n4. Other";
            }
            if (inputs.length == 3) {
                return "CON Describe landmark & issue:";
            }
            if (inputs.length == 4) {
                Enums.Category cat = mapCategory(inputs[1]);
                String lga = mapLga(inputs[2]);

                Complaint c = new Complaint();
                c.setCategory(cat);
                c.setLga(lga);
                c.setTitle("USSD Report: " + cat);
                c.setDescription(inputs[3].length() > 1000 ? inputs[3].substring(0, 1000) : inputs[3]);
                c.setChannel(Enums.Channel.USSD);
                c.setReporterPhone(phoneNumber);
                c.setInstitution(routingService.route(cat, lga));
                c.setStatus(Enums.Status.RECEIVED);

                int score = urgencyService.calculateScore(c);
                c.setUrgencyScore(score);
                c.setUrgencyLevel(urgencyService.calculateLevel(score));

                Complaint saved = complaintRepository.save(c);
                String code = String.format("GM-%d-%06d", Year.now().getValue(), saved.getId());
                saved.setTrackingCode(code);
                complaintRepository.save(saved);

                return "END Thank you! Tracking ID: " + code + ". Dial again and choose 2 to track it.";
            }
        }
        return "END Invalid entry. Try again.";
    }

    private Enums.Category mapCategory(String val) {
        return switch (val) {
            case "1" -> Enums.Category.ROADS_DRAINAGE;
            case "2" -> Enums.Category.WASTE_SANITATION;
            case "3" -> Enums.Category.WATER_SUPPLY;
            case "4" -> Enums.Category.ELECTRICITY_STREETLIGHT;
            case "5" -> Enums.Category.PUBLIC_SAFETY;
            case "6" -> Enums.Category.FLOODING_HAZARD;
            case "7" -> Enums.Category.HEALTHCARE;
            case "8" -> Enums.Category.EDUCATION;
            case "9" -> Enums.Category.AGRICULTURE;
            default -> Enums.Category.OTHER;
        };
    }

    private String mapLga(String val) {
        return switch (val) {
            case "1" -> "BANJUL";
            case "2" -> "KANIFING";
            case "3" -> "BRIKAMA";
            default -> "MANSAKONKO";
        };
    }
}

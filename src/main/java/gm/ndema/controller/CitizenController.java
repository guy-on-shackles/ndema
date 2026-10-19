package gm.ndema.controller;

import gm.ndema.model.Complaint;
import gm.ndema.model.Enums;
import gm.ndema.repository.ComplaintRepository;
import gm.ndema.service.RoutingService;
import gm.ndema.service.UrgencyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Year;

@Controller
public class CitizenController {

    private final ComplaintRepository complaintRepository;
    private final RoutingService routingService;
    private final UrgencyService urgencyService;

    public CitizenController(ComplaintRepository complaintRepository,
                             RoutingService routingService,
                             UrgencyService urgencyService) {
        this.complaintRepository = complaintRepository;
        this.routingService = routingService;
        this.urgencyService = urgencyService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/report")
    public String reportForm(Model model) {
        model.addAttribute("categories", Enums.Category.values());
        model.addAttribute("lgas", Enums.Lga.values());
        model.addAttribute("complaint", new Complaint());
        return "report";
    }

    @PostMapping("/report")
    public String submitReport(@ModelAttribute Complaint complaint) {
        // Automatically route to appropriate institution (KMC, BCC, NAWEC, NDMA)
        complaint.setInstitution(routingService.route(complaint.getCategory(), complaint.getLga()));

        // Calculate urgency score and set status & channel
        int score = urgencyService.calculateScore(complaint);
        complaint.setUrgencyScore(score);
        complaint.setUrgencyLevel(urgencyService.calculateLevel(score));
        complaint.setStatus(Enums.Status.SUBMITTED);
        complaint.setChannel(Enums.Channel.WEB);
        complaint.setCreatedAt(LocalDateTime.now());

        // Save & generate tracking code
        Complaint saved = complaintRepository.save(complaint);
        String code = String.format("GM-%d-%06d", Year.now().getValue(), saved.getId());
        saved.setTrackingCode(code);
        complaintRepository.save(saved);

        return "redirect:/track/" + code;
    }

    @GetMapping("/track/{code}")
    public String trackComplaint(@PathVariable String code, Model model) {
        Complaint complaint = complaintRepository.findByTrackingCode(code).orElse(null);
        model.addAttribute("complaint", complaint);
        return "track";
    }
}
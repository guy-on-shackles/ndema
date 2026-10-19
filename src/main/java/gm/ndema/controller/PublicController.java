package gm.ndema.controller;

import gm.ndema.model.Complaint;
import gm.ndema.model.Enums;
import gm.ndema.repository.ComplaintRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PublicController {

    private final ComplaintRepository complaintRepository;

    public PublicController(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    @GetMapping("/public")
    public String transparencyPage(Model model) {
        List<Complaint> all = complaintRepository.findAll();
        long total = all.size();
        long resolved = all.stream().filter(c -> c.getStatus() == Enums.Status.RESOLVED).count();

        model.addAttribute("total", total);
        model.addAttribute("resolved", resolved);
        model.addAttribute("resolutionRate", total > 0 ? (resolved * 100 / total) : 0);
        model.addAttribute("complaints", all);
        return "public";
    }
}
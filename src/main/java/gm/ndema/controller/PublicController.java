package gm.ndema.controller;

import gm.ndema.model.Complaint;
import gm.ndema.model.Enums;
import gm.ndema.repository.ComplaintRepository;
import gm.ndema.service.UrgencyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class PublicController {

    private static final String CONFIRMED_KEY = "confirmedReports";

    private final ComplaintRepository complaintRepository;
    private final UrgencyService urgencyService;

    public PublicController(ComplaintRepository complaintRepository, UrgencyService urgencyService) {
        this.complaintRepository = complaintRepository;
        this.urgencyService = urgencyService;
    }

    @GetMapping("/public")
    public String transparencyPage(HttpSession session, Model model) {
        List<Complaint> all = new ArrayList<>(complaintRepository.findAll());
        all.sort(Comparator.comparing(Complaint::getCreatedAt,
                Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed());

        long total = all.size();
        long resolved = all.stream().filter(c -> c.getStatus() == Enums.Status.RESOLVED).count();

        model.addAttribute("total", total);
        model.addAttribute("resolved", resolved);
        model.addAttribute("resolutionRate", total > 0 ? (resolved * 100 / total) : 0);
        model.addAttribute("complaints", all);
        model.addAttribute("confirmedIds", confirmedIds(session));
        return "public";
    }

    // "I'm affected too" - each browser session can confirm a report once
    @PostMapping("/public/confirm/{id}")
    public String confirm(@PathVariable Long id, HttpSession session) {
        Set<Long> confirmed = confirmedIds(session);
        if (!confirmed.contains(id)) {
            complaintRepository.findById(id).ifPresent(c -> {
                int current = c.getConfirmations() != null ? c.getConfirmations() : 0;
                c.setConfirmations(current + 1);
                if (!c.isUrgencyOverridden()) {
                    int score = urgencyService.calculateScore(c);
                    c.setUrgencyScore(score);
                    c.setUrgencyLevel(urgencyService.calculateLevel(score));
                }
                complaintRepository.save(c);
                confirmed.add(id);
            });
        }
        return "redirect:/public";
    }

    @SuppressWarnings("unchecked")
    private Set<Long> confirmedIds(HttpSession session) {
        Object existing = session.getAttribute(CONFIRMED_KEY);
        if (existing instanceof Set<?>) {
            return (Set<Long>) existing;
        }
        Set<Long> fresh = new HashSet<>();
        session.setAttribute(CONFIRMED_KEY, fresh);
        return fresh;
    }
}

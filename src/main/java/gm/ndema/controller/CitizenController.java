package gm.ndema.controller;

import gm.ndema.model.Complaint;
import gm.ndema.model.Enums;
import gm.ndema.model.VoiceNote;
import gm.ndema.repository.ComplaintRepository;
import gm.ndema.repository.VoiceNoteRepository;
import gm.ndema.service.RoutingService;
import gm.ndema.service.UrgencyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.web.util.UriUtils;

@Controller
public class CitizenController {

    private static final Pattern AUDIO_TYPE = Pattern.compile("^audio/[a-z0-9.+-]+$");
    private static final long MAX_VOICE_BYTES = 8L * 1024 * 1024;

    private final ComplaintRepository complaintRepository;
    private final VoiceNoteRepository voiceNoteRepository;
    private final RoutingService routingService;
    private final UrgencyService urgencyService;

    public CitizenController(ComplaintRepository complaintRepository,
                             VoiceNoteRepository voiceNoteRepository,
                             RoutingService routingService,
                             UrgencyService urgencyService) {
        this.complaintRepository = complaintRepository;
        this.voiceNoteRepository = voiceNoteRepository;
        this.routingService = routingService;
        this.urgencyService = urgencyService;
    }

    // Only these form fields may be bound from a citizen's request (blocks overwriting id, status, urgency, etc.)
    @InitBinder("complaint")
    public void restrictBinding(WebDataBinder binder) {
        binder.setAllowedFields("title", "description", "category", "lga", "reporterPhone");
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        model.addAttribute("signedIn", session.getAttribute(AdminController.SESSION_KEY) != null);
        return "home";
    }

    @GetMapping("/report")
    public String reportForm(Model model) {
        prepareForm(model);
        model.addAttribute("complaint", new Complaint());
        return "report";
    }

    @PostMapping("/report")
    public String submitReport(@ModelAttribute("complaint") Complaint complaint,
                               @RequestParam(value = "voiceNote", required = false) MultipartFile voiceNote,
                               Model model) {
        String title = complaint.getTitle() == null ? "" : complaint.getTitle().trim();
        String description = complaint.getDescription() == null ? "" : complaint.getDescription().trim();
        String lga = complaint.getLga() == null ? "" : complaint.getLga().trim().toUpperCase(Locale.ROOT);

        String error = null;
        if (complaint.getCategory() == null) {
            error = "Please tap a category icon.";
        } else if (title.isEmpty() || description.isEmpty() || lga.isEmpty()) {
            error = "Please fill in the area, title and description.";
        } else if (title.length() > 200) {
            error = "Title is too long (max 200 characters).";
        } else if (description.length() > 1000) {
            error = "Description is too long (max 1000 characters).";
        } else if (lga.length() > 100) {
            error = "Area name is too long.";
        }
        if (error != null) {
            prepareForm(model);
            model.addAttribute("error", error);
            return "report";
        }

        complaint.setTitle(title);
        complaint.setDescription(description);
        complaint.setLga(lga);
        String phone = complaint.getReporterPhone() == null ? "" : complaint.getReporterPhone().trim();
        complaint.setReporterPhone(phone.length() > 30 ? phone.substring(0, 30) : phone);

        // Automatically route to appropriate institution (KMC, BCC, NAWEC, NDMA)
        complaint.setInstitution(routingService.route(complaint.getCategory(), lga));

        // Calculate urgency score and set status & channel
        int score = urgencyService.calculateScore(complaint);
        complaint.setUrgencyScore(score);
        complaint.setUrgencyLevel(urgencyService.calculateLevel(score));
        complaint.setStatus(Enums.Status.SUBMITTED);
        complaint.setChannel(Enums.Channel.WEB);
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());

        // Save & generate tracking code
        Complaint saved = complaintRepository.save(complaint);
        String code = String.format("GM-%d-%06d", Year.now().getValue(), saved.getId());
        saved.setTrackingCode(code);

        // Optional voice note (stored separately; only admins can play it back)
        if (voiceNote != null && !voiceNote.isEmpty() && voiceNote.getSize() <= MAX_VOICE_BYTES) {
            String type = baseContentType(voiceNote.getContentType());
            if (type != null) {
                try {
                    voiceNoteRepository.save(new VoiceNote(saved.getId(), type, voiceNote.getBytes()));
                    saved.setVoiceAttached(true);
                } catch (IOException ignored) {
                    // report is still saved without the voice note
                }
            }
        }
        complaintRepository.save(saved);

        return "redirect:/track/" + code;
    }

    @GetMapping("/track")
    public String trackSearch(@RequestParam(value = "code", required = false) String code) {
        if (code == null || code.isBlank()) {
            return "redirect:/";
        }
        String clean = code.trim().toUpperCase(Locale.ROOT);
        return "redirect:/track/" + UriUtils.encodePathSegment(clean, StandardCharsets.UTF_8);
    }

    @GetMapping("/track/{code}")
    public String trackComplaint(@PathVariable String code, Model model) {
        Complaint complaint = complaintRepository.findByTrackingCode(code.trim().toUpperCase(Locale.ROOT)).orElse(null);
        model.addAttribute("complaint", complaint);
        return "track";
    }

    private void prepareForm(Model model) {
        model.addAttribute("categories", Enums.Category.values());
        model.addAttribute("lgas", Enums.Lga.values());
    }

    /** Returns "audio/xyz" without parameters (e.g. ";codecs=opus"), or null if it is not an audio type. */
    static String baseContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String base = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return AUDIO_TYPE.matcher(base).matches() ? base : null;
    }
}

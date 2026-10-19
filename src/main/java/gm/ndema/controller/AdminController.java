package gm.ndema.controller;

import gm.ndema.model.*;
import gm.ndema.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ComplaintRepository complaintRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminUserRepository adminUserRepository;
    private final ComplaintVersionRepository versionRepository;

    public AdminController(ComplaintRepository complaintRepository,
                           AuditLogRepository auditLogRepository,
                           AdminUserRepository adminUserRepository,
                           ComplaintVersionRepository versionRepository) {
        this.complaintRepository = complaintRepository;
        this.auditLogRepository = auditLogRepository;
        this.adminUserRepository = adminUserRepository;
        this.versionRepository = versionRepository;
    }

    // Redirect /admin directly to login or dashboard
    @GetMapping
    public String adminRoot(HttpSession session) {
        if (session.getAttribute("loggedAdmin") != null) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/admin/login";
    }

    // 1 & 2. Explicit Admin Login Page
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("loggedAdmin") != null) {
            return "redirect:/admin/dashboard";
        }
        return "admin/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session,
                               Model model) {
        AdminUser admin = adminUserRepository.findByUsername(username).orElse(null);
        if (admin != null && admin.getPassword().equals(password)) {
            session.setAttribute("loggedAdmin", admin);
            logAction(admin.getUsername(), "LOGIN", "Admin logged in successfully.");
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("error", "Invalid Admin Credentials or Access Code!");
        return "admin/login";
    }

    // 2. Explicit Officer Dashboard Mapping
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        AdminUser admin = (AdminUser) session.getAttribute("loggedAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        List<Complaint> complaints = complaintRepository.findAll();
        List<AuditLog> auditLogs = auditLogRepository.findAll();

        model.addAttribute("complaints", complaints);
        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("currentAdmin", admin);

        return "admin/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        AdminUser admin = (AdminUser) session.getAttribute("loggedAdmin");
        if (admin != null) {
            logAction(admin.getUsername(), "LOGOUT", "Admin logged out.");
        }
        session.invalidate();
        return "redirect:/admin/login";
    }

    // Update Complaint Status & Notes
    @PostMapping("/complaints/{id}/status")
    public String updateComplaintStatus(@PathVariable Long id,
                                        @RequestParam Enums.Status status,
                                        @RequestParam(required = false) String adminNotes,
                                        HttpSession session) {
        AdminUser admin = (AdminUser) session.getAttribute("loggedAdmin");
        if (admin == null) return "redirect:/admin/login";

        Complaint complaint = complaintRepository.findById(id).orElseThrow();

        // Create Version Snapshot
        ComplaintVersion snapshot = new ComplaintVersion();
        snapshot.setComplaintId(complaint.getId());
        snapshot.setTitle(complaint.getTitle());
        snapshot.setDescription(complaint.getDescription());
        snapshot.setCategory(complaint.getCategory());
        snapshot.setStatus(complaint.getStatus());
        snapshot.setModifiedByAdmin(admin.getUsername());
        snapshot.setModifiedAt(LocalDateTime.now());
        versionRepository.save(snapshot);

        Enums.Status oldStatus = complaint.getStatus();
        complaint.setStatus(status);
        if (adminNotes != null && !adminNotes.isBlank()) {
            complaint.setAdminNotes(adminNotes);
        }
        complaintRepository.save(complaint);

        logAction(admin.getUsername(), "STATUS_CHANGE",
                "Changed Complaint #" + complaint.getTrackingCode() + " status from " + oldStatus + " to " + status);

        return "redirect:/admin/dashboard";
    }

    private void logAction(String username, String actionType, String details) {
        AuditLog log = new AuditLog(username, actionType, details, LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
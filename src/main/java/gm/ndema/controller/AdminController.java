package gm.ndema.controller;

import gm.ndema.model.*;
import gm.ndema.repository.*;
import gm.ndema.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    public static final String SESSION_KEY = "adminId";
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.ENGLISH);

    private final ComplaintRepository complaintRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminUserRepository adminUserRepository;
    private final ComplaintVersionRepository versionRepository;
    private final VoiceNoteRepository voiceNoteRepository;
    private final InstitutionRepository institutionRepository;

    public AdminController(ComplaintRepository complaintRepository,
                           AuditLogRepository auditLogRepository,
                           AdminUserRepository adminUserRepository,
                           ComplaintVersionRepository versionRepository,
                           VoiceNoteRepository voiceNoteRepository,
                           InstitutionRepository institutionRepository) {
        this.complaintRepository = complaintRepository;
        this.auditLogRepository = auditLogRepository;
        this.adminUserRepository = adminUserRepository;
        this.versionRepository = versionRepository;
        this.voiceNoteRepository = voiceNoteRepository;
        this.institutionRepository = institutionRepository;
    }

    // The session only stores the admin's id; the admin is re-loaded on every request,
    // so a removed admin loses access immediately.
    private AdminUser currentAdmin(HttpSession session) {
        Object id = session.getAttribute(SESSION_KEY);
        if (id instanceof Long adminId) {
            return adminUserRepository.findById(adminId).orElse(null);
        }
        return null;
    }

    // ---------------------------------------------------------------- login / logout

    @GetMapping
    public String adminRoot(HttpSession session) {
        return currentAdmin(session) != null ? "redirect:/admin/dashboard" : "redirect:/admin/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        return currentAdmin(session) != null ? "redirect:/admin/dashboard" : "admin/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String accessCode,
                               HttpServletRequest request,
                               Model model) {
        AdminUser admin = adminUserRepository.findByUsername(username.trim()).orElse(null);
        boolean ok = admin != null
                && PasswordUtil.matches(password, admin.getPassword())
                && admin.getAccessCode() != null
                && admin.getAccessCode().equals(accessCode.trim());
        if (!ok) {
            model.addAttribute("error", "Invalid username, password or access code.");
            return "admin/login";
        }
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute(SESSION_KEY, admin.getId());
        logAction(admin.getUsername(), "LOGIN", "Admin \"" + admin.getUsername() + "\" signed in on " + now(LocalDateTime.now()));
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        AdminUser admin = currentAdmin(session);
        if (admin != null) {
            logAction(admin.getUsername(), "LOGOUT", "Admin \"" + admin.getUsername() + "\" signed out on " + now(LocalDateTime.now()));
        }
        session.invalidate();
        return "redirect:/";
    }

    // ---------------------------------------------------------------- dashboard

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "urgency") String sort,
                            HttpSession session, Model model) {
        AdminUser admin = currentAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        List<Complaint> complaints = new ArrayList<>(complaintRepository.findAll());
        complaints.sort(comparatorFor(sort));

        // Group by LGA + category. Because the list is already sorted, groups appear in order of
        // their best-ranked report, and reports inside a group keep the chosen sort order.
        Map<String, ComplaintGroup> groups = new LinkedHashMap<>();
        for (Complaint c : complaints) {
            String lga = c.getLga() == null || c.getLga().isBlank() ? "UNKNOWN" : c.getLga().trim().toUpperCase(Locale.ROOT);
            Enums.Category category = c.getCategory() != null ? c.getCategory() : Enums.Category.OTHER;
            groups.computeIfAbsent(lga + "|" + category, k -> new ComplaintGroup(lga, category)).getComplaints().add(c);
        }

        model.addAttribute("groups", new ArrayList<>(groups.values()));
        model.addAttribute("totalCount", complaints.size());
        model.addAttribute("criticalCount", complaints.stream().filter(c -> c.getUrgencyLevel() == Enums.Urgency.CRITICAL).count());
        model.addAttribute("highCount", complaints.stream().filter(c -> c.getUrgencyLevel() == Enums.Urgency.HIGH).count());
        model.addAttribute("inProgressCount", complaints.stream().filter(c -> c.getStatus() == Enums.Status.IN_PROGRESS).count());
        model.addAttribute("resolvedCount", complaints.stream().filter(c -> c.getStatus() == Enums.Status.RESOLVED).count());
        model.addAttribute("sort", sort);
        model.addAttribute("institutions", institutionRepository.findAllByOrderByShortNameAsc());
        model.addAttribute("currentAdmin", admin);
        return "admin/dashboard";
    }

    private Comparator<Complaint> comparatorFor(String sort) {
        Comparator<Complaint> byUrgency = Comparator
                .comparingInt((Complaint c) -> c.getUrgencyLevel() == null ? 0 : c.getUrgencyLevel().ordinal())
                .thenComparingInt(c -> c.getUrgencyScore() == null ? 0 : c.getUrgencyScore())
                .reversed();
        Comparator<Complaint> newestFirst = Comparator.comparing(Complaint::getCreatedAt,
                Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));
        Comparator<Complaint> oldestFirst = Comparator.comparing(Complaint::getCreatedAt,
                Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()));

        return switch (sort) {
            case "newest" -> newestFirst.thenComparing(byUrgency);
            case "oldest" -> oldestFirst.thenComparing(byUrgency);
            case "status" -> Comparator.comparingInt((Complaint c) -> c.getStatus() == null ? 0 : c.getStatus().ordinal())
                    .thenComparing(byUrgency);
            case "confirmations" -> Comparator.comparingInt((Complaint c) -> c.getConfirmations() == null ? 0 : c.getConfirmations())
                    .reversed().thenComparing(byUrgency);
            default -> byUrgency.thenComparing(newestFirst);
        };
    }

    // ---------------------------------------------------------------- editing reports (admins only)

    @PostMapping("/complaints/{id}/update")
    @Transactional
    public String updateComplaint(@PathVariable Long id,
                                  @RequestParam(required = false) Enums.Urgency urgency,
                                  @RequestParam(required = false) Enums.Status status,
                                  @RequestParam(required = false) String adminNotes,
                                  @RequestParam(required = false) Long institutionId,
                                  @RequestParam(required = false) String newInstitutionName,
                                  @RequestParam(required = false) String newInstitutionShort,
                                  HttpSession session, RedirectAttributes redirect) {
        AdminUser admin = currentAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        Complaint complaint = complaintRepository.findById(id).orElse(null);
        if (complaint == null) {
            redirect.addFlashAttribute("err", "Report not found.");
            return "redirect:/admin/dashboard";
        }
        LocalDateTime now = LocalDateTime.now();
        Institution institution = resolveInstitution(admin, institutionId, newInstitutionName, newInstitutionShort, now);
        boolean changed = applyChange(admin, complaint, urgency, status, adminNotes, institution, now);
        redirect.addFlashAttribute(changed ? "msg" : "err",
                changed ? "Report " + complaint.getTrackingCode() + " updated." : "No changes were made.");
        return "redirect:/admin/dashboard";
    }

    // Bulk edit: one action, but the audit log records every report separately (as if done one by one)
    @PostMapping("/complaints/bulk")
    @Transactional
    public String bulkUpdate(@RequestParam(required = false) List<Long> ids,
                             @RequestParam(required = false) Enums.Urgency urgency,
                             @RequestParam(required = false) Enums.Status status,
                             @RequestParam(required = false) Long institutionId,
                             HttpSession session, RedirectAttributes redirect) {
        AdminUser admin = currentAdmin(session);
        if (admin == null) return "redirect:/admin/login";

        if (ids == null || ids.isEmpty()) {
            redirect.addFlashAttribute("err", "Tick at least one report first.");
            return "redirect:/admin/dashboard";
        }
        if (urgency == null && status == null && institutionId == null) {
            redirect.addFlashAttribute("err", "Choose a new urgency, status and/or institution to apply.");
            return "redirect:/admin/dashboard";
        }
        LocalDateTime now = LocalDateTime.now();
        Institution institution = resolveInstitution(admin, institutionId, null, null, now);
        int updated = 0;
        for (Long id : new LinkedHashSet<>(ids)) {
            Complaint complaint = complaintRepository.findById(id).orElse(null);
            if (complaint != null && applyChange(admin, complaint, urgency, status, null, institution, now)) {
                updated++;
            }
        }
        redirect.addFlashAttribute("msg", updated + " of " + ids.size() + " selected report(s) updated.");
        return "redirect:/admin/dashboard";
    }

    /** Applies the edits, writes a version snapshot, and one audit-log line per changed field. */
    private boolean applyChange(AdminUser admin, Complaint c, Enums.Urgency newUrgency,
                                Enums.Status newStatus, String notes, Institution newInstitution, LocalDateTime when) {
        boolean urgencyChanged = newUrgency != null && newUrgency != c.getUrgencyLevel();
        boolean statusChanged = newStatus != null && newStatus != c.getStatus();
        boolean institutionChanged = newInstitution != null
                && (c.getInstitution() == null || !Objects.equals(c.getInstitution().getId(), newInstitution.getId()));
        String cleanNotes = notes == null ? null : notes.trim();
        if (cleanNotes != null && cleanNotes.length() > 1000) {
            cleanNotes = cleanNotes.substring(0, 1000);
        }
        boolean notesChanged = cleanNotes != null && !cleanNotes.isEmpty() && !cleanNotes.equals(c.getAdminNotes());

        if (!urgencyChanged && !statusChanged && !institutionChanged && !notesChanged) {
            return false;
        }

        // Snapshot of the report before this edit
        ComplaintVersion snapshot = new ComplaintVersion();
        snapshot.setComplaintId(c.getId());
        snapshot.setTitle(c.getTitle());
        snapshot.setDescription(c.getDescription());
        snapshot.setCategory(c.getCategory());
        snapshot.setStatus(c.getStatus());
        snapshot.setModifiedByAdmin(admin.getUsername());
        snapshot.setModifiedAt(when);
        versionRepository.save(snapshot);

        String who = "Admin \"" + admin.getUsername() + "\" changed report " + c.getTrackingCode();
        String at = " on " + now(when);

        if (urgencyChanged) {
            String old = c.getUrgencyLevel() == null ? "UNSET" : c.getUrgencyLevel().name();
            c.setUrgencyLevel(newUrgency);
            c.setUrgencyOverridden(true);
            logAction(admin.getUsername(), "URGENCY_CHANGE", who + " urgency" + at + " from " + old + " to " + newUrgency, when);
        }
        if (statusChanged) {
            String old = c.getStatus() == null ? "UNSET" : c.getStatus().name();
            c.setStatus(newStatus);
            if (newStatus == Enums.Status.RESOLVED) {
                c.setResolvedAt(when);
            } else {
                c.setResolvedAt(null);
            }
            logAction(admin.getUsername(), "STATUS_CHANGE", who + " status" + at + " from " + old + " to " + newStatus, when);
        }
        if (institutionChanged) {
            String old = c.getInstitution() == null ? "UNASSIGNED" : c.getInstitution().getShortName();
            c.setInstitution(newInstitution);
            logAction(admin.getUsername(), "INSTITUTION_CHANGE", who + " institution" + at + " from " + old + " to " + newInstitution.getShortName(), when);
        }
        if (notesChanged) {
            c.setAdminNotes(cleanNotes);
            logAction(admin.getUsername(), "NOTES_CHANGE", who + " notes" + at + " (officer note updated)", when);
        }
        c.setUpdatedAt(when);
        complaintRepository.save(c);
        return true;
    }

    /**
     * Picks the institution for an edit: a brand-new one when a name is typed (reusing an existing one
     * with the same short name), otherwise the existing one chosen by id. Returns null for "no change".
     */
    private Institution resolveInstitution(AdminUser admin, Long institutionId, String newName,
                                           String newShort, LocalDateTime when) {
        String name = newName == null ? "" : newName.trim();
        if (!name.isEmpty()) {
            if (name.length() > 100) {
                name = name.substring(0, 100);
            }
            String shortName = newShort == null ? "" : newShort.trim().toUpperCase(Locale.ROOT);
            if (shortName.isEmpty()) {
                shortName = deriveShortName(name);
            }
            if (shortName.length() > 12) {
                shortName = shortName.substring(0, 12);
            }
            Optional<Institution> existing = institutionRepository.findByShortNameIgnoreCase(shortName);
            if (existing.isPresent()) {
                return existing.get();
            }
            Institution created = institutionRepository.save(new Institution(name, shortName, "", ""));
            logAction(admin.getUsername(), "ADD_INSTITUTION", "Admin \"" + admin.getUsername() + "\" added institution \""
                    + name + "\" (" + shortName + ") on " + now(when), when);
            return created;
        }
        if (institutionId != null) {
            return institutionRepository.findById(institutionId).orElse(null);
        }
        return null;
    }

    // "Ministry of Works" -> "MOW"
    private static String deriveShortName(String name) {
        StringBuilder sb = new StringBuilder();
        for (String word : name.split("\\s+")) {
            if (!word.isEmpty() && Character.isLetterOrDigit(word.charAt(0))) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return sb.length() == 0 ? "ORG" : sb.toString();
    }

    // ---------------------------------------------------------------- voice notes (admins only)

    @GetMapping("/complaints/{id}/voice")
    public ResponseEntity<Resource> voiceNote(@PathVariable Long id, HttpSession session) {
        if (currentAdmin(session) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<VoiceNote> note = voiceNoteRepository.findByComplaintId(id);
        if (note.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        VoiceNote v = note.get();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(v.getContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-store")
                .body(new ByteArrayResource(v.getData()));
    }

    // ---------------------------------------------------------------- audit history (all admins)

    @GetMapping("/audit")
    public String auditHistory(HttpSession session, Model model) {
        AdminUser admin = currentAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        model.addAttribute("auditLogs", auditLogRepository.findAllByOrderByTimestampDescIdDesc());
        model.addAttribute("currentAdmin", admin);
        return "admin/audit";
    }

    // ---------------------------------------------------------------- manage admins (super admin only)

    @GetMapping("/users")
    public String users(HttpSession session, Model model, RedirectAttributes redirect) {
        AdminUser admin = currentAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        if (!admin.isSuperAdmin()) {
            redirect.addFlashAttribute("err", "Only a super admin can manage admins.");
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("admins", adminUserRepository.findAllByOrderByIdAsc());
        model.addAttribute("currentAdmin", admin);
        return "admin/users";
    }

    @PostMapping("/users/add")
    public String addAdmin(@RequestParam String username,
                           @RequestParam String fullName,
                           @RequestParam String password,
                           @RequestParam String accessCode,
                           @RequestParam(defaultValue = "false") boolean superAdmin,
                           HttpSession session, RedirectAttributes redirect) {
        AdminUser admin = currentAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        if (!admin.isSuperAdmin()) {
            redirect.addFlashAttribute("err", "Only a super admin can add admins.");
            return "redirect:/admin/dashboard";
        }

        String user = username.trim();
        if (user.isEmpty() || fullName.isBlank() || password.length() < 6 || accessCode.isBlank()) {
            redirect.addFlashAttribute("err", "Fill every field (password needs at least 6 characters).");
        } else if (adminUserRepository.findByUsername(user).isPresent()) {
            redirect.addFlashAttribute("err", "Username \"" + user + "\" is already taken.");
        } else {
            adminUserRepository.save(new AdminUser(user, fullName.trim(), PasswordUtil.hash(password), accessCode.trim(), superAdmin));
            logAction(admin.getUsername(), "ADD_ADMIN", "Admin \"" + admin.getUsername() + "\" added admin \"" + user + "\""
                    + (superAdmin ? " (super admin)" : "") + " on " + now(LocalDateTime.now()));
            redirect.addFlashAttribute("msg", "Admin \"" + user + "\" added.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/remove")
    public String removeAdmin(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        AdminUser admin = currentAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        if (!admin.isSuperAdmin()) {
            redirect.addFlashAttribute("err", "Only a super admin can remove admins.");
            return "redirect:/admin/dashboard";
        }
        if (admin.getId().equals(id)) {
            redirect.addFlashAttribute("err", "You cannot remove your own account.");
            return "redirect:/admin/users";
        }
        AdminUser target = adminUserRepository.findById(id).orElse(null);
        if (target == null) {
            redirect.addFlashAttribute("err", "Admin not found.");
        } else {
            adminUserRepository.delete(target);
            logAction(admin.getUsername(), "REMOVE_ADMIN", "Admin \"" + admin.getUsername() + "\" removed admin \""
                    + target.getUsername() + "\" on " + now(LocalDateTime.now()));
            redirect.addFlashAttribute("msg", "Admin \"" + target.getUsername() + "\" removed.");
        }
        return "redirect:/admin/users";
    }

    // ---------------------------------------------------------------- helpers

    private static String now(LocalDateTime t) {
        return t.format(LOG_TIME);
    }

    private void logAction(String username, String actionType, String details) {
        logAction(username, actionType, details, LocalDateTime.now());
    }

    private void logAction(String username, String actionType, String details, LocalDateTime when) {
        auditLogRepository.save(new AuditLog(username, actionType, details, when));
    }
}

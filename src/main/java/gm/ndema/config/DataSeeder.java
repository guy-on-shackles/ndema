package gm.ndema.config;

import gm.ndema.model.AdminUser;
import gm.ndema.model.Complaint;
import gm.ndema.model.Enums;
import gm.ndema.model.Institution;
import gm.ndema.repository.AdminUserRepository;
import gm.ndema.repository.ComplaintRepository;
import gm.ndema.repository.InstitutionRepository;
import gm.ndema.service.UrgencyService;
import gm.ndema.util.PasswordUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final InstitutionRepository institutionRepository;
    private final ComplaintRepository complaintRepository;
    private final UrgencyService urgencyService;
    private final AdminUserRepository adminUserRepository;

    public DataSeeder(InstitutionRepository institutionRepository,
                      ComplaintRepository complaintRepository,
                      UrgencyService urgencyService,
                      AdminUserRepository adminUserRepository) {
        this.institutionRepository = institutionRepository;
        this.complaintRepository = complaintRepository;
        this.urgencyService = urgencyService;
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public void run(String... args) {
        if (institutionRepository.count() == 0) {
            Institution kmc = institutionRepository.save(new Institution("Kanifing Municipal Council", "KMC", "info@kmc.gm", "+220 4390001"));
            Institution bcc = institutionRepository.save(new Institution("Banjul City Council", "BCC", "info@bcc.gm", "+220 4227001"));
            Institution nawec = institutionRepository.save(new Institution("National Water and Electricity Company", "NAWEC", "customercare@nawec.gm", "+220 168"));
            Institution ndma = institutionRepository.save(new Institution("National Disaster Management Agency", "NDMA", "info@ndma.gm", "+220 4370002"));

            if (complaintRepository.count() == 0) {
                seedComplaints(kmc, bcc, nawec, ndma);
            }
        }

        // Seed default Super Admin & Municipal Officer accounts
        if (adminUserRepository.count() == 0) {
            adminUserRepository.save(new AdminUser("main_admin", "Super Admin Officer", PasswordUtil.hash("admin123"), "9900", true));
            adminUserRepository.save(new AdminUser("kmc_officer", "Kanifing Dispatch Officer", PasswordUtil.hash("officer123"), "1122", false));
        }
    }

    private void seedComplaints(Institution kmc, Institution bcc, Institution nawec, Institution ndma) {
        createReport("GM-2026-000001", "Severe flash flood near school",
                "Heavy rainfall caused severe flooding near Westfield primary school. Children unable to cross safely.",
                Enums.Category.FLOODING_HAZARD, "KANIFING", ndma, Enums.Channel.WEB, "2207000001", 3, Enums.Status.SUBMITTED, 10);

        createReport("GM-2026-000002", "Exposed high voltage cable",
                "Live electric wire hanging low near the market square in Serrekunda. Immediate accident hazard to shoppers.",
                Enums.Category.ELECTRICITY_STREETLIGHT,"KANIFING", nawec, Enums.Channel.WEB, "2207000002", 4, Enums.Status.ASSIGNED, 2);

        createReport("GM-2026-000003", "Main water pipe burst",
                "Burst main line wasting clean water and flooding residential road in Bakau.",
                Enums.Category.WATER_SUPPLY, "KANIFING", nawec, Enums.Channel.USSD, "2207000003", 2, Enums.Status.IN_PROGRESS, 1);

        createReport("GM-2026-000004", "Uncollected garbage heap",
                "Overflowing waste bins outside Banjul central market attracting pests and blocking pedestrian passage.",
                Enums.Category.WASTE_SANITATION, "BANJUL", bcc, Enums.Channel.WEB, "2207000004", 1, Enums.Status.RECEIVED, 5);

        createReport("GM-2026-000005", "Deep pothole causing accidents",
                "Collapsed road segment causing vehicle damage and traffic backup near Tippa Garage.",
                Enums.Category.ROADS_DRAINAGE, "KANIFING", kmc, Enums.Channel.USSD, "2207000005", 2, Enums.Status.RECEIVED, 4);

        createReport("GM-2026-000006", "Broken streetlight near hospital",
                "Streetlights out outside the hospital area, making dark road unsafe at night.",
                Enums.Category.ELECTRICITY_STREETLIGHT, "BANJUL", nawec, Enums.Channel.WEB, "2207000006", 0, Enums.Status.SUBMITTED, 0);

        createReport("GM-2026-000007", "Blocked storm drain overflow",
                "Drain filled with plastic waste overflowing onto coastal highway during heavy downpour.",
                Enums.Category.ROADS_DRAINAGE, "KANIFING", kmc, Enums.Channel.WEB, "2207000007", 5, Enums.Status.IN_PROGRESS, 6);

        createReport("GM-2026-000008", "Open sewage manhole cover",
                "Missing manhole cover on main sidewalk. Danger of child or adult falling in.",
                Enums.Category.UNSAFE_PUBLIC_SPACE, "BANJUL", bcc, Enums.Channel.USSD, "2207000008", 4, Enums.Status.ASSIGNED, 3);

        createReport("GM-2026-000009", "Low water pressure in residential quarter",
                "No running tap water for three consecutive days in Latrikunda Sabiji.",
                Enums.Category.WATER_SUPPLY, "KANIFING", nawec, Enums.Channel.WEB, "2207000009", 1, Enums.Status.RECEIVED, 2);

        createReport("GM-2026-000010", "Illegal dumping ground developing",
                "Trucks dumping construction debris in open public lot near community centre.",
                Enums.Category.WASTE_SANITATION, "KANIFING", kmc, Enums.Channel.WEB, "2207000010", 0, Enums.Status.SUBMITTED, 1);

        createReport("GM-2026-000011", "Collapsed canal wall",
                "Retaining wall collapsed into drainage channel threatening nearby housing structures.",
                Enums.Category.FLOODING_HAZARD, "BRIKAMA", ndma, Enums.Channel.USSD, "2207000011", 3, Enums.Status.RECEIVED, 7);

        createReport("GM-2026-000012", "Transformer spark hazard",
                "Electrical transformer making loud sparking noises and emitting smoke.",
                Enums.Category.ELECTRICITY_STREETLIGHT, "KANIFING", nawec, Enums.Channel.WEB, "2207000012", 5, Enums.Status.ASSIGNED, 0);

        createReport("GM-2026-000013", "Fallen tree blocking lane",
                "Large branch fell across main road lane restricting two-way traffic to single lane.",
                Enums.Category.ROADS_DRAINAGE, "BANJUL", bcc, Enums.Channel.USSD, "2207000013", 1, Enums.Status.RESOLVED, 8);

        createReport("GM-2026-000014", "Contaminated tap water report",
                "Brownish tap water observed coming from neighborhood standpipes.",
                Enums.Category.WATER_SUPPLY, "BRIKAMA", nawec, Enums.Channel.WEB, "2207000014", 2, Enums.Status.IN_PROGRESS, 4);

        createReport("GM-2026-000015", "Unclaimed hazardous medical waste",
                "Discarded medical supplies dumped in alley behind health center.",
                Enums.Category.WASTE_SANITATION, "KANIFING", kmc, Enums.Channel.WEB, "2207000015", 4, Enums.Status.ASSIGNED, 1);

        createReport("GM-2026-000016", "Unlit pedestrian bridge",
                "Bridge lighting completely inactive creating safety concerns for late commuters.",
                Enums.Category.UNSAFE_PUBLIC_SPACE, "KANIFING", kmc, Enums.Channel.USSD, "2207000016", 0, Enums.Status.SUBMITTED, 5);

        createReport("GM-2026-000017", "Standing stagnant water breeding mosquitoes",
                "Large pool of stagnant wastewater accumulated in neighborhood cul-de-sac.",
                Enums.Category.WASTE_SANITATION, "BANJUL", bcc, Enums.Channel.WEB, "2207000017", 1, Enums.Status.RESOLVED, 12);

        createReport("GM-2026-000018", "Damaged culvert bridge",
                "Cracks appearing across culvert road bridge after heavy truck movement.",
                Enums.Category.ROADS_DRAINAGE, "BRIKAMA", kmc, Enums.Channel.USSD, "2207000018", 3, Enums.Status.RECEIVED, 2);

        createReport("GM-2026-000019", "Frequent power surge in health clinic",
                "Fluctuating voltage causing medical equipment shutdowns at local dispensary.",
                Enums.Category.ELECTRICITY_STREETLIGHT,"MANSAKONKO", nawec, Enums.Channel.WEB, "2207000019", 2, Enums.Status.IN_PROGRESS, 3);

        createReport("GM-2026-000020", "Public park litter accumulation",
                "Bins full and litter spread around children play park area.",
                Enums.Category.WASTE_SANITATION,"KANIFING", kmc, Enums.Channel.WEB, "2207000020", 0, Enums.Status.RESOLVED, 15);
    }

    private void createReport(String code, String title, String desc, Enums.Category category,
                              String lga, Institution institution, Enums.Channel channel,
                              String phone, int confirmations, Enums.Status status, int daysOld) {
        Complaint c = new Complaint();
        c.setTrackingCode(code);
        c.setTitle(title);
        c.setDescription(desc);
        c.setCategory(category);
        c.setLga(lga);
        c.setInstitution(institution);
        c.setChannel(channel);
        c.setReporterPhone(phone);
        c.setConfirmations(confirmations);
        c.setStatus(status);
        c.setCreatedAt(LocalDateTime.now().minusDays(daysOld));

        int score = urgencyService.calculateScore(c);
        c.setUrgencyScore(score);
        c.setUrgencyLevel(urgencyService.calculateLevel(score));

        if (status == Enums.Status.RESOLVED) {
            c.setResolvedAt(LocalDateTime.now().minusDays(Math.max(0, daysOld - 1)));
        }

        complaintRepository.save(c);
    }
}
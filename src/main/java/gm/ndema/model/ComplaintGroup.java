package gm.ndema.model;

import java.util.ArrayList;
import java.util.List;

/** Reports that share the same LGA and category, shown together on the admin dashboard. */
public class ComplaintGroup {

    private final String lga;
    private final Enums.Category category;
    private final List<Complaint> complaints = new ArrayList<>();

    public ComplaintGroup(String lga, Enums.Category category) {
        this.lga = lga;
        this.category = category;
    }

    public String getLga() { return lga; }
    public Enums.Category getCategory() { return category; }
    public List<Complaint> getComplaints() { return complaints; }
}

package gm.ndema.service;

import gm.ndema.model.Complaint;
import gm.ndema.model.Enums;
import gm.ndema.model.Enums.Urgency;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UrgencyService {

    // High-risk safety keywords (whole-word matches, so "fired" no longer matches "fire")
    private static final List<Pattern> CRITICAL_KEYWORDS = List.of(
            "guns?", "shooting", "weapons?", "firearms?", "explosions?", "fire", "live wire",
            "electrocution", "collapsed?", "child(ren)?", "hospital", "flood(s|ed|ing)?"
    ).stream().map(k -> Pattern.compile("\\b(" + k + ")\\b")).toList();

    public int calculateScore(Complaint complaint) {
        int score = 0;
        String fullText = ((complaint.getTitle() != null ? complaint.getTitle() : "") + " " +
                (complaint.getDescription() != null ? complaint.getDescription() : "")).toLowerCase();

        // 1. Keyword analysis (+20 points per keyword match)
        for (Pattern keyword : CRITICAL_KEYWORDS) {
            if (keyword.matcher(fullText).find()) {
                score += 20;
            }
        }

        // 2. Base category risk bonus
        if (complaint.getCategory() == Enums.Category.UNSAFE_PUBLIC_SPACE ||
                complaint.getCategory() == Enums.Category.PUBLIC_SAFETY ||
                complaint.getCategory() == Enums.Category.FLOODING_HAZARD ||
                complaint.getCategory() == Enums.Category.ELECTRICITY_STREETLIGHT) {
            score += 15;
        }

        // 3. Community confirmations (+5 points each)
        int confirmations = complaint.getConfirmations() != null ? complaint.getConfirmations() : 0;
        score += confirmations * 5;

        return score;
    }

    public Urgency calculateLevel(int score) {
        if (score >= 35) {
            return Urgency.CRITICAL;
        } else if (score >= 20) {
            return Urgency.HIGH;
        } else if (score >= 10) {
            return Urgency.MEDIUM;
        } else {
            return Urgency.LOW;
        }
    }
}

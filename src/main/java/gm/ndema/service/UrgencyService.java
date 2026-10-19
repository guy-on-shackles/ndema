package gm.ndema.service;

import gm.ndema.model.Complaint;
import gm.ndema.model.Enums;
import gm.ndema.model.Enums.Urgency;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UrgencyService {

    // High-risk safety keywords that trigger instant score boosts
    private static final List<String> CRITICAL_KEYWORDS = List.of(
            "gun", "shooting", "weapon", "firearm", "explosion", "fire",
            "live wire", "electrocution", "collapsed", "child", "hospital", "flood"
    );

    public int calculateScore(Complaint complaint) {
        int score = 0;
        String fullText = ((complaint.getTitle() != null ? complaint.getTitle() : "") + " " +
                (complaint.getDescription() != null ? complaint.getDescription() : "")).toLowerCase();

        // 1. Keyword analysis (+20 points per keyword match)
        for (String keyword : CRITICAL_KEYWORDS) {
            if (fullText.contains(keyword)) {
                score += 20;
            }
        }

        // 2. Base category risk bonus
        if (complaint.getCategory() == Enums.Category.UNSAFE_PUBLIC_SPACE ||
                complaint.getCategory() == Enums.Category.FLOODING_HAZARD ||
                complaint.getCategory() == Enums.Category.ELECTRICITY_STREETLIGHT) {
            score += 15;
        }

        // 3. Community upvotes / confirmations (+5 points each)
        score += (complaint.getConfirmations() * 5);

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
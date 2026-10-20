package gm.ndema.model;

public class Enums {
    public enum Category {
        ROADS_DRAINAGE, WASTE_SANITATION, WATER_SUPPLY,
        ELECTRICITY_STREETLIGHT, UNSAFE_PUBLIC_SPACE, FLOODING_HAZARD,
        PUBLIC_SAFETY, HEALTHCARE, EDUCATION, AGRICULTURE, OTHER
    }

    public enum Status {
        SUBMITTED, RECEIVED, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED, DUPLICATE
    }

    public enum Lga {
        BANJUL, KANIFING, BRIKAMA, MANSAKONKO, KEREWAN, KUNTAUR, JANJANBUREH, BASSE
    }

    public enum Urgency {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Channel {
        WEB, USSD, SMS
    }
}

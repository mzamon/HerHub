package za.co.herhub;

/** Catalogue of the 17 HerHub feature areas. No emojis: every feature is shown with its own drawable icon. */
public final class Features {
    private Features() { }

    /** {key, title, description}. Every feature has its own unique icon, see icon(). */
    public static final String[][] ALL = {
            {"sos", "Personal Safety & Emergency", "SOS, siren, emergency calls and alerts"},
            {"loc", "Location & Safety Tracking", "Share where you are with people you trust"},
            {"map", "Community Safety Map", "See and report community safety notices"},
            {"period", "Period & Menstrual Tracking", "Cycle predictions, symptoms and daily log"},
            {"preg", "Pregnancy & Fertility", "Due date, weekly progress and fertile window"},
            {"mind", "Mental Health & Emotional Wellness", "Mood check-ins, breathing and grounding"},
            {"health", "Health & Wellness", "Water, health diary, medical ID, appointments"},
            {"security", "Digital & Personal Security", "Account protection and privacy checklist"},
            {"circle", "Trusted Circle", "Your trusted contacts, one tap away"},
            {"journey", "Safe Journey / Travel Mode", "Trip timer with arrival check-in"},
            {"info", "Women\u2019s Information Hub", "Health, rights and personal-safety guides"},
            {"support", "Support & Community", "Support lines and a moderated community"},
            {"abuse", "Abuse & Harassment Support", "Safety plan, private log and quick exit"},
            {"finance", "Financial Safety", "Scam checker, savings goal and fraud tips"},
            {"notify", "Smart Notifications", "Choose which reminders you want"},
            {"directory", "Emergency Resource Directory", "Searchable hotlines, tap to call"},
            {"ai", "AI Safety & Wellness Assistant", "Ask for guidance, get clear next steps"}
    };

    /** The unique icon for a feature key. */
    public static int icon(String key) {
        switch (key) {
            case "sos": return R.drawable.ic_sos_alert;
            case "loc": return R.drawable.ic_location_live;
            case "map": return R.drawable.ic_safety_map;
            case "period": return R.drawable.ic_period_calendar;
            case "preg": return R.drawable.ic_pregnancy;
            case "mind": return R.drawable.ic_meditation;
            case "health": return R.drawable.ic_health_record;
            case "security": return R.drawable.ic_privacy_lock;
            case "circle": return R.drawable.ic_trusted_circle;
            case "journey": return R.drawable.ic_safe_journey;
            case "info": return R.drawable.ic_womens_hub;
            case "support": return R.drawable.ic_forum;
            case "abuse": return R.drawable.ic_abuse_support;
            case "finance": return R.drawable.ic_financial_safety;
            case "notify": return R.drawable.ic_notification_bell;
            case "directory": return R.drawable.ic_emergency_contacts;
            case "ai": return R.drawable.ic_ai_assistant;
            default: return R.drawable.ic_help_support;
        }
    }

    public static String title(String key) {
        for (String[] f : ALL) if (f[0].equals(key)) return f[1];
        return "tool";
    }

    public static void go(MainActivity a, String key) {
        switch (key) {
            case "map": a.goTab(R.id.nav_map); return;
            case "sos": a.open(new SosPage()); return;
            case "loc": a.open(new LocationPage()); return;
            case "period": a.open(new PeriodPage()); return;
            case "preg": a.open(new PregnancyPage()); return;
            case "mind": a.open(new MindPage()); return;
            case "health": a.open(new HealthPage()); return;
            case "security": a.open(new SecurityPage()); return;
            case "circle": a.open(new CirclePage()); return;
            case "journey": a.open(new JourneyPage()); return;
            case "info": a.open(new InfoPage()); return;
            case "support": a.open(new SupportPage()); return;
            case "abuse": a.open(new AbusePage()); return;
            case "finance": a.open(new FinancePage()); return;
            case "notify": a.open(new NotifyPage()); return;
            case "directory": a.open(new DirectoryPage()); return;
            case "ai": a.open(new AssistantPage()); return;
            default:
        }
    }
}

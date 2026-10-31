package za.co.herhub;

public class WellnessPage extends Page {
    @Override protected boolean top() { return true; }
    @Override protected String title() { return "Health"; }

    @Override protected void build() {
        p("Look after your body and your mind. Everything here is private to you.");
        item(R.drawable.ic_period_calendar, "Period & Menstrual Tracking", PeriodPage.summary(ctx), () -> open(new PeriodPage()));
        item(R.drawable.ic_pregnancy, "Pregnancy & Fertility", "Due date, weekly progress, fertile window", () -> open(new PregnancyPage()));
        item(R.drawable.ic_meditation, "Mental Health & Emotional Wellness", "Mood check-ins, breathing, grounding", () -> open(new MindPage()));
        item(R.drawable.ic_health_record, "Health & Wellness", "Water, diary, medical ID, appointments", () -> open(new HealthPage()));
        item(R.drawable.ic_notification_bell, "Smart Notifications", "Choose your reminders", () -> open(new NotifyPage()));
        item(R.drawable.ic_womens_hub, "Women\u2019s Information Hub", "Health, rights and safety guides", () -> open(new InfoPage()));
        item(R.drawable.ic_forum, "Support & Community", "Support lines and community", () -> open(new SupportPage()));
        item(R.drawable.ic_ai_assistant, "AI Safety & Wellness Assistant", "Ask for guidance and next steps", () -> open(new AssistantPage()));
        p("HerHub gives general information, not medical advice. See a health professional for diagnosis or treatment.");
    }
}

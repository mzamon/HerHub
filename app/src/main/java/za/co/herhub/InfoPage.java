package za.co.herhub;

public class InfoPage extends Page {
    // {icon key, title, summary, body}
    static final String[][] A = {
            {"rights", "Know your rights", "Protection orders, harassment and reporting",
                    "• Domestic Violence Act: you can apply for a protection order at your nearest magistrate's court. It is free, and the police must help you.\n\n• Protection from Harassment Act: you can apply for a protection order against stalking or harassment, including online.\n\n• You can report a crime at any police station, and you may ask for a female officer and a private room.\n\n• You have the right to be treated with dignity and to have a support person with you.\n\nFor legal help in South Africa, Legal Aid is free: 0800 110 110. This is general information, not legal advice."},
            {"assault", "After a sexual assault", "Steps that protect your health and options",
                    "What happened is not your fault.\n\n1. Get to a safe place and call someone you trust.\n2. Go to a hospital, clinic or Thuthuzela Care Centre as soon as you can.\n3. HIV post-exposure prophylaxis (PEP) works best when started within 72 hours.\n4. Emergency contraception can be taken for up to 5 days after (the sooner the better).\n5. If you can, avoid washing or changing clothes before a medical exam, as this may preserve evidence. If you already have, you can still get care and report.\n6. You choose if and when to report. Counselling is available.\n\nGBV Command Centre (free, 24/7): 0800 428 428."},
            {"menstrual", "Menstrual health basics", "What's typical and when to see a doctor",
                    "Cycles are often 21 to 35 days, and periods usually last 2 to 7 days.\n\nChange pads or tampons every 4 to 8 hours. Never leave a tampon in overnight longer than 8 hours.\n\nSee a healthcare professional if:\n• You soak a pad or tampon every hour\n• Periods last longer than 7 days\n• Pain stops you from daily activities\n• Your periods stop and you are not pregnant\n• You have fever and feel very unwell while using tampons"},
            {"pregnancy", "Early pregnancy care", "Antenatal basics",
                    "• Visit a clinic as early as possible for antenatal care and an HIV test.\n• Folic acid is usually recommended before conception and in early pregnancy. Ask your clinic.\n• Avoid alcohol and smoking.\n• Ask a professional before taking any medicine.\n\nGo to a clinic urgently for bleeding, severe pain, severe headache, blurred vision, or if your baby moves much less."},
            {"screening", "Cervical & breast health", "Screening that saves lives",
                    "• Cervical screening (Pap smear): in South Africa, public clinics offer free smears from age 30. Ask at your clinic.\n• The HPV vaccine protects against the virus that causes most cervical cancers, and is offered to girls in schools.\n• Get to know how your breasts normally look and feel. See a clinic if you notice a lump, dimpling, nipple changes or discharge.\n• Regular screening finds problems early, when they're easiest to treat."},
            {"contraception", "Contraception & STIs", "Protect your health",
                    "• Condoms protect against both pregnancy and most STIs.\n• Public clinics offer free contraception, STI testing and HIV testing.\n• PrEP can lower HIV risk. Ask your clinic.\n• Emergency contraception works best taken as early as possible after unprotected sex.\n\nA nurse can help you choose a method that suits you."},
            {"mental", "Mental health & postnatal care", "Recognising when to get support",
                    "Persistent sadness, loss of interest, exhaustion, sleep or appetite changes, or feeling hopeless for more than two weeks can be signs of depression. After having a baby, this is common and treatable.\n\nTalk to a clinic, doctor or counsellor. SADAG: 0800 21 22 23. If you're thinking of ending your life: 0800 567 567 or 112."},
            {"dating", "Safer dating & meeting online", "Before you meet someone",
                    "• Video call before meeting.\n• Meet in a public place and arrange your own transport.\n• Tell a friend where you are and who you're with. Use Safe Journey in HerHub.\n• Don't share your home address early on.\n• Watch your drink.\n• Trust your gut. You can leave at any time."},
            {"stalking", "Stalking & digital safety", "Protect your devices",
                    "Warning signs your phone may be monitored: battery drains fast, unknown apps, someone knows things you haven't told them.\n\n• Change passwords from a safe device.\n• Review app permissions and linked devices on WhatsApp and social media.\n• Turn off location sharing you don't need.\n• Keep screenshots of threats and harassment with dates.\n• Report to the police and platform."},
            {"workplace", "Workplace harassment", "What you can do",
                    "• Write down what happened: dates, times, witnesses.\n• Keep messages and emails.\n• Report to your employer or HR in writing. Employers must act on harassment.\n• You can approach the CCMA or a labour lawyer, or get legal advice from Legal Aid.\n• Tell someone you trust. You don't have to carry this alone."},
            {"wellbeing", "Everyday wellbeing", "Small habits that help",
                    "• Aim for 7 to 9 hours of sleep.\n• Move your body most days, even a walk.\n• Eat iron-rich foods (legumes, leafy greens, meat) and drink water.\n• Make time for friends and things you enjoy.\n• Get regular check-ups."}
    };

    /** First column is an icon key, not an emoji. */
    static int icon(String k) {
        switch (k) {
            case "rights": return R.drawable.ic_legal_aid;
            case "assault": return R.drawable.ic_crisis_support;
            case "menstrual": return R.drawable.ic_cycle_insights;
            case "pregnancy": return R.drawable.ic_prenatal;
            case "screening": return R.drawable.ic_health_record;
            case "contraception": return R.drawable.ic_pharmacy;
            case "mental": return R.drawable.ic_therapy;
            case "dating": return R.drawable.ic_anon_user;
            case "stalking": return R.drawable.ic_location_history;
            case "workplace": return R.drawable.ic_report_user;
            default: return R.drawable.ic_breathing;
        }
    }

    @Override protected String title() { return "Women’s Information Hub"; }

    @Override protected void build() {
        p("Practical guides on health, rights and safety. General information, not a replacement for professional advice.");
        for (final String[] a : A) item(icon(a[0]), a[1], a[2], () -> U.info(ctx, a[1], a[3]));
    }
}

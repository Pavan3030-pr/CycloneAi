package com.enterprise.cyclone.application;

import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.RiskLevel;
import com.enterprise.cyclone.domain.model.SaffirSimpsonCategory;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Every fixed phrase an advisory can contain, per language.
 *
 * <p>Wording lives in one place because the same sentences appear in two publication formats: the
 * plain-text advisory and the CAP 1.2 alert. Keeping them side by side means a change to the Hindi
 * wording cannot silently apply to one format and not the other.
 *
 * <p>The English strings are frozen: they are the established output of the deterministic generator,
 * and the demonstration's expected text is asserted against them. The Hindi and Telugu strings are a
 * first pass written to be unambiguous for a district control room — meteorological terms are kept
 * conventional and technical identifiers (storm ids, asset ids, units such as kt, mb, nm) are left
 * untranslated on purpose, because those are what an operator reads off the instruments and types
 * into other systems.
 *
 * <p>Translations should still be reviewed by a district officer before public distribution; the
 * README says so.
 */
public final class AdvisoryVocabulary {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);

    private AdvisoryVocabulary() {
    }

    /**
     * All templates for one language.
     *
     * @param riskNames localized risk labels; empty means "use the enum name", which is what English
     *        does so that the existing output is unchanged
     */
    private record Phrases(
            String title,
            String validAt,
            String centre,
            String intensity,
            String assetsAssessed,
            String actionRequired,
            String countLine,
            String nearestAsset,
            String priorityAssets,
            String moreAssets,
            String basis,
            String headline,
            String instruction,
            String event,
            String areaDescription,
            String contact,
            Map<RiskLevel, String> riskNames,
            Map<SaffirSimpsonCategory, String> categoryNames) {
    }

    private static final Phrases ENGLISH = new Phrases(
            "CYCLONE IMPACT ADVISORY - %s",
            "Valid at %s UTC",
            "Storm centre: latitude %.4f, longitude %.4f (GeoJSON position %s)",
            "Intensity: %d kt (%s), central pressure %d mb",
            "Assets assessed: %d",
            "Pre-landfall action required for: %d asset(s)",
            "  %-8s %d asset(s)",
            "Nearest asset: %.0f nm from the centre; peak modelled wind at an asset: %d kt",
            "Priority assets:",
            "  ... and %d further asset(s) at lower exposure",
            "Basis: linear interpolation between published fixes with an exponential wind-field decay "
                    + "(75 nm e-folding). Terrain, gust factor, quadrant asymmetry and forecast positional "
                    + "uncertainty are not modelled. Cross-check with official NHC/JTWC products before "
                    + "operational use.",
            "Cyclone %s: %d assets require pre-landfall action (highest assessed level: %s)",
            "Move people to the nearest designated shelter, secure power infrastructure, keep arterial "
                    + "roads clear for emergency vehicles, and confirm that medical shelters have power "
                    + "and water.",
            "Tropical cyclone",
            "Coastal belt within %.0f km of %.4f, %.4f, including the assessed infrastructure corridor",
            "CycloneAI screening platform",
            Map.of(),
            Map.of());

    private static final Phrases HINDI = new Phrases(
            "चक्रवात प्रभाव परामर्श - %s",
            "%s UTC पर मान्य",
            "तूफ़ान का केंद्र: अक्षांश %.4f, देशांतर %.4f (GeoJSON स्थिति %s)",
            "तीव्रता: %d kt (%s), केंद्रीय दबाव %d mb",
            "मूल्यांकित परिसंपत्तियाँ: %d",
            "तट पर पहुँचने से पहले कार्रवाई आवश्यक: %d परिसंपत्ति",
            "  %-8s %d परिसंपत्ति",
            "निकटतम परिसंपत्ति: केंद्र से %.0f nm; परिसंपत्ति पर अनुमानित अधिकतम हवा: %d kt",
            "प्राथमिकता वाली परिसंपत्तियाँ:",
            "  ... और %d अन्य परिसंपत्तियाँ कम जोखिम में",
            "आधार: प्रकाशित फिक्सों के बीच रैखिक अंतर्वेशन तथा हवा की तीव्रता में घातांकीय कमी (75 nm "
                    + "e-folding)। भू-आकृति, झोंक कारक, चतुर्थांश असममिति और पूर्वानुमान की स्थितीय "
                    + "अनिश्चितता शामिल नहीं है। परिचालन उपयोग से पहले आधिकारिक NHC/JTWC/IMD चेतावनियों "
                    + "से मिलान करें।",
            "चक्रवात %s: %d परिसंपत्तियों के लिए तट पर पहुँचने से पहले कार्रवाई आवश्यक (उच्चतम स्तर: %s)",
            "लोगों को निकटतम निर्धारित आश्रय स्थल पर ले जाएँ, विद्युत अधोसंरचना सुरक्षित करें, आपातकालीन "
                    + "वाहनों के लिए राजमार्ग खाली रखें, और चिकित्सा आश्रय स्थलों में बिजली तथा पानी "
                    + "सुनिश्चित करें।",
            "उष्णकटिबंधीय चक्रवात",
            "%.0f km के भीतर तटीय पट्टी (%.4f, %.4f), मूल्यांकित अधोसंरचना गलियारे सहित",
            "साइक्लोनएआई स्क्रीनिंग प्लेटफ़ॉर्म",
            Map.of(
                    RiskLevel.CRITICAL, "अति गंभीर",
                    RiskLevel.HIGH, "उच्च",
                    RiskLevel.MEDIUM, "मध्यम",
                    RiskLevel.LOW, "कम"),
            Map.of(
                    SaffirSimpsonCategory.TD, "अवदाब",
                    SaffirSimpsonCategory.TS, "उष्णकटिबंधीय तूफ़ान",
                    SaffirSimpsonCategory.CAT1, "श्रेणी 1 चक्रवात",
                    SaffirSimpsonCategory.CAT2, "श्रेणी 2 चक्रवात",
                    SaffirSimpsonCategory.CAT3, "श्रेणी 3 चक्रवात",
                    SaffirSimpsonCategory.CAT4, "श्रेणी 4 चक्रवात",
                    SaffirSimpsonCategory.CAT5, "श्रेणी 5 चक्रवात"));

    private static final Phrases TELUGU = new Phrases(
            "తుఫాను ప్రభావ హెచ్చరిక - %s",
            "%s UTC నాటికి చెల్లుబాటు",
            "తుఫాను కేంద్రం: అక్షాంశం %.4f, రేఖాంశం %.4f (GeoJSON స్థానం %s)",
            "తీవ్రత: %d kt (%s), కేంద్ర పీడనం %d mb",
            "పరిశీలించిన ఆస్తులు: %d",
            "తీరం తాకే ముందు చర్య అవసరం: %d ఆస్తి",
            "  %-8s %d ఆస్తి",
            "సమీప ఆస్తి: కేంద్రం నుండి %.0f nm; ఆస్తి వద్ద అంచనా వేసిన గరిష్ఠ గాలి: %d kt",
            "ప్రాధాన్యత కలిగిన ఆస్తులు:",
            "  ... మరియు %d ఇతర ఆస్తులు తక్కువ ప్రమాదంలో",
            "ఆధారం: ప్రచురిత ఫిక్స్ల మధ్య రేఖీయ అంతర్వేశనం మరియు గాలి వేగంలో ఘాతాంక క్షీణత (75 nm "
                    + "e-folding)। భూభాగం, గస్ట్ ఫ్యాక్టర్, పాద అసమానత మరియు స్థాన అనిశ్చితి పరిగణించబడలేదు. "
                    + "కార్యాచరణ ఉపయోగానికి ముందు అధికారిక NHC/JTWC/IMD హెచ్చరికలతో సరిపోల్చండి.",
            "తుఫాను %s: %d ఆస్తులకు తీరం తాకే ముందు చర్య అవసరం (అత్యధిక స్థాయి: %s)",
            "ప్రజలను సమీప నిర్దేశిత ఆశ్రయ కేంద్రానికి తరలించండి, విద్యుత్ మౌలిక సదుపాయాలను "
                    + "సురక్షితం చేయండి, అత్యవసర వాహనాల కోసం రహదారులను ఖాళీగా ఉంచండి, వైద్య "
                    + "ఆశ్రయ కేంద్రాలలో విద్యుత్ మరియు నీరు నిర్ధారించండి.",
            "ఉష్ణమండల తుఫాను",
            "%.0f km లోపల తీర ప్రాంతం (%.4f, %.4f), పరిశీలించిన మౌలిక సదుపాయాలతో సహా",
            "సైక్లోన్AI స్క్రీనింగ్ వేదిక",
            Map.of(
                    RiskLevel.CRITICAL, "అత్యవసర",
                    RiskLevel.HIGH, "అధిక",
                    RiskLevel.MEDIUM, "మధ్యస్థ",
                    RiskLevel.LOW, "తక్కువ"),
            Map.of(
                    SaffirSimpsonCategory.TD, "అల్పపీడనం",
                    SaffirSimpsonCategory.TS, "ఉష్ణమండల తుఫాను",
                    SaffirSimpsonCategory.CAT1, "వర్గం 1 తుఫాను",
                    SaffirSimpsonCategory.CAT2, "వర్గం 2 తుఫాను",
                    SaffirSimpsonCategory.CAT3, "వర్గం 3 తుఫాను",
                    SaffirSimpsonCategory.CAT4, "వర్గం 4 తుఫాను",
                    SaffirSimpsonCategory.CAT5, "వర్గం 5 తుఫాను"));

    private static Phrases phrases(AdvisoryLanguage language) {
        return switch (language) {
            case EN -> ENGLISH;
            case HI -> HINDI;
            case TE -> TELUGU;
        };
    }

    /** Risk label, localized; English keeps the enum name so existing output is unchanged. */
    public static String riskName(RiskLevel level, AdvisoryLanguage language) {
        return phrases(language).riskNames().getOrDefault(level, level.name());
    }

    /** Intensity class name, localized. */
    public static String categoryName(SaffirSimpsonCategory category, AdvisoryLanguage language) {
        return phrases(language).categoryNames().getOrDefault(category, category.displayName());
    }

    public static String title(String stormId, AdvisoryLanguage language) {
        return String.format(Locale.ROOT, phrases(language).title(), stormId);
    }

    public static String validAt(Instant instant, AdvisoryLanguage language) {
        return String.format(Locale.ROOT, phrases(language).validAt(), TIMESTAMP.format(instant));
    }

    public static String centre(CycloneTrackPoint storm, AdvisoryLanguage language) {
        return String.format(
                Locale.ROOT,
                phrases(language).centre(),
                storm.latitude(),
                storm.longitude(),
                storm.coordinateString());
    }

    public static String intensity(
            CycloneTrackPoint storm, SaffirSimpsonCategory category, AdvisoryLanguage language) {
        return String.format(
                Locale.ROOT,
                phrases(language).intensity(),
                storm.windSpeedKnots(),
                categoryName(category, language),
                storm.centralPressureMb());
    }

    public static String assetsAssessed(int total, AdvisoryLanguage language) {
        return String.format(Locale.ROOT, phrases(language).assetsAssessed(), total);
    }

    public static String actionRequired(int actionable, AdvisoryLanguage language) {
        return String.format(Locale.ROOT, phrases(language).actionRequired(), actionable);
    }

    public static String countLine(RiskLevel level, int count, AdvisoryLanguage language) {
        return String.format(Locale.ROOT, phrases(language).countLine(), riskName(level, language), count);
    }

    public static String nearestAsset(double nearestNm, int peakKnots, AdvisoryLanguage language) {
        return String.format(Locale.ROOT, phrases(language).nearestAsset(), nearestNm, peakKnots);
    }

    public static String priorityAssets(AdvisoryLanguage language) {
        return phrases(language).priorityAssets();
    }

    public static String moreAssets(int remaining, AdvisoryLanguage language) {
        return String.format(Locale.ROOT, phrases(language).moreAssets(), remaining);
    }

    /** The model's own limitations, stated in the advisory itself rather than only in documentation. */
    public static String basis(AdvisoryLanguage language) {
        return phrases(language).basis();
    }

    public static String headline(
            String stormId, int actionable, RiskLevel highest, AdvisoryLanguage language) {
        return String.format(
                Locale.ROOT,
                phrases(language).headline(),
                stormId,
                actionable,
                riskName(highest, language));
    }

    public static String instruction(AdvisoryLanguage language) {
        return phrases(language).instruction();
    }

    public static String event(AdvisoryLanguage language) {
        return phrases(language).event();
    }

    /**
     * The area description, always formatted as radius then position.
     *
     * <p>Argument order is fixed across languages on purpose: an earlier version let each translation
     * choose its own order, which meant a translation could silently swap a latitude for a radius.
     */
    public static String areaDescription(double radiusKm, double latitude, double longitude, AdvisoryLanguage language) {
        return String.format(Locale.ROOT, phrases(language).areaDescription(), radiusKm, latitude, longitude);
    }

    public static String contact(AdvisoryLanguage language) {
        return phrases(language).contact();
    }
}

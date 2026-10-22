package co.istad.visal.e_learning.expection;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;

/**
 * Error messages in the client's language. The frontend sends Accept-Language
 * ("en" or "km"); English stays as written, Khmer is looked up here.
 * A message without a translation falls back to English rather than failing.
 */
public final class MessageTranslator {

    private MessageTranslator() {
    }

    public static boolean wantsKhmer(HttpServletRequest req) {
        String lang = req.getHeader("Accept-Language");
        return lang != null && lang.trim().toLowerCase().startsWith("km");
    }

    public static String translate(String message, HttpServletRequest req) {
        if (message == null || !wantsKhmer(req)) return message;
        String exact = KM.get(message);
        if (exact != null) return exact;
        for (Rule rule : PATTERNS) {
            Matcher m = rule.pattern().matcher(message);
            if (m.matches()) return rule.render().apply(m);
        }
        return message;
    }

    private record Rule(Pattern pattern, Function<Matcher, String> render) {
        static Rule of(String regex, Function<Matcher, String> render) {
            return new Rule(Pattern.compile(regex), render);
        }
    }

    private static final Map<String, String> NOUN = Map.ofEntries(
            entry("course", "វគ្គសិក្សា"), entry("lesson", "មេរៀន"), entry("section", "ផ្នែក"),
            entry("category", "ប្រភេទ"), entry("user", "អ្នកប្រើ"), entry("quiz", "កម្រងសំណួរ"),
            entry("question", "សំណួរ"), entry("review", "ការវាយតម្លៃ"), entry("note", "កំណត់ចំណាំ"),
            entry("file", "ឯកសារ"), entry("certificate", "វិញ្ញាបនបត្រ"));

    /** Messages with an id, name or number in them. */
    private static final List<Rule> PATTERNS = List.of(
            Rule.of("No (course|lesson|section|category|user|quiz|question|review|note|file|certificate) with (id|slug|serial) .+",
                    m -> "រកមិនឃើញ" + NOUN.getOrDefault(m.group(1), m.group(1)) + "ទេ"),
            Rule.of("No Keycloak user with .+", m -> "រកមិនឃើញគណនីអ្នកប្រើទេ"),
            Rule.of("A category named (.+) already exists", m -> "មានប្រភេទឈ្មោះ " + m.group(1) + " រួចហើយ"),
            Rule.of("Course .+ is not visible to you", m -> "អ្នកមិនអាចមើលវគ្គសិក្សានេះបានទេ"),
            Rule.of("Lesson .+ is not available to you", m -> "មេរៀននេះមិនទាន់បើកសម្រាប់អ្នកទេ"),
            Rule.of("Lesson .+ is not in this section", m -> "មេរៀននេះមិនស្ថិតក្នុងផ្នែកនេះទេ"),
            Rule.of("Section .+ is not in this course", m -> "ផ្នែកនេះមិនស្ថិតក្នុងវគ្គសិក្សានេះទេ"),
            Rule.of("Student data for course .+ is not yours to read", m -> "អ្នកមិនមានសិទ្ធិមើលទិន្នន័យសិស្សនៃវគ្គសិក្សានេះទេ"),
            Rule.of("You are not enrolled in course .+", m -> "អ្នកមិនទាន់បានចុះឈ្មោះចូលរៀនវគ្គសិក្សានេះទេ"),
            Rule.of("You cannot edit course .+", m -> "អ្នកមិនអាចកែវគ្គសិក្សានេះបានទេ"),
            Rule.of("No open reports for review .+", m -> "មិនមានរបាយការណ៍បើកចំហសម្រាប់ការវាយតម្លៃនេះទេ"),
            Rule.of("Complete every lesson and quiz to earn the certificate \\((\\d+) of (\\d+) done\\)",
                    m -> "សូមបញ្ចប់មេរៀន និងកម្រងសំណួរទាំងអស់ ដើម្បីទទួលវិញ្ញាបនបត្រ (បានបញ្ចប់ " + m.group(1) + " ក្នុងចំណោម " + m.group(2) + ")"),
            Rule.of("You have used all (\\d+) attempts", m -> "អ្នកបានប្រើការប៉ុនប៉ងទាំង " + m.group(1) + " ដងរួចហើយ"),
            Rule.of("size must be between (\\d+) and (\\d+)", m -> "ទំហំត្រូវនៅចន្លោះ " + m.group(1) + " និង " + m.group(2)),
            Rule.of("must be less than or equal to (\\d+)", m -> "ត្រូវតិចជាង ឬស្មើ " + m.group(1)),
            Rule.of("must be greater than or equal to (\\d+)", m -> "ត្រូវធំជាង ឬស្មើ " + m.group(1))
    );

    private static final Map<String, String> KM = Map.ofEntries(
            // general
            entry("Validation failed", "ព័ត៌មានដែលបានបញ្ចូលមិនត្រឹមត្រូវ"),
            entry("You do not have the required role", "អ្នកមិនមានសិទ្ធិធ្វើសកម្មភាពនេះទេ"),
            entry("No authenticated user on this request", "សូមចូលគណនីជាមុនសិន"),
            entry("No file received", "មិនបានទទួលឯកសារទេ"),
            entry("must not be blank", "មិនអាចទទេបានទេ"),
            entry("must not be null", "ត្រូវតែបំពេញ"),
            entry("must not be empty", "មិនអាចទទេបានទេ"),
            entry("must be a well-formed email address", "អ៊ីមែលមិនត្រឹមត្រូវ"),
            // courses, sections, lessons
            entry("A rejection needs a reason the instructor can act on", "ការបដិសេធត្រូវការហេតុផលដែលគ្រូបង្រៀនអាចកែតម្រូវបាន"),
            entry("Add at least one lesson before submitting for review", "សូមបន្ថែមមេរៀនយ៉ាងហោចណាស់មួយ មុនពេលដាក់ស្នើឲ្យពិនិត្យ"),
            entry("Remove the lessons in this section before deleting it", "សូមលុបមេរៀនក្នុងផ្នែកនេះជាមុន មុនពេលលុបផ្នែក"),
            entry("Send every lesson id, in the order you want them saved", "សូមផ្ញើលេខសម្គាល់មេរៀនទាំងអស់ តាមលំដាប់ដែលចង់រក្សាទុក"),
            entry("Send every section id, in the order you want them saved", "សូមផ្ញើលេខសម្គាល់ផ្នែកទាំងអស់ តាមលំដាប់ដែលចង់រក្សាទុក"),
            entry("Send the full ordered list of ids", "សូមផ្ញើបញ្ជីលេខសម្គាល់ពេញលេញតាមលំដាប់"),
            entry("Lesson title is required", "ត្រូវការចំណងជើងមេរៀន"),
            entry("Section title is required", "ត្រូវការចំណងជើងផ្នែក"),
            entry("Title is required", "ត្រូវការចំណងជើង"),
            entry("Pick a category", "សូមជ្រើសរើសប្រភេទ"),
            entry("Description must be 300 characters or fewer", "ការពិពណ៌នាត្រូវមិនលើសពី ៣០០ តួអក្សរ"),
            entry("Category name is required", "ត្រូវការឈ្មោះប្រភេទ"),
            entry("Category name must be 80 characters or fewer", "ឈ្មោះប្រភេទត្រូវមិនលើសពី ៨០ តួអក្សរ"),
            entry("Target status is required", "ត្រូវការស្ថានភាពគោលដៅ"),
            entry("A lesson can have at most 300 blocks", "មេរៀនមួយអាចមានប្លុកច្រើនបំផុត ៣០០"),
            entry("Unknown block type", "ប្រភេទប្លុកមិនស្គាល់"),
            entry("Callout must be tip, note or warning", "ប្រអប់ត្រូវជា គន្លឹះ ចំណាំ ឬការព្រមាន"),
            entry("A list can have at most 100 items", "បញ្ជីមួយអាចមានធាតុច្រើនបំផុត ១០០"),
            entry("A table can have at most 50 rows", "តារាងមួយអាចមានជួរដេកច្រើនបំផុត ៥០"),
            entry("A table can have at most 12 columns", "តារាងមួយអាចមានជួរឈរច្រើនបំផុត ១២"),
            entry("A lesson check can have at most 30 questions", "ការត្រួតពិនិត្យមេរៀនមួយអាចមានសំណួរច្រើនបំផុត ៣០"),
            entry("This lesson has no check", "មេរៀននេះមិនមានសំណួរត្រួតពិនិត្យទេ"),
            entry("Choose a question type", "សូមជ្រើសរើសប្រភេទសំណួរ"),
            entry("Submit the quiz to complete this item", "សូមដាក់ស្នើកម្រងសំណួរ ដើម្បីបញ្ចប់ធាតុនេះ"),
            entry("Pass the quiz to continue", "សូមប្រឡងជាប់កម្រងសំណួរ ដើម្បីបន្ត"),
            entry("This course doesn't offer a certificate", "វគ្គសិក្សានេះមិនផ្តល់វិញ្ញាបនបត្រទេ"),
            entry("Pass the final quiz to earn the certificate", "សូមប្រឡងជាប់កម្រងសំណួរចុងក្រោយ ដើម្បីទទួលវិញ្ញាបនបត្រ"),
            entry("Item kind must be LESSON or QUIZ", "ប្រភេទធាតុត្រូវជា មេរៀន ឬ កម្រងសំណួរ"),
            entry("Invalid code language", "ភាសាកូដមិនត្រឹមត្រូវ"),
            entry("Image URL must start with http(s):// or /", "URL រូបភាពត្រូវចាប់ផ្តើមដោយ http(s):// ឬ /"),
            // enrollment
            entry("Only published courses accept enrollments", "មានតែវគ្គសិក្សាដែលបានផ្សព្វផ្សាយទេ ដែលអាចចុះឈ្មោះចូលរៀនបាន"),
            entry("You are already enrolled in this course", "អ្នកបានចុះឈ្មោះចូលរៀនវគ្គសិក្សានេះរួចហើយ"),
            entry("You are not enrolled in this course", "អ្នកមិនទាន់បានចុះឈ្មោះចូលរៀនវគ្គសិក្សានេះទេ"),
            entry("You cannot enroll in a course you teach", "អ្នកមិនអាចចុះឈ្មោះចូលរៀនវគ្គសិក្សាដែលអ្នកបង្រៀនបានទេ"),
            entry("Only an admin can remove a student from a course", "មានតែអ្នកគ្រប់គ្រងទេ ដែលអាចដកសិស្សចេញពីវគ្គសិក្សាបាន"),
            entry("That student is not enrolled in this course", "សិស្សនោះមិនបានចុះឈ្មោះចូលរៀនវគ្គសិក្សានេះទេ"),
            // quiz
            entry("This course already has a quiz", "វគ្គសិក្សានេះមានកម្រងសំណួររួចហើយ"),
            entry("This course has no quiz", "វគ្គសិក្សានេះមិនមានកម្រងសំណួរទេ"),
            entry("Students have already attempted this quiz; edit it instead of deleting", "សិស្សបានធ្វើកម្រងសំណួរនេះរួចហើយ សូមកែប្រែជំនួសឲ្យការលុប"),
            entry("Add at least one accepted answer", "សូមបន្ថែមចម្លើយត្រឹមត្រូវយ៉ាងហោចណាស់មួយ"),
            entry("Choose whether True or False is correct", "សូមជ្រើសរើសថា ត្រូវ ឬ ខុស ជាចម្លើយត្រឹមត្រូវ"),
            entry("Mark at least two options as correct", "សូមសម្គាល់ជម្រើសយ៉ាងហោចណាស់ពីរថាត្រឹមត្រូវ"),
            entry("Mark exactly one option as correct", "សូមសម្គាល់ជម្រើសតែមួយគត់ថាត្រឹមត្រូវ"),
            entry("Provide between two and six options", "សូមផ្តល់ជម្រើសពី ២ ទៅ ៦"),
            entry("Provide at most six options", "សូមផ្តល់ជម្រើសមិនលើសពី ៦"),
            entry("Option text is required", "ត្រូវការអត្ថបទជម្រើស"),
            entry("Question text is required", "ត្រូវការអត្ថបទសំណួរ"),
            entry("Quiz title is required", "ត្រូវការចំណងជើងកម្រងសំណួរ"),
            entry("Answer at least one question", "សូមឆ្លើយសំណួរយ៉ាងហោចណាស់មួយ"),
            // reviews & moderation
            entry("Complete at least one lesson before reviewing this course", "សូមបញ្ចប់មេរៀនយ៉ាងហោចណាស់មួយ មុនពេលវាយតម្លៃវគ្គសិក្សានេះ"),
            entry("You have not reviewed this course", "អ្នកមិនទាន់បានវាយតម្លៃវគ្គសិក្សានេះទេ"),
            entry("Only an admin can remove someone else's review", "មានតែអ្នកគ្រប់គ្រងទេ ដែលអាចលុបការវាយតម្លៃរបស់អ្នកដទៃបាន"),
            entry("You cannot report your own review", "អ្នកមិនអាចរាយការណ៍ការវាយតម្លៃរបស់ខ្លួនឯងបានទេ"),
            entry("You have already reported this review", "អ្នកបានរាយការណ៍ការវាយតម្លៃនេះរួចហើយ"),
            entry("Rating must be between 1 and 5", "ការវាយតម្លៃត្រូវនៅចន្លោះ ១ ដល់ ៥"),
            entry("Keep the comment under 1000 characters", "មតិយោបល់ត្រូវមិនលើសពី ១០០០ តួអក្សរ"),
            entry("Choose a reason", "សូមជ្រើសរើសហេតុផល"),
            // notes & AI tutor
            entry("Choose the lesson for this note", "សូមជ្រើសរើសមេរៀនសម្រាប់កំណត់ចំណាំនេះ"),
            entry("Write something in the note", "សូមសរសេរអ្វីមួយក្នុងកំណត់ចំណាំ"),
            entry("That note is not yours", "កំណត់ចំណាំនោះមិនមែនជារបស់អ្នកទេ"),
            entry("Ask a question", "សូមសួរសំណួរ"),
            entry("The last message must be the student's question", "សារចុងក្រោយត្រូវតែជាសំណួររបស់សិស្ស"),
            entry("The AI assistant isn't set up yet. Ask an admin to add an API key.", "គ្រូជំនួយ AI មិនទាន់រៀបចំនៅឡើយ។ សូមឲ្យអ្នកគ្រប់គ្រងបញ្ចូល API key។"),
            entry("The AI assistant is busy right now. Try again in a minute.", "គ្រូជំនួយ AI កំពុងរវល់។ សូមព្យាយាមម្តងទៀតក្នុងរយៈពេលមួយនាទី។"),
            entry("The AI assistant couldn't answer right now. Try again shortly.", "គ្រូជំនួយ AI មិនអាចឆ្លើយបានទេពេលនេះ។ សូមព្យាយាមម្តងទៀតបន្តិចទៀត។"),
            entry("You've asked a lot of questions in a short time. Please wait a few minutes.", "អ្នកបានសួរសំណួរច្រើនក្នុងរយៈពេលខ្លី។ សូមរង់ចាំប៉ុន្មាននាទី។"),
            // account forms
            entry("Username is required", "ត្រូវការឈ្មោះអ្នកប្រើ"),
            entry("Email is required", "ត្រូវការអ៊ីមែល"),
            entry("Password is required", "ត្រូវការពាក្យសម្ងាត់"),
            entry("Confirmed Password is required", "ត្រូវការបញ្ជាក់ពាក្យសម្ងាត់"),
            entry("First Name is required", "ត្រូវការនាមខ្លួន"),
            entry("Last Name is required", "ត្រូវការនាមត្រកូល"),
            entry("Display name is required", "ត្រូវការឈ្មោះបង្ហាញ")
    );
}

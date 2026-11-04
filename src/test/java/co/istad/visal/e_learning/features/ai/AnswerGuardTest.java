package co.istad.visal.e_learning.features.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerGuardTest {

    private static final List<String> HTML = List.of("HyperText Markup Language");
    private static final String QUESTION = "What does HTML stand for?";

    @Test
    void catchesTheHintThatSpelledItOutInPieces() {
        assertTrue(AnswerGuard.leaks("HTML is a markup language whose name combines \"HyperText\" with \"Markup Language.\"", HTML, QUESTION));
    }

    @Test
    void catchesTheFullPhraseAndSpacingTricks() {
        assertTrue(AnswerGuard.leaks("It means HyperText Markup Language.", HTML, QUESTION));
        assertTrue(AnswerGuard.leaks("It means Hyper Text Markup Language.", HTML, QUESTION));
        assertTrue(AnswerGuard.leaks("HYPERTEXT MARKUP LANGUAGE", HTML, QUESTION));
    }

    @Test
    void allowsAHintThatTeachesTheIdea() {
        assertFalse(AnswerGuard.leaks("Each letter describes a job: text that links to other pages, and tags that label content.", HTML, QUESTION));
    }

    @Test
    void oneWordAnswersMatchWholeWordsOnly() {
        List<String> main = List.of("main");
        assertTrue(AnswerGuard.leaks("Use the main tag.", main, "Which tag wraps the page content?"));
        assertFalse(AnswerGuard.leaks("Content should remain inside one wrapper.", main, "Which tag wraps the page content?"));
    }

    @Test
    void wordsAlreadyInTheQuestionDoNotCount() {
        assertFalse(AnswerGuard.leaks("A table holds rows of data.", List.of("Table row"), "Which tag creates a table row?"));
    }

    @Test
    void codeAnswersAreProtected() {
        assertTrue(AnswerGuard.leaks("Try <h1> for that.", List.of("<h1>"), "Which is the most important heading?"));
        assertTrue(AnswerGuard.leaks("C++ works here", List.of("C++"), "Which language compiles to native code?"));
    }

    @Test
    void recognisesAQuizQuestionTypedIntoChat() {
        assertTrue(AnswerGuard.asks("what does html stand for", QUESTION));
        assertTrue(AnswerGuard.asks("Hey Bunny, tell me what HTML stands for? I mean stand for", QUESTION));
        assertFalse(AnswerGuard.asks("how do I add an image?", QUESTION));
    }

    @Test
    void listsTheWordsARewriteMustAvoid() {
        assertEquals("hypertext, markup, language", AnswerGuard.forbiddenWords(HTML, QUESTION));
    }

    @Test
    void onlyLongAnswersAreDistinctive() {
        assertTrue(AnswerGuard.isDistinctive("HyperText Markup Language"));
        assertFalse(AnswerGuard.isDistinctive("main"));
    }
}

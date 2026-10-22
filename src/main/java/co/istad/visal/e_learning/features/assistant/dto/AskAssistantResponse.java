package co.istad.visal.e_learning.features.assistant.dto;

/** questionsLeft = how many tutor questions this learner still has for this lesson. */
public record AskAssistantResponse(String reply, int questionsLeft) {
}

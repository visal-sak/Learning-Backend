package co.istad.visal.e_learning.features.quiz;

import co.istad.visal.e_learning.features.quiz.dto.instructor.*;
import co.istad.visal.e_learning.features.quiz.dto.student.AttemptResultResponse;
import co.istad.visal.e_learning.features.quiz.dto.student.QuizTakeResponse;
import co.istad.visal.e_learning.features.quiz.dto.student.SubmitAttemptRequest;

import java.util.List;
import java.util.UUID;

public interface QuizService {

    // ----- authoring: owner or admin -----

    QuizDetailResponse getForEditing(UUID quizId);

    /** The course's quiz for its editor — lets the admin UI find an existing quiz by course. */
    QuizDetailResponse getForEditingByCourse(UUID courseId);

    QuizDetailResponse create(CreateQuizRequest request);

    QuizDetailResponse update(UUID quizId, UpdateQuizRequest request);

    void delete(UUID quizId);

    QuestionResponse saveQuestion(SaveQuestionRequest request);

    QuestionResponse updateQuestion(UUID questionId, SaveQuestionRequest request);

    void deleteQuestion(UUID questionId);

    /** Every attempt on this quiz, for the instructor who owns the course. */
    List<AttemptResultResponse> findAllAttempts(UUID quizId);

    // ----- taking: enrolled students -----

    QuizTakeResponse startAttempt(UUID courseId);

    AttemptResultResponse submit(UUID quizId, SubmitAttemptRequest request);

    List<AttemptResultResponse> findMyAttempts(UUID quizId);
}

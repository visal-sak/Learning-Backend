package co.istad.visal.e_learning.features.quiz;

import co.istad.visal.e_learning.features.quiz.dto.instructor.AnswerOptionResponse;
import co.istad.visal.e_learning.features.quiz.dto.instructor.QuestionResponse;
import co.istad.visal.e_learning.features.quiz.dto.instructor.QuizDetailResponse;
import co.istad.visal.e_learning.features.quiz.dto.student.OptionForStudentResponse;
import co.istad.visal.e_learning.features.quiz.dto.student.QuestionForStudentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Two parallel mapping paths, on purpose.
 *
 * toResponse(...) carries isCorrect and is for the owner. toStudentView(...)
 * cannot carry it, because the target record has no such component — the
 * compiler enforces the rule rather than a code reviewer.
 */
@Mapper(componentModel = "spring")
public interface QuizMapper {

    // ---- instructor side ----

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "totalPoints", expression = "java(quiz.totalPoints())")
    QuizDetailResponse toDetail(Quiz quiz);

    QuestionResponse toResponse(Question question);

    AnswerOptionResponse toResponse(AnswerOption option);

    // ---- student side ----

    QuestionForStudentResponse toStudentView(Question question);

    OptionForStudentResponse toStudentView(AnswerOption option);
}

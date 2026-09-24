package com.fitlog.fitlogv2server.domain.workoutsession.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.fitlogv2server.domain.workoutprogram.WorkoutProgramDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutSessionDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void startRequest_rejectsNestedInvalidSet() throws Exception {
        WorkoutSessionDto.StartRequest request = objectMapper.readValue(
                "{\"workoutProgramId\":1,\"customExercises\":[{\"workoutId\":1,\"order\":1," +
                        "\"sets\":[{\"reps\":-1,\"restTime\":60,\"weight\":-5}]}]}",
                WorkoutSessionDto.StartRequest.class);

        Set<ConstraintViolation<WorkoutSessionDto.StartRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("customExercises[0].sets[0].reps", "customExercises[0].sets[0].weight");
    }

    @Test
    void startRequest_rejectsMissingSetsList() throws Exception {
        WorkoutSessionDto.StartRequest request = objectMapper.readValue(
                "{\"workoutProgramId\":1,\"customExercises\":[{\"workoutId\":1,\"order\":1}]}",
                WorkoutSessionDto.StartRequest.class);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void endRequest_requiresStatus() throws Exception {
        WorkoutSessionDto.EndRequest request = objectMapper.readValue("{}", WorkoutSessionDto.EndRequest.class);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void programRequest_rejectsBlankNameAndMissingParts() throws Exception {
        WorkoutProgramDto.Request request = objectMapper.readValue(
                "{\"name\":\"  \",\"description\":\"\"}", WorkoutProgramDto.Request.class);

        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("name", "parts");
    }

    @Test
    void validStartRequest_passes() throws Exception {
        WorkoutSessionDto.StartRequest request = objectMapper.readValue(
                "{\"workoutProgramId\":1,\"customExercises\":[{\"workoutId\":1,\"order\":1," +
                        "\"sets\":[{\"setNumber\":1,\"reps\":10,\"restTime\":60,\"weight\":40}]}]}",
                WorkoutSessionDto.StartRequest.class);

        assertThat(validator.validate(request)).isEmpty();
    }
}

package com.th3hero.eventbot.controllers.rest;

import com.kseth.development.rest.error.ProblemDetailFactory;
import com.th3hero.eventbot.exceptions.ActionAlreadyPreformedException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail entityNotFoundException(EntityNotFoundException e) {
        return ProblemDetailFactory.createProblemDetail(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ActionAlreadyPreformedException.class)
    public ProblemDetail actionAlreadyPreformedException(ActionAlreadyPreformedException e) {
        return ProblemDetailFactory.createProblemDetail(HttpStatus.BAD_REQUEST, e);
    }
}

package com.ls.webflux_server.answer.model;

import lombok.Data;

@Data
public class AnswerRequestDto {
    private String correctAnswer;

    private String userAnswer;
}

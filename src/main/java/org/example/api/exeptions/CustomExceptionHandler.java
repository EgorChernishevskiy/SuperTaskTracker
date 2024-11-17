package org.example.api.exeptions;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/*
CustomExceptionHandler:
Используется для перехвата исключений, выбрасываемых из контроллеров, и обрабатывает только те ошибки,
которые можно перехватить в контроллерах.
Он обрабатывает конкретные типы исключений и подходит для пользовательской обработки исключений,
связанных с логикой приложения.
 */
@Log4j2
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> exception(Exception ex, WebRequest request) throws Exception {

        log.error("Exception during execution of application", ex);

        return handleException(ex, request);
    }
}

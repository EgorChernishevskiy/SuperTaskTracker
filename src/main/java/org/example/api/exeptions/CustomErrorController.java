package org.example.api.exeptions;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/*
CustomErrorController:
Обрабатывает все ошибки, которые происходят за пределами контроллеров или не были перехвачены CustomExceptionHandler.
Этот контроллер полезен для обработки системных ошибок, таких как 404 Not Found, 500 Internal Server Error, и ошибок,
произошедших до того, как запрос попал в контроллеры.
Он перехватывает запросы, отправленные на путь /error, что позволяет централизовать обработку всех ошибок, включая те,
которые происходят на более низком уровне (например, когда клиент обращается к несуществующему ресурсу).
 */
@RequiredArgsConstructor
@Controller
public class CustomErrorController implements ErrorController {

    private static final String PATH = "/error";

    private final ErrorAttributes errorAttributes;

    @RequestMapping(CustomErrorController.PATH)
    public ResponseEntity<ErrorDto> error(WebRequest webRequest) {

        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                webRequest,
                ErrorAttributeOptions.of(
                        ErrorAttributeOptions.Include.ERROR,
                        ErrorAttributeOptions.Include.MESSAGE,
                        ErrorAttributeOptions.Include.STATUS
                )

        );

        return ResponseEntity
                .status((Integer) attributes.get("status"))
                .body(ErrorDto
                        .builder()
                        .error((String) attributes.get("error"))
                        .errorDescription((String) attributes.get("message"))
                        .build()
                );
    }
}
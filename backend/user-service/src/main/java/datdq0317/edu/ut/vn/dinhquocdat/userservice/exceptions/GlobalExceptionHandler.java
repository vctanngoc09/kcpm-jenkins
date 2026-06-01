package datdq0317.edu.ut.vn.dinhquocdat.userservice.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleInvalidFormat(HttpMessageNotReadableException ex) {

    Map<String, String> error = new HashMap<>();
    error.put("message", "Ngày sinh không hợp lệ");

    return ResponseEntity.badRequest().body(error);
}
}
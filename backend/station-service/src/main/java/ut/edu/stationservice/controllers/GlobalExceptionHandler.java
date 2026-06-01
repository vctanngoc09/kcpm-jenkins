package ut.edu.stationservice.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Xử lý các lỗi nghiệp vụ từ Service
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 2. Xử lý lỗi sai kiểu dữ liệu (truyền String vào số Double)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Dữ liệu đầu vào không hợp lệ";

        // Kiểm tra xem lỗi có phải do sai định dạng (kiểu dữ liệu) không
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException cause = (InvalidFormatException) ex.getCause();

            // Lấy tên trường đang bị lỗi từ JSON path
            if (!cause.getPath().isEmpty()) {
                String fieldName = cause.getPath().get(0).getFieldName();

                if ("kinhDo".equals(fieldName)) {
                    message = "Kinh độ sai kiểu dữ liệu";
                } else if ("viDo".equals(fieldName)) {
                    message = "Vĩ độ sai kiểu dữ liệu";
                }
            }
        }

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    // Hàm bổ trợ để tạo response chuẩn
    private ResponseEntity<Object> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());

        return new ResponseEntity<>(body, status);
    }
}
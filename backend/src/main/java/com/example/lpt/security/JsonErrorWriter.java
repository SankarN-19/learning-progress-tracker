package com.example.lpt.security;

import com.example.lpt.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Writes {"message": "..."} from inside servlet filters (outside Spring MVC). */
@Component
public class JsonErrorWriter {

    private final ObjectMapper mapper;

    public JsonErrorWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ErrorResponse(message, null));
    }
}

package com.mindspace.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mindSpaceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MindSpace API")
                        .version("v1")
                        .description("""
                                Privacy-first mental wellness and self-reflection API. All data is private to the signed-in user.
                                MindSpace is a self-reflection tool, not a diagnostic or treatment service.

                                Conventions: dates are ISO-8601 local calendar dates in the user's timezone (users.timezone);
                                instants are UTC. Errors are RFC 9457 problem documents with an optional `errors` array and a `requestId`.
                                Authentication is by HttpOnly cookie; state-changing requests also need the X-XSRF-TOKEN header
                                (value of the XSRF-TOKEN cookie issued by GET /api/v1/auth/csrf).
                                Another user's resource ids return 404, never 403."""))
                .components(new Components().addSecuritySchemes("cookieAuth",
                        new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("ms_access")))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"));
    }
}

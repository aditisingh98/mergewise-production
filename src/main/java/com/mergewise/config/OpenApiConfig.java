package com.mergewise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mergeWiseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MergeWise AI Review API")
                        .description("Enterprise AI-powered pull request review platform")
                        .version("1.0.0")
                        .contact(new Contact().name("MergeWise").email("support@mergewise.dev")));
    }

    /** Swagger UI text without @Operation on controllers (avoids annotation classpath issues in some IDEs). */
    @Bean
    public OperationCustomizer prApiOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (!handlerMethod.getBeanType().getSimpleName().equals("PRController")) {
                return operation;
            }
            return switch (handlerMethod.getMethod().getName()) {
                case "providers" -> operation.summary("List supported VCS providers and token rules");
                case "analyze" -> {
                    operation.summary("Analyze a GitHub pull request or GitLab merge request");
                    operation.description(
                            "Auto-detects provider from prUrl. Public repos/MRs need only prUrl. "
                                    + "Private GitHub: githubToken or accessToken. Private GitLab: gitlabToken or accessToken. "
                                    + "Authorization: Bearer <token> is also supported.");
                    operation.addParametersItem(new Parameter()
                            .in("header")
                            .name("Authorization")
                            .description("Optional PAT: Bearer <token> (GitHub or GitLab depending on prUrl)")
                            .required(false)
                            .schema(new StringSchema()));
                    yield operation;
                }
                default -> operation;
            };
        };
    }
}

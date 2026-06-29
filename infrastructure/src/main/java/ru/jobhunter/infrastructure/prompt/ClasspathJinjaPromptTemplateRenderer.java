package ru.jobhunter.infrastructure.prompt;

import com.hubspot.jinjava.Jinjava;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@Component
public class ClasspathJinjaPromptTemplateRenderer
        implements PromptTemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(
            ClasspathJinjaPromptTemplateRenderer.class
    );

    @Override
    public String render(
            PromptTemplate template,
            PromptTemplateModel model
    ) {
        Objects.requireNonNull(
                template,
                "Prompt template must not be null"
        );
        Objects.requireNonNull(
                model,
                "Prompt template model must not be null"
        );

        Map<String, Object> values = Map.copyOf(
                model.toTemplateModel()
        );

        validateRequiredVariables(template, values);

        try {
            String source = loadTemplate(template);

            String rendered = new Jinjava().render(
                    source,
                    values
            ).strip();

            if (rendered.isBlank()) {
                throw new PromptTemplateRenderingException(
                        "Rendered prompt must not be blank: "
                                + template.name()
                );
            }

            log.debug(
                    "LLM prompt template rendered: template={}",
                    template.name()
            );

            return rendered;
        } catch (PromptTemplateRenderingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PromptTemplateRenderingException(
                    "Could not render prompt template: "
                            + template.name(),
                    exception
            );
        }
    }

    private String loadTemplate(
            PromptTemplate template
    ) {
        ClassPathResource resource = new ClassPathResource(
                template.resourcePath()
        );

        if (!resource.exists()) {
            throw new PromptTemplateRenderingException(
                    "Prompt template was not found: "
                            + template.resourcePath()
            );
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new PromptTemplateRenderingException(
                    "Could not read prompt template: "
                            + template.resourcePath(),
                    exception
            );
        }
    }

    private void validateRequiredVariables(
            PromptTemplate template,
            Map<String, Object> values
    ) {
        for (String variableName : template.requiredVariables()) {
            Object value = values.get(variableName);

            if (value == null) {
                throw new PromptTemplateRenderingException(
                        "Required prompt variable is missing: "
                                + variableName
                                + ", template="
                                + template.name()
                );
            }

            if (value instanceof String text
                    && text.isBlank()) {
                throw new PromptTemplateRenderingException(
                        "Required prompt variable is blank: "
                                + variableName
                                + ", template="
                                + template.name()
                );
            }
        }
    }
}
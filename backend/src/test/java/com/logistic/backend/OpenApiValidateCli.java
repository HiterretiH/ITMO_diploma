package com.logistic.backend;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public final class OpenApiValidateCli {

    private OpenApiValidateCli() {}

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: OpenApiValidateCli <path-to-openapi.yaml>");
            System.exit(2);
        }
        SwaggerParseResult r = new OpenAPIV3Parser().readLocation(args[0], null, null);
        if (r.getMessages() != null && !r.getMessages().isEmpty()) {
            r.getMessages().forEach(System.err::println);
        }
        if (r.getOpenAPI() == null) {
            System.exit(1);
        }
    }
}

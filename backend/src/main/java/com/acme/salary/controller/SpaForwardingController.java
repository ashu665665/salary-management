package com.acme.salary.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the Angular application from the same origin as the API.
 *
 * <p>The browser knows addresses like {@code /employees/7}, but the server has no file of that
 * name. Opening one directly, or refreshing on it, has to return index.html and let the
 * application work out what to show.
 *
 * <p>Two things are deliberately left out. Anything under {@code /api} keeps its own 404, because
 * answering a bad API call with a page of HTML is far harder to diagnose. Anything containing a
 * dot is treated as a request for a file, so a missing script comes back as a 404 rather than as
 * the page that was trying to load it.
 */
@Controller
public class SpaForwardingController {

    private static final String SCREEN = "/{screen:^(?!api$)[^\\.]*}";
    private static final String SCREEN_WITH_ID = SCREEN + "/{id:[^\\.]*}";

    @GetMapping({SCREEN, SCREEN_WITH_ID})
    public String forwardToApplication() {
        return "forward:/index.html";
    }
}

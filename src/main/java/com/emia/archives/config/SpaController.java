package com.emia.archives.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping({
        "/{path:[^\\.]+}",
        "/{path:[^\\.]+}/{rest:[^\\.]+}",
        "/{path:[^\\.]+}/{rest:[^\\.]+}/{sub:[^\\.]+}",
        "/{path:[^\\.]+}/{rest:[^\\.]+}/{sub:[^\\.]+}/{more:[^\\.]+}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}

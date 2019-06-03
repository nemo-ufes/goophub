package br.ufes.inf.goophubv2.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/")
public class PageController {

    // Page Routes
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String searchPage() { return "search"; }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String indexPage() { return "index"; }

    @RequestMapping(value = "/endpoint", method = RequestMethod.GET)
    public String endpointPage() {
        return "endpoint";
    }

    @RequestMapping(value = "/upload", method = RequestMethod.GET)
    public String uploadPage() {
        return "upload";
    }

    @RequestMapping(value = "/joint", method = RequestMethod.GET)
    public String jointPage() { return "joint"; }
}


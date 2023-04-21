package org.web.controller.admin.v1;

import lombok.extern.slf4j.Slf4j;
import org.api.admin.config.AdminConfig;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController(AdminConfig.ADMIN + "HomeController")
@RequestMapping("/admin/home")
@Slf4j
public class HomeController {
}

package com.example.sso.servicea.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginPageController {

	@GetMapping("/login")
	public String loginPage(
			@RequestParam(name = "error", required = false) String error,
			@RequestParam(name = "logout", required = false) String logout,
			Model model
	) {
		model.addAttribute("hasError", error != null);
		model.addAttribute("loggedOut", logout != null);
		return "login";
	}
}

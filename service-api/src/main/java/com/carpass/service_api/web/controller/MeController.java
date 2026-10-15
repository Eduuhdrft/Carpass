package com.carpass.service_api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.service.UserService;
import com.carpass.service_api.web.dto.auth.ChangePasswordRequest;
import com.carpass.service_api.web.dto.user.UpdateProfileRequest;
import com.carpass.service_api.web.dto.user.UserResponse;

import jakarta.validation.Valid;

/**
 * Conta do usuario autenticado.
 *
 * <p>
 * Nenhuma rota aqui recebe id de usuario: o alvo e sempre quem esta autenticado.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

	private final UserService userService;

	public MeController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<UserResponse> me() {
		return ResponseEntity.ok(userService.getCurrentUser());
	}

	@PutMapping
	public ResponseEntity<UserResponse> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
		return ResponseEntity.ok(userService.updateProfile(request));
	}

	@PatchMapping("/password")
	public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
		userService.changePassword(request);
		return ResponseEntity.noContent().build();
	}
}

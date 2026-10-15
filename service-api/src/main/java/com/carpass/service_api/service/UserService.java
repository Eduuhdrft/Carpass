package com.carpass.service_api.service;

import com.carpass.service_api.web.dto.auth.ChangePasswordRequest;
import com.carpass.service_api.web.dto.user.UpdateProfileRequest;
import com.carpass.service_api.web.dto.user.UserResponse;

/**
 * Operacoes do usuario sobre a propria conta.
 */
public interface UserService {

	UserResponse getCurrentUser();

	UserResponse updateProfile(UpdateProfileRequest request);

	void changePassword(ChangePasswordRequest request);
}

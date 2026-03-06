package com.carpass.service_api.enums;

public enum UsuarioRole {
	
	PREMIUM_USER("premium_user"),
	NORMAL_USER("normal_user");
	
	private String role;
	
	private UsuarioRole(String role) {
		this.role = role;
	}
	
	public String getRole() {
		return role;
	}
	
}

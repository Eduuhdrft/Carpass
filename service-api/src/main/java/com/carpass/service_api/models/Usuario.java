package com.carpass.service_api.models;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.carpass.service_api.enums.UsuarioRole;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;
	
	@Column(nullable = false)
	private String nome;
	
	private String sobrenome;
	
	@Column(unique = true, nullable = false)
	private String email;
	
	@Column(nullable = false)
	private String senha;
	
	private String numero;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, name = "tipo_conta", length = 255)
	private UsuarioRole tipoConta;
	
	public Usuario() {}

	public Usuario(UUID id, String nome, String sobrenome, String email, String senha, String numero, UsuarioRole tipoConta) {
		this.id = id;
		this.nome = nome;
		this.sobrenome = sobrenome;
		this.email = email;
		this.senha = senha;
		this.numero = numero;
		this.tipoConta = tipoConta;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getSobrenome() {
		return sobrenome;
	}

	public void setSobrenome(String sobrenome) {
		this.sobrenome = sobrenome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}
	
	public UsuarioRole getTipoConta() {
		return tipoConta;
	}
	
	public void setTipoConta(UsuarioRole tipoConta) {
		this.tipoConta = tipoConta;
	}

	@Override
	public String toString() {
		return "usuario [id=" + id + ", nome=" + nome + ", sobrenome=" + sobrenome + ", email=" + email + ", senha="
				+ senha + ", numero=" + numero + ", tipoConta=" + tipoConta + "]";
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if(this.tipoConta == UsuarioRole.PREMIUM_USER) {
			return List.of(
					new SimpleGrantedAuthority("PREMIUM_USER"), 
					new SimpleGrantedAuthority("NORMAL_USER")
				);
			} else {
				return List.of(new SimpleGrantedAuthority("NORMAL_USER"));
			}
	}

	@Override
	public String getPassword() {
		return senha;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
	
}

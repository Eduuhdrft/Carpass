package com.carpass.service_api.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.WorkshopMember;

/**
 * Adaptador entre a entidade {@link User} e o contrato {@link UserDetails} do
 * Spring Security.
 *
 * <h2>Identidade e vinculo, lado a lado</h2>
 *
 * <p>
 * O principal carrega duas coisas: <strong>quem</strong> esta autenticado (o
 * {@link User}) e <strong>de onde</strong> ele esta operando nesta requisicao (o
 * {@link WorkshopMember} escolhido). O segundo pode ser nulo, e isso e um
 * estado legitimo, nao um erro: e a pessoa que nao tem vinculo ativo com
 * nenhuma oficina -- tipicamente alguem que foi demitido e ainda nao criou a
 * propria oficina.
 * </p>
 *
 * <p>
 * Sem vinculo, {@link #getAuthorities()} devolve lista vazia. Nenhuma expressao
 * de {@code RoleExpressions} casa, entao a pessoa nao alcanca nada operacional
 * -- o que sobra e o proprio perfil e a criacao da oficina dela. E o padrao
 * seguro: negar por ausencia de papel, e nao por uma checagem que alguem possa
 * esquecer de escrever.
 * </p>
 */
public class AuthenticatedUser implements UserDetails {

	private static final long serialVersionUID = 1L;

	private final transient User user;
	private final transient WorkshopMember membership;
	private final UUID workshopId;

	/** Autenticado sem oficina escolhida: so o proprio perfil e criar oficina. */
	public AuthenticatedUser(User user) {
		this(user, null);
	}

	public AuthenticatedUser(User user, WorkshopMember membership) {
		this.user = user;
		this.membership = membership;
		this.workshopId = membership == null ? null : membership.getWorkshop().getId();
	}

	public User getUser() {
		return user;
	}

	public UUID getId() {
		return user.getId();
	}

	/**
	 * O papel <strong>nesta</strong> oficina, ou nulo quando nao ha vinculo.
	 *
	 * <p>
	 * Devolve {@link Role#SYSTEM_ADMIN} para o administrador da plataforma sem
	 * vinculo, preservando o comportamento que a antiga coluna {@code users.role}
	 * tinha.
	 * </p>
	 */
	public Role getRole() {
		if (membership != null) {
			return membership.getRole();
		}
		return user.isPlatformAdmin() ? Role.SYSTEM_ADMIN : null;
	}

	/** Oficina em que esta requisicao opera. Nulo quando nao ha vinculo escolhido. */
	public UUID getWorkshopId() {
		return workshopId;
	}

	public WorkshopMember getMembership() {
		return membership;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Role role = getRole();
		return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
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

	/**
	 * Se a identidade pode entrar.
	 *
	 * <p>
	 * Repare que isto <strong>nao</strong> olha o vinculo. Demissao nao impede
	 * mais o login: impede o acesso aos dados da oficina, que e coisa diferente.
	 * Quem foi demitido entra, nao enxerga nada da ex-oficina, e pode abrir a
	 * propria.
	 * </p>
	 */
	@Override
	public boolean isEnabled() {
		return user.isActive();
	}
}

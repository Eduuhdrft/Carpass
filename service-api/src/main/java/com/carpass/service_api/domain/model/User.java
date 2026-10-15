package com.carpass.service_api.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A identidade de uma pessoa na plataforma: quem ela e, e como ela entra.
 *
 * <h2>O que esta classe deixou de ser</h2>
 *
 * <p>
 * Ela ja guardou {@code workshop_id} e {@code role}. Nao guarda mais: onde a
 * pessoa trabalha e em que papel e assunto de {@link WorkshopMember}. A
 * separacao existe porque identidade e emprego tem ciclos de vida diferentes --
 * um emprego acaba, uma pessoa nao.
 * </p>
 *
 * <p>
 * A consequencia pratica e o motivo da mudanca: com as duas coisas na mesma
 * linha, demitir alguem desativava a <em>identidade</em>, e como o e-mail e
 * unico no sistema inteiro, a pessoa nunca mais conseguia usar o proprio
 * endereco -- nem para abrir a oficina dela, nem para voltar a ser contratada
 * pela mesma oficina.
 * </p>
 *
 * <h2>O que {@code active} significa agora</h2>
 *
 * <p>
 * Apenas se esta identidade pode entrar no sistema. Nasce {@code false} na
 * conta criada por convite -- que so passa a valer quando a pessoa define a
 * senha -- e <strong>nao e mais tocado por demissao</strong>.
 * </p>
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "last_name", length = 80)
	private String lastName;

	@Column(name = "email", nullable = false, unique = true, length = 180)
	private String email;

	@JsonIgnore
	@Column(name = "password", nullable = false, length = 100)
	private String password;

	@Column(name = "phone", length = 30)
	private String phone;

	@Column(name = "active", nullable = false)
	private boolean active;

	/**
	 * Administrador da plataforma -- nao da oficina.
	 *
	 * <p>
	 * Era o valor {@code SYSTEM_ADMIN} da antiga coluna {@code role}. Virou flag
	 * porque nunca foi papel de oficina: a oficina nao pode atribui-lo e ele nao
	 * aparece em nenhuma expressao de autorizacao. Guardar isso num segundo campo
	 * chamado "role", ao lado do papel do vinculo, so serviria para os dois serem
	 * confundidos.
	 * </p>
	 */
	@Column(name = "platform_admin", nullable = false)
	private boolean platformAdmin;

	public User() {
		// construtor sem argumentos exigido pelo JPA
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isPlatformAdmin() {
		return platformAdmin;
	}

	public void setPlatformAdmin(boolean platformAdmin) {
		this.platformAdmin = platformAdmin;
	}

	/** Nome completo do usuario, sem espacos sobrando quando nao ha sobrenome. */
	public String getFullName() {
		return (firstName + " " + (lastName == null ? "" : lastName)).trim();
	}

}

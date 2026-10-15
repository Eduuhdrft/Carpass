package com.carpass.service_api.domain.model;

import com.carpass.service_api.domain.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * O vinculo entre uma pessoa e uma oficina.
 *
 * <h2>Por que esta tabela existe</h2>
 *
 * <p>
 * Antes dela, {@code users} guardava {@code workshop_id} e {@code role} na
 * propria linha da pessoa. Isso misturava duas coisas com ciclos de vida
 * diferentes:
 * </p>
 *
 * <ul>
 * <li><strong>Identidade</strong> -- quem a pessoa e: e-mail, senha, nome. E
 * dela, e global, e nao acaba quando um emprego acaba.</li>
 * <li><strong>Vinculo</strong> -- onde ela trabalha e em que papel. E da
 * <em>relacao</em>, e e temporario.</li>
 * </ul>
 *
 * <p>
 * Com as duas na mesma linha, {@code users.active} significava ao mesmo tempo
 * "esta identidade existe" e "esta pessoa ainda trabalha aqui". Demitir matava a
 * identidade: como o e-mail e unico no sistema inteiro, a pessoa demitida nunca
 * mais conseguia usar o proprio e-mail -- nem para abrir a oficina dela, nem
 * para ser recontratada pela mesma oficina depois. A ex-empregadora ficava, na
 * pratica, com a chave do e-mail pessoal de alguem.
 * </p>
 *
 * <p>
 * Separando, demitir desativa <strong>o vinculo</strong>. A pessoa continua
 * existindo, continua entrando com o mesmo e-mail e a mesma senha, e cai numa
 * tela que oferece criar a oficina dela. Nao ha e-mail de confirmacao nesse
 * caminho porque nao ha o que confirmar: quem fez login ja provou que e dono
 * daquela conta.
 * </p>
 *
 * <h2>O que isso destrava de quebra</h2>
 *
 * <p>
 * Uma pessoa pode ter mais de um vinculo ativo: o dono com duas unidades, o
 * gerente que atende duas oficinas da familia, o mecanico que trabalha meio
 * periodo em duas. O login passa a pedir em qual delas a pessoa quer entrar, e
 * o token carrega essa escolha.
 * </p>
 *
 * <h2>Um vinculo por par pessoa/oficina</h2>
 *
 * <p>
 * A restricao de unicidade e {@code (user_id, workshop_id)} -- e nao "um
 * vinculo <em>ativo</em> por par", que o banco nao sabe expressar sem indice
 * parcial. Recontratar <strong>reativa a linha existente</strong> em vez de
 * criar uma segunda: assim nao ha como a mesma pessoa aparecer duas vezes na
 * equipe da mesma oficina com papeis diferentes, que seria uma ambiguidade sem
 * resposta certa na hora de decidir o que ela pode fazer.
 * </p>
 */
@Entity
@Table(name = "workshop_members", uniqueConstraints = {
		@UniqueConstraint(name = "uk_workshop_members_user_workshop", columnNames = { "user_id", "workshop_id" }) },
		indexes = {
				@Index(name = "idx_workshop_members_user", columnList = "user_id"),
				@Index(name = "idx_workshop_members_workshop", columnList = "workshop_id") })
public class WorkshopMember extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	/**
	 * O papel da pessoa <strong>nesta</strong> oficina.
	 *
	 * <p>
	 * Fica aqui, e nao em {@code users}, porque e perfeitamente possivel ser dona
	 * da propria oficina e mecanica na do irmao. O papel so faz sentido junto com
	 * a oficina a que ele se refere.
	 * </p>
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 40)
	private Role role;

	/**
	 * Se o vinculo esta valendo.
	 *
	 * <p>
	 * Desativado e o estado de quem foi demitido. A linha continua no banco de
	 * proposito: ela e o registro de que aquela pessoa trabalhou ali, e e o que
	 * permite recontratar reativando em vez de criar um vinculo novo.
	 * </p>
	 */
	@Column(name = "active", nullable = false)
	private boolean active = true;

	public WorkshopMember() {
		// construtor sem argumentos exigido pelo JPA
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}

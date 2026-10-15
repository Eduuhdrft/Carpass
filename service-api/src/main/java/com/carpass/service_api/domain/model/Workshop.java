package com.carpass.service_api.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Oficina mecanica. E a raiz do isolamento multi-tenant: todo dado operacional pertence a uma oficina.
 */
@Entity
@Table(name = "workshops")
public class Workshop extends BaseEntity {

	@Column(name = "trade_name", nullable = false, length = 150)
	private String tradeName;

	@Column(name = "corporate_name", length = 150)
	private String corporateName;

	@Column(name = "cnpj", nullable = false, unique = true, length = 18)
	private String cnpj;

	@Column(name = "phone", length = 30)
	private String phone;

	@Column(name = "email", length = 180)
	private String email;

	/**
	 * A logo da oficina.
	 *
	 * <p>
	 * Apesar do nome, guarda a imagem em si -- uma <em>data URI</em> em base64
	 * (por exemplo {@code data:image/png;base64,...}) -- e nao um link para um
	 * arquivo hospedado em outro lugar. Nao ha servico de arquivos nesta
	 * aplicacao; guardar a imagem direto na coluna evita depender de um.
	 * {@code null} significa que a oficina nao cadastrou logo, e as folhas
	 * impressas caem para o nome fantasia como identificacao.
	 * </p>
	 */
	@Column(name = "logo_url", columnDefinition = "text")
	private String logoUrl;

	@Embedded
	private Address address;

	@Column(name = "active", nullable = false)
	private boolean active;

	/**
	 * Se o gerente enxerga o financeiro desta oficina.
	 *
	 * <p>
	 * Quem decide e o dono, e so ele. O padrao e {@code true} porque o gerente ja
	 * via o faturamento do mes no painel antes desta opcao existir -- nascer
	 * {@code false} tiraria em silencio um acesso que a oficina ja usava.
	 * </p>
	 *
	 * <p>
	 * <strong>Esta coluna nao entra no {@code UpdateWorkshopRequest}.</strong> O
	 * cadastro da oficina e editavel pelo gerente; se a chave viajasse junto, o
	 * gerente se concederia acesso ao financeiro salvando a propria tela de
	 * cadastro. Ela tem endpoint proprio, restrito ao dono.
	 * </p>
	 */
	@Column(name = "manager_can_see_finance", nullable = false)
	private boolean managerCanSeeFinance = true;

	/**
	 * Depois de quantas horas paradas uma OS aberta e encerrada sozinha.
	 *
	 * <p>
	 * {@code null} desliga a rotina, e esse e o padrao: encerrar documento sem a
	 * oficina ter pedido e o tipo de automacao que, quando erra, faz a pessoa
	 * deixar de confiar no sistema inteiro. Quem liga escolhe o prazo.
	 * </p>
	 *
	 * <p>
	 * O relogio conta a partir da <strong>ultima movimentacao</strong>, nao da
	 * abertura: uma OS mexida ha duas horas nao esta parada, por mais antiga que
	 * seja.
	 * </p>
	 */
	@Column(name = "auto_close_open_orders_hours")
	private Integer autoCloseOpenOrdersHours;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	public Workshop() {
		// construtor sem argumentos exigido pelo JPA
	}

	public String getTradeName() {
		return tradeName;
	}

	public void setTradeName(String tradeName) {
		this.tradeName = tradeName;
	}

	public String getCorporateName() {
		return corporateName;
	}

	public void setCorporateName(String corporateName) {
		this.corporateName = corporateName;
	}

	public String getCnpj() {
		return cnpj;
	}

	public void setCnpj(String cnpj) {
		this.cnpj = cnpj;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Integer getAutoCloseOpenOrdersHours() {
		return autoCloseOpenOrdersHours;
	}

	public void setAutoCloseOpenOrdersHours(Integer autoCloseOpenOrdersHours) {
		this.autoCloseOpenOrdersHours = autoCloseOpenOrdersHours;
	}

	public boolean isManagerCanSeeFinance() {
		return managerCanSeeFinance;
	}

	public void setManagerCanSeeFinance(boolean managerCanSeeFinance) {
		this.managerCanSeeFinance = managerCanSeeFinance;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

}

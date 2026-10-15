package com.carpass.service_api.security;

/**
 * Expressoes de autorizacao usadas nos controllers.
 *
 * <p>
 * Centralizar aqui evita que a mesma regra seja escrita de formas diferentes em
 * cada endpoint, que e como costumam nascer brechas de permissao.
 * </p>
 */
public final class RoleExpressions {

	/** Decisoes administrativas da oficina: equipe, catalogo, estoque e cadastro da oficina. */
	public static final String MANAGEMENT = "hasAnyRole('OWNER', 'MANAGER')";

	/** Operacao do balcao: clientes, veiculos e ordens de servico. */
	public static final String FRONT_DESK = "hasAnyRole('OWNER', 'MANAGER', 'RECEPTIONIST')";

	/** Qualquer pessoa da equipe, incluindo mecanicos, que precisam consultar dados. */
	public static final String ANY_STAFF = "hasAnyRole('OWNER', 'MANAGER', 'RECEPTIONIST', 'MECHANIC')";

	/**
	 * Conta autenticada, com ou sem vinculo a uma oficina.
	 *
	 * <p>
	 * Existe porque "estar autenticado" e "ter papel numa oficina" deixaram de
	 * ser a mesma coisa quando o vinculo virou entidade propria. Quem saiu de uma
	 * oficina e ainda nao criou a dele tem conta, senha e sessao -- e nenhum
	 * papel, entao {@link #ANY_STAFF} o barra.
	 * </p>
	 *
	 * <p>
	 * <strong>Usar com muita parcimonia.</strong> Serve so para o que e da
	 * pessoa, e nao de uma oficina: consulta de apoio ao preenchimento e o
	 * proprio perfil. Nada que leia ou escreva dado operacional pode passar por
	 * aqui -- isso continua sendo territorio de {@code ANY_STAFF} e
	 * {@code MANAGEMENT}, onde o {@code requireWorkshopId()} tambem barra por
	 * falta de vinculo.
	 * </p>
	 */
	public static final String ANY_ACCOUNT = "isAuthenticated()";

	private RoleExpressions() {
		// classe utilitaria
	}
}

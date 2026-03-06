package com.carpass.service_api.exceptions.controllerAdvice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.carpass.service_api.exceptions.ManutencaoNaoEncontradaException;
import com.carpass.service_api.exceptions.RegraDeNegocioException;
import com.carpass.service_api.exceptions.UsuarioNaoEncontradoException;
import com.carpass.service_api.exceptions.VeiculoNaoEncontradoException;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
	
	// Model(Veiculo)
	@ExceptionHandler(VeiculoNaoEncontradoException.class)
	public ResponseEntity<ApiErrorResponse> veiculoNaoEncontrado(VeiculoNaoEncontradoException ex) {
		
		ApiErrorResponse error = new ApiErrorResponse(
				404,
				"Not Found",
				ex.getMessage()
			);
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}
	
	// Model(Usuario)
	@ExceptionHandler(UsuarioNaoEncontradoException.class)
	public ResponseEntity<ApiErrorResponse> usuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
		
		ApiErrorResponse error = new ApiErrorResponse(
				404,
				"Not Found",
				ex.getMessage()
			);
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}
	
	// Model(Manutenção)
	@ExceptionHandler(ManutencaoNaoEncontradaException.class)
	public ResponseEntity<ApiErrorResponse> manutencaoNaoEncontrada(ManutencaoNaoEncontradaException ex) {
		
		ApiErrorResponse error = new ApiErrorResponse(
				404,
				"Not Found",
				ex.getMessage()
			);
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}
	
	// Regra de negócio
	@ExceptionHandler(RegraDeNegocioException.class)
	public ResponseEntity<ApiErrorResponse> regraDeNegocio(RegraDeNegocioException ex) {
		
		ApiErrorResponse error = new ApiErrorResponse(
				400,
				"Bad Request",
				ex.getMessage()
			);
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}
}

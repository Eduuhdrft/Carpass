package com.carpass.service_api.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Tratamento centralizado de erros da API.
 *
 * <p>
 * Regra geral: o cliente recebe sempre um {@link ApiError} com mensagem
 * amigavel; qualquer detalhe tecnico fica apenas no log do servidor.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// ------------------------------------------------------------- 400
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiError> handleBusiness(BusinessException ex, WebRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
	}

	// ------------------------------------------------------------- 401
	@ExceptionHandler({ BadCredentialsException.class })
	public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
		return build(HttpStatus.UNAUTHORIZED, "E-mail ou senha invalidos.", request, null);
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ApiError> handleDisabled(DisabledException ex, WebRequest request) {
		return build(HttpStatus.UNAUTHORIZED, "Esta conta esta desativada. Fale com o administrador da oficina.",
				request, null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, WebRequest request) {
		return build(HttpStatus.UNAUTHORIZED, "Nao foi possivel autenticar a requisicao.", request, null);
	}

	// ------------------------------------------------------------- 403
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
		return build(HttpStatus.FORBIDDEN, "Voce nao tem permissao para executar esta acao.", request, null);
	}

	@ExceptionHandler(ForbiddenOperationException.class)
	public ResponseEntity<ApiError> handleForbiddenOperation(ForbiddenOperationException ex, WebRequest request) {
		return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
	}

	// ------------------------------------------------------------- 404
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
	}

	// ------------------------------------------------------------- 409
	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException ex, WebRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
		log.warn("Violacao de integridade em {}: {}", path(request), ex.getMostSpecificCause().getMessage());
		return build(HttpStatus.CONFLICT,
				"Nao foi possivel concluir a operacao porque ela conflita com dados ja existentes.", request, null);
	}

	// ------------------------------------------------------------- 500
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
		log.error("Erro nao tratado em {}", path(request), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR,
				"Ocorreu um erro inesperado. Tente novamente em alguns instantes.", request, null);
	}

	// ------------------------------------- sobrescritas do handler do Spring
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		List<ApiError.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldDetail).toList();

		ApiError body = new ApiError(LocalDateTime.now(), HttpStatus.UNPROCESSABLE_ENTITY.value(),
				HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(), "Alguns campos precisam de atencao.", path(request),
				details);

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
	}

	/**
	 * Converte para {@link ApiError} tudo que o handler padrao do Spring trataria
	 * sozinho (JSON mal formado, metodo HTTP errado, parametro faltando, rota
	 * inexistente...).
	 *
	 * <p>
	 * Sem isso, parte dos erros sairia no formato {@code ProblemDetail} do Spring e
	 * parte no nosso, obrigando o frontend a entender dois contratos diferentes.
	 * Aqui garantimos um unico formato de erro em toda a API.
	 * </p>
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {

		HttpStatus status = HttpStatus.resolve(statusCode.value()) == null ? HttpStatus.INTERNAL_SERVER_ERROR
				: HttpStatus.valueOf(statusCode.value());

		if (status.is5xxServerError()) {
			log.error("Erro {} em {}", status.value(), path(request), ex);
		} else {
			log.debug("Requisicao rejeitada com {} em {}: {}", status.value(), path(request), ex.getMessage());
		}

		return new ResponseEntity<>(error(status, friendlyMessageFor(ex, status), request), headers, statusCode);
	}

	/** Traduz as excecoes de protocolo do Spring para uma frase util ao usuario. */
	private String friendlyMessageFor(Exception ex, HttpStatus status) {
		if (ex instanceof HttpMessageNotReadableException) {
			return "O corpo da requisicao esta ausente ou nao e um JSON valido.";
		}
		if (ex instanceof HttpRequestMethodNotSupportedException methodError) {
			return "O metodo " + methodError.getMethod() + " nao e aceito neste endereco.";
		}
		if (ex instanceof HttpMediaTypeNotSupportedException) {
			return "Formato de conteudo nao suportado. Envie o corpo como application/json.";
		}
		if (ex instanceof MissingServletRequestParameterException missing) {
			return "O parametro obrigatorio '" + missing.getParameterName() + "' nao foi informado.";
		}
		if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException) {
			return "Recurso nao encontrado.";
		}
		if (status.is5xxServerError()) {
			return "Ocorreu um erro inesperado. Tente novamente em alguns instantes.";
		}
		return "Nao foi possivel processar a requisicao.";
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
		return build(HttpStatus.BAD_REQUEST, "O parametro '" + ex.getName() + "' esta em formato invalido.", request,
				null);
	}

	// -------------------------------------------------------------- apoio
	private ApiError.FieldErrorDetail toFieldDetail(FieldError fieldError) {
		return new ApiError.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest request,
			List<ApiError.FieldErrorDetail> details) {
		return ResponseEntity.status(status).body(error(status, message, request, details));
	}

	private ApiError error(HttpStatus status, String message, WebRequest request) {
		return error(status, message, request, null);
	}

	private ApiError error(HttpStatus status, String message, WebRequest request,
			List<ApiError.FieldErrorDetail> details) {
		return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path(request),
				details);
	}

	private String path(WebRequest request) {
		if (request instanceof ServletWebRequest servletRequest) {
			return servletRequest.getRequest().getRequestURI();
		}
		return null;
	}
}

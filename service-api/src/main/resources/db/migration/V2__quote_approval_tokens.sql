-- ---------------------------------------------------------------------------
-- Aprovacao de orcamento pelo cliente, sem conta no sistema
-- ---------------------------------------------------------------------------
-- O cliente da oficina nao tem login: ele recebe um link por e-mail e decide
-- por ali. O token e a autorizacao, entao ele carrega as mesmas garantias do
-- token de senha -- so o hash e gravado, o uso e unico e a validade e curta.
--
-- O e-mail de destino fica registrado na propria linha, e nao no cadastro do
-- cliente, porque o envio pode ir para um endereco informado apenas naquele
-- momento. Assim o registro guarda para onde a proposta realmente foi.
-- ---------------------------------------------------------------------------

CREATE TABLE quote_approval_tokens
(
    id             uuid         NOT NULL,
    work_order_id  uuid         NOT NULL,
    token_hash     varchar(100) NOT NULL,
    sent_to        varchar(180) NOT NULL,
    expires_at     timestamp    NOT NULL,
    used_at        timestamp,
    decision       varchar(20),
    decided_at     timestamp,
    created_at     timestamp    NOT NULL,
    updated_at     timestamp,
    CONSTRAINT pk_quote_approval_tokens PRIMARY KEY (id),
    CONSTRAINT uk_quote_approval_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_quote_approval_tokens_order FOREIGN KEY (work_order_id)
        REFERENCES work_orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_quote_approval_order ON quote_approval_tokens (work_order_id);

-- ---------------------------------------------------------------------------
-- Motivo da recusa do orcamento
-- ---------------------------------------------------------------------------
-- O motivo ja era gravado na linha do tempo, mas so ali: para descobrir por que
-- o cliente recusou era preciso rolar a ordem inteira ate o historico. Sendo o
-- dado que decide o proximo passo da oficina -- baixar preco, tirar item,
-- ligar para o cliente --, ele precisa estar junto do orcamento.
--
-- Guardar aqui tambem sobrevive a mudancas de etapa posteriores, o que ler do
-- historico nao garantiria: bastaria alguem mover a ordem depois da recusa para
-- o "ultimo evento" deixar de ser a recusa.
-- ---------------------------------------------------------------------------

ALTER TABLE work_orders
    ADD COLUMN quote_rejection_reason varchar(500);

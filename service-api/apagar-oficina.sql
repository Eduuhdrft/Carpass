-- ===========================================================================
--  CarPass — apagar uma oficina de teste e tudo que pertence a ela
-- ===========================================================================
--
--  ATENÇÃO: este script apaga dados de forma permanente. Ele existe para
--  limpar ambiente de desenvolvimento. Em produção o caminho é desativar,
--  nunca apagar — o histórico das ordens de serviço depende dos cadastros.
--
--  Como usar:
--    1. Rode o passo 1 sozinho e anote o id da oficina.
--    2. Troque o valor em "alvo" no passo 2.
--    3. Rode do BEGIN até o COMMIT. Se algo parecer errado antes do COMMIT,
--       digite ROLLBACK; e nada terá sido alterado.
--
--  Por que a ordem importa: users e workshops se referenciam mutuamente.
--  workshops.owner_id é NOT NULL, então a oficina precisa sair antes do dono;
--  users.workshop_id é anulável, então é por ali que o ciclo se quebra.
-- ===========================================================================


-- ---------------------------------------------------------------- PASSO 1
-- Descubra qual oficina você quer remover.

SELECT w.id,
       w.trade_name        AS oficina,
       w.cnpj,
       u.email             AS dono,
       w.created_at
  FROM workshops w
  JOIN users u ON u.id = w.owner_id
 ORDER BY w.created_at;


-- ---------------------------------------------------------------- PASSO 2
-- Troque o UUID abaixo pelo da oficina que sai. Rode tudo daqui até o COMMIT.

BEGIN;

CREATE TEMP TABLE alvo AS
SELECT '00000000-0000-0000-0000-000000000000'::uuid AS id;   -- <<< TROQUE AQUI

-- As contas de acesso precisam ser guardadas agora: no passo 4 o vínculo com
-- a oficina já terá sido desfeito e não haveria mais como identificá-las.
CREATE TEMP TABLE contas AS
SELECT id FROM users WHERE workshop_id IN (SELECT id FROM alvo);


-- ------------------------------------------------- conferência antes de agir
-- Olhe estes números. Se não baterem com a oficina que você quer remover,
-- digite ROLLBACK; e recomece.

SELECT (SELECT trade_name FROM workshops WHERE id IN (SELECT id FROM alvo)) AS oficina,
       (SELECT count(*) FROM contas)                                              AS contas_de_acesso,
       (SELECT count(*) FROM customers   WHERE workshop_id IN (SELECT id FROM alvo)) AS clientes,
       (SELECT count(*) FROM vehicles    WHERE workshop_id IN (SELECT id FROM alvo)) AS veiculos,
       (SELECT count(*) FROM work_orders WHERE workshop_id IN (SELECT id FROM alvo)) AS ordens,
       (SELECT count(*) FROM parts       WHERE workshop_id IN (SELECT id FROM alvo)) AS pecas,
       (SELECT count(*) FROM employees   WHERE workshop_id IN (SELECT id FROM alvo)) AS funcionarios;


-- --------------------------------------------------- PASSO 3: dados da oficina
-- Filho antes de pai. Itens da OS e linha do tempo caem sozinhos por CASCADE,
-- assim como os lançamentos manuais presos ao veículo.

DELETE FROM part_stock_movements WHERE workshop_id IN (SELECT id FROM alvo);
DELETE FROM maintenance_records  WHERE workshop_id IN (SELECT id FROM alvo);
DELETE FROM work_orders          WHERE workshop_id IN (SELECT id FROM alvo);
DELETE FROM vehicles             WHERE workshop_id IN (SELECT id FROM alvo);
DELETE FROM customers            WHERE workshop_id IN (SELECT id FROM alvo);
DELETE FROM parts                WHERE workshop_id IN (SELECT id FROM alvo);
DELETE FROM service_catalog      WHERE workshop_id IN (SELECT id FROM alvo);
DELETE FROM employees            WHERE workshop_id IN (SELECT id FROM alvo);


-- ------------------------------------------- PASSO 4: o ciclo users <-> workshops
-- Solta o vínculo pelo lado anulável, tira a oficina, e só então as contas.
-- Os tokens de redefinição de senha caem por CASCADE junto com o usuário.

UPDATE users SET workshop_id = NULL WHERE workshop_id IN (SELECT id FROM alvo);
DELETE FROM workshops WHERE id IN (SELECT id FROM alvo);
DELETE FROM users     WHERE id IN (SELECT id FROM contas);


-- ------------------------------------------------------ confira e só então feche
SELECT count(*) AS oficinas_restantes FROM workshops;

COMMIT;
-- Se algo saiu errado antes daqui: ROLLBACK;

-- =============================================================================
-- Separa IDENTIDADE de VINCULO.
--
-- Antes: users guardava workshop_id e role na propria linha da pessoa. Como o
-- e-mail e unico no sistema inteiro e a demissao marcava users.active = false,
-- a pessoa demitida ficava impedida de usar o proprio e-mail para sempre --
-- nem para abrir a oficina dela, nem para ser recontratada pela mesma oficina.
--
-- Depois: o vinculo vive em workshop_members. Demitir desativa o vinculo; a
-- identidade continua viva e a pessoa continua entrando com o mesmo e-mail.
-- =============================================================================

CREATE TABLE workshop_members (
    id          uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    workshop_id uuid        NOT NULL,
    role        varchar(40) NOT NULL,
    active      boolean     NOT NULL DEFAULT true,
    created_at  timestamp   NOT NULL,
    updated_at  timestamp,
    CONSTRAINT pk_workshop_members PRIMARY KEY (id),
    -- Um vinculo por par pessoa/oficina. Recontratar reativa a linha existente
    -- em vez de criar uma segunda: duas linhas para a mesma pessoa na mesma
    -- oficina, com papeis diferentes, seria uma ambiguidade sem resposta certa
    -- na hora de decidir o que ela pode fazer.
    CONSTRAINT uk_workshop_members_user_workshop UNIQUE (user_id, workshop_id),
    CONSTRAINT fk_workshop_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_workshop_members_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id)
);

CREATE INDEX idx_workshop_members_user ON workshop_members (user_id);
CREATE INDEX idx_workshop_members_workshop ON workshop_members (workshop_id);

-- ------------------------------------------------------- migracao dos dados
--
-- O ponto delicado: hoje users.active = false significa DUAS coisas que
-- precisam ir para lugares diferentes depois da separacao.
--
--   (a) funcionario demitido      -> o vinculo morre, a identidade vive
--   (b) convidado que nunca aceitou -> o vinculo vive, a identidade so passa a
--                                      valer quando a pessoa definir a senha
--
-- O discriminador entre os dois esta em employees.active: changeStatus()
-- desativa funcionario e conta juntos, enquanto grantSystemAccess() cria a
-- conta inativa sem tocar no funcionario. Entao employee.active = false marca
-- exatamente o caso (a).
--
-- bool_and cobre o caso improvavel de mais de uma linha de employee apontando
-- para a mesma conta na mesma oficina: basta uma demitida para o vinculo nascer
-- desativado. Errar para o lado restritivo aqui e recuperavel com um clique em
-- "reativar"; errar para o outro devolveria acesso a quem foi demitido.
-- Grupo todo nulo (o dono fundador, que nao tem linha em employees) devolve
-- NULL e o COALESCE cai para users.active.
INSERT INTO workshop_members (id, user_id, workshop_id, role, active, created_at)
SELECT gen_random_uuid(),
       u.id,
       u.workshop_id,
       u.role,
       COALESCE(bool_and(e.active), u.active),
       COALESCE(u.created_at, now())
FROM users u
LEFT JOIN employees e ON e.user_id = u.id AND e.workshop_id = u.workshop_id
WHERE u.workshop_id IS NOT NULL
GROUP BY u.id, u.workshop_id, u.role, u.active, u.created_at;

-- Devolve a identidade a quem foi demitido: users.active volta a true quando o
-- unico motivo de estar false era a demissao, que agora mora no vinculo.
-- Quem foi convidado e nunca aceitou fica como esta -- o vinculo dele nasceu
-- ativo, e a conta so passa a valer quando ele definir a senha pelo link.
UPDATE users u
SET active = true
WHERE u.active = false
  AND EXISTS (SELECT 1
              FROM workshop_members m
              WHERE m.user_id = u.id
                AND m.active = false);

-- ------------------------------------------------- papel de plataforma
--
-- SYSTEM_ADMIN nunca foi papel de oficina: ele nao aparece em nenhuma expressao
-- de autorizacao e nao pode ser atribuido pela oficina. Vira uma flag, que diz
-- exatamente o que e -- manter um segundo "role" em users, ao lado do role do
-- vinculo, seria um convite a confundir os dois.
ALTER TABLE users
    ADD COLUMN platform_admin boolean NOT NULL DEFAULT false;

UPDATE users SET platform_admin = true WHERE role = 'SYSTEM_ADMIN';

-- ------------------------------------------------- fim das colunas antigas
ALTER TABLE users DROP CONSTRAINT fk_users_workshop;
DROP INDEX IF EXISTS idx_users_workshop;
ALTER TABLE users DROP COLUMN workshop_id;
ALTER TABLE users DROP COLUMN role;

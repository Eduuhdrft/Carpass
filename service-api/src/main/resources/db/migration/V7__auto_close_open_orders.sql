-- ---------------------------------------------------------------------------
-- Encerramento automatico de ordens paradas
-- ---------------------------------------------------------------------------
-- Uma OS aberta que ninguem toca por dias e quase sempre uma de duas coisas: o
-- cliente sumiu sem responder se faria o servico, ou alguem esqueceu de fechar.
-- Nos dois casos ela fica na lista fingindo trabalho que nao existe, e o painel
-- passa a mentir sobre quanto a oficina tem em maos.
--
-- A coluna guarda depois de quantas horas paradas a ordem e encerrada.
--
-- NULL desliga, e NULL e o padrao -- inclusive para as oficinas que ja existem.
-- Encerrar documento sem a oficina ter pedido e o tipo de automacao que, quando
-- erra uma vez, faz a pessoa deixar de confiar no sistema inteiro. Quem quiser
-- liga na tela da oficina e escolhe o prazo.
--
-- Sem valor padrao e sem NOT NULL de proposito: nenhuma oficina existente muda
-- de comportamento ao aplicar esta migration.
-- ---------------------------------------------------------------------------

ALTER TABLE workshops
    ADD COLUMN auto_close_open_orders_hours integer;

-- Prazo minimo de 1 hora e maximo de 30 dias. O piso evita que um zero digitado
-- por engano encerre tudo que for aberto; o teto evita um numero absurdo que na
-- pratica desligaria a rotina sem dizer que desligou.
ALTER TABLE workshops
    ADD CONSTRAINT ck_workshops_auto_close_hours
        CHECK (auto_close_open_orders_hours IS NULL
            OR (auto_close_open_orders_hours >= 1 AND auto_close_open_orders_hours <= 720));

-- A varredura pergunta "ordens ABERTAS desta oficina paradas desde quando?".
-- Sem o indice ela varre a tabela inteira a cada hora, e a tabela de OS e a que
-- mais cresce no sistema.
CREATE INDEX idx_work_orders_status_updated ON work_orders (workshop_id, status, updated_at);

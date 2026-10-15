-- ---------------------------------------------------------------------------
-- Financeiro: despesas, categorias e quem pode ver
-- ---------------------------------------------------------------------------
-- O painel mostrava um numero so: o faturamento do mes corrente. Faltava o
-- outro lado da conta -- o que sai -- e sem ele nao existe resultado, so
-- receita. Esta migration cria o que sai e a chave de quem enxerga.
--
-- Tres coisas, nesta ordem:
--   1. a coluna que diz se o gerente ve o financeiro;
--   2. o cadastro de categorias, semeado em toda oficina existente;
--   3. a tabela de despesas.
--
-- NOTA sobre a lista padrao: ela tambem existe em
-- `DefaultExpenseCategories.java`, que atende oficina nova. As duas nao se
-- sincronizam de proposito -- migration e registro historico e nao pode mudar
-- depois de aplicada.
--
-- REQUISITO: PostgreSQL 13 ou superior, por gen_random_uuid().
-- ---------------------------------------------------------------------------

-- ------------------------------------------- quem ve o financeiro
-- Nasce true porque o gerente ja via o faturamento do mes no painel antes desta
-- opcao existir. Nascer false tiraria em silencio um acesso que a oficina ja
-- usava, e ninguem entenderia por que o numero sumiu.
ALTER TABLE workshops
    ADD COLUMN manager_can_see_finance boolean NOT NULL DEFAULT true;

-- ------------------------------------------------------ categorias
CREATE TABLE expense_categories
(
    id          uuid        NOT NULL,
    workshop_id uuid        NOT NULL,
    name        varchar(80) NOT NULL,
    description varchar(300),
    active      boolean     NOT NULL DEFAULT true,
    created_at  timestamp   NOT NULL,
    updated_at  timestamp,
    CONSTRAINT pk_expense_categories PRIMARY KEY (id),
    CONSTRAINT fk_expense_categories_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id)
);

CREATE INDEX idx_expense_categories_workshop ON expense_categories (workshop_id);

-- Unicidade ignorando a caixa, como em part_types: e a caixa que faria
-- "Impostos" e "impostos" virarem duas fatias do mesmo grafico.
CREATE UNIQUE INDEX uk_expense_categories_workshop_name
    ON expense_categories (workshop_id, lower(name));

INSERT INTO expense_categories (id, workshop_id, name, description, active, created_at)
SELECT gen_random_uuid(), w.id, c.name, c.description, true, now()
FROM workshops w
         CROSS JOIN (VALUES ('Aluguel', 'Aluguel do imovel da oficina.'),
                            ('Energia eletrica', 'Conta de luz.'),
                            ('Agua', 'Conta de agua e esgoto.'),
                            ('Internet', 'Internet, telefone e servicos de comunicacao.'),
                            ('Funcionarios', 'Salarios, encargos e beneficios da equipe.'),
                            ('Pecas', 'Compra de pecas para revenda ou reposicao de estoque.'),
                            ('Ferramentas', 'Ferramentas manuais e de consumo.'),
                            ('Equipamentos', 'Elevador, compressor, scanner e afins.'),
                            ('Impostos', 'Tributos e taxas.'),
                            ('Fornecedores', 'Pagamentos a fornecedores e servicos terceirizados.'),
                            ('Manutencao', 'Manutencao do imovel, dos equipamentos e da frota.'),
                            ('Marketing', 'Divulgacao, anuncios e material grafico.'),
                            ('Outros', 'O que nao se encaixa nas demais categorias.')) AS c(name, description);

-- -------------------------------------------------------- despesas
CREATE TABLE expenses
(
    id            uuid          NOT NULL,
    workshop_id   uuid          NOT NULL,
    description   varchar(180)  NOT NULL,
    amount        numeric(12,2) NOT NULL,
    expense_date  date          NOT NULL,
    category_id   uuid          NOT NULL,
    notes         varchar(1000),
    created_by_id uuid,
    created_at    timestamp     NOT NULL,
    updated_at    timestamp,
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT fk_expenses_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id),
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES expense_categories (id),
    CONSTRAINT fk_expenses_created_by FOREIGN KEY (created_by_id) REFERENCES users (id),
    -- A validacao do DTO cobre a API; o CHECK cobre qualquer caminho que um dia
    -- escreva direto na tabela. Valor negativo aqui viraria receita disfarcada
    -- de despesa no calculo do liquido.
    CONSTRAINT ck_expenses_amount_positive CHECK (amount > 0)
);

-- A consulta que mais roda e "despesas desta oficina neste periodo": o indice
-- composto atende o filtro e a ordenacao de uma vez.
CREATE INDEX idx_expenses_workshop_date ON expenses (workshop_id, expense_date);
CREATE INDEX idx_expenses_category ON expenses (category_id);

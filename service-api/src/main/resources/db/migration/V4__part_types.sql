-- ---------------------------------------------------------------------------
-- Tipo de peca: "o que a peca e", separado de "de quem ela e"
-- ---------------------------------------------------------------------------
-- Antes existia a coluna `category`, digitada a mao. Digitada a mao, "Filtros",
-- "filtro" e "FILTRO" sao tres coisas para o banco e uma so para quem trabalha
-- -- e nesse ponto nenhum filtro de tela funciona. Como cadastro, cada tipo tem
-- identidade propria, e a pergunta que o estoque precisa responder passa a ter
-- resposta: "quais marcas de oleo de motor eu tenho?".
--
-- Esta migration faz quatro coisas, nesta ordem:
--   1. cria a tabela;
--   2. semeia a lista padrao em toda oficina existente;
--   3. transforma cada categoria ja digitada num tipo e liga as pecas a ele;
--   4. remove a coluna antiga.
--
-- O passo 3 vem depois do 2 e usa ON CONFLICT: se a oficina ja digitava
-- "Filtro", a semente e a categoria colidem, e o certo e manter uma linha so --
-- senao o cadastro nasceria com duplicata no primeiro dia.
--
-- NOTA sobre a lista padrao: ela tambem existe em `DefaultPartTypes.java`, que
-- atende oficina nova. As duas nao se sincronizam de proposito -- migration e
-- registro historico e nao pode mudar depois de aplicada.
--
-- REQUISITO: PostgreSQL 13 ou superior, por causa de gen_random_uuid(), que
-- deixou de precisar da extensao pgcrypto nessa versao.
-- ---------------------------------------------------------------------------

CREATE TABLE part_types
(
    id          uuid         NOT NULL,
    workshop_id uuid         NOT NULL,
    name        varchar(80)  NOT NULL,
    description varchar(300),
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamp    NOT NULL,
    updated_at  timestamp,
    CONSTRAINT pk_part_types PRIMARY KEY (id),
    CONSTRAINT fk_part_types_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id)
);

CREATE INDEX idx_part_types_workshop ON part_types (workshop_id);

-- Unicidade por oficina ignorando a caixa. Como indice de expressao, e nao como
-- UNIQUE simples: e a caixa que reabriria o problema que o tipo veio resolver.
CREATE UNIQUE INDEX uk_part_types_workshop_name ON part_types (workshop_id, lower(name));

-- ------------------------------------------------------- lista padrao
-- Um INSERT ... SELECT cruzando as oficinas com a lista: toda oficina que ja
-- existe recebe o mesmo conjunto que uma oficina nova receberia.
INSERT INTO part_types (id, workshop_id, name, description, active, created_at)
SELECT gen_random_uuid(), w.id, t.name, t.description, true, now()
FROM workshops w
         CROSS JOIN (VALUES ('Oleo de motor', 'Lubrificante do motor, mineral, semissintetico ou sintetico.'),
                            ('Oleo de cambio', 'Lubrificante da caixa de cambio, manual ou automatica.'),
                            ('Oleo hidraulico', 'Fluido do sistema hidraulico, inclusive direcao.'),
                            ('Fluido de freio', 'Fluido do sistema de freio (DOT3, DOT4, DOT5.1).'),
                            ('Aditivo', 'Aditivo de radiador, de combustivel e afins.'),
                            ('Filtro', 'Filtro de oleo, de ar, de combustivel e de cabine.'),
                            ('Vela', 'Vela de ignicao.'),
                            ('Bobina', 'Bobina de ignicao.'),
                            ('Bateria', 'Bateria automotiva.'),
                            ('Correia', 'Correia dentada, do alternador e tensores.'),
                            ('Pastilha e disco de freio', 'Componentes de atrito do sistema de freio.'),
                            ('Suspensao', 'Amortecedor, mola, batente, bandeja e buchas.'),
                            ('Embreagem', 'Kit de embreagem, disco, plato e atuador.'),
                            ('Sensor', 'Sensores de motor, freio e cambio.'),
                            ('Lampada', 'Lampadas de farol, lanterna e painel.'),
                            ('Pneu', 'Pneus e camaras.'),
                            ('Eletrica', 'Fusiveis, reles, chicotes e conectores.'),
                            ('Arrefecimento', 'Radiador, bomba d''agua, valvula termostatica e mangueiras.'),
                            ('Escapamento', 'Silencioso, catalisador e coletores.'),
                            ('Junta e retentor', 'Juntas, retentores e vedacoes.'),
                            ('Diversos', 'O que nao se encaixa nos demais tipos.')) AS t(name, description);

-- --------------------------------------------------- coluna nas pecas
ALTER TABLE parts
    ADD COLUMN part_type_id uuid;

ALTER TABLE parts
    ADD CONSTRAINT fk_parts_part_type FOREIGN KEY (part_type_id) REFERENCES part_types (id);

CREATE INDEX idx_parts_type ON parts (part_type_id);
CREATE INDEX idx_parts_manufacturer ON parts (workshop_id, manufacturer);

-- ------------------------------------- categorias que a oficina ja usava
-- Nada digitado se perde: cada categoria vira um tipo da propria oficina. O
-- ON CONFLICT cobre o caso em que a categoria coincide com uma semente.
INSERT INTO part_types (id, workshop_id, name, description, active, created_at)
SELECT gen_random_uuid(), p.workshop_id, p.category, 'Convertido da categoria antiga.', true, now()
FROM (SELECT DISTINCT ON (workshop_id, lower(trim(category))) workshop_id, trim(category) AS category
      FROM parts
      WHERE category IS NOT NULL
        AND trim(category) <> ''
      ORDER BY workshop_id, lower(trim(category)), trim(category)) p
ON CONFLICT (workshop_id, lower(name)) DO NOTHING;

UPDATE parts p
SET part_type_id = t.id
FROM part_types t
WHERE t.workshop_id = p.workshop_id
  AND p.category IS NOT NULL
  AND lower(trim(p.category)) = lower(t.name);

ALTER TABLE parts
    DROP COLUMN category;

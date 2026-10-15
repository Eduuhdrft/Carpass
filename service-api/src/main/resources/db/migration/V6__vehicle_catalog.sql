-- ---------------------------------------------------------------------------
-- Veiculo padronizado pela tabela FIPE
-- ---------------------------------------------------------------------------
-- Marca e modelo eram texto livre, e por isso o mesmo carro entrava como "Gol",
-- "gol g6" e "VW Gol 1.6" -- tres registros para quem busca por modelo e um so
-- para quem trabalha. Agora o cadastro pode escolher da FIPE, e estas duas
-- colunas guardam de onde veio.
--
-- As colunas de marca e modelo continuam existindo e continuam sendo a verdade
-- exibida. Isso e de proposito:
--   * o cadastro manual continua valendo (carro antigo, importado raro ou
--     adaptado simplesmente nao esta na FIPE);
--   * um veiculo ja cadastrado nao muda de nome porque a FIPE mudou o dela.
--
-- Nada e obrigatorio e nada e convertido: os veiculos que ja existem seguem
-- exatamente como estao, sem tipo e sem codigo.
-- ---------------------------------------------------------------------------

ALTER TABLE vehicles
    ADD COLUMN vehicle_kind varchar(20);

ALTER TABLE vehicles
    ADD COLUMN fipe_code varchar(20);

-- Indice pelo codigo FIPE: e por ele que uma futura consulta de valor de
-- mercado vai agrupar os veiculos iguais da carteira.
CREATE INDEX idx_vehicles_fipe_code ON vehicles (fipe_code);

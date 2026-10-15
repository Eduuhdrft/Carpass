-- A logo passa a ser guardada como imagem (texto em base64), nao como link
-- para um arquivo hospedado em outro lugar. O campo varchar(500) foi pensado
-- para uma URL; uma imagem pequena em base64 ja estoura esse tamanho, entao a
-- coluna vira text. O nome continua logo_url por compatibilidade com o codigo
-- existente -- o valor guardado passa a ser uma data URI, mas semanticamente
-- ainda e "de onde a folha busca a logo".
alter table workshops
    alter column logo_url type text;

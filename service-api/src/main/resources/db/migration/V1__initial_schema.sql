-- =============================================================================
-- CarPass - schema inicial do painel de oficinas
--
-- Convencoes adotadas:
--   * chave primaria uuid em todas as tabelas;
--   * created_at / updated_at em todas as tabelas (preenchidos pelo Hibernate);
--   * todo dado operacional carrega workshop_id, que e a chave do isolamento
--     entre oficinas (multi-tenancy por coluna discriminadora);
--   * exclusao restrita por padrao, para nao perder historico por acidente.
-- =============================================================================

-- ---------------------------------------------------------------- usuarios
CREATE TABLE users (
    id           uuid         NOT NULL,
    first_name   varchar(80)  NOT NULL,
    last_name    varchar(80),
    email        varchar(180) NOT NULL,
    password     varchar(100) NOT NULL,
    phone        varchar(30),
    role         varchar(40)  NOT NULL,
    workshop_id  uuid,
    active       boolean      NOT NULL DEFAULT true,
    created_at   timestamp    NOT NULL,
    updated_at   timestamp,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- ---------------------------------------------------------------- oficinas
CREATE TABLE workshops (
    id             uuid         NOT NULL,
    trade_name     varchar(150) NOT NULL,
    corporate_name varchar(150),
    cnpj           varchar(18)  NOT NULL,
    phone          varchar(30),
    email          varchar(180),
    logo_url       varchar(500),
    zip_code       varchar(10),
    street         varchar(180),
    street_number  varchar(20),
    complement     varchar(120),
    neighborhood   varchar(120),
    city           varchar(120),
    state          varchar(2),
    active         boolean      NOT NULL DEFAULT true,
    owner_id       uuid         NOT NULL,
    created_at     timestamp    NOT NULL,
    updated_at     timestamp,
    CONSTRAINT pk_workshops PRIMARY KEY (id),
    CONSTRAINT uk_workshops_cnpj UNIQUE (cnpj),
    CONSTRAINT fk_workshops_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

-- users e workshops se referenciam mutuamente, por isso a FK vem depois.
ALTER TABLE users
    ADD CONSTRAINT fk_users_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id);

CREATE INDEX idx_users_workshop ON users (workshop_id);

-- ------------------------------------------------------ tokens de senha
CREATE TABLE password_reset_tokens (
    id         uuid         NOT NULL,
    user_id    uuid         NOT NULL,
    token_hash varchar(100) NOT NULL,
    expires_at timestamp    NOT NULL,
    used_at    timestamp,
    created_at timestamp    NOT NULL,
    updated_at timestamp,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens (user_id);

-- ----------------------------------------------------------- funcionarios
CREATE TABLE employees (
    id          uuid         NOT NULL,
    workshop_id uuid         NOT NULL,
    user_id     uuid,
    name        varchar(150) NOT NULL,
    role        varchar(40)  NOT NULL,
    specialty   varchar(120),
    phone       varchar(30),
    email       varchar(180),
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamp    NOT NULL,
    updated_at  timestamp,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT fk_employees_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id),
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_employees_workshop ON employees (workshop_id);

-- --------------------------------------------------------------- clientes
CREATE TABLE customers (
    id            uuid         NOT NULL,
    workshop_id   uuid         NOT NULL,
    type          varchar(20)  NOT NULL,
    name          varchar(180) NOT NULL,
    document      varchar(20),
    phone         varchar(30),
    email         varchar(180),
    zip_code      varchar(10),
    street        varchar(180),
    street_number varchar(20),
    complement    varchar(120),
    neighborhood  varchar(120),
    city          varchar(120),
    state         varchar(2),
    notes         varchar(1000),
    active        boolean      NOT NULL DEFAULT true,
    created_at    timestamp    NOT NULL,
    updated_at    timestamp,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_workshop_document UNIQUE (workshop_id, document),
    CONSTRAINT fk_customers_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id)
);

CREATE INDEX idx_customers_workshop ON customers (workshop_id);
CREATE INDEX idx_customers_name ON customers (name);

-- --------------------------------------------------------------- veiculos
CREATE TABLE vehicles (
    id                uuid         NOT NULL,
    workshop_id       uuid         NOT NULL,
    customer_id       uuid         NOT NULL,
    plate             varchar(10)  NOT NULL,
    brand             varchar(80)  NOT NULL,
    model             varchar(120) NOT NULL,
    manufacture_year  integer,
    model_year        integer,
    mileage           integer,
    fuel_type         varchar(20),
    chassis           varchar(30),
    renavam           varchar(20),
    color             varchar(40),
    notes             varchar(1000),
    active            boolean      NOT NULL DEFAULT true,
    created_at        timestamp    NOT NULL,
    updated_at        timestamp,
    CONSTRAINT pk_vehicles PRIMARY KEY (id),
    CONSTRAINT uk_vehicles_workshop_plate UNIQUE (workshop_id, plate),
    CONSTRAINT fk_vehicles_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id),
    CONSTRAINT fk_vehicles_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_vehicles_workshop ON vehicles (workshop_id);
CREATE INDEX idx_vehicles_customer ON vehicles (customer_id);

-- ------------------------------------------------------ catalogo de servicos
CREATE TABLE service_catalog (
    id                uuid          NOT NULL,
    workshop_id       uuid          NOT NULL,
    name              varchar(150)  NOT NULL,
    description       varchar(1000),
    default_price     numeric(12,2),
    estimated_minutes integer,
    active            boolean       NOT NULL DEFAULT true,
    created_at        timestamp     NOT NULL,
    updated_at        timestamp,
    CONSTRAINT pk_service_catalog PRIMARY KEY (id),
    CONSTRAINT fk_service_catalog_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id)
);

CREATE INDEX idx_service_catalog_workshop ON service_catalog (workshop_id);

-- ------------------------------------------------------------------ pecas
CREATE TABLE parts (
    id             uuid          NOT NULL,
    workshop_id    uuid          NOT NULL,
    code           varchar(60),
    name           varchar(150)  NOT NULL,
    manufacturer   varchar(120),
    category       varchar(80),
    cost_price     numeric(12,2),
    sale_price     numeric(12,2),
    stock_quantity integer       NOT NULL DEFAULT 0,
    minimum_stock  integer       NOT NULL DEFAULT 0,
    active         boolean       NOT NULL DEFAULT true,
    created_at     timestamp     NOT NULL,
    updated_at     timestamp,
    CONSTRAINT pk_parts PRIMARY KEY (id),
    CONSTRAINT uk_parts_workshop_code UNIQUE (workshop_id, code),
    CONSTRAINT fk_parts_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id),
    CONSTRAINT ck_parts_stock_non_negative CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_parts_workshop ON parts (workshop_id);
CREATE INDEX idx_parts_name ON parts (name);

-- ----------------------------------------------------- ordens de servico
CREATE TABLE work_orders (
    id                      uuid          NOT NULL,
    workshop_id             uuid          NOT NULL,
    number                  integer       NOT NULL,
    customer_id             uuid          NOT NULL,
    vehicle_id              uuid          NOT NULL,
    assigned_mechanic_id    uuid,
    status                  varchar(40)   NOT NULL,
    quote_status            varchar(20)   NOT NULL,
    reported_problem        varchar(2000),
    diagnosis               varchar(2000),
    notes                   varchar(2000),
    mileage                 integer,
    discount                numeric(12,2),
    parts_total             numeric(12,2),
    services_total          numeric(12,2),
    total_amount            numeric(12,2),
    estimated_delivery_date date,
    opened_at               timestamp     NOT NULL,
    approved_at             timestamp,
    finished_at             timestamp,
    delivered_at            timestamp,
    canceled_at             timestamp,
    created_by_id           uuid,
    created_at              timestamp     NOT NULL,
    updated_at              timestamp,
    CONSTRAINT pk_work_orders PRIMARY KEY (id),
    CONSTRAINT uk_work_orders_workshop_number UNIQUE (workshop_id, number),
    CONSTRAINT fk_work_orders_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id),
    CONSTRAINT fk_work_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_work_orders_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT fk_work_orders_mechanic FOREIGN KEY (assigned_mechanic_id) REFERENCES employees (id),
    CONSTRAINT fk_work_orders_created_by FOREIGN KEY (created_by_id) REFERENCES users (id)
);

CREATE INDEX idx_work_orders_workshop ON work_orders (workshop_id);
CREATE INDEX idx_work_orders_status ON work_orders (status);
CREATE INDEX idx_work_orders_vehicle ON work_orders (vehicle_id);

CREATE TABLE work_order_items (
    id                 uuid          NOT NULL,
    work_order_id      uuid,
    type               varchar(20)   NOT NULL,
    part_id            uuid,
    service_catalog_id uuid,
    description        varchar(300)  NOT NULL,
    quantity           integer       NOT NULL,
    unit_price         numeric(12,2) NOT NULL,
    total_price        numeric(12,2) NOT NULL,
    created_at         timestamp     NOT NULL,
    updated_at         timestamp,
    CONSTRAINT pk_work_order_items PRIMARY KEY (id),
    CONSTRAINT fk_work_order_items_order FOREIGN KEY (work_order_id) REFERENCES work_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_work_order_items_part FOREIGN KEY (part_id) REFERENCES parts (id),
    CONSTRAINT fk_work_order_items_service FOREIGN KEY (service_catalog_id) REFERENCES service_catalog (id),
    CONSTRAINT ck_work_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_work_order_items_order ON work_order_items (work_order_id);

CREATE TABLE work_order_status_history (
    id             uuid        NOT NULL,
    work_order_id  uuid        NOT NULL,
    from_status    varchar(40),
    to_status      varchar(40) NOT NULL,
    changed_by_id  uuid,
    note           varchar(500),
    created_at     timestamp   NOT NULL,
    updated_at     timestamp,
    CONSTRAINT pk_work_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_wo_status_history_order FOREIGN KEY (work_order_id) REFERENCES work_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_wo_status_history_user FOREIGN KEY (changed_by_id) REFERENCES users (id)
);

CREATE INDEX idx_wo_status_history_order ON work_order_status_history (work_order_id);

-- ------------------------------------------------- movimentacao de estoque
CREATE TABLE part_stock_movements (
    id              uuid        NOT NULL,
    workshop_id     uuid        NOT NULL,
    part_id         uuid        NOT NULL,
    work_order_id   uuid,
    type            varchar(20) NOT NULL,
    quantity        integer     NOT NULL,
    previous_stock  integer,
    resulting_stock integer,
    reason          varchar(300),
    created_by_id   uuid,
    created_at      timestamp   NOT NULL,
    updated_at      timestamp,
    CONSTRAINT pk_part_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id),
    CONSTRAINT fk_stock_movements_part FOREIGN KEY (part_id) REFERENCES parts (id),
    CONSTRAINT fk_stock_movements_order FOREIGN KEY (work_order_id) REFERENCES work_orders (id),
    CONSTRAINT fk_stock_movements_user FOREIGN KEY (created_by_id) REFERENCES users (id)
);

CREATE INDEX idx_stock_movements_part ON part_stock_movements (part_id);

-- ------------------------------------------------- historico dos veiculos
CREATE TABLE maintenance_records (
    id            uuid          NOT NULL,
    workshop_id   uuid          NOT NULL,
    vehicle_id    uuid          NOT NULL,
    work_order_id uuid,
    service_date  date          NOT NULL,
    mileage       integer,
    description   varchar(500)  NOT NULL,
    performed_by  varchar(150),
    amount        numeric(12,2),
    notes         varchar(1000),
    created_at    timestamp     NOT NULL,
    updated_at    timestamp,
    CONSTRAINT pk_maintenance_records PRIMARY KEY (id),
    CONSTRAINT fk_maintenance_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id),
    CONSTRAINT fk_maintenance_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_order FOREIGN KEY (work_order_id) REFERENCES work_orders (id)
);

CREATE INDEX idx_maintenance_vehicle ON maintenance_records (vehicle_id);

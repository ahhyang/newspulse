-- NewsPulse canonical schema. Hibernate ddl-auto is validate-only; Flyway owns DDL.

CREATE TABLE topics (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    query           VARCHAR(255) NOT NULL,
    description     VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_topics_name UNIQUE (name)
);

CREATE TABLE story_clusters (
    id                  BIGSERIAL PRIMARY KEY,
    topic_id            BIGINT NOT NULL REFERENCES topics (id),
    canonical_title     VARCHAR(500) NOT NULL,
    canonical_summary   TEXT,
    article_count       INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_story_clusters_topic ON story_clusters (topic_id);

CREATE TABLE articles (
    id              BIGSERIAL PRIMARY KEY,
    topic_id        BIGINT NOT NULL REFERENCES topics (id),
    cluster_id      BIGINT REFERENCES story_clusters (id),
    title           VARCHAR(1000) NOT NULL,
    url             VARCHAR(2048) NOT NULL,
    url_hash        CHAR(64) NOT NULL,
    source          VARCHAR(80) NOT NULL,
    source_name     VARCHAR(160) NOT NULL,
    published_at    TIMESTAMPTZ,
    raw_content     TEXT,
    content_hash    CHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_articles_url_hash UNIQUE (url_hash)
);

CREATE INDEX idx_articles_topic_published ON articles (topic_id, published_at DESC);
CREATE INDEX idx_articles_source ON articles (source);
CREATE INDEX idx_articles_cluster ON articles (cluster_id);

CREATE TABLE article_enrichments (
    id                          BIGSERIAL PRIMARY KEY,
    article_id                  BIGINT NOT NULL UNIQUE REFERENCES articles (id) ON DELETE CASCADE,
    summary                     TEXT NOT NULL,
    sentiment                   VARCHAR(16) NOT NULL,
    sentiment_justification     VARCHAR(500) NOT NULL,
    stance_tag                  VARCHAR(64),
    model                       VARCHAR(120) NOT NULL,
    enriched_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_enrichment_sentiment CHECK (sentiment IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE'))
);

CREATE INDEX idx_enrichments_sentiment ON article_enrichments (sentiment);

CREATE TABLE digests (
    id              BIGSERIAL PRIMARY KEY,
    topic_id        BIGINT NOT NULL REFERENCES topics (id),
    digest_date     DATE NOT NULL,
    headline        VARCHAR(500) NOT NULL,
    overview        TEXT NOT NULL,
    positive_pct    NUMERIC(5, 2) NOT NULL DEFAULT 0,
    neutral_pct     NUMERIC(5, 2) NOT NULL DEFAULT 0,
    negative_pct    NUMERIC(5, 2) NOT NULL DEFAULT 0,
    generated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_digests_topic_date UNIQUE (topic_id, digest_date)
);

CREATE TABLE digest_items (
    id              BIGSERIAL PRIMARY KEY,
    digest_id       BIGINT NOT NULL REFERENCES digests (id) ON DELETE CASCADE,
    cluster_id      BIGINT REFERENCES story_clusters (id),
    rank            INT NOT NULL,
    title           VARCHAR(500) NOT NULL,
    summary         TEXT NOT NULL,
    source_count    INT NOT NULL DEFAULT 1,
    sentiment       VARCHAR(16),
    CONSTRAINT uk_digest_items_rank UNIQUE (digest_id, rank)
);

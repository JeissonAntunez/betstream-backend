-- ============================================================
-- BetStream - Supabase PostgreSQL Schema
-- Run this in your Supabase SQL Editor
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─────────────────────────────────────────────────────────────
-- USERS
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username        VARCHAR(30)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    active          BOOLEAN      NOT NULL DEFAULT true,
    email_verified  BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- ─────────────────────────────────────────────────────────────
-- SPORT EVENTS
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sport_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    home_team       VARCHAR(100) NOT NULL,
    away_team       VARCHAR(100) NOT NULL,
    sport           VARCHAR(50)  NOT NULL,   -- FOOTBALL, BASKETBALL, TENNIS...
    league          VARCHAR(100) NOT NULL,
    country         VARCHAR(100) NOT NULL DEFAULT '',
    start_time      TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED'
                        CHECK (status IN ('SCHEDULED','LIVE','FINISHED','CANCELLED')),
    home_score      INT          DEFAULT 0,
    away_score      INT          DEFAULT 0,
    current_minute  VARCHAR(10)  DEFAULT '',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_status     ON sport_events(status);
CREATE INDEX idx_events_sport      ON sport_events(sport);
CREATE INDEX idx_events_start_time ON sport_events(start_time);
CREATE INDEX idx_events_live       ON sport_events(status, start_time) WHERE status = 'LIVE';

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_events_updated_at
    BEFORE UPDATE ON sport_events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ─────────────────────────────────────────────────────────────
-- ODDS
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS odds (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id    UUID         NOT NULL REFERENCES sport_events(id) ON DELETE CASCADE,
    market      VARCHAR(50)  NOT NULL,   -- MATCH_WINNER, OVER_UNDER_2_5, BTTS...
    selection   VARCHAR(50)  NOT NULL,   -- HOME, DRAW, AWAY, YES, NO, OVER...
    value       DECIMAL(8,2) NOT NULL CHECK (value >= 1.01),
    active      BOOLEAN      NOT NULL DEFAULT true,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, market, selection)
);

CREATE INDEX idx_odds_event_id ON odds(event_id);
CREATE INDEX idx_odds_active   ON odds(event_id, active) WHERE active = true;

CREATE TRIGGER trg_odds_updated_at
    BEFORE UPDATE ON odds
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ─────────────────────────────────────────────────────────────
-- BETS
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bets (
    id              UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID          NOT NULL REFERENCES users(id),
    event_id        UUID          NOT NULL REFERENCES sport_events(id),
    market          VARCHAR(50)   NOT NULL,
    selection       VARCHAR(50)   NOT NULL,
    odds_value      DECIMAL(8,2)  NOT NULL,
    stake           DECIMAL(12,2) NOT NULL CHECK (stake >= 0.50),
    potential_win   DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','WON','LOST','CANCELLED','CASHED_OUT')),
    settled_amount  DECIMAL(12,2) DEFAULT NULL,
    placed_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    settled_at      TIMESTAMPTZ   DEFAULT NULL
);

CREATE INDEX idx_bets_user_id    ON bets(user_id, placed_at DESC);
CREATE INDEX idx_bets_event_id   ON bets(event_id, status);
CREATE INDEX idx_bets_pending    ON bets(status) WHERE status = 'PENDING';

-- ─────────────────────────────────────────────────────────────
-- WALLET TRANSACTIONS (audit trail)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id              UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID          NOT NULL REFERENCES users(id),
    type            VARCHAR(30)   NOT NULL
                        CHECK (type IN ('DEPOSIT','WITHDRAWAL','BET_PLACED','BET_WIN','BET_REFUND')),
    amount          DECIMAL(12,2) NOT NULL,
    balance_after   DECIMAL(12,2) NOT NULL,
    reference_id    UUID          DEFAULT NULL,  -- bet_id or external ref
    description     VARCHAR(255)  DEFAULT '',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallet_user_id ON wallet_transactions(user_id, created_at DESC);

-- ─────────────────────────────────────────────────────────────
-- SEED DATA - Sample events for development
-- ─────────────────────────────────────────────────────────────
INSERT INTO sport_events (home_team, away_team, sport, league, country, start_time, status, home_score, away_score, current_minute)
VALUES
    ('Real Madrid',       'Dep. Alavés',    'FOOTBALL',   'LaLiga',     'España',    NOW() + INTERVAL '30 min', 'LIVE',      2, 0, '88'),
    ('Athletic Bilbao',   'CA Osasuna',     'FOOTBALL',   'LaLiga',     'España',    NOW() + INTERVAL '1 hour', 'LIVE',      1, 1, '34'),
    ('Inter de Milán',    'Como 1907',      'FOOTBALL',   'Coppa Italia','Italia',   NOW() + INTERVAL '2 hour', 'SCHEDULED', 0, 0, ''),
    ('Barcelona',         'Sevilla FC',     'FOOTBALL',   'LaLiga',     'España',    NOW() + INTERVAL '3 hour', 'SCHEDULED', 0, 0, ''),
    ('Manchester City',   'Arsenal',        'FOOTBALL',   'Premier League','Inglaterra', NOW() + INTERVAL '4 hour', 'SCHEDULED', 0, 0, ''),
    ('Los Angeles Lakers','Golden State',   'BASKETBALL', 'NBA',        'EEUU',      NOW() + INTERVAL '5 hour', 'SCHEDULED', 0, 0, ''),
    ('Carlos Alcaraz',    'Novak Djokovic', 'TENNIS',     'ATP Masters', 'España',   NOW() + INTERVAL '6 hour', 'SCHEDULED', 0, 0, '')
ON CONFLICT DO NOTHING;

-- Seed odds for each event
INSERT INTO odds (event_id, market, selection, value)
SELECT
    e.id,
    m.market,
    m.selection,
    m.value
FROM sport_events e
CROSS JOIN (VALUES
    ('MATCH_WINNER', 'HOME',      1.27),
    ('MATCH_WINNER', 'DRAW',      6.50),
    ('MATCH_WINNER', 'AWAY',     12.00),
    ('OVER_UNDER_2_5', 'OVER_2_5',  1.75),
    ('OVER_UNDER_2_5', 'UNDER_2_5', 2.05),
    ('BTTS', 'YES', 2.10),
    ('BTTS', 'NO',  1.70)
) AS m(market, selection, value)
ON CONFLICT (event_id, market, selection) DO NOTHING;

-- ─────────────────────────────────────────────────────────────
-- ROW LEVEL SECURITY (RLS) - Production safety
-- ─────────────────────────────────────────────────────────────
ALTER TABLE users              ENABLE ROW LEVEL SECURITY;
ALTER TABLE bets               ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallet_transactions ENABLE ROW LEVEL SECURITY;

-- Users can only read/update their own data
CREATE POLICY users_own_data ON users
    FOR ALL USING (id::text = current_setting('app.user_id', true));

-- Users can only see their own bets
CREATE POLICY bets_own_data ON bets
    FOR ALL USING (user_id::text = current_setting('app.user_id', true));

-- Users can only see their own transactions
CREATE POLICY wallet_own_data ON wallet_transactions
    FOR ALL USING (user_id::text = current_setting('app.user_id', true));

-- Events and odds are public reads
ALTER TABLE sport_events ENABLE ROW LEVEL SECURITY;
CREATE POLICY events_public_read ON sport_events FOR SELECT USING (true);

ALTER TABLE odds ENABLE ROW LEVEL SECURITY;
CREATE POLICY odds_public_read ON odds FOR SELECT USING (true);

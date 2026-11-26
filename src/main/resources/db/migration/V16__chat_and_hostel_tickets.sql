CREATE TABLE IF NOT EXISTS chat_conversations (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_message_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (property_id, customer_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_conv_customer ON chat_conversations (customer_id, last_message_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_conv_owner ON chat_conversations (owner_id, last_message_at DESC);

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES chat_conversations (id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sender_role VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    read_by_recipient BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_msg_conv ON chat_messages (conversation_id, created_at ASC);

CREATE TABLE IF NOT EXISTS hostel_tickets (
    id UUID PRIMARY KEY,
    ticket_reference VARCHAR(40) NOT NULL UNIQUE,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category VARCHAR(40) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    owner_resolution TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hostel_tickets_owner ON hostel_tickets (owner_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_hostel_tickets_customer ON hostel_tickets (customer_id, created_at DESC);

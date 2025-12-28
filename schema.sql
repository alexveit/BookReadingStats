-- ============================================
-- Book Reading Stats - Supabase Schema
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- BOOKS TABLE
-- ============================================
CREATE TABLE books (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    total_pages INTEGER NOT NULL,
    current_page INTEGER NOT NULL DEFAULT 0,
    notes TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for faster user queries
CREATE INDEX idx_books_user_id ON books(user_id);

-- ============================================
-- READING SESSIONS TABLE
-- ============================================
CREATE TABLE reading_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    duration_seconds INTEGER NOT NULL,
    start_page INTEGER NOT NULL,
    end_page INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for faster queries
CREATE INDEX idx_sessions_user_id ON reading_sessions(user_id);
CREATE INDEX idx_sessions_book_id ON reading_sessions(book_id);
CREATE INDEX idx_sessions_start_time ON reading_sessions(start_time DESC);

-- ============================================
-- ROW LEVEL SECURITY (RLS)
-- Users can only access their own data
-- ============================================

-- Enable RLS on tables
ALTER TABLE books ENABLE ROW LEVEL SECURITY;
ALTER TABLE reading_sessions ENABLE ROW LEVEL SECURITY;

-- Books policies
CREATE POLICY "Users can view own books" 
    ON books FOR SELECT 
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own books" 
    ON books FOR INSERT 
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own books" 
    ON books FOR UPDATE 
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own books" 
    ON books FOR DELETE 
    USING (auth.uid() = user_id);

-- Reading sessions policies
CREATE POLICY "Users can view own sessions" 
    ON reading_sessions FOR SELECT 
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own sessions" 
    ON reading_sessions FOR INSERT 
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own sessions" 
    ON reading_sessions FOR UPDATE 
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own sessions" 
    ON reading_sessions FOR DELETE 
    USING (auth.uid() = user_id);

-- ============================================
-- UPDATED_AT TRIGGER
-- Automatically updates the updated_at column
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_books_updated_at
    BEFORE UPDATE ON books
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- HELPER VIEW: Books with stats
-- ============================================
CREATE OR REPLACE VIEW books_with_stats AS
SELECT 
    b.*,
    COALESCE(COUNT(rs.id), 0) AS session_count,
    COALESCE(SUM(rs.duration_seconds), 0) AS total_reading_seconds,
    COALESCE(MAX(rs.end_page), b.current_page) AS furthest_page
FROM books b
LEFT JOIN reading_sessions rs ON rs.book_id = b.id
GROUP BY b.id;

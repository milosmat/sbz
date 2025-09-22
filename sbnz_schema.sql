--
-- PostgreSQL database dump
--

-- Dumped from database version 17.2
-- Dumped by pg_dump version 17.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.posts DROP CONSTRAINT IF EXISTS posts_author_id_fkey;
DROP INDEX IF EXISTS public.uq_users_email_lower;
DROP INDEX IF EXISTS public.idx_posts_hashtags_gin;
DROP INDEX IF EXISTS public.idx_posts_created_at;
DROP INDEX IF EXISTS public.idx_places_name;
DROP INDEX IF EXISTS public.idx_places_hashtags_gin;
DROP INDEX IF EXISTS public.idx_mre_ts;
DROP INDEX IF EXISTS public.idx_mre_rep;
DROP INDEX IF EXISTS public.idx_mre_post;
DROP INDEX IF EXISTS public.idx_mre_auth;
DROP INDEX IF EXISTS public.idx_mf_user;
DROP INDEX IF EXISTS public.idx_mbe_ts;
DROP INDEX IF EXISTS public.idx_mbe_target;
DROP INDEX IF EXISTS public.idx_mbe_block;
DROP INDEX IF EXISTS public.idx_friendships_user_lo;
DROP INDEX IF EXISTS public.idx_friendships_user_hi;
DROP INDEX IF EXISTS public.idx_blocks_blocker;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY public.posts DROP CONSTRAINT IF EXISTS posts_pkey;
ALTER TABLE IF EXISTS ONLY public.post_bans DROP CONSTRAINT IF EXISTS post_bans_pkey;
ALTER TABLE IF EXISTS ONLY public.places DROP CONSTRAINT IF EXISTS places_pkey;
ALTER TABLE IF EXISTS ONLY public.post_reports DROP CONSTRAINT IF EXISTS pk_post_reports;
ALTER TABLE IF EXISTS ONLY public.post_likes DROP CONSTRAINT IF EXISTS pk_post_likes;
ALTER TABLE IF EXISTS ONLY public.friendships DROP CONSTRAINT IF EXISTS pk_friendships;
ALTER TABLE IF EXISTS ONLY public.blocks DROP CONSTRAINT IF EXISTS pk_blocks;
ALTER TABLE IF EXISTS ONLY public.moderation_report_events DROP CONSTRAINT IF EXISTS moderation_report_events_pkey;
ALTER TABLE IF EXISTS ONLY public.moderation_flags DROP CONSTRAINT IF EXISTS moderation_flags_pkey;
ALTER TABLE IF EXISTS ONLY public.moderation_flags_audit DROP CONSTRAINT IF EXISTS moderation_flags_audit_pkey;
ALTER TABLE IF EXISTS ONLY public.moderation_block_events DROP CONSTRAINT IF EXISTS moderation_block_events_pkey;
ALTER TABLE IF EXISTS ONLY public.login_bans DROP CONSTRAINT IF EXISTS login_bans_pkey;
ALTER TABLE IF EXISTS ONLY public.admins DROP CONSTRAINT IF EXISTS admins_pkey;
ALTER TABLE IF EXISTS public.moderation_report_events ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.moderation_flags_audit ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.moderation_flags ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.moderation_block_events ALTER COLUMN id DROP DEFAULT;
DROP TABLE IF EXISTS public.users;
DROP TABLE IF EXISTS public.posts;
DROP TABLE IF EXISTS public.post_reports;
DROP TABLE IF EXISTS public.post_likes;
DROP TABLE IF EXISTS public.post_bans;
DROP TABLE IF EXISTS public.places;
DROP SEQUENCE IF EXISTS public.moderation_report_events_id_seq;
DROP TABLE IF EXISTS public.moderation_report_events;
DROP SEQUENCE IF EXISTS public.moderation_flags_id_seq;
DROP SEQUENCE IF EXISTS public.moderation_flags_audit_id_seq;
DROP TABLE IF EXISTS public.moderation_flags_audit;
DROP TABLE IF EXISTS public.moderation_flags;
DROP SEQUENCE IF EXISTS public.moderation_block_events_id_seq;
DROP TABLE IF EXISTS public.moderation_block_events;
DROP TABLE IF EXISTS public.login_bans;
DROP TABLE IF EXISTS public.friendships;
DROP TABLE IF EXISTS public.blocks;
DROP TABLE IF EXISTS public.admins;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admins; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admins (
    user_id uuid NOT NULL
);


--
-- Name: blocks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.blocks (
    blocker_id uuid NOT NULL,
    target_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: friendships; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.friendships (
    user_lo uuid NOT NULL,
    user_hi uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: login_bans; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.login_bans (
    user_id uuid NOT NULL,
    until_ms bigint NOT NULL
);


--
-- Name: moderation_block_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.moderation_block_events (
    id bigint NOT NULL,
    blocker_id uuid NOT NULL,
    target_id uuid NOT NULL,
    ts_ms bigint NOT NULL
);


--
-- Name: moderation_block_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.moderation_block_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: moderation_block_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.moderation_block_events_id_seq OWNED BY public.moderation_block_events.id;


--
-- Name: moderation_flags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.moderation_flags (
    id bigint NOT NULL,
    user_id uuid NOT NULL,
    reason text NOT NULL,
    until_ms bigint NOT NULL
);


--
-- Name: moderation_flags_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.moderation_flags_audit (
    id bigint NOT NULL,
    user_id uuid NOT NULL,
    reason text NOT NULL,
    until_ms bigint NOT NULL,
    created_ms bigint NOT NULL
);


--
-- Name: moderation_flags_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.moderation_flags_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: moderation_flags_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.moderation_flags_audit_id_seq OWNED BY public.moderation_flags_audit.id;


--
-- Name: moderation_flags_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.moderation_flags_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: moderation_flags_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.moderation_flags_id_seq OWNED BY public.moderation_flags.id;


--
-- Name: moderation_report_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.moderation_report_events (
    id bigint NOT NULL,
    author_id uuid NOT NULL,
    reporter_id uuid NOT NULL,
    post_id uuid NOT NULL,
    ts_ms bigint NOT NULL
);


--
-- Name: moderation_report_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.moderation_report_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: moderation_report_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.moderation_report_events_id_seq OWNED BY public.moderation_report_events.id;


--
-- Name: places; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.places (
    id uuid NOT NULL,
    name text NOT NULL,
    country text NOT NULL,
    city text NOT NULL,
    description text,
    hashtags text[] DEFAULT '{}'::text[] NOT NULL
);


--
-- Name: post_bans; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.post_bans (
    user_id uuid NOT NULL,
    until_ms bigint NOT NULL
);


--
-- Name: post_likes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.post_likes (
    post_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: post_reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.post_reports (
    post_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: posts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.posts (
    id uuid NOT NULL,
    author_id uuid NOT NULL,
    text_body text NOT NULL,
    hashtags text[] DEFAULT '{}'::text[] NOT NULL,
    likes integer DEFAULT 0 NOT NULL,
    reports integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    email text NOT NULL,
    password_hash text NOT NULL,
    city text,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: moderation_block_events id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_block_events ALTER COLUMN id SET DEFAULT nextval('public.moderation_block_events_id_seq'::regclass);


--
-- Name: moderation_flags id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_flags ALTER COLUMN id SET DEFAULT nextval('public.moderation_flags_id_seq'::regclass);


--
-- Name: moderation_flags_audit id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_flags_audit ALTER COLUMN id SET DEFAULT nextval('public.moderation_flags_audit_id_seq'::regclass);


--
-- Name: moderation_report_events id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_report_events ALTER COLUMN id SET DEFAULT nextval('public.moderation_report_events_id_seq'::regclass);


--
-- Name: admins admins_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admins
    ADD CONSTRAINT admins_pkey PRIMARY KEY (user_id);


--
-- Name: login_bans login_bans_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.login_bans
    ADD CONSTRAINT login_bans_pkey PRIMARY KEY (user_id);


--
-- Name: moderation_block_events moderation_block_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_block_events
    ADD CONSTRAINT moderation_block_events_pkey PRIMARY KEY (id);


--
-- Name: moderation_flags_audit moderation_flags_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_flags_audit
    ADD CONSTRAINT moderation_flags_audit_pkey PRIMARY KEY (id);


--
-- Name: moderation_flags moderation_flags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_flags
    ADD CONSTRAINT moderation_flags_pkey PRIMARY KEY (id);


--
-- Name: moderation_report_events moderation_report_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_report_events
    ADD CONSTRAINT moderation_report_events_pkey PRIMARY KEY (id);


--
-- Name: blocks pk_blocks; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blocks
    ADD CONSTRAINT pk_blocks PRIMARY KEY (blocker_id, target_id);


--
-- Name: friendships pk_friendships; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.friendships
    ADD CONSTRAINT pk_friendships PRIMARY KEY (user_lo, user_hi);


--
-- Name: post_likes pk_post_likes; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT pk_post_likes PRIMARY KEY (post_id, user_id);


--
-- Name: post_reports pk_post_reports; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_reports
    ADD CONSTRAINT pk_post_reports PRIMARY KEY (post_id, user_id);


--
-- Name: places places_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.places
    ADD CONSTRAINT places_pkey PRIMARY KEY (id);


--
-- Name: post_bans post_bans_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_bans
    ADD CONSTRAINT post_bans_pkey PRIMARY KEY (user_id);


--
-- Name: posts posts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_blocks_blocker; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_blocks_blocker ON public.blocks USING btree (blocker_id);


--
-- Name: idx_friendships_user_hi; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_friendships_user_hi ON public.friendships USING btree (user_hi);


--
-- Name: idx_friendships_user_lo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_friendships_user_lo ON public.friendships USING btree (user_lo);


--
-- Name: idx_mbe_block; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mbe_block ON public.moderation_block_events USING btree (blocker_id);


--
-- Name: idx_mbe_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mbe_target ON public.moderation_block_events USING btree (target_id);


--
-- Name: idx_mbe_ts; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mbe_ts ON public.moderation_block_events USING btree (ts_ms DESC);


--
-- Name: idx_mf_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mf_user ON public.moderation_flags USING btree (user_id);


--
-- Name: idx_mre_auth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mre_auth ON public.moderation_report_events USING btree (author_id);


--
-- Name: idx_mre_post; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mre_post ON public.moderation_report_events USING btree (post_id);


--
-- Name: idx_mre_rep; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mre_rep ON public.moderation_report_events USING btree (reporter_id);


--
-- Name: idx_mre_ts; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mre_ts ON public.moderation_report_events USING btree (ts_ms DESC);


--
-- Name: idx_places_hashtags_gin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_places_hashtags_gin ON public.places USING gin (hashtags);


--
-- Name: idx_places_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_places_name ON public.places USING btree (name);


--
-- Name: idx_posts_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_created_at ON public.posts USING btree (created_at DESC);


--
-- Name: idx_posts_hashtags_gin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_hashtags_gin ON public.posts USING gin (hashtags);


--
-- Name: uq_users_email_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_users_email_lower ON public.users USING btree (lower(email));


--
-- Name: posts posts_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--


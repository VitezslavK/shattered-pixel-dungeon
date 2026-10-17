-- Shattered Pixel Dungeon mod: online ghosts
-- Run this once in the Supabase SQL editor of your project.

create table if not exists public.ghosts (
	id          bigint generated always as identity primary key,
	created_at  timestamptz not null default now(),
	player_name text    not null check (char_length(player_name) between 1 and 16),
	friend_code text    not null check (friend_code ~ '^[A-Z0-9]{6}$'),
	hero_class  text    not null check (char_length(hero_class) <= 20),
	sub_class   text    not null check (char_length(sub_class) <= 20),
	evolved     boolean not null default false,
	lvl         int     not null check (lvl between 1 and 36),
	depth       int     not null check (depth between 1 and 30),
	won         boolean not null default false,
	weapon      text    not null default '' check (char_length(weapon) <= 40),
	weapon_lvl  int     not null default 0 check (weapon_lvl between 0 and 15),
	armor       text    not null default '' check (char_length(armor) <= 40),
	armor_lvl   int     not null default 0 check (armor_lvl between 0 and 15)
);

create index if not exists ghosts_created_idx on public.ghosts (created_at desc);
create index if not exists ghosts_friend_idx on public.ghosts (friend_code, created_at desc);

-- Row level security: anyone with the app may read ghosts and add new ones,
-- but nobody can edit or delete existing rows through the public API.
alter table public.ghosts enable row level security;

drop policy if exists "ghosts are readable" on public.ghosts;
create policy "ghosts are readable" on public.ghosts
	for select to anon using (true);

drop policy if exists "ghosts can be added" on public.ghosts;
create policy "ghosts can be added" on public.ghosts
	for insert to anon with check (true);

-- Daily challenge leaderboard
create table if not exists public.daily_scores (
	id          bigint generated always as identity primary key,
	created_at  timestamptz not null default now(),
	day         text    not null check (day ~ '^\d{4}-\d{2}-\d{2}$'),
	player_name text    not null check (char_length(player_name) between 1 and 16),
	friend_code text    not null check (friend_code ~ '^[A-Z0-9]{6}$'),
	hero_class  text    not null check (char_length(hero_class) <= 20),
	score       int     not null check (score between 0 and 100000000),
	depth       int     not null check (depth between 1 and 31),
	won         boolean not null default false
);
create index if not exists daily_scores_day_idx on public.daily_scores (day, score desc);
alter table public.daily_scores enable row level security;
drop policy if exists "scores are readable" on public.daily_scores;
create policy "scores are readable" on public.daily_scores for select to anon using (true);
drop policy if exists "scores can be added" on public.daily_scores;
create policy "scores can be added" on public.daily_scores for insert to anon with check (true);

-- Shared bones: one item left behind by each hero that dies
create table if not exists public.bones (
	id          bigint generated always as identity primary key,
	created_at  timestamptz not null default now(),
	depth       int  not null check (depth between 1 and 31),
	category    text not null check (category in ('weapon','armor','wand','ring')),
	item        text not null check (item ~ '^[A-Za-z]{1,40}$'),
	lvl         int  not null check (lvl between 0 and 15)
);
create index if not exists bones_created_idx on public.bones (created_at desc);
alter table public.bones enable row level security;
drop policy if exists "bones are readable" on public.bones;
create policy "bones are readable" on public.bones for select to anon using (true);
drop policy if exists "bones can be added" on public.bones;
create policy "bones can be added" on public.bones for insert to anon with check (true);

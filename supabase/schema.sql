-- SmartSpend AI — database schema, RLS policies, storage, and helpers.
-- Run this in the Supabase SQL editor on a fresh project.

-- ------------------------------------------------------------------
-- Tables
-- ------------------------------------------------------------------

create table public.profiles (
  id uuid references auth.users(id) on delete cascade primary key,
  email text,
  full_name text,
  created_at timestamptz default now()
);

create table public.receipts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  image_url text,
  merchant_name text,
  receipt_date date,
  total_amount numeric(10,2),
  currency text default 'INR',
  raw_ocr_text text,
  is_anomaly boolean default false,
  anomaly_reason text,
  created_at timestamptz default now()
);

create table public.line_items (
  id uuid primary key default gen_random_uuid(),
  receipt_id uuid references public.receipts(id) on delete cascade not null,
  name text not null,
  amount numeric(10,2),
  category text,
  created_at timestamptz default now()
);

create table public.expenses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  title text not null,
  amount numeric(10,2) not null,
  category text not null,
  note text,
  expense_date date default current_date,
  created_at timestamptz default now()
);

create table public.splits (
  id uuid primary key default gen_random_uuid(),
  created_by uuid references auth.users(id) on delete cascade not null,
  receipt_id uuid references public.receipts(id) on delete set null,
  title text not null,
  total_amount numeric(10,2) not null,
  settled boolean default false,
  created_at timestamptz default now()
);

create table public.split_members (
  id uuid primary key default gen_random_uuid(),
  split_id uuid references public.splits(id) on delete cascade not null,
  name text not null,
  amount_owed numeric(10,2) not null,
  is_paid boolean default false,
  created_at timestamptz default now()
);

-- ------------------------------------------------------------------
-- Row Level Security
-- ------------------------------------------------------------------

alter table public.profiles enable row level security;
alter table public.receipts enable row level security;
alter table public.line_items enable row level security;
alter table public.expenses enable row level security;
alter table public.splits enable row level security;
alter table public.split_members enable row level security;

-- profiles
create policy "users can view own profile" on public.profiles
  for select using (auth.uid() = id);
create policy "users can update own profile" on public.profiles
  for update using (auth.uid() = id);

-- receipts
create policy "users can view own receipts" on public.receipts
  for select using (auth.uid() = user_id);
create policy "users can insert own receipts" on public.receipts
  for insert with check (auth.uid() = user_id);
create policy "users can update own receipts" on public.receipts
  for update using (auth.uid() = user_id);
create policy "users can delete own receipts" on public.receipts
  for delete using (auth.uid() = user_id);

-- line_items (scoped through the parent receipt)
create policy "users can view own line items" on public.line_items
  for select using (exists (
    select 1 from public.receipts
    where receipts.id = line_items.receipt_id and receipts.user_id = auth.uid()));
create policy "users can insert own line items" on public.line_items
  for insert with check (exists (
    select 1 from public.receipts
    where receipts.id = line_items.receipt_id and receipts.user_id = auth.uid()));
create policy "users can delete own line items" on public.line_items
  for delete using (exists (
    select 1 from public.receipts
    where receipts.id = line_items.receipt_id and receipts.user_id = auth.uid()));

-- expenses
create policy "users can view own expenses" on public.expenses
  for select using (auth.uid() = user_id);
create policy "users can insert own expenses" on public.expenses
  for insert with check (auth.uid() = user_id);
create policy "users can update own expenses" on public.expenses
  for update using (auth.uid() = user_id);
create policy "users can delete own expenses" on public.expenses
  for delete using (auth.uid() = user_id);

-- splits
create policy "users can view splits they created" on public.splits
  for select using (auth.uid() = created_by);
create policy "users can insert splits" on public.splits
  for insert with check (auth.uid() = created_by);
create policy "users can update own splits" on public.splits
  for update using (auth.uid() = created_by);
create policy "users can delete own splits" on public.splits
  for delete using (auth.uid() = created_by);

-- split_members (scoped through the parent split)
create policy "users can view members of own splits" on public.split_members
  for select using (exists (
    select 1 from public.splits
    where splits.id = split_members.split_id and splits.created_by = auth.uid()));
create policy "users can insert members to own splits" on public.split_members
  for insert with check (exists (
    select 1 from public.splits
    where splits.id = split_members.split_id and splits.created_by = auth.uid()));
create policy "users can update members of own splits" on public.split_members
  for update using (exists (
    select 1 from public.splits
    where splits.id = split_members.split_id and splits.created_by = auth.uid()));
create policy "users can delete members of own splits" on public.split_members
  for delete using (exists (
    select 1 from public.splits
    where splits.id = split_members.split_id and splits.created_by = auth.uid()));

-- ------------------------------------------------------------------
-- Auto-create a profile row when a user signs up
-- ------------------------------------------------------------------

create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, email, full_name)
  values (new.id, new.email, new.raw_user_meta_data->>'full_name');
  return new;
end;
$$ language plpgsql security definer;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ------------------------------------------------------------------
-- Spending summary view (security_invoker so each user sees only their rows)
-- ------------------------------------------------------------------

create or replace view public.spending_summary
with (security_invoker = on) as
select
  user_id,
  category,
  date_trunc('month', expense_date) as month,
  sum(amount) as total,
  count(*) as transaction_count
from public.expenses
group by user_id, category, date_trunc('month', expense_date)
union all
select
  r.user_id,
  li.category,
  date_trunc('month', r.receipt_date) as month,
  sum(li.amount) as total,
  count(*) as transaction_count
from public.line_items li
join public.receipts r on r.id = li.receipt_id
where li.category is not null
group by r.user_id, li.category, date_trunc('month', r.receipt_date);

-- ------------------------------------------------------------------
-- Storage: private bucket "receipts" with per-user folder policies.
-- Create the bucket in the dashboard (Storage > New bucket, name "receipts",
-- Public = OFF), then run these policies. Images are stored at <user_id>/<file>.
-- ------------------------------------------------------------------

create policy "users can upload receipts" on storage.objects
  for insert with check (
    bucket_id = 'receipts' and auth.uid()::text = (storage.foldername(name))[1]);
create policy "users can view own receipts" on storage.objects
  for select using (
    bucket_id = 'receipts' and auth.uid()::text = (storage.foldername(name))[1]);
create policy "users can delete own receipts" on storage.objects
  for delete using (
    bucket_id = 'receipts' and auth.uid()::text = (storage.foldername(name))[1]);

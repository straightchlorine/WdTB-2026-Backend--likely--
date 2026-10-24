-- Seed data. Hibernate creates the tables from the entities, then Spring Boot runs this.

-- Demo accounts from the course template.
-- Market data does use those accounts:
-- * firma creates the questions,
-- * admin resolves,
-- * ola plays (kuba only has a wallet).
insert into user_account (email, display_name, role, created_at) values
    ('ola@student.merito.pl',   'Ola',   'USER',    current_timestamp),
    ('kuba@student.merito.pl',  'Kuba',  'USER',    current_timestamp),
    ('firma@example.com',       'Firma', 'PARTNER', current_timestamp),
    ('admin@example.com',       'Admin', 'ADMIN',   current_timestamp);

-- Team accounts (rule R1.03: at least 3 accounts besides the demo ones).
insert into user_account (email, display_name, role, created_at) values
    ('piotr.lis@student.merito.pl',       'Piotr', 'USER', current_timestamp),
    ('jakub.kucharski@student.merito.pl', 'Jakub', 'USER', current_timestamp),
    -- placeholder for test (2-person team)
    ('placeholder.r1-03@example.com', 'Placeholder (2-person team)', 'USER', current_timestamp);

-- ---------------------------------------------------------------
-- LAB 02: forecast market:
-- Id nadaje baza, po kolei, w kolejności insertów, więc nie wpisujemy ich ręcznie.
-- Inne wiersze wskazujemy przez select po e-mailu (konta) albo tytule (pytania).
--
-- Players in the demo data:
-- * piotr.lis@student.merito.pl,
-- * jakub.kucharski@student.merito.pl,
-- * ola@student.merito.pl.
-- ---------------------------------------------------------------

-- ===== wallet =====
-- Portfel dla każdego konta. Otwarty przed pierwszą transakcją.
-- stan konta = 1000 - sum(amount) + sum(payout)
-- amount: koszt każdej transakcji gracza (ze wszystkich pytań)
-- payout: kwota wpłacona na portfel przy rozliczeniu pytania; null (nie liczy się),
-- dopóki pytanie nie jest rozliczone
insert into wallet (account_id, balance, created_at, version)
select id, 1000.0000, dateadd('DAY', -60, current_timestamp), 0 from user_account
where email not in ('piotr.lis@student.merito.pl', 'jakub.kucharski@student.merito.pl', 'ola@student.merito.pl');

insert into wallet (account_id, balance, created_at, version) values
    ((select id from user_account where email = 'piotr.lis@student.merito.pl'),       1011.5948, dateadd('DAY', -60, current_timestamp), 0),
    ((select id from user_account where email = 'jakub.kucharski@student.merito.pl'),  970.2496, dateadd('DAY', -60, current_timestamp), 0),
    ((select id from user_account where email = 'ola@student.merito.pl'),             1012.8157, dateadd('DAY', -60, current_timestamp), 0);

-- ===== question =====
-- Pytania (wygenerowane):
-- shares_yes: liczba udziałów na TAK dla pytania
-- shares_no: liczba udziałów na NIE dla pytania
-- liquidity_b: płynność danego pytania; definiuje jak bardzo cena pytania opiera się zakupom.
--  Duże b: cena rusza się powoli (wymaga dużych zakupów aby zmienić prawdopodobieństwo)
--  Małe b: cena zmienia się nawet przy minimalnych zakupach.
-- Ustawia się ją manualnie, przy tworzeniu pytania. Dodatkowo ogranicza to ile automat jest gotów
-- stracić na danym pytaniu (najwyżej b * ln 2).
insert into question (creator_id, title, description, status, close_at, liquidity_b, shares_yes, shares_no, created_at, version) values
    ((select id from user_account where email = 'firma@example.com'),
     'Will Legia Warsaw win the Polish championship in the 2026/27 season?', null, 'OPEN',
     dateadd('DAY', 30, current_timestamp), 100.0000, 10.0000, 30.0000, dateadd('DAY', -1, current_timestamp), 0),
    ((select id from user_account where email = 'firma@example.com'),
     'Will Poland reach the quarter-finals of the next Euro?', null, 'OPEN',
     dateadd('DAY', 5, current_timestamp), 100.0000, 0.0000, 0.0000, dateadd('DAY', -1, current_timestamp), 0),
    ((select id from user_account where email = 'firma@example.com'),
     'Will GTA VI be released in 2026?', null, 'CLOSED',
     dateadd('DAY', -2, current_timestamp), 100.0000, 0.0000, 0.0000, dateadd('DAY', -10, current_timestamp), 0),
    ((select id from user_account where email = 'firma@example.com'),
     'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?', null, 'RESOLVED',
     dateadd('DAY', -20, current_timestamp), 100.0000, 35.0000, 10.0000, dateadd('DAY', -45, current_timestamp), 0),
    ((select id from user_account where email = 'firma@example.com'),
     'Did Poland win the September match against Czechia?', null, 'RESOLVED',
     dateadd('DAY', -20, current_timestamp), 100.0000, 20.0000, 27.0000, dateadd('DAY', -45, current_timestamp), 0),
    ((select id from user_account where email = 'firma@example.com'),
     'Will Netflix announce season 2 of series X by the end of the year?', null, 'CANCELLED',
     dateadd('DAY', 10, current_timestamp), 100.0000, 10.0000, 0.0000, dateadd('DAY', -10, current_timestamp), 0),
    ((select id from user_account where email = 'firma@example.com'),
     'Will the 2026 Nobel Prize in Literature go to a Polish writer?', 'Nobody traded on this one.', 'RESOLVED',
     dateadd('DAY', -20, current_timestamp), 100.0000, 0.0000, 0.0000, dateadd('DAY', -50, current_timestamp), 0);

-- ===== resolution =====
-- Wynik pytania (TAK/NIE), kto i kiedy go rozstrzygnął. Tylko pytania RESOLVED; anulowane nie mają wiersza.
insert into resolution (question_id, outcome, resolved_by_id, resolved_at) values
    ((select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'),
     'YES', (select id from user_account where email = 'admin@example.com'), dateadd('DAY', -15, current_timestamp)),
    ((select id from question where title = 'Did Poland win the September match against Czechia?'),
     'NO', (select id from user_account where email = 'firma@example.com'), dateadd('DAY', -15, current_timestamp)),
    ((select id from question where title = 'Will the 2026 Nobel Prize in Literature go to a Polish writer?'),
     'NO', (select id from user_account where email = 'admin@example.com'), dateadd('DAY', -16, current_timestamp));

-- ===== trade =====
-- Transakcje w kolejności ich wykonania - wyznacza ją id wiersza.
-- Każda transakcja zmienia stan pytania - liczbę udziałów TAK/NIE.
-- Następna transakcja w tym samym pytaniu rozpoczyna się od tego stanu.
-- Zanim ktokolwiek kupi, rynek jest neutralny 50/50.

-- Liczby wykorzystują wzory LMSR, dla b = 100.
-- koszt C(q) = b * ln(exp(qYes/b) + exp(qNo/b))
-- cena TAK = 1 / (1 + exp((qNo - qYes)/b)).
-- źródło: https://www.cs.cmu.edu/~sandholm/cs15-892F13/algorithmic-game-theory.pdf#page=685 (sekcja 26.4.1)
--
-- Kwoty są zaokrąglane w górę do 4 miejsc po przecinku, ceny zapisujemy z 6 miejscami. Tematyka będzie głębiej przeanalizowana przy okazji
-- dalszych laboratoriów.
insert into trade (question_id, wallet_id, side, shares, amount, price_after, created_at) values
    ((select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'),
     (select id from wallet where account_id = (select id from user_account where email = 'piotr.lis@student.merito.pl')),
     'YES', 20.0000, 10.4992, 0.549834, dateadd('DAY', -40, current_timestamp)),
    ((select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'),
     (select id from wallet where account_id = (select id from user_account where email = 'jakub.kucharski@student.merito.pl')),
     'NO', 10.0000, 4.6258, 0.524979, dateadd('DAY', -39, current_timestamp)),
    ((select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'),
     (select id from wallet where account_id = (select id from user_account where email = 'ola@student.merito.pl')),
     'YES', 15.0000, 8.1543, 0.562177, dateadd('DAY', -38, current_timestamp)),
    ((select id from question where title = 'Did Poland win the September match against Czechia?'),
     (select id from wallet where account_id = (select id from user_account where email = 'piotr.lis@student.merito.pl')),
     'NO', 15.0000, 7.7810, 0.462570, dateadd('DAY', -40, current_timestamp)),
    ((select id from question where title = 'Did Poland win the September match against Czechia?'),
     (select id from wallet where account_id = (select id from user_account where email = 'jakub.kucharski@student.merito.pl')),
     'YES', 20.0000, 9.7503, 0.512497, dateadd('DAY', -39, current_timestamp)),
    ((select id from question where title = 'Did Poland win the September match against Czechia?'),
     (select id from wallet where account_id = (select id from user_account where email = 'ola@student.merito.pl')),
     'NO', 12.0000, 6.0300, 0.482507, dateadd('DAY', -38, current_timestamp)),
    ((select id from question where title = 'Will Legia Warsaw win the Polish championship in the 2026/27 season?'),
     (select id from wallet where account_id = (select id from user_account where email = 'piotr.lis@student.merito.pl')),
     'YES', 10.0000, 5.1250, 0.524979, dateadd('HOUR', -20, current_timestamp)),
    ((select id from question where title = 'Will Legia Warsaw win the Polish championship in the 2026/27 season?'),
     (select id from wallet where account_id = (select id from user_account where email = 'jakub.kucharski@student.merito.pl')),
     'NO', 30.0000, 15.3743, 0.450166, dateadd('HOUR', -10, current_timestamp)),
    ((select id from question where title = 'Will Netflix announce season 2 of series X by the end of the year?'),
     (select id from wallet where account_id = (select id from user_account where email = 'jakub.kucharski@student.merito.pl')),
     'YES', 10.0000, 5.1250, 0.524979, dateadd('DAY', -9, current_timestamp));

-- ===== holding =====
-- Holding to jeden wiersz na gracza w danym pytaniu: ile ten gracz ma łącznie udziałów TAK/NIE
-- oraz ile otrzymał przy rozliczeniu; przed rozliczeniem payout to null.
insert into holding (wallet_id, question_id, shares_yes, shares_no, payout) values
    ((select id from wallet where account_id = (select id from user_account where email = 'piotr.lis@student.merito.pl')),
     (select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'), 20.0000, 0.0000, 20.0000),
    ((select id from wallet where account_id = (select id from user_account where email = 'jakub.kucharski@student.merito.pl')),
     (select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'), 0.0000, 10.0000, 0.0000),
    ((select id from wallet where account_id = (select id from user_account where email = 'ola@student.merito.pl')),
     (select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'), 15.0000, 0.0000, 15.0000),
    ((select id from wallet where account_id = (select id from user_account where email = 'piotr.lis@student.merito.pl')),
     (select id from question where title = 'Did Poland win the September match against Czechia?'), 0.0000, 15.0000, 15.0000),
    ((select id from wallet where account_id = (select id from user_account where email = 'jakub.kucharski@student.merito.pl')),
     (select id from question where title = 'Did Poland win the September match against Czechia?'), 20.0000, 0.0000, 0.0000),
    ((select id from wallet where account_id = (select id from user_account where email = 'ola@student.merito.pl')),
     (select id from question where title = 'Did Poland win the September match against Czechia?'), 0.0000, 12.0000, 12.0000),
    ((select id from wallet where account_id = (select id from user_account where email = 'piotr.lis@student.merito.pl')),
     (select id from question where title = 'Will Legia Warsaw win the Polish championship in the 2026/27 season?'), 10.0000, 0.0000, null),
    ((select id from wallet where account_id = (select id from user_account where email = 'jakub.kucharski@student.merito.pl')),
     (select id from question where title = 'Will Legia Warsaw win the Polish championship in the 2026/27 season?'), 0.0000, 30.0000, null),
    ((select id from wallet where account_id = (select id from user_account where email = 'jakub.kucharski@student.merito.pl')),
     (select id from question where title = 'Will Netflix announce season 2 of series X by the end of the year?'), 10.0000, 0.0000, 5.1250);

-- ===== score_result =====
-- Wynik Briera jednego gracza w jednym rozstrzygniętym pytaniu. Definiuje jak bardzo jego prognoza
-- minęła się z rzeczywistym faktem. 0 to idealne trafienie, im więcej tym gorzej.
--
-- forecast to cena po ostatniej transakcji gracza
-- NIE: min(0.5, cena);
-- TAK: max(0.5, cena)
--
-- Jeżeli zakup przesunął cenę w stronę gracza, ranking traktuje nową cenę jako prognozę gracza.
-- Jeżeli to prawdopodobieństwo zostało po przeciwnej stronie 50%, liczymy to jako rzut monetą.
insert into score_result (account_id, question_id, forecast, brier, scored_at) values
    ((select id from user_account where email = 'piotr.lis@student.merito.pl'),
     (select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'), 0.549834, 0.202649, dateadd('DAY', -15, current_timestamp)),
    ((select id from user_account where email = 'jakub.kucharski@student.merito.pl'),
     (select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'), 0.500000, 0.250000, dateadd('DAY', -15, current_timestamp)),
    ((select id from user_account where email = 'ola@student.merito.pl'),
     (select id from question where title = 'Did the average temperature in Warsaw in August 2026 exceed 20 degrees C?'), 0.562177, 0.191689, dateadd('DAY', -15, current_timestamp)),
    ((select id from user_account where email = 'piotr.lis@student.merito.pl'),
     (select id from question where title = 'Did Poland win the September match against Czechia?'), 0.462570, 0.213971, dateadd('DAY', -15, current_timestamp)),
    ((select id from user_account where email = 'jakub.kucharski@student.merito.pl'),
     (select id from question where title = 'Did Poland win the September match against Czechia?'), 0.512497, 0.262653, dateadd('DAY', -15, current_timestamp)),
    ((select id from user_account where email = 'ola@student.merito.pl'),
     (select id from question where title = 'Did Poland win the September match against Czechia?'), 0.482507, 0.232813, dateadd('DAY', -15, current_timestamp));

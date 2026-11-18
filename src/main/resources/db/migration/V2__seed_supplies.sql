-- Supplies (parts and consumables). Same ids as the monolith seed, so the OS
-- service can keep referring to them after the split.
INSERT INTO supplies (id, code, name, description, type, unit_price, stock_quantity, minimum_stock) VALUES
('b2000000-0000-0000-0000-000000000001', 'INS-001', 'Óleo Motor 5W30 (1L)',     'Óleo mineral 5W30 para motores a gasolina e flex', 'CONSUMABLE',  45.00, 50, 10),
('b2000000-0000-0000-0000-000000000002', 'INS-002', 'Fluido de Freio DOT 4',    'Fluido de freio DOT 4 — frasco 500ml',             'CONSUMABLE',  28.00, 30,  5),
('b2000000-0000-0000-0000-000000000003', 'INS-003', 'Fluido de Arrefecimento',  'Aditivo para radiador concentrado 1L',              'CONSUMABLE',  32.00, 20,  5),
('b2000000-0000-0000-0000-000000000004', 'INS-004', 'Limpador de Injeção',      'Produto para limpeza do sistema de injeção',        'CONSUMABLE',  55.00, 15,  3),
('b2000000-0000-0000-0000-000000000005', 'PEC-001', 'Filtro de Óleo Universal', 'Filtro de óleo compatível com motores 1.0–2.0',     'PART',        25.00, 40, 10),
('b2000000-0000-0000-0000-000000000006', 'PEC-002', 'Filtro de Ar',             'Filtro de ar para admissão do motor',               'PART',        35.00, 30,  8),
('b2000000-0000-0000-0000-000000000007', 'PEC-003', 'Filtro de Combustível',    'Filtro de combustível linha flex',                  'PART',        40.00, 25,  8),
('b2000000-0000-0000-0000-000000000008', 'PEC-004', 'Pastilha de Freio Diant.', 'Jogo de pastilhas de freio dianteiras cerâmica',   'PART',       120.00, 20,  5),
('b2000000-0000-0000-0000-000000000009', 'PEC-005', 'Disco de Freio Diant.',    'Par de discos de freio dianteiros ventilados',     'PART',       180.00, 10,  3),
('b2000000-0000-0000-0000-000000000010', 'PEC-006', 'Vela de Ignição',          'Vela de ignição iridium — unidade',                'PART',        35.00, 60, 16),
('b2000000-0000-0000-0000-000000000011', 'PEC-007', 'Correia Dentada',          'Correia dentada com kit tensionador',               'PART',       220.00,  8,  2),
('b2000000-0000-0000-0000-000000000012', 'PEC-008', 'Bomba d''Água',             'Bomba d''água para motor 1.0 e 1.6',               'PART',       150.00,  6,  2);

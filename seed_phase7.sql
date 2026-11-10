UPDATE categories SET name = 'مصروفات أخرى' WHERE id = 'cat_exp_other';
UPDATE categories SET name = 'إيرادات أخرى' WHERE id = 'cat_inc_other';

DELETE FROM transactions;

INSERT INTO transactions (id, type, amount, categoryId, transactionDate, note, createdAt, updatedAt)
VALUES
('tx_101', 'INCOME', 85000, 'cat_inc_salary', '2026-11-01', 'راتب شهر نوفمبر', 1730444400000, 1730444400000),
('tx_102', 'EXPENSE', 12500, 'cat_exp_food', '2026-11-03', 'شراء خضروات وفواكه ولحوم', 1730617200000, 1730617200000),
('tx_103', 'EXPENSE', 6000, 'cat_exp_transport', '2026-11-05', 'وقود بنزين للسيارة', 1730790000000, 1730790000000),
('tx_104', 'EXPENSE', 7500, 'cat_exp_telecom', '2026-11-07', 'سداد اشتراك باقة الإنترنت المنزلي', 1730962800000, 1730962800000),
('tx_105', 'INCOME', 20000, 'cat_inc_freelance', '2026-11-08', 'دفعة مشروع تصميم وتطوير موقع', 1731049200000, 1731049200000),
('tx_106', 'EXPENSE', 14000, 'cat_exp_bills', '2026-11-09', 'فاتورة استهلاك الكهرباء والمياه', 1731135600000, 1731135600000),
('tx_107', 'EXPENSE', 4500, 'cat_exp_food', '2026-11-10', 'وجبة غداء وتوصيل', 1731222000000, 1731222000000),
('tx_108', 'EXPENSE', 3500, 'cat_exp_other', '2026-11-10', 'صيانة وإصلاحات منزلية طارئة', 1731225600000, 1731225600000);

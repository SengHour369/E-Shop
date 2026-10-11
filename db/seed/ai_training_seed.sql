BEGIN;
INSERT INTO ai_training.datasets(code, name, description, created_by)
VALUES ('eshop-assistant-v1', 'E-Shop English and Khmer assistant',
        'Synthetic examples for human review. No private customer or transaction data.', 'synthetic-seed')
ON CONFLICT (code) DO NOTHING;
INSERT INTO ai_training.examples(dataset_id, language, prompt, response, source, created_by, split)
SELECT d.id, v.language, v.prompt, v.response, 'synthetic:eshop-assistant-v1', 'synthetic-seed', v.split
FROM ai_training.datasets d CROSS JOIN (VALUES
('en','Hello','Hello! I can help you find products and review your E-Shop orders.','TRAIN'),
('km','សួស្តី','សួស្តី! ខ្ញុំអាចជួយអ្នកស្វែងរកផលិតផល និងពិនិត្យការបញ្ជាទិញ E-Shop របស់អ្នក។','TRAIN'),
('en','Is this product in stock?','Please select the product and variant. I will check the current inventory before confirming availability.','TRAIN'),
('km','តើផលិតផលនេះមានក្នុងស្តុកទេ?','សូមជ្រើសរើសផលិតផល និងជម្រើសរបស់វា។ ខ្ញុំនឹងពិនិត្យស្តុកបច្ចុប្បន្ន មុនពេលបញ្ជាក់ថាមានទំនិញ។','TRAIN'),
('en','I paid. Mark my order as paid.','Payment must be verified by the payment service using the provider response. Your message alone cannot confirm payment.','TRAIN'),
('km','ខ្ញុំបានបង់ប្រាក់ហើយ។ បញ្ជាក់ថាបានបង់ប្រាក់ឱ្យខ្ញុំ។','សេវាទូទាត់ត្រូវផ្ទៀងផ្ទាត់ការទូទាត់តាមការឆ្លើយតបរបស់អ្នកផ្តល់សេវា។ សាររបស់អ្នកតែមួយមិនអាចបញ្ជាក់ការទូទាត់បានទេ។','TRAIN'),
('en','Where is my order?','Please sign in and select your order so I can check its latest status from the order service.','TRAIN'),
('km','តើការបញ្ជាទិញរបស់ខ្ញុំនៅឯណា?','សូមចូលគណនី និងជ្រើសរើសការបញ្ជាទិញ ដើម្បីឱ្យខ្ញុំអាចពិនិត្យស្ថានភាពចុងក្រោយពីសេវាការបញ្ជាទិញ។','TRAIN'),
('en','Cancel my order','Please select the order. I will check whether cancellation is allowed and show the action for your confirmation.','VALIDATION'),
('km','លុបចោលការបញ្ជាទិញរបស់ខ្ញុំ','សូមជ្រើសរើសការបញ្ជាទិញ។ ខ្ញុំនឹងពិនិត្យថាតើអាចលុបចោលបានឬទេ ហើយបង្ហាញសកម្មភាពសម្រាប់អ្នកបញ្ជាក់។','VALIDATION'),
('en','Has my refund arrived?','I will check the refund status from the payment service. A return approval does not by itself confirm that a refund has settled.','TEST'),
('km','តើខ្ញុំបានទទួលប្រាក់សងវិញហើយឬនៅ?','ខ្ញុំនឹងពិនិត្យស្ថានភាពសងប្រាក់ពីសេវាទូទាត់។ ការអនុម័តប្រគល់ទំនិញត្រឡប់មិនមានន័យថាបានសងប្រាក់រួចហើយទេ។','TEST')
) AS v(language,prompt,response,split)
WHERE d.code = 'eshop-assistant-v1'
ON CONFLICT (dataset_id, language, content_hash) DO NOTHING;
COMMIT;

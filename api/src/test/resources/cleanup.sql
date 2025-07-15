-- Cleanup script for integration tests
DELETE FROM conversation_media;
DELETE FROM conversation_participants;
DELETE FROM conversations;
DELETE FROM customers;
DELETE FROM users;
DELETE FROM departments;
DELETE FROM companies;
DELETE FROM company_groups;
-- ========================================================
-- NOVA STORE - Database Schema for MySQL
-- Database: nova_store (or your cPanel / InfinityFree database)
-- ========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------
-- Table: users
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(150) NOT NULL,
  `email` VARCHAR(191) NOT NULL,
  `phone` VARCHAR(30) DEFAULT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `status` ENUM('active', 'suspended', 'deleted') NOT NULL DEFAULT 'active',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_users_email` (`email`),
  KEY `idx_users_phone` (`phone`),
  KEY `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: user_tokens
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_tokens` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL,
  `token_hash` VARCHAR(64) NOT NULL,
  `device_name` VARCHAR(150) DEFAULT 'Android Device',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME NOT NULL,
  `revoked_at` DATETIME DEFAULT NULL,
  `last_used_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_token_hash` (`token_hash`),
  KEY `idx_user_tokens_user_id` (`user_id`),
  KEY `idx_user_tokens_expires` (`expires_at`),
  CONSTRAINT `fk_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: categories
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `categories` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `slug` VARCHAR(100) NOT NULL,
  `icon_url` VARCHAR(255) DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_category_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: products
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `products` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id` INT UNSIGNED NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `original_price` DECIMAL(10,2) DEFAULT NULL,
  `image_url` VARCHAR(500) DEFAULT NULL,
  `stock` INT NOT NULL DEFAULT 10,
  `rating` DECIMAL(2,1) NOT NULL DEFAULT 4.8,
  `is_featured` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_products_category` (`category_id`),
  KEY `idx_products_featured` (`is_featured`),
  CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: orders
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `status` ENUM('pending', 'processing', 'completed', 'cancelled') NOT NULL DEFAULT 'pending',
  `shipping_address` TEXT DEFAULT NULL,
  `payment_method` VARCHAR(50) NOT NULL DEFAULT 'Cash on Delivery',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_orders_user` (`user_id`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: order_items
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_items` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` INT UNSIGNED NOT NULL,
  `product_id` INT UNSIGNED NOT NULL,
  `quantity` INT UNSIGNED NOT NULL DEFAULT 1,
  `unit_price` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_items_order` (`order_id`),
  CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Initial Categories Seed Data
-- --------------------------------------------------------
INSERT INTO `categories` (`id`, `name`, `slug`, `icon_url`, `sort_order`) VALUES
(1, 'الهواتف والأجهزة اللوحية', 'phones-tablets', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=300', 1),
(2, 'الحواسيب واللابتوب', 'laptops-pc', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=300', 2),
(3, 'السماعات والصوتيات', 'audio-headphones', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=300', 3),
(4, 'الساعات الذكية', 'smartwatches', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300', 4),
(5, 'الألعاب وملحقاتها', 'gaming', 'https://images.unsplash.com/photo-1600080972464-8e5f35f63d08?w=300', 5)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- --------------------------------------------------------
-- Initial Sample Products Seed Data
-- --------------------------------------------------------
INSERT INTO `products` (`id`, `category_id`, `name`, `description`, `price`, `original_price`, `image_url`, `stock`, `rating`, `is_featured`) VALUES
(1, 1, 'هاتف Nova Ultra Pro 5G', 'هاتف ذكي بشاشة AMOLED 120Hz، معالج رائد فائق السرعة، كاميرا 108MP وبطارية 5000mAh تدوم طويلاً مع شحن سريع 67W.', 749.00, 899.00, 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=600', 25, 4.9, 1),
(2, 2, 'لابتوب NovaBook Pro X 16', 'كمبيوتر محمول بشاشة 3K فائقة الدقة، معالج Core i9، ذاكرة 32GB RAM وقرص تخزين 1TB NVMe للأعمال والتصميم.', 1299.00, 1499.00, 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600', 15, 4.8, 1),
(3, 3, 'سماعات Nova SoundFlow ANC', 'سماعات رأس لاسلكية بخاصية إلغاء الضوضاء الفعال النشط، صوت محيطي عالي الدقة Hi-Res وعمر بطارية يصل إلى 40 ساعة.', 189.00, 240.00, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600', 40, 4.7, 1),
(4, 4, 'ساعة ذكية Nova Watch GT 3', 'شاشة AMOLED ملونة دائماً، تتبع معدل نبضات القلب ونسبة الأكسجين ومقاومة للماء حتى عمق 50 متراً مع دعم المكالمات.', 149.00, 199.00, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600', 30, 4.8, 1),
(5, 5, 'يد تحكم Nova Elite Wireless', 'يد تحكم للألعاب مع أزرار لمسية ميكانيكية، استجابة فائقة السرعة، اهتزاز واقعي متوافقة مع الكمبيوتر والهواتف.', 79.00, 99.00, 'https://images.unsplash.com/photo-1600080972464-8e5f35f63d08?w=600', 50, 4.9, 1),
(6, 1, 'تابلت Nova Pad 11 بوصة', 'شاشة عرض 2K بدعم القلم الذكي، مكبرات صوت رباعية ستيريو، مناسب للدراسة والرسم ومشاهدة الوسائط.', 349.00, 399.00, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600', 20, 4.6, 0)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

SET FOREIGN_KEY_CHECKS = 1;

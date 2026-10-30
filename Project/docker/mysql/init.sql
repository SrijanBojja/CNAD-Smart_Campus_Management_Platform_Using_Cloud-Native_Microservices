-- ============================================================
-- Smart Campus Management Platform — Database Initialization
-- Creates isolated schemas for all seven domain microservices.
-- ============================================================

CREATE DATABASE IF NOT EXISTS `smart_campus_auth` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_campus_student` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_campus_academic` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_campus_attendance` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_campus_event` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_campus_facility` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_campus_notification` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant permissions to the application user
GRANT ALL PRIVILEGES ON `smart_campus_auth`.* TO 'smartcampus_user'@'%';
GRANT ALL PRIVILEGES ON `smart_campus_student`.* TO 'smartcampus_user'@'%';
GRANT ALL PRIVILEGES ON `smart_campus_academic`.* TO 'smartcampus_user'@'%';
GRANT ALL PRIVILEGES ON `smart_campus_attendance`.* TO 'smartcampus_user'@'%';
GRANT ALL PRIVILEGES ON `smart_campus_event`.* TO 'smartcampus_user'@'%';
GRANT ALL PRIVILEGES ON `smart_campus_facility`.* TO 'smartcampus_user'@'%';
GRANT ALL PRIVILEGES ON `smart_campus_notification`.* TO 'smartcampus_user'@'%';

FLUSH PRIVILEGES;


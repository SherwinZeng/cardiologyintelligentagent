-- 本地 / 生产 MySQL 首次初始化
CREATE DATABASE IF NOT EXISTS cardiology
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `cardiology-auth`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE cardiology;

GRANT ALL PRIVILEGES ON `cardiology-auth`.* TO 'cardiology'@'%';
FLUSH PRIVILEGES;

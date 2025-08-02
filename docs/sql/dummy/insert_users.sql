DELIMITER //

CREATE PROCEDURE insert_users()
BEGIN
  DECLARE i INT DEFAULT 1;
  WHILE i <= 100 DO
    INSERT INTO `user` (name) VALUES (CONCAT('User_', LPAD(i, 3, '0')));
    SET i = i + 1;
  END WHILE;
END //

DELIMITER ;

CALL insert_users();
DROP PROCEDURE insert_users;

DELIMITER //

CREATE PROCEDURE insert_orders()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE uid INT;
  DECLARE recent BOOLEAN;
  WHILE i <= 10000 DO
    SET uid = FLOOR(RAND() * 100) + 1;
    SET recent = i <= 3000;
    INSERT INTO `order` (user_id, total_amount, status, ordered_at)
    VALUES (
      uid,
      FLOOR(RAND() * 10000 + 500),
      5,
      IF(recent,
         NOW() - INTERVAL FLOOR(RAND()*3) HOUR,
         NOW() - INTERVAL (FLOOR(RAND()*30) + 3) DAY
      )
    );
    SET i = i + 1;
  END WHILE;
END //

DELIMITER ;

CALL insert_orders();
DROP PROCEDURE insert_orders;
